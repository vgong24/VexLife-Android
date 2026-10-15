package com.vextreme.vexlife.android.screens.shared.maps.distribution

import com.vextreme.vexlife.android.screens.shared.maps.distribution.convergence.VexMapsDistributionModeRunCoherence
import com.vextreme.vexlife.android.screens.shared.maps.distribution.convergence.VexMapsDistributionModeRunConvergenceRelay
import com.vextreme.vexlife.android.screens.shared.maps.distribution.mode.VexMapsDistributionModeConnection
import com.vextreme.vexlife.android.screens.shared.maps.distribution.mode.VexMapsDistributionModeControl
import com.vextreme.vexlife.android.screens.shared.maps.distribution.run.VexMapsDistributionRunConnection
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VexMapsDistributionModeRunConvergenceRelayTest {
    @Test
    fun nonEmptyRunRequestsOnlyModeReconciliationThenBecomesCoherent() =
        runBlocking {
            val mode =
                VexMapsDistributionModeConnection()
            val run =
                VexMapsDistributionRunConnection()
            val relay =
                VexMapsDistributionModeRunConvergenceRelay(
                    modeProjections =
                        mode.projections,
                    runProjections =
                        run.projections,
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
                                    VexMapsDistributionModeRunCoherence
                                        .NeedsDistributionMode
                            }
                    }

                assertEquals(
                    VexMapsDistributionModeControl.ToggleDistribution,
                    needsMode.requestedModeControl,
                )

                mode.activate(
                    requireNotNull(
                        needsMode.requestedModeControl,
                    ),
                )

                val coherent =
                    withTimeout(
                        1_000L,
                    ) {
                        relay.outputStates
                            .first {
                                it.coherence ==
                                    VexMapsDistributionModeRunCoherence
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
