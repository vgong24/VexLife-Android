package com.vextreme.vexlife.android.screens.shared.maps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VexMapsDeviceLocationProjectionTest {
    @Test
    fun preciseLocatedResultRemainsRenderable() {
        val located =
            VexDeviceLocationResult.Located(
                latitude = 34.5794,
                longitude = -118.1165,
                accuracyMeters = 8f,
            )

        assertEquals(
            located,
            projectVexMapsDeviceLocation(
                located,
            ),
        )
    }

    @Test
    fun activeNavigationVisualLocationWinsMapProjection() {
        val planner =
            VexDeviceLocationResult.Located(
                latitude = 34.5794,
                longitude = -118.1165,
                accuracyMeters = 8f,
            )
        val visual =
            VexMapsNavigationVisualLocation(
                sampleRef = "sample.active",
                coordinate =
                    VexMapCoordinate(
                        latitude = 34.6,
                        longitude = -118.2,
                    ),
                accuracyMeters = 5f,
                elapsedRealtimeMillis = 12_000L,
            )

        assertEquals(
            VexDeviceLocationResult.Located(
                latitude = 34.6,
                longitude = -118.2,
                accuracyMeters = 5f,
            ),
            projectVexMapsMapDeviceLocation(
                plannerLocation = planner,
                navigationVisualLocation = visual,
            ),
        )
    }

    @Test
    fun plannerLocationReturnsAfterReadyOnlyProjection() {
        val planner =
            VexDeviceLocationResult.Located(
                latitude = 34.5794,
                longitude = -118.1165,
                accuracyMeters = 8f,
            )

        assertEquals(
            planner,
            projectVexMapsMapDeviceLocation(
                plannerLocation = planner,
                navigationVisualLocation = null,
            ),
        )
        assertNull(
            projectVexMapsMapDeviceLocation(
                plannerLocation = null,
                navigationVisualLocation = null,
            ),
        )
    }

    @Test
    fun unusablePreciseResultsDoNotRemainRenderable() {
        listOf(
            VexDeviceLocationResult.PermissionMissing,
            VexDeviceLocationResult.ApproximateOnly,
            VexDeviceLocationResult.LocationDisabled,
            VexDeviceLocationResult.Unavailable,
        ).forEach { result ->
            assertNull(
                projectVexMapsDeviceLocation(
                    result,
                ),
            )
        }
    }
}

// [VXG RealForever]
