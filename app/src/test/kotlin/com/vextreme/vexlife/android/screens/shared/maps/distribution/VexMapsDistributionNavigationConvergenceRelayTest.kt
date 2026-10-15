package com.vextreme.vexlife.android.screens.shared.maps.distribution

import com.vextreme.vexlife.android.screens.shared.maps.VexNavigationSessionPhase
import com.vextreme.vexlife.android.screens.shared.maps.VexNavigationSessionState
import com.vextreme.vexlife.android.screens.shared.maps.distribution.execution.VexMapsDistributionNavigationConvergenceRelay
import com.vextreme.vexlife.android.screens.shared.maps.distribution.execution.VexMapsDistributionNavigationLayer
import com.vextreme.vexlife.android.screens.shared.maps.distribution.execution.VexMapsDistributionNavigationRequest
import com.vextreme.vexlife.android.screens.shared.maps.distribution.run.VexMapsDistributionRunConnection
import com.vextreme.vexlife.android.screens.shared.maps.distribution.run.VexMapsDistributionRunControl
import com.vextreme.vexlife.android.screens.shared.maps.distribution.run.VexMapsDistributionRunState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VexMapsDistributionNavigationConvergenceRelayTest {
    private fun readyRun(
        returnToStart: Boolean = true,
    ): VexMapsDistributionRunConnection =
        VexMapsDistributionRunConnection()
            .also { connection ->
                connection.observeDraft(
                    stopCount = 2,
                    returnToStart =
                        returnToStart,
                )
                connection.activate(
                    VexMapsDistributionRunControl.Prepare,
                )
            }

    @Test
    fun readyRunNeedsFirstLegBeforeHumanStart() {
        val run =
            readyRun()
        val relay =
            VexMapsDistributionNavigationConvergenceRelay(
                initialRun =
                    run.currentProjection,
                initialNavigation =
                    VexNavigationSessionState(),
            )

        assertEquals(
            VexMapsDistributionNavigationLayer.NeedsLeg,
            relay.currentOutput.layer,
        )
        assertEquals(
            VexMapsDistributionNavigationRequest.PrepareLeg(
                stopIndex = 0,
            ),
            relay.currentOutput.request,
        )
    }

    @Test
    fun readyNavigationWaitsForHumanStart() {
        val run =
            readyRun()
        val relay =
            VexMapsDistributionNavigationConvergenceRelay(
                initialRun =
                    run.currentProjection,
                initialNavigation =
                    VexNavigationSessionState(
                        phase =
                            VexNavigationSessionPhase.READY,
                    ),
            )

        assertEquals(
            VexMapsDistributionNavigationLayer.ReadyToStart,
            relay.currentOutput.layer,
        )
        assertNull(
            relay.currentOutput.request,
        )
    }

    @Test
    fun runningRunStartsPreparedNavigationThenAdmitsArrivalThroughRunControl() {
        val run =
            readyRun()
                .also { connection ->
                    connection.activate(
                        VexMapsDistributionRunControl.Start,
                    )
                }
        val relay =
            VexMapsDistributionNavigationConvergenceRelay(
                initialRun =
                    run.currentProjection,
                initialNavigation =
                    VexNavigationSessionState(
                        phase =
                            VexNavigationSessionPhase.READY,
                    ),
            )

        assertEquals(
            VexMapsDistributionNavigationRequest.StartPreparedNavigation,
            relay.currentOutput.request,
        )

        relay.navigationChanged(
            VexNavigationSessionState(
                phase =
                    VexNavigationSessionPhase.ARRIVED,
            ),
        )

        assertEquals(
            VexMapsDistributionNavigationRequest.RunControl(
                VexMapsDistributionRunControl.Arrive,
            ),
            relay.currentOutput.request,
        )
    }

    @Test
    fun returningRunRequestsReturnLegThenCompletesFromArrival() {
        val run =
            VexMapsDistributionRunConnection(
                initialState =
                    VexMapsDistributionRunState.Returning(
                        stopCount = 2,
                    ),
            )
        val relay =
            VexMapsDistributionNavigationConvergenceRelay(
                initialRun =
                    run.currentProjection,
                initialNavigation =
                    VexNavigationSessionState(),
            )

        assertEquals(
            VexMapsDistributionNavigationRequest.PrepareReturn,
            relay.currentOutput.request,
        )

        relay.navigationChanged(
            VexNavigationSessionState(
                phase =
                    VexNavigationSessionPhase.ARRIVED,
            ),
        )

        assertEquals(
            VexMapsDistributionNavigationRequest.RunControl(
                VexMapsDistributionRunControl.Arrive,
            ),
            relay.currentOutput.request,
        )
    }
}

// [VXG RealForever]
