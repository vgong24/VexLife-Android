package com.vextreme.vexlife.android.screens.shared.maps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VexMapsPlanningDraftRestoreTest {
    @Test
    fun savedCoordinateRestoresTypedMapCoordinate() {
        assertEquals(
            VexMapCoordinate(
                latitude = 34.5794,
                longitude = -118.1165,
            ),
            restoreVexMapCoordinate(
                latitude = 34.5794,
                longitude = -118.1165,
            ),
        )
    }

    @Test
    fun partialCoordinateDoesNotInventPlanningIntent() {
        assertNull(
            restoreVexMapCoordinate(
                latitude = 34.5794,
                longitude = null,
            ),
        )
    }

    @Test
    fun savedSelectionRoleRestoresOrFallsBackToDestination() {
        assertEquals(
            VexMapsMapSelectionTarget.ORIGIN,
            restoreVexMapsMapSelectionTarget(
                "ORIGIN",
            ),
        )
        assertEquals(
            VexMapsMapSelectionTarget.DESTINATION,
            restoreVexMapsMapSelectionTarget(
                "unknown",
            ),
        )
    }

    @Test
    fun savedTravelModeRestoresOrFallsBackToDrive() {
        assertEquals(
            VexMapsTravelMode.WALK,
            restoreVexMapsTravelMode(
                "WALK",
            ),
        )
        assertEquals(
            VexMapsTravelMode.DRIVE,
            restoreVexMapsTravelMode(
                "unknown",
            ),
        )
    }
}

// [VXG RealForever]
