package com.vextreme.vexlife.android.screens.shared.maps.distribution

import com.vextreme.vexlife.android.data.maps.distribution.VexMapsDistributionDraft
import com.vextreme.vexlife.android.data.maps.distribution.VexMapsDistributionStop
import com.vextreme.vexlife.android.screens.shared.maps.VexMapCoordinate
import com.vextreme.vexlife.android.screens.shared.maps.VexMapsTravelMode
import com.vextreme.vexlife.android.screens.shared.maps.distribution.planning.VexMapsDistributionLegCost
import com.vextreme.vexlife.android.screens.shared.maps.distribution.planning.VexMapsDistributionLegCostProvider
import com.vextreme.vexlife.android.screens.shared.maps.distribution.planning.VexMapsDistributionOptimizer
import com.vextreme.vexlife.android.screens.shared.maps.distribution.planning.VexMapsDistributionPreparationSnapshot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VexMapsDistributionOptimizerTest {
    private val origin =
        VexMapCoordinate(
            latitude = 0.0,
            longitude = 0.0,
        )

    private fun stop(
        ref: String,
        longitude: Double,
        inputOrder: Int,
    ) =
        VexMapsDistributionStop(
            stopRef = ref,
            placeRef = ref,
            name = ref,
            subtitle = null,
            latitude = 0.0,
            longitude = longitude,
            inputOrder = inputOrder,
        )

    private fun preparation(
        draft: VexMapsDistributionDraft,
        origin:
            VexMapCoordinate =
            this.origin,
        travelMode:
            VexMapsTravelMode =
            VexMapsTravelMode.DRIVE,
        regionPackIdentity:
            String =
            "pack.a",
    ) =
        VexMapsDistributionPreparationSnapshot(
            preparationRef =
                "preparation." +
                    draft.revision,
            draftRevision =
                draft.revision,
            stopRefs =
                draft.stops
                    .sortedBy {
                        it.inputOrder
                    }
                    .map {
                        it.stopRef
                    },
            returnToStart =
                draft.returnToStart,
            origin =
                origin,
            travelMode =
                travelMode,
            regionPackIdentity =
                regionPackIdentity,
        )

    private val provider =
        VexMapsDistributionLegCostProvider {
                from,
                to,
                _,
                ->
            val key =
                from.longitude to
                    to.longitude
            val seconds =
                when (key) {
                    0.0 to 1.0 -> 30L
                    0.0 to 2.0 -> 10L
                    0.0 to 3.0 -> 20L
                    2.0 to 1.0 -> 10L
                    2.0 to 3.0 -> 50L
                    1.0 to 3.0 -> 10L
                    3.0 to 1.0 -> 50L
                    3.0 to 2.0 -> 10L
                    1.0 to 2.0 -> 10L
                    else -> 100L
                }

            VexMapsDistributionLegCost(
                durationSeconds =
                    seconds,
            )
        }

    @Test
    fun planPreservesDraftTruthWhileOrderingByProviderTravelTime() =
        runBlocking {
            val draft =
                VexMapsDistributionDraft(
                    revision = 7L,
                    returnToStart = false,
                    stops =
                        listOf(
                            stop(
                                "alpha",
                                1.0,
                                0,
                            ),
                            stop(
                                "beta",
                                2.0,
                                1,
                            ),
                            stop(
                                "gamma",
                                3.0,
                                2,
                            ),
                        ),
                )
            val snapshot =
                preparation(
                    draft,
                )

            val plan =
                VexMapsDistributionOptimizer(
                    provider,
                ).optimize(
                    draft =
                        draft,
                    preparation =
                        snapshot,
                )

            assertEquals(
                listOf(
                    "beta",
                    "alpha",
                    "gamma",
                ),
                plan.orderedStops.map {
                    it.stopRef
                },
            )
            assertEquals(
                listOf(
                    "alpha",
                    "beta",
                    "gamma",
                ),
                draft.stops.map {
                    it.stopRef
                },
            )
            assertTrue(
                plan.isCurrentFor(
                    draft =
                        draft,
                    origin =
                        origin,
                    travelMode =
                        VexMapsTravelMode.DRIVE,
                    regionPackIdentity =
                        "pack.a",
                ),
            )
        }

    @Test
    fun planCurrentnessIncludesEveryMaterialPreparationInput() =
        runBlocking {
            val draft =
                VexMapsDistributionDraft(
                    revision = 2L,
                    stops =
                        listOf(
                            stop(
                                "alpha",
                                1.0,
                                0,
                            ),
                        ),
                )
            val plan =
                VexMapsDistributionOptimizer(
                    provider,
                ).optimize(
                    draft =
                        draft,
                    preparation =
                        preparation(
                            draft,
                        ),
                )

            assertFalse(
                plan.isCurrentFor(
                    draft =
                        draft.copy(
                            revision = 3L,
                        ),
                    origin =
                        origin,
                    travelMode =
                        VexMapsTravelMode.DRIVE,
                    regionPackIdentity =
                        "pack.a",
                ),
            )
            assertFalse(
                plan.isCurrentFor(
                    draft =
                        draft,
                    origin =
                        VexMapCoordinate(
                            0.0,
                            0.5,
                        ),
                    travelMode =
                        VexMapsTravelMode.DRIVE,
                    regionPackIdentity =
                        "pack.a",
                ),
            )
            assertFalse(
                plan.isCurrentFor(
                    draft =
                        draft,
                    origin =
                        origin,
                    travelMode =
                        VexMapsTravelMode.WALK,
                    regionPackIdentity =
                        "pack.a",
                ),
            )
            assertFalse(
                plan.isCurrentFor(
                    draft =
                        draft,
                    origin =
                        origin,
                    travelMode =
                        VexMapsTravelMode.DRIVE,
                    regionPackIdentity =
                        "pack.b",
                ),
            )
        }
}

// [VXG RealForever]
