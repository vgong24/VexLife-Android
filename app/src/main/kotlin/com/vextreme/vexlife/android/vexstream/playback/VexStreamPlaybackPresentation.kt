package com.vextreme.vexlife.android.vexstream.playback

import com.vextreme.vexlife.android.elements.ElementIcon

internal data class VexStreamPlaybackControlPresentation(
    val icon:
        ElementIcon,
    val enabled:
        Boolean,
)

internal fun VexStreamPlaybackProjection.present(
    control:
        VexStreamPlaybackControl,
):
    VexStreamPlaybackControlPresentation {
    check(
        control ==
            VexStreamPlaybackControl.PlayPause,
    ) {
        "Unknown VexStream Playback Control: $control"
    }

    val icon =
        if (
            !displayState.playPauseAvailable
        ) {
            ElementIcon.PlayPause
        } else {
            when (
                val current =
                    state
            ) {
                VexStreamPlaybackState.NoMedia ->
                    ElementIcon.PlayPause

                is VexStreamPlaybackState.Media ->
                    if (
                        current.playRequested
                    ) {
                        ElementIcon.Pause
                    } else {
                        ElementIcon.Play
                    }

                is VexStreamPlaybackState.Ended ->
                    ElementIcon.Play
            }
        }

    return VexStreamPlaybackControlPresentation(
        icon =
            icon,
        enabled =
            displayState.playPauseAvailable,
    )
}

// [VXG RealForever][VEXSTREAM][PLAYBACK][DIRECT-FAMILY]
