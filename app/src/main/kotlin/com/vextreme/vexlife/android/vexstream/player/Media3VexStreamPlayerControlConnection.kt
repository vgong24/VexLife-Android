package com.vextreme.vexlife.android.vexstream.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Handler

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.vextreme.vexlife.android.media.vexstream.VexStreamPlaybackService
import com.vextreme.vexlife.android.vexstream.collection.ResolvedCollectionPlayback
import com.vextreme.vexlife.android.vexstream.collection.VexStreamCollectionPlaybackRealization
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackEffect
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackEffectConnection
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackSeekConnection
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackSeekRequest
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackProgress
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackProgressConnection
import com.vextreme.vexlife.android.vexstream.playback.vexStreamPlaybackSeekTarget
import com.vextreme.vexlife.android.vexstream.queue.VexStreamQueueEffect
import com.vextreme.vexlife.android.vexstream.queue.VexStreamQueueEffectConnection
import com.vextreme.vexlife.android.vexstream.queue.VexStreamQueuePlayConnection
import com.vextreme.vexlife.android.vexstream.queue.VexStreamQueuePlayRequest
import com.vextreme.vexlife.android.vexstream.queue.VexStreamQueueReorderConnection
import com.vextreme.vexlife.android.vexstream.queue.VexStreamQueueReorderRequest
import com.vextreme.vexlife.android.vexstream.queue.vexStreamQueuePlayTargetIndex
import com.vextreme.vexlife.android.vexstream.queue.vexStreamQueueReorderTargetIndex
import com.vextreme.vexlife.android.vexstream.queue.shuffleVexStreamFutureQueue
import com.vextreme.vexlife.android.vexstream.repeat.VexStreamRepeatEffect
import com.vextreme.vexlife.android.vexstream.repeat.VexStreamRepeatEffectConnection
import com.vextreme.vexlife.android.vexstream.track.ResolvedTrackPlay
import com.vextreme.vexlife.android.vexstream.track.VexStreamTrackPlaybackRealization
import com.vextreme.vexlife.android.vexstream.track.VexStreamTrackQueueRequest
import com.vextreme.vexlife.android.vexstream.track.VexStreamTrackQueueTarget
import com.vextreme.vexlife.android.vexstream.track.vexStreamTrackPlayCommandsAvailable
import com.vextreme.vexlife.android.vexstream.track.vexStreamTrackQueueTarget
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
internal class Media3VexStreamPlayerControlConnection(
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

    override val isTrackPlayAvailable:
        Boolean
        get() =
            playCommandsAvailable()

    override fun isTrackQueueAvailable(
        request:
            VexStreamTrackQueueRequest,
    ):
        Boolean =
        queueTarget(
            request,
        ) !=
            null

    override suspend fun realizeTrackPlay(
        resolved:
            ResolvedTrackPlay,
    ):
        Boolean =
        withContext(
            providerDispatcher,
        ) {
            if (
                !playCommandsAvailable()
            ) {
                return@withContext false
            }

            controller.setMediaItems(
                resolved.items.map {
                    it.toMedia3Item()
                },
                resolved.selectedIndex,
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

    override suspend fun realizeTrackQueue(
        request:
            VexStreamTrackQueueRequest,
        media:
            VexStreamPlayerPartner.Media,
    ):
        Boolean =
        withContext(
            providerDispatcher,
        ) {
            when (
                val target =
                    queueTarget(
                        request,
                    )
                        ?: return@withContext false
            ) {
                VexStreamTrackQueueTarget.StartPlayback -> {
                    controller.setMediaItem(
                        media.toMedia3Item(),
                    )
                    controller.prepare()
                    controller.play()
                    true
                }

                is VexStreamTrackQueueTarget.Insert -> {
                    controller.addMediaItem(
                        target.index,
                        media.toMedia3Item(),
                    )
                    true
                }

                VexStreamTrackQueueTarget.Append -> {
                    controller.addMediaItem(
                        media.toMedia3Item(),
                    )
                    true
                }
            }
        }

    override val isCollectionPlaybackAvailable:
        Boolean
        get() =
            playCommandsAvailable()

    override suspend fun realizeCollectionPlayback(
        resolved:
            ResolvedCollectionPlayback,
    ):
        Boolean =
        withContext(
            providerDispatcher,
        ) {
            if (
                resolved.items.isEmpty() ||
                !playCommandsAvailable()
            ) {
                return@withContext false
            }

            controller.setMediaItems(
                resolved.items.map {
                    it.toMedia3Item()
                },
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

    private fun playCommandsAvailable():
        Boolean =
        vexStreamTrackPlayCommandsAvailable(
            changeMediaItemsAvailable =
                controller.isCommandAvailable(
                    Player.COMMAND_CHANGE_MEDIA_ITEMS,
                ),
            prepareAvailable =
                controller.isCommandAvailable(
                    Player.COMMAND_PREPARE,
                ),
            playAvailable =
                controller.isCommandAvailable(
                    Player.COMMAND_PLAY_PAUSE,
                ),
            shuffleEnabled =
                controller.shuffleModeEnabled,
            setShuffleModeAvailable =
                controller.isCommandAvailable(
                    Player.COMMAND_SET_SHUFFLE_MODE,
                ),
        )

    private fun queueTarget(
        request:
            VexStreamTrackQueueRequest,
    ):
        VexStreamTrackQueueTarget? {
        val timelineAvailable =
            controller.isCommandAvailable(
                Player.COMMAND_GET_TIMELINE,
            )
        val needsTimeline =
            request.placement ==
                com.vextreme.vexlife.android.vexstream.track
                    .VexStreamTrackQueuePlacement.Next

        return vexStreamTrackQueueTarget(
            placement =
                request.placement,
            mediaItemCount =
                if (
                    needsTimeline &&
                    timelineAvailable
                ) {
                    controller.mediaItemCount
                } else {
                    null
                },
            currentMediaItemIndex =
                if (
                    needsTimeline &&
                    timelineAvailable
                ) {
                    controller.currentMediaItemIndex
                } else {
                    null
                },
            timelineAvailable =
                timelineAvailable,
            setMediaItemAvailable =
                controller.isCommandAvailable(
                    Player.COMMAND_SET_MEDIA_ITEM,
                ),
            changeMediaItemsAvailable =
                controller.isCommandAvailable(
                    Player.COMMAND_CHANGE_MEDIA_ITEMS,
                ),
            prepareAvailable =
                controller.isCommandAvailable(
                    Player.COMMAND_PREPARE,
                ),
            playAvailable =
                controller.isCommandAvailable(
                    Player.COMMAND_PLAY_PAUSE,
                ),
        )
    }

    override fun realize(
        effect:
            VexStreamPlaybackEffect,
    ) {
        when (
            effect
        ) {
            VexStreamPlaybackEffect.RequestPlay ->
                controller.play()

            VexStreamPlaybackEffect.RequestPause ->
                controller.pause()

            VexStreamPlaybackEffect.RestartCurrent -> {
                controller.seekToDefaultPosition()
                controller.play()
            }
        }
    }

    override fun realize(
        effect:
            VexStreamQueueEffect,
    ) {
        when (
            effect
        ) {
            VexStreamQueueEffect.SeekPrevious ->
                controller
                    .seekToPreviousMediaItem()

            VexStreamQueueEffect.SeekNext ->
                controller
                    .seekToNextMediaItem()

            VexStreamQueueEffect.ShuffleFuture ->
                controller
                    .shuffleVexStreamFutureQueue()

            VexStreamQueueEffect.ClearQueue ->
                controller
                    .clearMediaItems()
        }
    }

    override fun realize(
        effect:
            VexStreamRepeatEffect,
    ) {
        when (
            effect
        ) {
            VexStreamRepeatEffect.RequestEnableAll ->
                controller.repeatMode =
                    Player.REPEAT_MODE_ALL

            VexStreamRepeatEffect.RequestDisable ->
                controller.repeatMode =
                    Player.REPEAT_MODE_OFF
        }
    }

    override val isPlayAvailable:
        Boolean
        get() =
            controller.isCommandAvailable(
                Player.COMMAND_SEEK_TO_MEDIA_ITEM,
            ) &&
                controller.isCommandAvailable(
                    Player.COMMAND_PLAY_PAUSE,
                )

    override fun play(
        request:
            VexStreamQueuePlayRequest,
    ):
        Boolean {
        val currentMediaIds =
            (0 until controller.mediaItemCount)
                .map { index ->
                    controller
                        .getMediaItemAt(
                            index,
                        )
                        .mediaId
                }
        val targetIndex =
            vexStreamQueuePlayTargetIndex(
                request =
                    request,
                currentMediaIds =
                    currentMediaIds,
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

    override val isReorderAvailable:
        Boolean
        get() =
            controller.mediaItemCount >
                1 &&
                controller.isCommandAvailable(
                    Player.COMMAND_CHANGE_MEDIA_ITEMS,
                )

    override fun move(
        request:
            VexStreamQueueReorderRequest,
    ):
        Boolean {
        val currentMediaIds =
            (0 until controller.mediaItemCount)
                .map { index ->
                    controller
                        .getMediaItemAt(
                            index,
                        )
                        .mediaId
                }
        val targetIndex =
            vexStreamQueueReorderTargetIndex(
                request =
                    request,
                currentMediaIds =
                    currentMediaIds,
                changeAvailable =
                    controller.isCommandAvailable(
                        Player.COMMAND_CHANGE_MEDIA_ITEMS,
                    ),
            )
                ?: return false

        controller.moveMediaItem(
            request.occurrence.index,
            targetIndex,
        )
        return true
    }

    override val isPlaying:
        Boolean
        get() =
            controller.isPlaying

    override fun readProgress():
        VexStreamPlaybackProgress {
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

        return VexStreamPlaybackProgress(
            positionMs =
                positionMs,
            durationMs =
                durationMs,
            bufferedPositionMs =
                bufferedMs,
        )
    }

    override val isAvailable:
        Boolean
        get() =
            controller.currentMediaItem !=
                null &&
                controller.isCommandAvailable(
                    Player
                        .COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,
                )

    override fun seek(
        request:
            VexStreamPlaybackSeekRequest,
    ):
        Boolean {
        val target =
            vexStreamPlaybackSeekTarget(
                request =
                    request,
                currentMediaId =
                    controller
                        .currentMediaItem
                        ?.mediaId,
                currentMediaItemIndex =
                    controller
                        .currentMediaItemIndex,
                seekAvailable =
                    controller
                        .isCommandAvailable(
                            Player
                                .COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,
                        ),
            )
                ?: return false

        controller.seekTo(
            target,
        )
        return true
    }
}

// [VXG RealForever][VEXSTREAM][PLAYER-PARTNER][MEDIA3-CONTROL]


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
        val providerDispatcher =
            Handler(
                controller.applicationLooper,
            ).asCoroutineDispatcher(
                "VexStream Player Partner Media3",
            )

        withContext(
            providerDispatcher,
        ) {
            block(
                Media3VexStreamPlayerControlConnection(
                    controller,
                ),
            )
        }
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
