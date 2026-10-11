package com.vextreme.vexlife.android.screens.shared.maps

import org.junit.Assert.assertEquals
import org.junit.Test

class VexMapsNavigationVisualLocationTest {
    @Test
    fun rawDeviceSampleProjectsOnlyToMapVisualLocation() {
        val sample =
            VexNavigationLocationSample(
                sampleRef = "sample.visual.1",
                latitude = 34.5794,
                longitude = -118.1165,
                accuracyMeters = 7f,
                elapsedRealtimeMillis = 5_000L,
            )

        assertEquals(
            VexMapsNavigationVisualLocation(
                sampleRef = "sample.visual.1",
                coordinate =
                    VexMapCoordinate(
                        latitude = 34.5794,
                        longitude = -118.1165,
                    ),
                accuracyMeters = 7f,
                elapsedRealtimeMillis = 5_000L,
            ),
            sample
                .toVexMapsNavigationVisualLocation(),
        )
    }
}

// [VXG RealForever]
