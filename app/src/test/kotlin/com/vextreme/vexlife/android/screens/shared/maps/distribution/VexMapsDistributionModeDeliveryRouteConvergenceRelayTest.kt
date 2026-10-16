package com.vextreme.vexlife.android.screens.shared.maps.distribution

import com.vextreme.vexlife.android.screens.shared.maps.distribution.convergence.VexMapsDistributionModeDeliveryRouteCoherence
import com.vextreme.vexlife.android.screens.shared.maps.distribution.convergence.VexMapsDistributionModeDeliveryRouteConvergenceRelay
import com.vextreme.vexlife.android.screens.shared.maps.distribution.mode.VexMapsDistributionModeConnection
import com.vextreme.vexlife.android.screens.shared.maps.distribution.mode.VexMapsDistributionModeControl
import com.vextreme.vexlife.android.screens.shared.maps.distribution.mode.VexMapsDistributionModeState
import com.vextreme.vexlife.android.screens.shared.maps.distribution.deliveryroute.VexMapsDeliveryRouteConnection
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VexMapsDistributionModeDeliveryRouteConvergenceRelayTest {
    @Test
    fun nonEmptyRunRequestsOnlyModeReconciliationThenBecomesCoherent() =
        runBlocking {
            val mode =
                VexMapsDistributionModeConnection()
            val deliveryRoute =
                VexMapsDeliveryRouteConnection()
            val relay =
                VexMapsDistributionModeDeliveryRouteConvergenceRelay(
                    modeProjections =
                        mode.projections,
                    deliveryRouteProjections =
                        deliveryRoute.projections,
                )

            try {
                relay.activate(
                    this,
                )

                run.observeDraft(
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
                    VexMapsDistributionModeControl.UseDistribution,
                    needsMode.requestedModeControl,
                )

                // Satisfy the relationship before consuming the stale Relay output.
                mode.activate(
                    VexMapsDistributionModeControl.ToggleDistribution,
                )
                assertEquals(
                    VexMapsDistributionModeState.Distribution,
                    mode.currentProjection.state,
                )

                // Directional feedback is now unavailable instead of toggling back to Regular.
                mode.activate(
                    requireNotNull(
                        needsMode.requestedModeControl,
                    ),
                )
                assertEquals(
                    VexMapsDistributionModeState.Distribution,
                    mode.currentProjection.state,
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
