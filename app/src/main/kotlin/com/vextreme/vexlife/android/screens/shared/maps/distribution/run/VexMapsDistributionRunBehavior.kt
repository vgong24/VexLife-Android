package com.vextreme.vexlife.android.screens.shared.maps.distribution.run

import com.vextreme.vexlife.android.behavior.VexBehaviorBlueprint
import com.vextreme.vexlife.android.behavior.VexBehaviorControlBlueprint
import com.vextreme.vexlife.android.behavior.vexBehaviorBlueprint

internal fun vexMapsDistributionRunBehavior():
    VexBehaviorBlueprint<
        VexMapsDistributionRunFamily,
        VexMapsDistributionRunState,
        VexMapsDistributionRunAct,
        VexMapsDistributionRunEffect
    > =
    vexBehaviorBlueprint(
        behaviorRef = "vexmaps.distribution.run",
    ) {
        state<VexMapsDistributionRunState.Empty>()

        state<VexMapsDistributionRunState.Draft> {
            on(
                act =
                    VexMapsDistributionRunAct.PrepareRun,
                effect =
                    VexMapsDistributionRunEffect.SealDraft,
            ).admit<VexMapsDistributionRunState.Ready>()

            on(
                act =
                    VexMapsDistributionRunAct.ResetRun,
                effect =
                    VexMapsDistributionRunEffect.ClearRun,
            ).admit<VexMapsDistributionRunState.Empty>()
        }

        state<VexMapsDistributionRunState.Ready> {
            on(
                act =
                    VexMapsDistributionRunAct.BeginRun,
                effect =
                    VexMapsDistributionRunEffect.BeginRun,
            ).admit<VexMapsDistributionRunState.Running>()

            on(
                act =
                    VexMapsDistributionRunAct.ResetRun,
                effect =
                    VexMapsDistributionRunEffect.ClearRun,
            ).admit<VexMapsDistributionRunState.Empty>()
        }

        state<VexMapsDistributionRunState.Running> {
            on(
                act =
                    VexMapsDistributionRunAct.MarkArrival,
                effect =
                    VexMapsDistributionRunEffect.MarkArrived,
            ).admit<VexMapsDistributionRunState.AtStop>()

            on(
                act =
                    VexMapsDistributionRunAct.ResetRun,
                effect =
                    VexMapsDistributionRunEffect.ClearRun,
            ).admit<VexMapsDistributionRunState.Empty>()
        }

        state<VexMapsDistributionRunState.AtStop> {
            on(
                act =
                    VexMapsDistributionRunAct.CompleteStop,
                effect =
                    VexMapsDistributionRunEffect.AdvanceToNextStop,
                conditionName =
                    "more-stops-remain",
            ) { current ->
                !current.isLastStop
            }.admit<VexMapsDistributionRunState.Running>()

            on(
                act =
                    VexMapsDistributionRunAct.CompleteStop,
                effect =
                    VexMapsDistributionRunEffect.BeginReturn,
                conditionName =
                    "last-stop-completed-return-to-start",
            ) { current ->
                current.isLastStop &&
                    current.returnToStart
            }.admit<VexMapsDistributionRunState.Returning>()

            on(
                act =
                    VexMapsDistributionRunAct.CompleteStop,
                effect =
                    VexMapsDistributionRunEffect.CompleteRun,
                conditionName =
                    "last-stop-completed-finish-here",
            ) { current ->
                current.isLastStop &&
                    !current.returnToStart
            }.admit<VexMapsDistributionRunState.Completed>()

            on(
                act =
                    VexMapsDistributionRunAct.ResetRun,
                effect =
                    VexMapsDistributionRunEffect.ClearRun,
            ).admit<VexMapsDistributionRunState.Empty>()
        }

        state<VexMapsDistributionRunState.Returning> {
            on(
                act =
                    VexMapsDistributionRunAct.MarkArrival,
                effect =
                    VexMapsDistributionRunEffect.CompleteRun,
            ).admit<VexMapsDistributionRunState.Completed>()

            on(
                act =
                    VexMapsDistributionRunAct.ResetRun,
                effect =
                    VexMapsDistributionRunEffect.ClearRun,
            ).admit<VexMapsDistributionRunState.Empty>()
        }

        state<VexMapsDistributionRunState.Completed> {
            on(
                act =
                    VexMapsDistributionRunAct.ResetRun,
                effect =
                    VexMapsDistributionRunEffect.ClearRun,
            ).admit<VexMapsDistributionRunState.Empty>()
        }
    }

internal fun VexBehaviorBlueprint<
    VexMapsDistributionRunFamily,
    VexMapsDistributionRunState,
    VexMapsDistributionRunAct,
    VexMapsDistributionRunEffect
>.vexMapsDistributionRunControls():
    VexBehaviorControlBlueprint<
        VexMapsDistributionRunFamily,
        VexMapsDistributionRunState,
        VexMapsDistributionRunAct,
        VexMapsDistributionRunEffect,
        VexMapsDistributionRunControl
    > =
    controls {
        control(
            VexMapsDistributionRunControl.Prepare,
            VexMapsDistributionRunAct.PrepareRun,
        )
        control(
            VexMapsDistributionRunControl.Start,
            VexMapsDistributionRunAct.BeginRun,
        )
        control(
            VexMapsDistributionRunControl.Arrive,
            VexMapsDistributionRunAct.MarkArrival,
        )
        control(
            VexMapsDistributionRunControl.CompleteStop,
            VexMapsDistributionRunAct.CompleteStop,
        )
        control(
            VexMapsDistributionRunControl.Reset,
            VexMapsDistributionRunAct.ResetRun,
        )
    }

// [VXG RealForever]
