package com.vextreme.vexlife.android.vexstream.playback

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import com.vextreme.vexlife.android.vexstream.VexStreamMusicViewModel
import com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerPartner

/**
 * Feature-local Compose binding for Playback Family meaning plus Player read data.
 *
 * This is presentation composition, not a reusable VexArchitecture Experience root.
 */
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

    val playback by
        rememberVexStreamPlaybackReadState()

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
        ) {
            family.playbackProjectionFor(
                semanticState,
            )
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
        owner.executionRoot,
    ) {
        CurrentVexStreamPlaybackExperience(
            displayState =
                displayState,
            family =
                family,
            connection =
                connection,
            executionRoot =
                owner.executionRoot,
        )
    }
}

// [VXG RealForever][VEXSTREAM][PLAYBACK][DIRECT-FAMILY]
