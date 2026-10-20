package com.vextreme.vexlife.android.data.maps

import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InMemoryVexMapsRepositoryTest {
    @Test
    fun locationAndAppearanceRemainIndependentDurableQuestions() = runTest {
        val repository =
            InMemoryVexMapsRepository()

        repository.saveAppearancePreference(
            VexMapsAppearancePreference.LIGHT,
        )
        repository.saveLastLocation(
            VexMapsStoredLocation(
                latitude = 34.5,
                longitude = -118.1,
                accuracyMeters = 8f,
                capturedAt =
                    Instant.parse(
                        "2026-10-13T05:00:00Z",
                    ),
            ),
        )

        val state =
            repository.load()

        assertEquals(
            VexMapsAppearancePreference.LIGHT,
            state.appearancePreference,
        )
        val storedLocation =
            requireNotNull(
                state.lastLocation,
            )
        assertEquals(
            34.5,
            storedLocation.latitude,
            0.0,
        )
        assertEquals(
            -118.1,
            storedLocation.longitude,
            0.0,
        )
        assertEquals(
            8f,
            requireNotNull(
                storedLocation.accuracyMeters,
            ),
            0f,
        )
    }

    @Test
    fun emptyRepositoryDefaultsToAutomaticAppearanceWithoutInventingLocation() = runTest {
        val state =
            InMemoryVexMapsRepository()
                .load()

        assertNull(
            state.lastLocation,
        )
        assertEquals(
            VexMapsAppearancePreference.AUTOMATIC,
            state.appearancePreference,
        )
    }
}

// [VXG RealForever]
