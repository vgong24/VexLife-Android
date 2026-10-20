package com.vextreme.vexlife.android.vexstream.repeat

import com.vextreme.vexlife.android.behavior.VexBehaviorBlueprint
import com.vextreme.vexlife.android.behavior.vexBehaviorBlueprint

internal fun vexStreamRepeatBehavior():
    VexBehaviorBlueprint<
        VexStreamRepeatFamily,
        VexStreamRepeatState,
        VexStreamRepeatAct,
        VexStreamRepeatEffect
    > =
    vexBehaviorBlueprint(
        behaviorRef = "vexstream.repeat",
    ) {
        state<VexStreamRepeatState.Disabled> {
            on(
                act = VexStreamRepeatAct.Enable,
                effect =
                    VexStreamRepeatEffect.RequestEnableAll,
                conditionName =
                    "repeat-toggle-available",
            ) { current ->
                current.toggleAvailable
            }.stay()
        }

        state<VexStreamRepeatState.Enabled> {
            on(
                act = VexStreamRepeatAct.Disable,
                effect =
                    VexStreamRepeatEffect.RequestDisable,
                conditionName =
                    "repeat-toggle-available",
            ) { current ->
                current.toggleAvailable
            }.stay()
        }
    }

// [VXG RealForever]
