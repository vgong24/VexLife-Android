package com.vextreme.vexlife.android.screens.shared.maps.distribution

import com.vextreme.vexlife.android.screens.shared.maps.distribution.deliveryroute.VexMapsDeliveryRouteAct
import com.vextreme.vexlife.android.screens.shared.maps.distribution.deliveryroute.VexMapsDeliveryRouteControl
import com.vextreme.vexlife.android.screens.shared.maps.distribution.deliveryroute.VexMapsDeliveryRouteDraftIdentity
import com.vextreme.vexlife.android.screens.shared.maps.distribution.deliveryroute.VexMapsDeliveryRouteEffect
import com.vextreme.vexlife.android.screens.shared.maps.distribution.deliveryroute.VexMapsDeliveryRouteState
import com.vextreme.vexlife.android.screens.shared.maps.distribution.deliveryroute.vexMapsDeliveryRouteBehavior
import com.vextreme.vexlife.android.screens.shared.maps.distribution.deliveryroute.vexMapsDeliveryRouteControls
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class VexMapsDeliveryRouteBehaviorTest {
    private val behavior =
        vexMapsDeliveryRouteBehavior()
    private val controls =
        behavior.vexMapsDeliveryRouteControls()

    private fun source(
        returnToStart: Boolean,
    ) =
        VexMapsDeliveryRouteDraftIdentity(
            revision = 9L,
            stopRefs =
                listOf(
                    "alpha",
                    "beta",
                    "gamma",
                ),
            returnToStart =
                returnToStart,
        )

    @Test
    fun prepareAdmitsPreparingInsteadOfPretendingProviderSuccess() {
        val resolution =
            behavior.resolve(
                current =
                    VexMapsDeliveryRouteState.Draft(
                        source =
                            source(
                                returnToStart = true,
                            ),
                    ),
                act =
                    VexMapsDeliveryRouteAct.PrepareRoute,
            )

        assertSame(
            VexMapsDeliveryRouteEffect.BeginPreparation,
            resolution?.effect,
        )
        assertEquals(
            VexMapsDeliveryRouteState.Preparing::class,
            resolution?.admittedStateKind,
        )
    }

    @Test
    fun atDeliveryStateChoosesNextDeliveryFinishOrReturnWithoutTypeExplosion() {
        val ordered =
            listOf(
                "beta",
                "alpha",
                "gamma",
            )
        val moreDeliveriesRemain =
            VexMapsDeliveryRouteState.AtDelivery(
                planRef =
                    "plan.1",
                source =
                    source(
                        returnToStart = true,
                    ),
                orderedStopRefs =
                    ordered,
                currentStopRef =
                    "alpha",
            )
        val finishHere =
            VexMapsDeliveryRouteState.AtDelivery(
                planRef =
                    "plan.2",
                source =
                    source(
                        returnToStart = false,
                    ),
                orderedStopRefs =
                    ordered,
                currentStopRef =
                    "gamma",
            )
        val returnToStart =
            VexMapsDeliveryRouteState.AtDelivery(
                planRef =
                    "plan.3",
                source =
                    source(
                        returnToStart = true,
                    ),
                orderedStopRefs =
                    ordered,
                currentStopRef =
                    "gamma",
            )

        assertSame(
            VexMapsDeliveryRouteAct.MarkDelivered,
            controls.projection(
                moreDeliveriesRemain,
            )[
                VexMapsDeliveryRouteControl.MarkDelivered
            ].act,
        )
        assertSame(
            VexMapsDeliveryRouteEffect.AdvanceToNextDelivery,
            behavior.resolve(
                current = moreDeliveriesRemain,
                act =
                    VexMapsDeliveryRouteAct.MarkDelivered,
            )?.effect,
        )
        assertEquals(
            "gamma",
            moreDeliveriesRemain.nextStopRef,
        )
        assertSame(
            VexMapsDeliveryRouteEffect.CompleteDeliveryRoute,
            behavior.resolve(
                current = finishHere,
                act =
                    VexMapsDeliveryRouteAct.MarkDelivered,
            )?.effect,
        )
        assertSame(
            VexMapsDeliveryRouteEffect.BeginReturnToStart,
            behavior.resolve(
                current = returnToStart,
                act =
                    VexMapsDeliveryRouteAct.MarkDelivered,
            )?.effect,
        )
        assertEquals(
            moreDeliveriesRemain::class,
            returnToStart::class,
        )
    }

    @Test
    fun arriveControlNamesTheCurrentDestinationInsteadOfOneGenericArrival() {
        val ordered =
            listOf(
                "beta",
                "alpha",
                "gamma",
            )
        val delivering =
            VexMapsDeliveryRouteState.Delivering(
                planRef =
                    "plan.delivery",
                source =
                    source(
                        returnToStart = true,
                    ),
                orderedStopRefs =
                    ordered,
                currentStopRef =
                    "beta",
            )
        val returning =
            VexMapsDeliveryRouteState.ReturningToStart(
                planRef =
                    "plan.return",
                source =
                    source(
                        returnToStart = true,
                    ),
                orderedStopRefs =
                    ordered,
            )

        assertSame(
            VexMapsDeliveryRouteAct.ArriveAtDelivery,
            controls.projection(
                delivering,
            )[
                VexMapsDeliveryRouteControl.Arrive
            ].act,
        )
        assertSame(
            VexMapsDeliveryRouteAct.ArriveAtStart,
            controls.projection(
                returning,
            )[
                VexMapsDeliveryRouteControl.Arrive
            ].act,
        )
    }

    @Test
    fun returningCompletesOnlyFromObservedArrival() {
        val resolution =
            behavior.resolve(
                current =
                    VexMapsDeliveryRouteState.ReturningToStart(
                        planRef =
                            "plan.return",
                        source =
                            source(
                                returnToStart = true,
                            ),
                        orderedStopRefs =
                            listOf(
                                "beta",
                                "alpha",
                                "gamma",
                            ),
                    ),
                act =
                    VexMapsDeliveryRouteAct.ArriveAtStart,
            )

        assertSame(
            VexMapsDeliveryRouteEffect.CompleteDeliveryRoute,
            resolution?.effect,
        )
        assertEquals(
            VexMapsDeliveryRouteState.Completed::class,
            resolution?.admittedStateKind,
        )
    }
}

// [VXG RealForever]
