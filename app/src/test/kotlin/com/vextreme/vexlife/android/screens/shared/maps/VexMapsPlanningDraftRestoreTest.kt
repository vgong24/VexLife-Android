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
    fun completeViewportRestoresTypedSnapshot() {
        assertEquals(
            VexMapsViewportSnapshot(
                center =
                    VexMapCoordinate(
                        latitude = 34.5794,
                        longitude = -118.1165,
                    ),
                zoom = 13.5,
                bearing = 42.0,
                tilt = 30.0,
            ),
            restoreVexMapsViewportSnapshot(
                latitude = 34.5794,
                longitude = -118.1165,
                zoom = 13.5,
                bearing = 42.0,
                tilt = 30.0,
            ),
        )
    }

    @Test
    fun partialViewportDoesNotInventPresentationState() {
        assertNull(
            restoreVexMapsViewportSnapshot(
                latitude = 34.5794,
                longitude = -118.1165,
                zoom = null,
                bearing = 42.0,
                tilt = 30.0,
            ),
        )
    }

    @Test
    fun invalidViewportDoesNotRestore() {
        assertNull(
            restoreVexMapsViewportSnapshot(
                latitude = 34.5794,
                longitude = -118.1165,
                zoom = 13.5,
                bearing = 42.0,
                tilt = 70.0,
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
