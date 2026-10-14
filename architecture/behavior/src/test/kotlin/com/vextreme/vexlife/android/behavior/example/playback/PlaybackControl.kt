package com.vextreme.vexlife.android.behavior.example.playback

import com.vextreme.vexlife.android.behavior.VexBehaviorBlueprint
import com.vextreme.vexlife.android.behavior.VexBehaviorControlBlueprint

/**
 * Complete finite set of Playback affordances in this practicum.
 *
 * Enum is intentional here: Controls are identity-only possibilities and every member must be
 * linked when the Control blueprint is constructed.
 */
enum class PlaybackControl {
    Rewind,
    PlayPause,
    FastForward,
}

/**
 * Bind the complete PlaybackControl vocabulary to this exact Playback Behavior instance.
 *
 * There are no detached val control objects. The finite Control family and its Act relationships
 * are authored together under one typed blueprint.
 */
fun VexBehaviorBlueprint<
    PlaybackState,
    PlaybackAct,
    PlaybackEffect
>.playbackControls():
    VexBehaviorControlBlueprint<
        PlaybackState,
        PlaybackAct,
        PlaybackEffect,
        PlaybackControl
    > =
    controls<PlaybackControl> {
        control(
            PlaybackControl.Rewind,
            PlaybackAct.Rewind,
        )

        /*
         * When both are available from current State, Play intentionally wins over Pause.
         */
        control(
            PlaybackControl.PlayPause,
            PlaybackAct.Play,
            PlaybackAct.Pause,
        )

        control(
            PlaybackControl.FastForward,
            PlaybackAct.FastForward,
        )
    }

// [VXG RealForever]
