package com.vextreme.vexlife.android.vexstream.player

import android.content.Context
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackControl
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackFamily
import com.vextreme.vexlife.android.vexstream.playback.activateVexStreamPlayback
import com.vextreme.vexlife.android.vexstream.queue.VexStreamQueueControl
import com.vextreme.vexlife.android.vexstream.queue.VexStreamQueueFamily
import com.vextreme.vexlife.android.vexstream.queue.activateVexStreamQueue

/**
 * One finite VexStream request from a caller that owns no long-lived MediaController.
 *
 * Callers today: the home-screen widget and the app-shell Now strip. A Vex request is the intended
 * next caller. They all converge on the same process Playback / Queue Families as the Music screen;
 * this type names the request, never a second meaning.
 */
internal sealed interface VexStreamFiniteRequest {
    data class Playback(
        val control:
            VexStreamPlaybackControl,
    ) : VexStreamFiniteRequest

    data class Queue(
        val control:
            VexStreamQueueControl,
    ) : VexStreamFiniteRequest
}

/**
 * The exact process-lifetime owners a finite outside request needs.
 *
 * A narrower composition root (widget callback, ViewModel) resolves these from the process
 * dependency graph and passes them in explicitly; this file never reaches into VexApplication.
 */
internal class VexStreamFiniteRequestOwners(
    val playerStore:
        VexStreamPlayerStore,
    val playbackFamily:
        VexStreamPlaybackFamily,
    val queueFamily:
        VexStreamQueueFamily,
    val executionRoot:
        VexExecutionRoot,
)

/**
 * Borrow a controller for exactly one request, wait until the target Family has reconciled current
 * Player Store truth, then activate it through the existing activation function and join the
 * realization Job.
 *
 * Returns whether a realization was launched and completed. It is NOT evidence that the provider
 * changed: later Player Store observation remains the only outcome truth.
 */
internal suspend fun requestVexStreamFiniteControl(
    context:
        Context,
    owners:
        VexStreamFiniteRequestOwners,
    request:
        VexStreamFiniteRequest,
):
    Boolean =
    withVexStreamPlayerRequestConnection(
        context,
    ) { connection ->
        awaitVexStreamPlayerSnapshot(
            owners.playerStore,
        )
            ?: return@withVexStreamPlayerRequestConnection false

        when (
            request
        ) {
            is VexStreamFiniteRequest.Playback -> {
                if (
                    !awaitVexStreamPlaybackFamilyCurrent(
                        family =
                            owners.playbackFamily,
                        store =
                            owners.playerStore,
                    )
                ) {
                    return@withVexStreamPlayerRequestConnection false
                }
                val job =
                    activateVexStreamPlayback(
                        family =
                            owners.playbackFamily,
                        control =
                            request.control,
                        connection =
                            connection,
                        playerStore =
                            owners.playerStore,
                        executionRoot =
                            owners.executionRoot,
                    )

                job?.join()
                job !=
                    null
            }

            is VexStreamFiniteRequest.Queue -> {
                if (
                    !awaitVexStreamQueueFamilyCurrent(
                        family =
                            owners.queueFamily,
                        store =
                            owners.playerStore,
                    )
                ) {
                    return@withVexStreamPlayerRequestConnection false
                }
                val job =
                    activateVexStreamQueue(
                        family =
                            owners.queueFamily,
                        control =
                            request.control,
                        connection =
                            connection,
                        executionRoot =
                            owners.executionRoot,
                    )

                job?.join()
                job !=
                    null
            }
        }
    } ==
        true

// [VXG RealForever][VEXSTREAM][PLAYER][FINITE-REQUEST]
