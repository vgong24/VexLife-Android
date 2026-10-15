package com.vextreme.vexlife.android.screens.shared.maps.distribution.mode

import com.vextreme.vexlife.android.behavior.VexBehaviorBlueprint
import com.vextreme.vexlife.android.behavior.VexBehaviorControlBlueprint
import com.vextreme.vexlife.android.behavior.vexBehaviorBlueprint

internal fun vexMapsDistributionModeBehavior():
    VexBehaviorBlueprint<
        VexMapsDistributionModeFamily,
        VexMapsDistributionModeState,
        VexMapsDistributionModeAct,
        VexMapsDistributionModeEffect
    > =
    vexBehaviorBlueprint(
        behaviorRef = "vexmaps.distribution.mode",
    ) {
        state<VexMapsDistributionModeState.Regular> {
            on(
                act =
                    VexMapsDistributionModeAct.EnterDistribution,
                effect =
                    VexMapsDistributionModeEffect.UseDistributionMode,
            ).admit<VexMapsDistributionModeState.Distribution>()
        }

        state<VexMapsDistributionModeState.Distribution> {
            on(
                act =
                    VexMapsDistributionModeAct.ExitDistribution,
                effect =
                    VexMapsDistributionModeEffect.UseRegularMode,
            ).admit<VexMapsDistributionModeState.Regular>()
        }
    }

internal fun VexBehaviorBlueprint<
    VexMapsDistributionModeFamily,
    VexMapsDistributionModeState,
    VexMapsDistributionModeAct,
    VexMapsDistributionModeEffect
>.vexMapsDistributionModeControls():
    VexBehaviorControlBlueprint<
        VexMapsDistributionModeFamily,
        VexMapsDistributionModeState,
        VexMapsDistributionModeAct,
        VexMapsDistributionModeEffect,
        VexMapsDistributionModeControl
    > =
    controls {
        control(
            VexMapsDistributionModeControl.ToggleDistribution,
            VexMapsDistributionModeAct.EnterDistribution,
            VexMapsDistributionModeAct.ExitDistribution,
        )
        control(
            VexMapsDistributionModeControl.UseDistribution,
            VexMapsDistributionModeAct.EnterDistribution,
        )
    }

// [VXG RealForever]
