package com.vextreme.vexlife.android.screens.shared.maps

import org.junit.Assert.assertEquals
import org.junit.Test

class VexMapsPlaceSelectionTest {
    @Test
    fun choosingStartKeepsDestinationAsTheNextSelectionRole() {
        assertEquals(
            VexMapsMapSelectionTarget.DESTINATION,
            nextVexMapsSelectionTarget(
                VexMapsMapSelectionTarget.ORIGIN,
            ),
        )
    }

    @Test
    fun choosingDestinationKeepsDestinationSelectionStable() {
        assertEquals(
            VexMapsMapSelectionTarget.DESTINATION,
            nextVexMapsSelectionTarget(
                VexMapsMapSelectionTarget.DESTINATION,
            ),
        )
    }
}

// [VXG RealForever]
