package com.vextreme.vexlife.android.data.maps

import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InMemoryVexMapsRepositoryTest {
    @Test
    fun locationAndStyleRemainIndependentDurableQuestions() = runTest {
        val repository =
            InMemoryVexMapsRepository()

        repository.saveOnlineStyle(
            "liberty",
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
            "liberty",
            state.onlineStyleRef,
        )
        val location =
            requireNotNull(
                state.lastLocation,
            )

        assertEquals(
            34.5,
            location.latitude,
            0.0,
        )
        assertEquals(
            -118.1,
            location.longitude,
            0.0,
        )
        assertEquals(
            8f,
            requireNotNull(
                location.accuracyMeters,
            ),
            0f,
        )
    }

    @Test
    fun emptyRepositoryDoesNotInventLocationOrStyle() = runTest {
        val state =
            InMemoryVexMapsRepository()
                .load()

        assertNull(
            state.lastLocation,
        )
        assertNull(
            state.onlineStyleRef,
        )
    }
}

// [VXG RealForever]
