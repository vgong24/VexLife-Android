package com.vextreme.vexlife.android.screens.shared.maps

import org.junit.Assert.assertEquals
import org.junit.Test

class VexValhallaRouteTranslatorTest {
    @Test
    fun segmentTransitionsBecomeStableVexManeuvers() {
        val providerRoute =
            VexValhallaRouteProjection(
                totalDistanceMeters = 1_000,
                segments =
                    listOf(
                        VexValhallaManeuverSegment(
                            instruction =
                                "Drive east",
                            lengthMeters = 300,
                        ),
                        VexValhallaManeuverSegment(
                            instruction =
                                "Turn right onto Main Street",
                            lengthMeters = 500,
                        ),
                        VexValhallaManeuverSegment(
                            instruction =
                                "Arrive at Home",
                            lengthMeters = 200,
                        ),
                    ),
            )

        val route =
            with(VexValhallaRouteTranslator) {
                providerRoute.toNavigationRoute(
                    routeRef = "route.home",
                    routeRevision = 7L,
                    destinationLabel = "Home",
                )
            }

        assertEquals(
            "route.home",
            route.routeRef,
        )
        assertEquals(
            7L,
            route.routeRevision,
        )
        assertEquals(
            1_000,
            route.totalDistanceMeters,
        )
        assertEquals(
            listOf(
                VexNavigationManeuver(
                    maneuverRef =
                        "valhalla.maneuver.1",
                    instructionProjection =
                        "Turn right onto Main Street",
                    distanceFromRouteStartMeters =
                        300,
                ),
                VexNavigationManeuver(
                    maneuverRef =
                        "valhalla.maneuver.2",
                    instructionProjection =
                        "Arrive at Home",
                    distanceFromRouteStartMeters =
                        800,
                ),
            ),
            route.maneuvers,
        )
    }

    @Test
    fun duplicateZeroLengthTransitionsDoNotBreakRouteOrdering() {
        val providerRoute =
            VexValhallaRouteProjection(
                totalDistanceMeters = 500,
                segments =
                    listOf(
                        VexValhallaManeuverSegment(
                            "Start",
                            250,
                        ),
                        VexValhallaManeuverSegment(
                            "Continue",
                            0,
                        ),
                        VexValhallaManeuverSegment(
                            "Turn left",
                            250,
                        ),
                    ),
            )

        val route =
            with(VexValhallaRouteTranslator) {
                providerRoute.toNavigationRoute(
                    routeRef = "route.test",
                    routeRevision = 1L,
                    destinationLabel = "Test",
                )
            }

        assertEquals(
            listOf(250),
            route.maneuvers.map {
                it.distanceFromRouteStartMeters
            },
        )
    }
}

// [VXG RealForever]
