package com.vextreme.vexlife.android.behavior.example.playback

import com.vextreme.vexlife.android.behavior.VexBehaviorBlueprint
import com.vextreme.vexlife.android.behavior.VexBehaviorControlBlueprint
import com.vextreme.vexlife.android.behavior.VexBehaviorFamily

enum class PlaybackControl :
    VexBehaviorFamily.Control<
        PlaybackFamily
    > {
    Rewind,
    PlayPause,
    FastForward,
}

fun VexBehaviorBlueprint<
    PlaybackFamily,
    PlaybackState,
    PlaybackAct,
    PlaybackEffect
>.playbackControls():
    VexBehaviorControlBlueprint<
        PlaybackFamily,
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
