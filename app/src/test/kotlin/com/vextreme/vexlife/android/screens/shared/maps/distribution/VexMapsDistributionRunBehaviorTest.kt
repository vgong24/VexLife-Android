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
    fun atStopFlavorChoosesAdvanceOrCompleteWithoutSplittingStateKind() {
        val moreStops =
            VexMapsDistributionRunState.AtStop(
                stopCount = 3,
                currentStopIndex = 1,
            )
        val lastStop =
            VexMapsDistributionRunState.AtStop(
                stopCount = 3,
                currentStopIndex = 2,
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
                current = lastStop,
                act =
                    VexMapsDistributionRunAct.CompleteStop,
            )?.effect,
        )

        assertEquals(
            moreStops::class,
            lastStop::class,
        )
    }
}

// [VXG RealForever]
