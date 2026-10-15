package com.vextreme.vexlife.android.vexstream.playback

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * UI-facing membrane for the VexStream Playback Behavior experience.
 *
 * Compose should depend on this boundary instead of assembling Connection + VexPortFlow +
 * Projection + DisplayState itself.
 *
 * This interface owns no semantic truth. It exposes the final derived display classification and
 * carries Controls back into the existing VexPort boundary.
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

/**
 * Observe the internal Playback Port and expose one feature membrane to Compose.
 */
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

    val projections =
        connection?.projections

    val projection:
        VexStreamPlaybackProjection? =
        if (
            projections == null
        ) {
            null
        } else {
            val current by
                projections
                    .collectAsStateWithLifecycle()
            current
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

// [VXG RealForever]
