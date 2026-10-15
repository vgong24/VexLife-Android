package com.vextreme.vexlife.android.screens.shared.maps.distribution

import com.vextreme.vexlife.android.screens.shared.maps.distribution.run.VexMapsDistributionRunAct
import com.vextreme.vexlife.android.screens.shared.maps.distribution.run.VexMapsDistributionRunControl
import com.vextreme.vexlife.android.screens.shared.maps.distribution.run.VexMapsDistributionRunDraftIdentity
import com.vextreme.vexlife.android.screens.shared.maps.distribution.run.VexMapsDistributionRunEffect
import com.vextreme.vexlife.android.screens.shared.maps.distribution.run.VexMapsDistributionRunState
import com.vextreme.vexlife.android.screens.shared.maps.distribution.run.vexMapsDistributionRunBehavior
import com.vextreme.vexlife.android.screens.shared.maps.distribution.run.vexMapsDistributionRunControls
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class VexMapsDistributionRunBehaviorTest {
    private val behavior =
        vexMapsDistributionRunBehavior()
    private val controls =
        behavior.vexMapsDistributionRunControls()

    private fun source(
        returnToStart: Boolean,
    ) =
        VexMapsDistributionRunDraftIdentity(
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
                    VexMapsDistributionRunState.Draft(
                        source =
                            source(
                                returnToStart = true,
                            ),
                    ),
                act =
                    VexMapsDistributionRunAct.PrepareRun,
            )

        assertSame(
            VexMapsDistributionRunEffect.SealDraft,
            resolution?.effect,
        )
        assertEquals(
            VexMapsDistributionRunState.Preparing::class,
            resolution?.admittedStateKind,
        )
    }

    @Test
    fun atStopFlavorChoosesAdvanceFinishOrReturnWithoutTypeExplosion() {
        val ordered =
            listOf(
                "beta",
                "alpha",
                "gamma",
            )
        val moreStops =
            VexMapsDistributionRunState.AtStop(
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
            VexMapsDistributionRunState.AtStop(
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
        val returnHome =
            VexMapsDistributionRunState.AtStop(
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
            VexMapsDistributionRunAct.CompleteStop,
            controls.projection(
                moreStops,
            )[
                VexMapsDistributionRunControl.CompleteStop
            ].act,
        )
        assertSame(
            VexMapsDistributionRunEffect.AdvanceToNextStop,
            behavior.resolve(
                current = moreStops,
                act =
                    VexMapsDistributionRunAct.CompleteStop,
            )?.effect,
        )
        assertEquals(
            "gamma",
            moreStops.nextStopRef,
        )
        assertSame(
            VexMapsDistributionRunEffect.CompleteRun,
            behavior.resolve(
                current = finishHere,
                act =
                    VexMapsDistributionRunAct.CompleteStop,
            )?.effect,
        )
        assertSame(
            VexMapsDistributionRunEffect.BeginReturn,
            behavior.resolve(
                current = returnHome,
                act =
                    VexMapsDistributionRunAct.CompleteStop,
            )?.effect,
        )
        assertEquals(
            moreStops::class,
            returnHome::class,
        )
    }

    @Test
    fun returningCompletesOnlyFromObservedArrival() {
        val resolution =
            behavior.resolve(
                current =
                    VexMapsDistributionRunState.Returning(
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
                    VexMapsDistributionRunAct.MarkArrival,
            )

        assertSame(
            VexMapsDistributionRunEffect.CompleteRun,
            resolution?.effect,
        )
        assertEquals(
            VexMapsDistributionRunState.Completed::class,
            resolution?.admittedStateKind,
        )
    }
}

// [VXG RealForever]
