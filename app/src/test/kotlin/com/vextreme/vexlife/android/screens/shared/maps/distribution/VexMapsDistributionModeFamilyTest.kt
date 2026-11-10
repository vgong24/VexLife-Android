package com.vextreme.vexlife.android.screens.shared.maps.distribution

import com.vextreme.vexlife.android.screens.shared.maps.distribution.mode.VexMapsDistributionModeFamily
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class VexMapsDistributionModeFamilyTest {
    @Test
    fun toggleAndDirectionalControlResolveAgainstExactCurrentMode() {
        val family =
            VexMapsDistributionModeFamily()

        assertSame(
            VexMapsDistributionModeFamily.State.Regular,
            family.currentState,
        )
        assertTrue(
            family.isAvailable(
                VexMapsDistributionModeFamily.Control.ToggleDistribution,
            ),
        )
        assertTrue(
            family.isAvailable(
                VexMapsDistributionModeFamily.Control.UseDistribution,
            ),
        )

        val enter =
            family.activate(
                VexMapsDistributionModeFamily.Control.ToggleDistribution,
            )

        assertSame(
            VexMapsDistributionModeFamily.State.Distribution,
            enter?.admittedState,
        )
        assertNull(
            enter?.effect,
        )
        assertSame(
            VexMapsDistributionModeFamily.State.Distribution,
            family.currentState,
        )
        assertFalse(
            family.isAvailable(
                VexMapsDistributionModeFamily.Control.UseDistribution,
            ),
        )

        val redundantDirectionalRequest =
            family.activate(
                VexMapsDistributionModeFamily.Control.UseDistribution,
            )

        assertNull(
            redundantDirectionalRequest,
        )
        assertSame(
            VexMapsDistributionModeFamily.State.Distribution,
            family.currentState,
        )

        family.activate(
            VexMapsDistributionModeFamily.Control.ToggleDistribution,
        )

        assertSame(
            VexMapsDistributionModeFamily.State.Regular,
            family.currentState,
        )
    }
}

// [VXG RealForever][VEXMAPS][DISTRIBUTION][MODE][DIRECT-FAMILY]
