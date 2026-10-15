package com.vextreme.vexlife.android.screens.shared.maps.distribution

import com.vextreme.vexlife.android.screens.shared.maps.distribution.run.VexMapsDistributionRunAct
import com.vextreme.vexlife.android.screens.shared.maps.distribution.run.VexMapsDistributionRunControl
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

    @Test
    fun atStopFlavorChoosesAdvanceFinishOrReturnWithoutTypeExplosion() {
        val moreStops =
            VexMapsDistributionRunState.AtStop(
                stopCount = 3,
                currentStopIndex = 1,
                returnToStart = true,
            )
        val finishHere =
            VexMapsDistributionRunState.AtStop(
                stopCount = 3,
                currentStopIndex = 2,
                returnToStart = false,
            )
        val returnHome =
            finishHere.copy(
                returnToStart = true,
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
                        stopCount = 3,
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
