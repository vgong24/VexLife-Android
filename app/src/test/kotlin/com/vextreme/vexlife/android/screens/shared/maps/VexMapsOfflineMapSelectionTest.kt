package com.vextreme.vexlife.android.screens.shared.maps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class VexMapsOfflineMapSelectionTest {
    @Test
    fun preferredCompleteRegionWins() {
        val chosen =
            selectPreferredOfflineMapRegion(
                preferredRegionId = 2L,
                candidates =
                    listOf(
                        candidate(
                            id = 1L,
                            complete = true,
                        ),
                        candidate(
                            id = 2L,
                            complete = true,
                        ),
                    ),
            )

        assertEquals(
            2L,
            chosen?.regionId,
        )
    }

    @Test
    fun newestCompleteRegionWinsWhenPreferenceIsMissing() {
        val chosen =
            selectPreferredOfflineMapRegion(
                preferredRegionId = 99L,
                candidates =
                    listOf(
                        candidate(
                            id = 4L,
                            complete = true,
                        ),
                        candidate(
                            id = 8L,
                            complete = true,
                        ),
                    ),
            )

        assertEquals(
            8L,
            chosen?.regionId,
        )
    }

    @Test
    fun loadingAndErrorRetainTheLastUsableOfflineMap() {
        val ready =
            VexMapsOfflineMapState.Ready(
                preferred =
                    VexMapsOfflineMapRegion(
                        regionId = 7L,
                        styleUri =
                            "https://maps.example/styles/offline",
                        completedResourceCount =
                            100L,
                        requiredResourceCount =
                            100L,
                    ),
                completeRegionCount = 1,
            )

        assertSame(
            ready,
            VexMapsOfflineMapState
                .Loading(
                    retainedReady = ready,
                )
                .effectiveReady(),
        )
        assertSame(
            ready,
            VexMapsOfflineMapState
                .Error(
                    retainedReady = ready,
                )
                .effectiveReady(),
        )
    }

    @Test
    fun initialLoadingAndErrorDoNotInventAnOfflineMap() {
        assertNull(
            VexMapsOfflineMapState
                .Loading()
                .effectiveReady(),
        )
        assertNull(
            VexMapsOfflineMapState
                .Error()
                .effectiveReady(),
        )
    }

    @Test
    fun incompleteOrStylelessRegionsAreNotAdvertisedAsOfflineReady() {
        val chosen =
            selectPreferredOfflineMapRegion(
                preferredRegionId = null,
                candidates =
                    listOf(
                        candidate(
                            id = 1L,
                            complete = false,
                        ),
                        candidate(
                            id = 2L,
                            complete = true,
                            styleUri = null,
                        ),
                    ),
            )

        assertNull(chosen)
    }

    private fun candidate(
        id: Long,
        complete: Boolean,
        styleUri: String? =
            "https://maps.example/styles/vex",
    ) =
        VexMapsOfflineMapCandidate(
            regionId = id,
            styleUri = styleUri,
            isComplete = complete,
            completedResourceCount =
                if (complete) 100L else 50L,
            requiredResourceCount = 100L,
        )
}

// [VXG RealForever]
