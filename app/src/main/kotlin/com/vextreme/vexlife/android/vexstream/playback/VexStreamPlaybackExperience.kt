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
 * Compose observes process-shared Family semantics plus retained Player Partner outside truth.
 * The configuration-lifetime Player Partner controller Connection only realizes Effects/progress;
 * it is not another semantic truth owner.
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

    val playback by
        rememberVexStreamPlaybackReadState()

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
