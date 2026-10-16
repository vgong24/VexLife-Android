package com.vextreme.vexlife.android.vexstream.queue

import com.vextreme.vexlife.android.behavior.VexBehaviorBlueprint
import com.vextreme.vexlife.android.behavior.VexBehaviorControlBlueprint
import com.vextreme.vexlife.android.behavior.VexBehaviorFamily

enum class VexStreamQueueControl :
    VexBehaviorFamily.Control<
        VexStreamQueueFamily
    > {
    Previous,
    Next,
    Clear,
}

internal fun VexBehaviorBlueprint<
    VexStreamQueueFamily,
    VexStreamQueueState,
    VexStreamQueueAct,
    VexStreamQueueEffect
>.vexStreamQueueControls():
    VexBehaviorControlBlueprint<
        VexStreamQueueFamily,
        VexStreamQueueState,
        VexStreamQueueAct,
        VexStreamQueueEffect,
        VexStreamQueueControl
    > =
    controls<VexStreamQueueControl> {
        control(
            VexStreamQueueControl.Previous,
            VexStreamQueueAct.Previous,
        )
        control(
            VexStreamQueueControl.Next,
            VexStreamQueueAct.Next,
        )
        control(
            VexStreamQueueControl.Clear,
            VexStreamQueueAct.Clear,
        )
    }

// [VXG RealForever]
