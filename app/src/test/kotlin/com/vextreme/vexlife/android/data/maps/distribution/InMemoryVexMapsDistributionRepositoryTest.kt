package com.vextreme.vexlife.android.data.maps.distribution

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InMemoryVexMapsDistributionRepositoryTest {
    @Test
    fun draftRevisionAdvancesOnlyWhenMeaningChanges() =
        runBlocking {
            val repository =
                InMemoryVexMapsDistributionRepository()

            val initial =
                repository.loadDraft()
            val first =
                repository.addStop(
                    placeRef =
                        "place.alpha",
                    name =
                        "Alpha",
                    subtitle =
                        null,
                    latitude =
                        34.0,
                    longitude =
                        -118.0,
                )
            val unchangedPolicy =
                repository.setReturnToStart(
                    true,
                )
            val changedPolicy =
                repository.setReturnToStart(
                    false,
                )

            assertEquals(
                initial.revision + 1L,
                first.revision,
            )
            assertEquals(
                first.revision,
                unchangedPolicy.revision,
            )
            assertEquals(
                first.revision + 1L,
                changedPolicy.revision,
            )
        }

    @Test
    fun removingAStopPreservesOriginalInputOrderOfRemainingObligations() =
        runBlocking {
            val repository =
                InMemoryVexMapsDistributionRepository()

            val first =
                repository.addStop(
                    placeRef = "place.alpha",
                    name = "Alpha",
                    subtitle = null,
                    latitude = 34.0,
                    longitude = -118.0,
                )
            val second =
                repository.addStop(
                    placeRef = "place.beta",
                    name = "Beta",
                    subtitle = null,
                    latitude = 34.1,
                    longitude = -118.1,
                )
            val third =
                repository.addStop(
                    placeRef = "place.gamma",
                    name = "Gamma",
                    subtitle = null,
                    latitude = 34.2,
                    longitude = -118.2,
                )

            val removed =
                repository.removeStop(
                    second.stops.single {
                        it.placeRef ==
                            "place.beta"
                    }.stopRef,
                )

            assertEquals(
                listOf(
                    "place.alpha",
                    "place.gamma",
                ),
                removed.stops.map {
                    it.placeRef
                },
            )
            assertEquals(
                listOf(
                    0,
                    2,
                ),
                removed.stops.map {
                    it.inputOrder
                },
            )
            assertNotEquals(
                first.stops.first().stopRef,
                third.stops.last().stopRef,
            )
            assertTrue(
                removed.revision >
                    second.revision,
            )
        }
}

// [VXG RealForever]
