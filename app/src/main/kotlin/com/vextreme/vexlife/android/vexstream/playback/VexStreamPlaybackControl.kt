package com.vextreme.vexlife.android.vexstream.playback

import com.vextreme.vexlife.android.behavior.VexBehaviorBlueprint
import com.vextreme.vexlife.android.behavior.VexBehaviorControlBlueprint

/** Complete finite transport affordance vocabulary admitted by this first VexStream slice. */
enum class VexStreamPlaybackControl {
    PlayPause,
}

internal fun VexBehaviorBlueprint<
    VexStreamPlaybackState,
    VexStreamPlaybackAct,
    VexStreamPlaybackEffect
>.vexStreamPlaybackControls():
    VexBehaviorControlBlueprint<
        VexStreamPlaybackState,
        VexStreamPlaybackAct,
        VexStreamPlaybackEffect,
        VexStreamPlaybackControl
    > =
    controls<VexStreamPlaybackControl> {
        control(
            VexStreamPlaybackControl.PlayPause,
            VexStreamPlaybackAct.Play,
            VexStreamPlaybackAct.Pause,
        )
    }
