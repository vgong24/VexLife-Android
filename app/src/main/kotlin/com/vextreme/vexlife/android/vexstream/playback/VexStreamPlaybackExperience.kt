package com.vextreme.vexlife.android.vexstream.playback

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import com.vextreme.vexlife.android.vexstream.VexStreamMusicViewModel
import com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerPartner
import com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerStore

/** UI-facing membrane for the process-shared VexStream Playback Family. */
internal interface VexStreamPlaybackExperience {
    val displayState:
        VexStreamPlaybackDisplayState

    fun activate(
        control:
            VexStreamPlaybackControl,
    )
}

private class CurrentVexStreamPlaybackExperience(
    override val displayState:
        VexStreamPlaybackDisplayState,
    private val family:
        VexStreamPlaybackFamily,
    private val connection:
        VexStreamPlayerPartner.Connection.Request?,
    private val playerStore:
        VexStreamPlayerStore,
    private val executionRoot:
        VexExecutionRoot,
) :
    VexStreamPlaybackExperience {
    override fun activate(
        control:
            VexStreamPlaybackControl,
    ) {
        val requestConnection =
            connection
                ?: return

        activateVexStreamPlayback(
            family =
                family,
            control =
                control,
            connection =
                requestConnection,
            playerStore =
                playerStore,
            executionRoot =
                executionRoot,
        )
    }
}

@Composable
internal fun rememberVexStreamPlaybackExperience():
    VexStreamPlaybackExperience {
    val owner:
        VexStreamMusicViewModel =
        viewModel()
    val family =
        owner.playbackFamily
    val connection =
        owner.playerRequestConnection

    val playerSnapshot by
        owner
            .playerStore
            .states
            .collectAsStateWithLifecycle(
                initialValue =
                    owner.playerStore.current,
            )
    val telemetry by
        rememberVexStreamPlaybackTelemetry(
            owner.controller,
        )
    val playback =
        remember(
            playerSnapshot,
            telemetry,
        ) {
            playerSnapshot
                .toVexStreamPlaybackReadState(
                    telemetry,
                )
        }

    val semanticState by
        family
            .states
            .collectAsStateWithLifecycle(
                initialValue =
                    family.currentState,
            )

    val projection =
        remember(
            semanticState,
            connection,
        ) {
            if (
                connection ==
                null
            ) {
                null
            } else {
                family.projectionFor(
                    semanticState,
                )
            }
        }

    val displayState =
        projection
            .toVexStreamPlaybackDisplayState(
                playback =
                    playback,
            )

    return remember(
        displayState,
        family,
        connection,
        owner.playerStore,
        owner.executionRoot,
    ) {
        CurrentVexStreamPlaybackExperience(
            displayState =
                displayState,
            family =
                family,
            connection =
                connection,
            playerStore =
                owner.playerStore,
            executionRoot =
                owner.executionRoot,
        )
    }
}

// [VXG RealForever][VEXSTREAM][PLAYBACK][DIRECT-FAMILY]
