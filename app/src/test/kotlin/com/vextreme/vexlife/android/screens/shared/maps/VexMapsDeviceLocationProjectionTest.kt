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
