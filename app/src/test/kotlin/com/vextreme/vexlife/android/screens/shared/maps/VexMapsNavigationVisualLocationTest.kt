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

    @Test
    fun newerActiveSampleAdvancesVisualLocation() {
        val current =
            VexMapsNavigationVisualLocation(
                sampleRef = "sample.visual.1",
                coordinate =
                    VexMapCoordinate(
                        latitude = 34.5794,
                        longitude = -118.1165,
                    ),
                accuracyMeters = 7f,
                elapsedRealtimeMillis = 5_000L,
            )
        val newer =
            VexNavigationLocationSample(
                sampleRef = "sample.visual.2",
                latitude = 34.58,
                longitude = -118.12,
                accuracyMeters = 9f,
                elapsedRealtimeMillis = 6_000L,
            )

        assertEquals(
            newer.toVexMapsNavigationVisualLocation(),
            advanceVexMapsNavigationVisualLocation(
                current = current,
                sample = newer,
                phase =
                    VexNavigationSessionPhase.ACTIVE,
            ),
        )
    }

    @Test
    fun outOfOrderSampleCannotMoveVisualLocationBackward() {
        val current =
            VexMapsNavigationVisualLocation(
                sampleRef = "sample.visual.current",
                coordinate =
                    VexMapCoordinate(
                        latitude = 34.58,
                        longitude = -118.12,
                    ),
                accuracyMeters = 6f,
                elapsedRealtimeMillis = 6_000L,
            )
        val older =
            VexNavigationLocationSample(
                sampleRef = "sample.visual.older",
                latitude = 34.57,
                longitude = -118.11,
                accuracyMeters = 5f,
                elapsedRealtimeMillis = 5_000L,
            )

        assertEquals(
            current,
            advanceVexMapsNavigationVisualLocation(
                current = current,
                sample = older,
                phase =
                    VexNavigationSessionPhase.ACTIVE,
            ),
        )
    }

    @Test
    fun nonActiveSessionCannotAdvanceVisualLocation() {
        val sample =
            VexNavigationLocationSample(
                sampleRef = "sample.visual.after-arrival",
                latitude = 34.58,
                longitude = -118.12,
                accuracyMeters = 5f,
                elapsedRealtimeMillis = 7_000L,
            )

        assertEquals(
            null,
            advanceVexMapsNavigationVisualLocation(
                current = null,
                sample = sample,
                phase =
                    VexNavigationSessionPhase.ARRIVED,
            ),
        )
    }
}

// [VXG RealForever]
