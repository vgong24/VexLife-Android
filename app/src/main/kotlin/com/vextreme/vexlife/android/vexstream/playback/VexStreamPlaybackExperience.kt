package com.vextreme.vexlife.android.vexstream.playback

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * UI-facing membrane for the process-shared VexStream Playback Family.
 *
 * Compose observes the same Family State that background Service/Relay consumers observe. The
 * configuration-lifetime MediaController connection only realizes Controls; it is not another
 * semantic truth owner.
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
        VexStreamPlaybackConnectionViewModel =
        viewModel()

    val connection =
        owner.connection

    val playback by
        rememberVexStreamPlaybackReadState(
            owner.controller,
        )

    val semanticState by
        owner
            .family
            .states
            .collectAsStateWithLifecycle()

    val projection =
        remember(
            semanticState,
        ) {
            owner
                .family
                .playbackProjectionFor(
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
