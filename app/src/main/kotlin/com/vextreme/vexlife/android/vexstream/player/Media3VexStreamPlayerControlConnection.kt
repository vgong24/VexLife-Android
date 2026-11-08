package com.vextreme.vexlife.android.vexstream.player

import android.content.ComponentName
import android.content.Context
import android.os.Handler

import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.vextreme.vexlife.android.media.vexstream.VexStreamPlaybackService
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
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.withContext

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
    VexStreamPlayerPartner.Connection.Outbound {
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


/**
 * One bounded Media3 controller occurrence for a short-lived caller such as a Widget action.
 *
 * Media3 connection, timeout, provider-looper handoff and release remain inside the concrete Player
 * Partner realization boundary.
 */
internal suspend fun <
    Result,
> withVexStreamPlayerOutboundConnection(
    context:
        Context,
    block:
        suspend (
            VexStreamPlayerPartner.Connection.Outbound,
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
