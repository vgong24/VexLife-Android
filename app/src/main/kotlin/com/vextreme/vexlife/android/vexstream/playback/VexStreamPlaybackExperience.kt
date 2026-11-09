package com.vextreme.vexlife.android.vexstream.playback

import com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerConnectionViewModel
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * UI-facing membrane for the process-shared VexStream Playback Family.
 *
 * Compose observes one process-current Playback Experience Relay formed from exact Player Store +
 * admitted Playback Family current. Live progress is layered separately from the configuration-
 * lifetime Player Partner Progress connection; neither becomes another semantic truth owner.
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
    private val connection:
        VexStreamPlaybackConnection?,
) :
    VexStreamPlaybackExperience {
    override fun activate(
        control:
            VexStreamPlaybackControl,
    ) {
        connection?.activate(
            control,
        )
    }
}

@Composable
internal fun rememberVexStreamPlaybackExperience():
    VexStreamPlaybackExperience {
    val owner:
        VexStreamPlayerConnectionViewModel =
        viewModel()

    val connection =
        owner.connection

    val relationshipCurrent by
        owner
            .playbackExperienceRelay
            .outputStates
            .collectAsStateWithLifecycle(
                initialValue =
                    owner
                        .playbackExperienceRelay
                        .currentOutput,
            )
    val playback by
        rememberVexStreamPlaybackReadState(
            baseReadState =
                relationshipCurrent
                    .readState,
            progressConnection =
                owner.progressConnection,
        )

    val displayState =
        relationshipCurrent
            .playbackProjection
            .toVexStreamPlaybackDisplayState(
                playback =
                    playback,
            )

    return remember(
        connection,
        displayState,
    ) {
        CurrentVexStreamPlaybackExperience(
            displayState =
                displayState,
            connection =
                connection,
        )
    }
}

// [VXG RealForever][VEXSTREAM][PLAYBACK][PROCESS-FAMILY]
