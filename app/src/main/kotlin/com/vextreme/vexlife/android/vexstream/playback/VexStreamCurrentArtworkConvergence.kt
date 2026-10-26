package com.vextreme.vexlife.android.vexstream.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Handler
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.vextreme.vexlife.android.data.vexstream.VexStreamArtworkStore
import com.vextreme.vexlife.android.data.vexstream.VexStreamMusicRepository
import com.vextreme.vexlife.android.media.vexstream.VexStreamPlaybackService
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Result of reconciling current repository artwork truth into the already-playing Media3 item.
 *
 * Artwork is presentation metadata only. None of these results change media identity or claim that
 * ancillary artwork failure is playback failure.
 */
internal enum class VexStreamCurrentArtworkConvergence {
    UPDATED,
    ALREADY_CURRENT,
    NO_CURRENT_ITEM,
    TRACK_NOT_FOUND,
    NO_ARTWORK_SOURCE,
    ARTWORK_UNAVAILABLE,
    PLAYER_CHANGED,
    COMMAND_UNAVAILABLE,
    CONTROLLER_UNAVAILABLE,
}

internal enum class VexStreamCurrentArtworkCommitPlan {
    UPDATE,
    ALREADY_CURRENT,
    PLAYER_CHANGED,
    COMMAND_UNAVAILABLE,
}

/**
 * Pure final-commit decision after artwork preparation may have crossed IO/network boundaries.
 *
 * Re-checking the current media identity prevents a late artwork fetch for track A from mutating
 * track B after the listener has already moved on.
 */
internal fun vexStreamCurrentArtworkCommitPlan(
    expectedMediaId: String,
    expectedIndex: Int,
    liveMediaId: String?,
    liveIndex: Int,
    liveArtworkUri: String?,
    desiredArtworkUri: String,
    changeMediaItemsAvailable: Boolean,
): VexStreamCurrentArtworkCommitPlan =
    when {
        liveMediaId !=
            expectedMediaId ||
            liveIndex !=
            expectedIndex ->
            VexStreamCurrentArtworkCommitPlan
                .PLAYER_CHANGED

        !changeMediaItemsAvailable ->
            VexStreamCurrentArtworkCommitPlan
                .COMMAND_UNAVAILABLE

        liveArtworkUri ==
            desiredArtworkUri ->
            VexStreamCurrentArtworkCommitPlan
                .ALREADY_CURRENT

        else ->
            VexStreamCurrentArtworkCommitPlan
                .UPDATE
    }

private data class VexStreamCurrentArtworkSnapshot(
    val mediaId: String,
    val index: Int,
)

/**
 * Converge artwork for the exact Media3 item that is current now.
 *
 * Repository/catalog + artwork-cache truth remains upstream. Media3 remains playback truth. This
 * function only composes those truths at the provider boundary so a track that was already playing
 * when artwork later became admitted/cached can receive refreshed metadata without restarting audio.
 */
internal suspend fun convergeVexStreamCurrentPlaybackArtwork(
    context: Context,
    repository: VexStreamMusicRepository,
): VexStreamCurrentArtworkConvergence {
    val appContext =
        context.applicationContext
    val controllerFuture =
        MediaController.Builder(
            appContext,
            SessionToken(
                appContext,
                ComponentName(
                    appContext,
                    VexStreamPlaybackService::class.java,
                ),
            ),
        ).buildAsync()

    return try {
        val controller =
            withContext(
                Dispatchers.IO,
            ) {
                controllerFuture.get(
                    CURRENT_ARTWORK_CONTROLLER_TIMEOUT_MS,
                    TimeUnit.MILLISECONDS,
                )
            }
        val providerDispatcher =
            Handler(
                controller.applicationLooper,
            ).asCoroutineDispatcher(
                "VexStream current artwork Media3",
            )

        val snapshot =
            withContext(
                providerDispatcher,
            ) {
                val current =
                    controller.currentMediaItem
                        ?: return@withContext null
                VexStreamCurrentArtworkSnapshot(
                    mediaId =
                        current.mediaId,
                    index =
                        controller.currentMediaItemIndex,
                )
            }
                ?: return VexStreamCurrentArtworkConvergence
                    .NO_CURRENT_ITEM

        val trackState =
            repository
                .state
                .value
                .tracks
                .firstOrNull {
                    it.track
                        .trackRef
                        .value ==
                        snapshot.mediaId
                }
                ?: return VexStreamCurrentArtworkConvergence
                    .TRACK_NOT_FOUND

        val track =
            trackState.track
        if (
            track.artworkUrl == null &&
            track.artworkRef == null
        ) {
            return VexStreamCurrentArtworkConvergence
                .NO_ARTWORK_SOURCE
        }

        repository.preparePlaybackArtwork(
            trackRef =
                track.trackRef,
            expectedMediaSha256 =
                track.mediaSha256,
        )

        val desiredArtworkUri =
            VexStreamArtworkStore
                .production(
                    appContext.filesDir,
                )
                .fileFor(
                    track,
                )
                ?.let(
                    Uri::fromFile,
                )
                ?: track.artworkUrl
                    ?.let(
                        Uri::parse,
                    )
                ?: return VexStreamCurrentArtworkConvergence
                    .ARTWORK_UNAVAILABLE

        val desiredArtworkUriText =
            desiredArtworkUri.toString()

        val result =
            withContext(
                providerDispatcher,
            ) {
                val liveItem =
                    controller.currentMediaItem
                val plan =
                    vexStreamCurrentArtworkCommitPlan(
                        expectedMediaId =
                            snapshot.mediaId,
                        expectedIndex =
                            snapshot.index,
                        liveMediaId =
                            liveItem?.mediaId,
                        liveIndex =
                            controller.currentMediaItemIndex,
                        liveArtworkUri =
                            liveItem
                                ?.mediaMetadata
                                ?.artworkUri
                                ?.toString(),
                        desiredArtworkUri =
                            desiredArtworkUriText,
                        changeMediaItemsAvailable =
                            controller.isCommandAvailable(
                                Player.COMMAND_CHANGE_MEDIA_ITEMS,
                            ),
                    )

                if (
                    plan ==
                    VexStreamCurrentArtworkCommitPlan
                        .UPDATE
                ) {
                    val current =
                        requireNotNull(
                            liveItem,
                        )
                    controller.replaceMediaItem(
                        snapshot.index,
                        current
                            .withVexStreamArtworkUri(
                                desiredArtworkUri,
                            ),
                    )
                }

                plan
            }

        when (
            result
        ) {
            VexStreamCurrentArtworkCommitPlan.UPDATE ->
                VexStreamCurrentArtworkConvergence.UPDATED

            VexStreamCurrentArtworkCommitPlan.ALREADY_CURRENT ->
                VexStreamCurrentArtworkConvergence.ALREADY_CURRENT

            VexStreamCurrentArtworkCommitPlan.PLAYER_CHANGED ->
                VexStreamCurrentArtworkConvergence.PLAYER_CHANGED

            VexStreamCurrentArtworkCommitPlan.COMMAND_UNAVAILABLE ->
                VexStreamCurrentArtworkConvergence.COMMAND_UNAVAILABLE
        }
            .also { convergence ->
                VexStreamPlaybackTrace.record(
                    stage =
                        "artwork.current.convergence",
                    mediaId =
                        snapshot.mediaId,
                    detail =
                        "result=$convergence",
                )
            }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        VexStreamCurrentArtworkConvergence
            .CONTROLLER_UNAVAILABLE
    } finally {
        MediaController.releaseFuture(
            controllerFuture,
        )
    }
}

private fun MediaItem.withVexStreamArtworkUri(
    artworkUri: Uri,
): MediaItem =
    buildUpon()
        .setMediaMetadata(
            mediaMetadata
                .buildUpon()
                .setArtworkUri(
                    artworkUri,
                )
                .build(),
        )
        .build()

private const val CURRENT_ARTWORK_CONTROLLER_TIMEOUT_MS =
    2_000L

// [VXG RealForever]
