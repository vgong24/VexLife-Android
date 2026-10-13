package com.vextreme.vexlife.android.screens.shared.maps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VexRouteGeometryMatcherTest {
    private val geometry =
        listOf(
            VexRouteGeometryPoint(
                34.0,
                -118.0,
            ),
            VexRouteGeometryPoint(
                34.0,
                -117.999,
            ),
            VexRouteGeometryPoint(
                34.001,
                -117.999,
            ),
        )

    private val route =
        VexNavigationRoute(
            routeRef = "route.test",
            routeRevision = 3L,
            destinationLabel = "Test",
            totalDistanceMeters =
                cumulativeGeometryMeters(
                    geometry,
                ).last(),
            maneuvers = emptyList(),
        )

    @Test
    fun matchingFixProjectsOntoSameRouteDistanceAxis() {
        val matcher =
            VexRouteGeometryMatcher(
                route = route,
                geometry = geometry,
            )

        val result =
            matcher.match(
                sample =
                    VexNavigationLocationSample(
                        sampleRef = "fix.1",
                        latitude = 34.0,
                        longitude = -117.9995,
                        accuracyMeters = 5f,
                        elapsedRealtimeMillis =
                            1_000L,
                    ),
                nowElapsedRealtimeMillis =
                    1_100L,
            )

        val matched =
            result as
                VexRouteMatchResult.Matched

        val firstSegmentMeters =
            cumulativeGeometryMeters(
                geometry,
            )[1]

        assertTrue(
            matched
                .navigationFix
                .distanceAlongRouteMeters in
                (firstSegmentMeters / 3)..
                    (firstSegmentMeters * 2 / 3),
        )
        assertTrue(
            matched.distanceFromRouteMeters <
                2.0,
        )
    }

    @Test
    fun staleAndOutOfOrderSamplesCannotOverwriteCurrentProgress() {
        val matcher =
            VexRouteGeometryMatcher(
                route = route,
                geometry = geometry,
                policy =
                    VexRouteMatchingPolicy(
                        maximumFixAgeMillis =
                            1_000L,
                    ),
            )

        assertEquals(
            VexRouteMatchResult.Stale,
            matcher.match(
                sample =
                    sample(
                        ref = "fix.stale",
                        elapsed = 1_000L,
                    ),
                nowElapsedRealtimeMillis =
                    2_500L,
            ),
        )

        val current =
            matcher.match(
                sample =
                    sample(
                        ref = "fix.current",
                        elapsed = 3_000L,
                    ),
                nowElapsedRealtimeMillis =
                    3_100L,
            )
        assertTrue(
            current is
                VexRouteMatchResult.Matched,
        )

        assertEquals(
            VexRouteMatchResult.OutOfOrder,
            matcher.match(
                sample =
                    sample(
                        ref = "fix.old",
                        elapsed = 2_900L,
                    ),
                nowElapsedRealtimeMillis =
                    3_200L,
            ),
        )
    }

    @Test
    fun inaccurateFixIsHeldBeforeOffRouteClassification() {
        val matcher =
            VexRouteGeometryMatcher(
                route = route,
                geometry = geometry,
                policy =
                    VexRouteMatchingPolicy(
                        maximumAccuracyMeters =
                            30f,
                    ),
            )

        assertEquals(
            VexRouteMatchResult.Inaccurate(
                80f,
            ),
            matcher.match(
                sample =
                    VexNavigationLocationSample(
                        sampleRef =
                            "fix.inaccurate",
                        latitude = 34.0,
                        longitude = -117.9995,
                        accuracyMeters = 80f,
                        elapsedRealtimeMillis =
                            1_000L,
                    ),
                nowElapsedRealtimeMillis =
                    1_050L,
            ),
        )
    }

    @Test
    fun distantFixBecomesOffRouteRatherThanFakeProgress() {
        val matcher =
            VexRouteGeometryMatcher(
                route = route,
                geometry = geometry,
            )

        val result =
            matcher.match(
                sample =
                    VexNavigationLocationSample(
                        sampleRef =
                            "fix.off-route",
                        latitude = 34.01,
                        longitude = -117.99,
                        accuracyMeters = 5f,
                        elapsedRealtimeMillis =
                            1_000L,
                    ),
                nowElapsedRealtimeMillis =
                    1_100L,
            )

        assertTrue(
            result is
                VexRouteMatchResult.OffRoute,
        )
    }

    private fun sample(
        ref: String,
        elapsed: Long,
    ) =
        VexNavigationLocationSample(
            sampleRef = ref,
            latitude = 34.0,
            longitude = -117.9995,
            accuracyMeters = 5f,
            elapsedRealtimeMillis =
                elapsed,
        )
}

// [VXG RealForever]
