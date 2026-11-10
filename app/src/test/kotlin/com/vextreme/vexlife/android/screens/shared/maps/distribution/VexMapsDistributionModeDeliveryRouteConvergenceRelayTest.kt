package com.vextreme.vexlife.android.screens.shared.maps.distribution

import com.vextreme.vexlife.android.screens.shared.maps.distribution.convergence.VexMapsDistributionModeDeliveryRouteCoherence
import com.vextreme.vexlife.android.screens.shared.maps.distribution.convergence.VexMapsDistributionModeDeliveryRouteConvergenceRelay
import com.vextreme.vexlife.android.screens.shared.maps.distribution.mode.VexMapsDistributionModeFamily
import com.vextreme.vexlife.android.screens.shared.maps.distribution.deliveryroute.VexMapsDeliveryRouteConnection
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VexMapsDistributionModeDeliveryRouteConvergenceRelayTest {
    @Test
    fun nonEmptyDeliveryRouteRequestsOnlyModeReconciliationThenBecomesCoherent() =
        runBlocking {
            val mode =
                VexMapsDistributionModeFamily()
            val deliveryRoute =
                VexMapsDeliveryRouteConnection()
            val relay =
                VexMapsDistributionModeDeliveryRouteConvergenceRelay(
                    modeStates =
                        mode.states,
                    deliveryRouteProjections =
                        deliveryRoute.projections,
                )

            try {
                relay.activate(
                    this,
                )

                deliveryRoute.observeDraft(
                    revision = 1L,
                    stopRefs =
                        listOf(
                            "alpha",
                        ),
                    returnToStart =
                        true,
                )

                val needsMode =
                    withTimeout(
                        1_000L,
                    ) {
                        relay.outputStates
                            .first {
                                it.coherence ==
                                    VexMapsDistributionModeDeliveryRouteCoherence
                                        .NeedsDistributionMode
                            }
                    }

                assertEquals(
                    VexMapsDistributionModeFamily.Control.UseDistribution,
                    needsMode.requestedModeControl,
                )

                // Satisfy the relationship before consuming the stale Relay output.
                mode.activate(
                    VexMapsDistributionModeFamily.Control.ToggleDistribution,
                )
                assertEquals(
                    VexMapsDistributionModeFamily.State.Distribution,
                    mode.currentState,
                )

                // Directional feedback is now unavailable instead of toggling back to Regular.
                mode.activate(
                    requireNotNull(
                        needsMode.requestedModeControl,
                    ),
                )
                assertEquals(
                    VexMapsDistributionModeFamily.State.Distribution,
                    mode.currentState,
                )

                val coherent =
                    withTimeout(
                        1_000L,
                    ) {
                        relay.outputStates
                            .first {
                                it.coherence ==
                                    VexMapsDistributionModeDeliveryRouteCoherence
                                        .Coherent
                            }
                    }

                assertNull(
                    coherent.requestedModeControl,
                )
            } finally {
                relay.deactivate()
            }
        }
}

// [VXG RealForever]
