package com.vextreme.vexlife.android.vexstream.queue

import com.vextreme.vexlife.android.behavior.VexBehaviorBlueprint
import com.vextreme.vexlife.android.behavior.vexBehaviorBlueprint

internal fun vexStreamQueueBehavior():
    VexBehaviorBlueprint<
        VexStreamQueueFamily,
        VexStreamQueueState,
        VexStreamQueueAct,
        VexStreamQueueEffect
    > =
    vexBehaviorBlueprint(
        behaviorRef = "vexstream.queue",
    ) {
        state<VexStreamQueueState.Empty>()

        state<VexStreamQueueState.NonEmpty> {
            on(
                act = VexStreamQueueAct.Previous,
                effect =
                    VexStreamQueueEffect.SeekPrevious,
                conditionName =
                    "previous-available",
            ) { current ->
                current.previousAvailable
            }.stay()

            on(
                act = VexStreamQueueAct.Next,
                effect =
                    VexStreamQueueEffect.SeekNext,
                conditionName =
                    "next-available",
            ) { current ->
                current.nextAvailable
            }.stay()

            on(
                act = VexStreamQueueAct.Clear,
                effect =
                    VexStreamQueueEffect.ClearQueue,
                conditionName =
                    "clear-available",
            ) { current ->
                current.clearAvailable
            }.stay()
        }
    }

// [VXG RealForever]
