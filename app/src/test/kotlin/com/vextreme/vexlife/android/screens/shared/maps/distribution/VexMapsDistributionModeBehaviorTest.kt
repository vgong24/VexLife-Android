package com.vextreme.vexlife.android.screens.shared.maps.distribution

import com.vextreme.vexlife.android.screens.shared.maps.distribution.mode.VexMapsDistributionModeAct
import com.vextreme.vexlife.android.screens.shared.maps.distribution.mode.VexMapsDistributionModeControl
import com.vextreme.vexlife.android.screens.shared.maps.distribution.mode.VexMapsDistributionModeEffect
import com.vextreme.vexlife.android.screens.shared.maps.distribution.mode.VexMapsDistributionModeState
import com.vextreme.vexlife.android.screens.shared.maps.distribution.mode.vexMapsDistributionModeBehavior
import com.vextreme.vexlife.android.screens.shared.maps.distribution.mode.vexMapsDistributionModeControls
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Test

class VexMapsDistributionModeBehaviorTest {
    private val behavior =
        vexMapsDistributionModeBehavior()

    private val controls =
        behavior.vexMapsDistributionModeControls()

    @Test
    fun oneControlMeansEnterOrExitFromCurrentModeTruth() {
        val regular =
            controls.projection(
                VexMapsDistributionModeState.Regular,
            )

        assertSame(
            VexMapsDistributionModeAct.EnterDistribution,
            regular[
                VexMapsDistributionModeControl.ToggleDistribution
            ].act,
        )

        val distribution =
            controls.projection(
                VexMapsDistributionModeState.Distribution,
            )

        assertSame(
            VexMapsDistributionModeAct.ExitDistribution,
            distribution[
                VexMapsDistributionModeControl.ToggleDistribution
            ].act,
        )
        assertSame(
            VexMapsDistributionModeAct.EnterDistribution,
            regular[
                VexMapsDistributionModeControl.UseDistribution
            ].act,
        )
        assertFalse(
            distribution[
                VexMapsDistributionModeControl.UseDistribution
            ].isAvailable,
        )
    }

    @Test
    fun enterDistributionAdmitsDistributionState() {
        val resolution =
            behavior.resolve(
                current =
                    VexMapsDistributionModeState.Regular,
                act =
                    VexMapsDistributionModeAct.EnterDistribution,
            )

        assertSame(
            VexMapsDistributionModeEffect.UseDistributionMode,
            resolution?.effect,
        )
        assertEquals(
            VexMapsDistributionModeState.Distribution::class,
            resolution?.admittedStateKind,
        )
    }
}

// [VXG RealForever]
