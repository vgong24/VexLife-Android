package com.vextreme.vexlife.android.screens.shared.maps.distribution.deliveryroute

import com.vextreme.vexlife.android.behavior.VexBehaviorBlueprint
import com.vextreme.vexlife.android.behavior.VexBehaviorControlBlueprint
import com.vextreme.vexlife.android.behavior.vexBehaviorBlueprint

internal fun vexMapsDeliveryRouteBehavior():
    VexBehaviorBlueprint<
        VexMapsDeliveryRouteFamily,
        VexMapsDeliveryRouteState,
        VexMapsDeliveryRouteAct,
        VexMapsDeliveryRouteEffect
    > =
    vexBehaviorBlueprint(
        behaviorRef = "vexmaps.distribution.delivery-route",
    ) {
        state<VexMapsDeliveryRouteState.Empty>()

        state<VexMapsDeliveryRouteState.Draft> {
            on(
                act =
                    VexMapsDeliveryRouteAct.PrepareRoute,
                effect =
                    VexMapsDeliveryRouteEffect.BeginPreparation,
            ).admit<VexMapsDeliveryRouteState.Preparing>()

            on(
                act =
                    VexMapsDeliveryRouteAct.ClearRoute,
                effect =
                    VexMapsDeliveryRouteEffect.ClearDeliveryRoute,
            ).admit<VexMapsDeliveryRouteState.Empty>()
        }

        state<VexMapsDeliveryRouteState.Preparing> {
            on(
                act =
                    VexMapsDeliveryRouteAct.ClearRoute,
                effect =
                    VexMapsDeliveryRouteEffect.ClearDeliveryRoute,
            ).admit<VexMapsDeliveryRouteState.Empty>()
        }

        state<VexMapsDeliveryRouteState.Ready> {
            on(
                act =
                    VexMapsDeliveryRouteAct.StartRoute,
                effect =
                    VexMapsDeliveryRouteEffect.BeginDeliveryRoute,
            ).admit<VexMapsDeliveryRouteState.Delivering>()

            on(
                act =
                    VexMapsDeliveryRouteAct.ClearRoute,
                effect =
                    VexMapsDeliveryRouteEffect.ClearDeliveryRoute,
            ).admit<VexMapsDeliveryRouteState.Empty>()
        }

        state<VexMapsDeliveryRouteState.Delivering> {
            on(
                act =
                    VexMapsDeliveryRouteAct.ArriveAtDelivery,
                effect =
                    VexMapsDeliveryRouteEffect.RecordDeliveryArrival,
            ).admit<VexMapsDeliveryRouteState.AtDelivery>()

            on(
                act =
                    VexMapsDeliveryRouteAct.ClearRoute,
                effect =
                    VexMapsDeliveryRouteEffect.ClearDeliveryRoute,
            ).admit<VexMapsDeliveryRouteState.Empty>()
        }

        state<VexMapsDeliveryRouteState.AtDelivery> {
            on(
                act =
                    VexMapsDeliveryRouteAct.MarkDelivered,
                effect =
                    VexMapsDeliveryRouteEffect.AdvanceToNextDelivery,
                conditionName =
                    "more-deliveries-remain",
            ) { current ->
                !current.isLastStop
            }.admit<VexMapsDeliveryRouteState.Delivering>()

            on(
                act =
                    VexMapsDeliveryRouteAct.MarkDelivered,
                effect =
                    VexMapsDeliveryRouteEffect.BeginReturnToStart,
                conditionName =
                    "last-delivery-completed-return-to-start",
            ) { current ->
                current.isLastStop &&
                    current.source.returnToStart
            }.admit<VexMapsDeliveryRouteState.ReturningToStart>()

            on(
                act =
                    VexMapsDeliveryRouteAct.MarkDelivered,
                effect =
                    VexMapsDeliveryRouteEffect.CompleteDeliveryRoute,
                conditionName =
                    "last-delivery-completed-finish-here",
            ) { current ->
                current.isLastStop &&
                    !current.source.returnToStart
            }.admit<VexMapsDeliveryRouteState.Completed>()

            on(
                act =
                    VexMapsDeliveryRouteAct.ClearRoute,
                effect =
                    VexMapsDeliveryRouteEffect.ClearDeliveryRoute,
            ).admit<VexMapsDeliveryRouteState.Empty>()
        }

        state<VexMapsDeliveryRouteState.ReturningToStart> {
            on(
                act =
                    VexMapsDeliveryRouteAct.ArriveAtStart,
                effect =
                    VexMapsDeliveryRouteEffect.CompleteDeliveryRoute,
            ).admit<VexMapsDeliveryRouteState.Completed>()

            on(
                act =
                    VexMapsDeliveryRouteAct.ClearRoute,
                effect =
                    VexMapsDeliveryRouteEffect.ClearDeliveryRoute,
            ).admit<VexMapsDeliveryRouteState.Empty>()
        }

        state<VexMapsDeliveryRouteState.Completed> {
            on(
                act =
                    VexMapsDeliveryRouteAct.ClearRoute,
                effect =
                    VexMapsDeliveryRouteEffect.ClearDeliveryRoute,
            ).admit<VexMapsDeliveryRouteState.Empty>()
        }
    }

internal fun VexBehaviorBlueprint<
    VexMapsDeliveryRouteFamily,
    VexMapsDeliveryRouteState,
    VexMapsDeliveryRouteAct,
    VexMapsDeliveryRouteEffect
>.vexMapsDeliveryRouteControls():
    VexBehaviorControlBlueprint<
        VexMapsDeliveryRouteFamily,
        VexMapsDeliveryRouteState,
        VexMapsDeliveryRouteAct,
        VexMapsDeliveryRouteEffect,
        VexMapsDeliveryRouteControl
    > =
    controls {
        control(
            VexMapsDeliveryRouteControl.Prepare,
            VexMapsDeliveryRouteAct.PrepareRoute,
        )
        control(
            VexMapsDeliveryRouteControl.Start,
            VexMapsDeliveryRouteAct.StartRoute,
        )
        control(
            VexMapsDeliveryRouteControl.Arrive,
            VexMapsDeliveryRouteAct.ArriveAtDelivery,
            VexMapsDeliveryRouteAct.ArriveAtStart,
        )
        control(
            VexMapsDeliveryRouteControl.MarkDelivered,
            VexMapsDeliveryRouteAct.MarkDelivered,
        )
        control(
            VexMapsDeliveryRouteControl.Clear,
            VexMapsDeliveryRouteAct.ClearRoute,
        )
    }

// [VXG RealForever]
