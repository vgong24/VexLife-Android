package com.vextreme.vexlife.android.vexstream.playback

import com.vextreme.vexlife.android.behavior.VexBehaviorBlueprint
import com.vextreme.vexlife.android.behavior.VexBehaviorControlBlueprint
import com.vextreme.vexlife.android.behavior.VexBehaviorFamily

enum class VexStreamPlaybackControl :
    VexBehaviorFamily.Control<
        VexStreamPlaybackFamily
    > {
    PlayPause,
}

internal fun VexBehaviorBlueprint<
    VexStreamPlaybackFamily,
    VexStreamPlaybackState,
    VexStreamPlaybackAct,
    VexStreamPlaybackEffect
>.vexStreamPlaybackControls():
    VexBehaviorControlBlueprint<
        VexStreamPlaybackFamily,
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

// [VXG RealForever]
