package com.vextreme.vexlife.android.vexstream.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.vextreme.vexlife.android.media.vexstream.VexStreamPlaybackService
import com.vextreme.vexlife.android.vexstream.queue.shuffleVexStreamFutureQueue
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Media3 controller realization of the request/progress sides of the same VexStream Player Partner.
 *
 * Feature semantics arrive here only after their rightful owner has resolved a Control/Act/Effect.
 * Media3 types do not leave this concrete Connection.
 */
internal class Media3ControllerConnection(
    private val controller:
        MediaController,
) :
    VexStreamPlayerPartner.Connection.Request,
    VexStreamPlayerPartner.Connection.Progress {
    override val id =
        VexStreamPlayerPartner.Connection.Id
            .next(
                "media3-controller",
            )

    private val providerDispatcher =
        Handler(
            controller.applicationLooper,
        ).asCoroutineDispatcher(
            "VexStream Player Partner Media3 persistent",
        )

    override val mediaPlaybackAvailable:
        Boolean
        get() =
            playCommandsAvailable()

    override suspend fun playMedia(
        items:
            List<
                VexStreamPlayerPartner.Media
            >,
        selectedIndex:
            Int,
    ):
        Boolean =
        withContext(
            providerDispatcher,
        ) {
            if (
                items.isEmpty() ||
                selectedIndex !in
                items.indices ||
                !playCommandsAvailable()
            ) {
                return@withContext false
            }

            controller.setMediaItems(
                items.map {
                    it.toMedia3Item()
                },
                selectedIndex,
                0L,
            )

            if (
                controller.shuffleModeEnabled
            ) {
                controller.shuffleModeEnabled =
                    false
            }

            controller.prepare()
            controller.play()
            true
        }

    override fun mediaQueueAvailable(
        placement:
            VexStreamPlayerPartner.QueuePlacement,
    ):
        Boolean =
        queueTarget(
            placement,
        ) !=
            null

    override suspend fun queueMedia(
        media:
            VexStreamPlayerPartner.Media,
        placement:
            VexStreamPlayerPartner.QueuePlacement,
    ):
        Boolean =
        withContext(
            providerDispatcher,
        ) {
            when (
                val target =
                    queueTarget(
                        placement,
                    )
                        ?: return@withContext false
            ) {
                QueueTarget.StartPlayback -> {
                    controller.setMediaItem(
                        media.toMedia3Item(),
                    )
                    controller.prepare()
                    controller.play()
                    true
                }

                is QueueTarget.Insert -> {
                    controller.addMediaItem(
                        target.index,
                        media.toMedia3Item(),
                    )
                    true
                }

                QueueTarget.Append -> {
                    controller.addMediaItem(
                        media.toMedia3Item(),
                    )
                    true
                }
            }
        }

    private fun playCommandsAvailable():
        Boolean =
        controller.isCommandAvailable(
            Player.COMMAND_CHANGE_MEDIA_ITEMS,
        ) &&
            controller.isCommandAvailable(
                Player.COMMAND_PREPARE,
            ) &&
            controller.isCommandAvailable(
                Player.COMMAND_PLAY_PAUSE,
            ) &&
            (
                !controller.shuffleModeEnabled ||
                    controller.isCommandAvailable(
                        Player.COMMAND_SET_SHUFFLE_MODE,
                    )
            )

    private sealed interface QueueTarget {
        data object StartPlayback :
            QueueTarget

        data class Insert(
            val index:
                Int,
        ) :
            QueueTarget

        data object Append :
            QueueTarget
    }

    private fun queueTarget(
        placement:
            VexStreamPlayerPartner.QueuePlacement,
    ):
        QueueTarget? =
        when (
            placement
        ) {
            VexStreamPlayerPartner.QueuePlacement.End ->
                QueueTarget.Append
                    .takeIf {
                        controller.isCommandAvailable(
                            Player.COMMAND_CHANGE_MEDIA_ITEMS,
                        )
                    }

            VexStreamPlayerPartner.QueuePlacement.Next -> {
                if (
                    !controller.isCommandAvailable(
                        Player.COMMAND_GET_TIMELINE,
                    )
                ) {
                    null
                } else if (
                    controller.mediaItemCount ==
                    0
                ) {
                    QueueTarget.StartPlayback
                        .takeIf {
                            controller.isCommandAvailable(
                                Player.COMMAND_SET_MEDIA_ITEM,
                            ) &&
                                controller.isCommandAvailable(
                                    Player.COMMAND_PREPARE,
                                ) &&
                                controller.isCommandAvailable(
                                    Player.COMMAND_PLAY_PAUSE,
                                )
                        }
                } else {
                    QueueTarget.Insert(
                        index =
                            (
                                controller.currentMediaItemIndex +
                                    1
                            ).coerceIn(
                                0,
                                controller.mediaItemCount,
                            ),
                    )
                        .takeIf {
                            controller.isCommandAvailable(
                                Player.COMMAND_CHANGE_MEDIA_ITEMS,
                            )
                        }
                }
            }
        }

    override suspend fun play() {
        withContext(
            providerDispatcher,
        ) {
            controller.play()
        }
    }

    override suspend fun pause() {
        withContext(
            providerDispatcher,
        ) {
            controller.pause()
        }
    }

    override suspend fun restartCurrent() {
        withContext(
            providerDispatcher,
        ) {
            controller.seekToDefaultPosition()
            controller.play()
        }
    }

    override suspend fun previous() {
        withContext(
            providerDispatcher,
        ) {
            controller.seekToPreviousMediaItem()
        }
    }

    override suspend fun next() {
        withContext(
            providerDispatcher,
        ) {
            controller.seekToNextMediaItem()
        }
    }

    override suspend fun shuffleFuture() {
        withContext(
            providerDispatcher,
        ) {
            controller.shuffleVexStreamFutureQueue()
        }
    }

    override suspend fun clearQueue() {
        withContext(
            providerDispatcher,
        ) {
            controller.clearMediaItems()
        }
    }

    override suspend fun setRepeatEnabled(
        enabled:
            Boolean,
    ) {
        withContext(
            providerDispatcher,
        ) {
            controller.repeatMode =
                if (
                    enabled
                ) {
                    Player.REPEAT_MODE_ALL
                } else {
                    Player.REPEAT_MODE_OFF
                }
        }
    }

    override fun playQueueOccurrence(
        occurrence:
            VexStreamPlayerPartner.QueueOccurrence,
    ):
        Boolean {
        checkProviderLooper()

        val targetIndex =
            queuePlayTargetIndex(
                occurrence =
                    occurrence,
                currentMediaIds =
                    currentMediaIds(),
                seekAvailable =
                    controller.isCommandAvailable(
                        Player.COMMAND_SEEK_TO_MEDIA_ITEM,
                    ),
                playAvailable =
                    controller.isCommandAvailable(
                        Player.COMMAND_PLAY_PAUSE,
                    ),
            )
                ?: return false

        if (
            targetIndex !=
            controller.currentMediaItemIndex
        ) {
            controller.seekToDefaultPosition(
                targetIndex,
            )
        }
        controller.play()
        return true
    }

    override fun moveQueueOccurrence(
        occurrence:
            VexStreamPlayerPartner.QueueOccurrence,
        move:
            VexStreamPlayerPartner.QueueMove,
    ):
        Boolean {
        checkProviderLooper()

        val targetIndex =
            queueMoveTargetIndex(
                occurrence =
                    occurrence,
                move =
                    move,
                currentMediaIds =
                    currentMediaIds(),
                changeAvailable =
                    controller.isCommandAvailable(
                        Player.COMMAND_CHANGE_MEDIA_ITEMS,
                    ),
            )
                ?: return false

        controller.moveMediaItem(
            occurrence.index,
            targetIndex,
        )
        return true
    }

    override val isPlaying:
        Boolean
        get() =
            controller.isPlaying

    override fun readProgress():
        VexStreamPlayerPartner.Connection.Progress.Snapshot {
        val durationMs =
            controller.duration
                .takeIf {
                    it >
                    0L
                } ?: 0L
        val positionMs =
            controller
                .currentPosition
                .coerceAtLeast(
                    0L,
                )
                .let { position ->
                    if (
                        durationMs >
                        0L
                    ) {
                        position.coerceAtMost(
                            durationMs,
                        )
                    } else {
                        position
                    }
                }
        val bufferedMs =
            controller
                .bufferedPosition
                .coerceAtLeast(
                    0L,
                )
                .let { buffered ->
                    if (
                        durationMs >
                        0L
                    ) {
                        buffered.coerceAtMost(
                            durationMs,
                        )
                    } else {
                        buffered
                    }
                }

        return VexStreamPlayerPartner.Connection.Progress.Snapshot(
            positionMs =
                positionMs,
            durationMs =
                durationMs,
            bufferedPositionMs =
                bufferedMs,
        )
    }

    override fun seek(
        mediaId:
            String,
        mediaItemIndex:
            Int,
        positionMs:
            Long,
    ):
        Boolean {
        checkProviderLooper()

        val target =
            seekTarget(
                mediaId =
                    mediaId,
                mediaItemIndex =
                    mediaItemIndex,
                positionMs =
                    positionMs,
                currentMediaId =
                    controller
                        .currentMediaItem
                        ?.mediaId,
                currentMediaItemIndex =
                    controller.currentMediaItemIndex,
                seekAvailable =
                    controller.isCommandAvailable(
                        Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,
                    ),
            )
                ?: return false

        controller.seekTo(
            target,
        )
        return true
    }

    private fun checkProviderLooper() {
        check(
            Looper.myLooper() ==
                controller.applicationLooper,
        ) {
            "Media3 Controller request must run on its application looper"
        }
    }

    private fun currentMediaIds():
        List<
            String
        > =
        (0 until controller.mediaItemCount)
            .map { index ->
                controller
                    .getMediaItemAt(
                        index,
                    )
                    .mediaId
            }

}

// [VXG RealForever][VEXSTREAM][PLAYER-PARTNER][MEDIA3-CONTROLLER]


/**
 * One bounded Media3 controller occurrence for a short-lived caller such as a Widget action.
 *
 * Media3 connection, timeout, provider-looper handoff and release remain inside the concrete Player
 * Partner realization boundary.
 */
internal suspend fun <
    Result,
> withVexStreamPlayerRequestConnection(
    context:
        Context,
    block:
        suspend (
            VexStreamPlayerPartner.Connection.Request,
        ) -> Result,
):
    Result? {
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
                controllerFuture
                    .get(
                        PLAYER_CONTROLLER_CONNECT_TIMEOUT_MS,
                        TimeUnit.MILLISECONDS,
                    )
            }
        block(
            Media3ControllerConnection(
                controller,
            ),
        )
    } catch (
        cancelled:
            CancellationException,
    ) {
        throw cancelled
    } catch (
        failure:
            Exception,
    ) {
        null
    } finally {
        MediaController.releaseFuture(
            controllerFuture,
        )
    }
}

private const val PLAYER_CONTROLLER_CONNECT_TIMEOUT_MS =
    2_000L


internal fun vexStreamPlayerQueueOccurrences(
    mediaIds:
        List<
            String
        >,
):
    List<
        VexStreamPlayerPartner.QueueOccurrence
    > {
    val seen =
        mutableMapOf<
            String,
            Int
        >()

    return mediaIds.mapIndexed { index, mediaId ->
        require(
            mediaId.isNotBlank(),
        )

        val occurrence =
            (seen[mediaId] ?: 0) +
                1
        seen[mediaId] =
            occurrence

        VexStreamPlayerPartner.QueueOccurrence(
            mediaId =
                mediaId,
            occurrence =
                occurrence,
            index =
                index,
        )
    }
}

internal fun vexStreamPlayerQueueOccurrenceAt(
    mediaIds:
        List<
            String
        >,
    index:
        Int,
):
    VexStreamPlayerPartner.QueueOccurrence? =
    if (
        index !in
        mediaIds.indices
    ) {
        null
    } else {
        vexStreamPlayerQueueOccurrences(
            mediaIds,
        )[
            index
        ]
    }

internal fun queuePlayTargetIndex(
    occurrence:
        VexStreamPlayerPartner.QueueOccurrence,
    currentMediaIds:
        List<
            String
        >,
    seekAvailable:
        Boolean,
    playAvailable:
        Boolean,
):
    Int? {
    if (
        !seekAvailable ||
        !playAvailable
    ) {
        return null
    }

    val current =
        vexStreamPlayerQueueOccurrenceAt(
            mediaIds =
                currentMediaIds,
            index =
                occurrence.index,
        )
            ?: return null

    return occurrence.index
        .takeIf {
            current ==
                occurrence
        }
}

internal fun queueMoveTargetIndex(
    occurrence:
        VexStreamPlayerPartner.QueueOccurrence,
    move:
        VexStreamPlayerPartner.QueueMove,
    currentMediaIds:
        List<
            String
        >,
    changeAvailable:
        Boolean,
):
    Int? {
    if (
        !changeAvailable
    ) {
        return null
    }

    val source =
        vexStreamPlayerQueueOccurrenceAt(
            mediaIds =
                currentMediaIds,
            index =
                occurrence.index,
        )
            ?: return null

    if (
        source !=
        occurrence
    ) {
        return null
    }

    val targetIndex =
        when (
            move
        ) {
            VexStreamPlayerPartner.QueueMove.Earlier ->
                source.index -
                    1

            VexStreamPlayerPartner.QueueMove.Later ->
                source.index +
                    1
        }

    if (
        targetIndex !in
        currentMediaIds.indices
    ) {
        return null
    }

    if (
        currentMediaIds[
            targetIndex
        ] ==
        source.mediaId
    ) {
        return null
    }

    return targetIndex
}

internal fun seekTarget(
    mediaId:
        String,
    mediaItemIndex:
        Int,
    positionMs:
        Long,
    currentMediaId:
        String?,
    currentMediaItemIndex:
        Int,
    seekAvailable:
        Boolean,
):
    Long? =
    positionMs
        .takeIf {
            mediaId.isNotBlank() &&
                mediaItemIndex >=
                    0 &&
                positionMs >=
                    0L &&
                seekAvailable &&
                currentMediaId ==
                    mediaId &&
                currentMediaItemIndex ==
                    mediaItemIndex
        }

private fun VexStreamPlayerPartner.Media.toMedia3Item():
    MediaItem {
    val clipping =
        MediaItem.ClippingConfiguration.Builder()
            .setStartPositionMs(
                playbackStartMs,
            )
            .apply {
                playbackEndMs
                    ?.let(
                        ::setEndPositionMs,
                    )
            }
            .build()

    return MediaItem.Builder()
        .setMediaId(
            mediaId,
        )
        .setUri(
            Uri.parse(
                sourceRef,
            ),
        )
        .setMimeType(
            mimeType,
        )
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(
                    title,
                )
                .setArtist(
                    artist,
                )
                .apply {
                    album
                        ?.let(
                            ::setAlbumTitle,
                        )
                    artworkRef
                        ?.let { ref ->
                            setArtworkUri(
                                Uri.parse(
                                    ref,
                                ),
                            )
                        }
                }
                .build(),
        )
        .setClippingConfiguration(
            clipping,
        )
        .build()
}
