package com.vextreme.vexlife.android.vexstream.player

import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackEffect
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackEffectConnection
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackSeekConnection
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackSeekRequest
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackProgress
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackProgressConnection
import com.vextreme.vexlife.android.vexstream.playback.vexStreamPlaybackSeekTarget
import com.vextreme.vexlife.android.vexstream.queue.VexStreamQueueEffect
import com.vextreme.vexlife.android.vexstream.queue.VexStreamQueueEffectConnection
import com.vextreme.vexlife.android.vexstream.queue.shuffleVexStreamFutureQueue
import com.vextreme.vexlife.android.vexstream.repeat.VexStreamRepeatEffect
import com.vextreme.vexlife.android.vexstream.repeat.VexStreamRepeatEffectConnection

/**
 * Media3 controller realization at the outbound side of the same VexStream Player Partner.
 *
 * Feature semantics arrive here only after their rightful owner has resolved a Control/Act/Effect.
 * Media3 types do not leave this concrete Connection.
 */
internal class Media3VexStreamPlayerControlConnection(
    private val controller:
        MediaController,
) :
    VexStreamPlayerPartner.Connection,
    VexStreamPlaybackEffectConnection,
    VexStreamQueueEffectConnection,
    VexStreamRepeatEffectConnection,
    VexStreamPlaybackSeekConnection,
    VexStreamPlaybackProgressConnection {
    override val id =
        VexStreamPlayerPartner.Connection.Id
            .next(
                "media3-controller",
            )

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
