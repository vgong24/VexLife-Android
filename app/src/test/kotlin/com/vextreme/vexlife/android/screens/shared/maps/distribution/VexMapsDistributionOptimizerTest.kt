package com.vextreme.vexlife.android.screens.shared.maps.distribution

import com.vextreme.vexlife.android.data.maps.distribution.VexMapsDistributionDraft
import com.vextreme.vexlife.android.data.maps.distribution.VexMapsDistributionStop
import com.vextreme.vexlife.android.screens.shared.maps.VexMapCoordinate
import com.vextreme.vexlife.android.screens.shared.maps.VexMapsTravelMode
import com.vextreme.vexlife.android.screens.shared.maps.distribution.planning.VexMapsDistributionLegCost
import com.vextreme.vexlife.android.screens.shared.maps.distribution.planning.VexMapsDistributionLegCostProvider
import com.vextreme.vexlife.android.screens.shared.maps.distribution.planning.VexMapsDistributionOptimizer
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

    @Test
    fun planPreservesDraftTruthWhileOrderingByProviderTravelTime() =
        runBlocking {
            val alpha =
                stop(
                    "alpha",
                    longitude = 1.0,
                    inputOrder = 0,
                )
            val beta =
                stop(
                    "beta",
                    longitude = 2.0,
                    inputOrder = 1,
                )
            val gamma =
                stop(
                    "gamma",
                    longitude = 3.0,
                    inputOrder = 2,
                )
            val draft =
                VexMapsDistributionDraft(
                    revision = 7L,
                    returnToStart = false,
                    stops =
                        listOf(
                            alpha,
                            beta,
                            gamma,
                        ),
                )

            val provider =
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

            val plan =
                VexMapsDistributionOptimizer(
                    provider,
                ).optimize(
                    draft = draft,
                    origin = origin,
                    travelMode =
                        VexMapsTravelMode.DRIVE,
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
                    draft,
                ),
            )
        }

    @Test
    fun planCurrentnessIsBoundToDraftRevision() =
        runBlocking {
            val draft =
                VexMapsDistributionDraft(
                    revision = 2L,
                    stops =
                        listOf(
                            stop(
                                "alpha",
                                longitude = 1.0,
                                inputOrder = 0,
                            ),
                        ),
                )
            val provider =
                VexMapsDistributionLegCostProvider {
                        _,
                        _,
                        _,
                        ->
                    VexMapsDistributionLegCost(
                        durationSeconds =
                            10L,
                    )
                }
            val plan =
                VexMapsDistributionOptimizer(
                    provider,
                ).optimize(
                    draft = draft,
                    origin = origin,
                    travelMode =
                        VexMapsTravelMode.DRIVE,
                )

            assertFalse(
                plan.isCurrentFor(
                    draft.copy(
                        revision = 3L,
                    ),
                ),
            )
        }
}

// [VXG RealForever]
