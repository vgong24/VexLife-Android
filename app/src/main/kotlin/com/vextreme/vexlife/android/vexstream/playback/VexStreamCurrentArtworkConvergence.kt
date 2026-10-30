package com.vextreme.vexlife.android.vexstream.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.vextreme.vexlife.android.data.vexstream.VexStreamArtworkStore
import com.vextreme.vexlife.android.data.vexstream.VexStreamMusicRepository
import com.vextreme.vexlife.android.media.vexstream.VexStreamPlaybackService
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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
 * Widget republishing is warranted only when Media3 now owns the desired artwork URI.
 *
 * UPDATED means this convergence call just committed it. ALREADY_CURRENT matters too because a
 * service/widget observation may have missed the earlier metadata event; a fresh projection can
 * safely re-read the same canonical Media3 truth without becoming another artwork owner.
 */
internal val VexStreamCurrentArtworkConvergence.shouldRefreshWidgetProjection: Boolean
    get() =
        this == VexStreamCurrentArtworkConvergence.UPDATED ||
            this == VexStreamCurrentArtworkConvergence.ALREADY_CURRENT

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
 * Playback-side trigger for current artwork convergence.
 *
 * Repository changes already trigger convergence through VexLifeSession. This companion stream
 * closes the other half of the composition: an existing Media3 queue can advance to an item whose
 * metadata was formed before artwork became available. Media3 remains the current-item truth; this
 * stream emits only that the current item changed and carries no artwork bytes or repository state.
 */
internal fun vexStreamCurrentPlaybackArtworkTriggers(
    context: Context,
): Flow<Unit> =
    callbackFlow {
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
            )
                .setApplicationLooper(
                    Looper.getMainLooper(),
                )
                .buildAsync()

        var controller: MediaController? =
            null
        var listener: Player.Listener? =
            null

        controllerFuture.addListener(
            {
                val connected =
                    runCatching {
                        controllerFuture.get()
                    }.getOrNull()
                        ?: return@addListener

                controller =
                    connected
                val playbackListener =
                    object : Player.Listener {
                        override fun onMediaItemTransition(
                            mediaItem: MediaItem?,
                            reason: Int,
                        ) {
                            VexStreamPlaybackTrace.record(
                                stage =
                                    "artwork.current.playback-trigger",
                                mediaId =
                                    mediaItem?.mediaId,
                                detail =
                                    "reason=$reason",
                            )
                            trySend(Unit)
                        }
                    }
                listener =
                    playbackListener
                connected.addListener(
                    playbackListener,
                )

                // Cover an already-current item when this observer first connects.
                trySend(Unit)
            },
            ContextCompat.getMainExecutor(
                appContext,
            ),
        )

        awaitClose {
            val connected =
                controller
            val playbackListener =
                listener

            Handler(
                Looper.getMainLooper(),
            ).post {
                if (
                    connected != null &&
                    playbackListener != null
                ) {
                    runCatching {
                        connected.removeListener(
                            playbackListener,
                        )
                    }
                }

                MediaController.releaseFuture(
                    controllerFuture,
                )
            }
        }
    }

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
        )
            .setApplicationLooper(
                Looper.getMainLooper(),
            )
            .buildAsync()

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
        Handler(
            Looper.getMainLooper(),
        ).post {
            MediaController.releaseFuture(
                controllerFuture,
            )
        }
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
