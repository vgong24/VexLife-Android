package com.vextreme.vexlife.android.vexstream.player

import android.content.ComponentName
import android.content.Context
import android.os.Handler
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
 * Configuration/widget realization of the finite VexStream Player request relationship.
 *
 * This class does not reconstruct Playback / Queue / Repeat semantic State. It receives exact
 * already-authored Family Effects through the Partner request vocabulary and hands them to Media3
 * on the controller application looper.
 *
 * Track / Collection submission, Seek, Queue-row Play and Reorder intentionally retain their
 * existing payload/currentness connections outside this finite Partner request boundary.
 */
internal class Media3ControllerConnection(
    private val controller:
        MediaController,
) :
    VexStreamPlayerPartner.Connection.Request {
    override val id =
        VexStreamPlayerPartner.Connection.Id
            .next(
                "media3-controller",
            )

    private val providerDispatcher =
        Handler(
            controller.applicationLooper,
        ).asCoroutineDispatcher(
            "VexStream Media3 controller",
        )

    override suspend fun play() {
        withContext(
            providerDispatcher,
        ) {
            if (
                controller.isCommandAvailable(
                    Player.COMMAND_PLAY_PAUSE,
                )
            ) {
                controller.play()
            }
        }
    }

    override suspend fun pause() {
        withContext(
            providerDispatcher,
        ) {
            if (
                controller.isCommandAvailable(
                    Player.COMMAND_PLAY_PAUSE,
                )
            ) {
                controller.pause()
            }
        }
    }

    override suspend fun restartCurrent() {
        withContext(
            providerDispatcher,
        ) {
            if (
                controller.isCommandAvailable(
                    Player.COMMAND_SEEK_TO_DEFAULT_POSITION,
                ) &&
                controller.isCommandAvailable(
                    Player.COMMAND_PLAY_PAUSE,
                )
            ) {
                controller.seekToDefaultPosition()
                controller.play()
            }
        }
    }

    override suspend fun previous() {
        withContext(
            providerDispatcher,
        ) {
            if (
                controller.isCommandAvailable(
                    Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
                )
            ) {
                controller.seekToPreviousMediaItem()
            }
        }
    }

    override suspend fun next() {
        withContext(
            providerDispatcher,
        ) {
            if (
                controller.isCommandAvailable(
                    Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                )
            ) {
                controller.seekToNextMediaItem()
            }
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
            if (
                controller.isCommandAvailable(
                    Player.COMMAND_CHANGE_MEDIA_ITEMS,
                )
            ) {
                controller.clearMediaItems()
            }
        }
    }

    override suspend fun setRepeatEnabled(
        enabled:
            Boolean,
    ) {
        withContext(
            providerDispatcher,
        ) {
            if (
                controller.isCommandAvailable(
                    Player.COMMAND_SET_REPEAT_MODE,
                )
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
    }
}

/**
 * One bounded controller occurrence for a platform callback such as a Widget action.
 *
 * Connection, timeout, provider-looper handoff and release remain inside the concrete Partner
 * realization boundary. The caller receives only the finite request vocabulary.
 */
internal suspend fun <Result> withVexStreamPlayerRequestConnection(
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

// [VXG RealForever][VEXSTREAM][PLAYER-PARTNER][MEDIA3-CONTROLLER]
