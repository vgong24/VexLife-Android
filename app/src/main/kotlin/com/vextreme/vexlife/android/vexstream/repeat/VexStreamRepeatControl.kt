package com.vextreme.vexlife.android.vexstream.repeat

import com.vextreme.vexlife.android.behavior.VexBehaviorBlueprint
import com.vextreme.vexlife.android.behavior.VexBehaviorControlBlueprint
import com.vextreme.vexlife.android.behavior.VexBehaviorFamily

enum class VexStreamRepeatControl :
    VexBehaviorFamily.Control<
        VexStreamRepeatFamily
    > {
    Toggle,
}

internal fun VexBehaviorBlueprint<
    VexStreamRepeatFamily,
    VexStreamRepeatState,
    VexStreamRepeatAct,
    VexStreamRepeatEffect
>.vexStreamRepeatControls():
    VexBehaviorControlBlueprint<
        VexStreamRepeatFamily,
        VexStreamRepeatState,
        VexStreamRepeatAct,
        VexStreamRepeatEffect,
        VexStreamRepeatControl
    > =
    controls<VexStreamRepeatControl> {
        control(
            VexStreamRepeatControl.Toggle,
            VexStreamRepeatAct.Enable,
            VexStreamRepeatAct.Disable,
        )
    }

// [VXG RealForever]
