package com.vextreme.vexlife.android.screens.shared.maps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class VexNavigationSessionTest {
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

    private val routeResult =
        VexOfflineRouteResult(
            route =
                VexNavigationRoute(
                    routeRef =
                        "route.session",
                    routeRevision = 1L,
                    destinationLabel = "Home",
                    totalDistanceMeters =
                        cumulativeGeometryMeters(
                            geometry,
                        ).last(),
                    maneuvers =
                        listOf(
                            VexNavigationManeuver(
                                maneuverRef =
                                    "turn.1",
                                instructionProjection =
                                    "Turn left",
                                distanceFromRouteStartMeters =
                                    cumulativeGeometryMeters(
                                        geometry,
                                    )[1],
                            ),
                        ),
                ),
            geometry = geometry,
        )

    @Test
    fun preparedSessionDoesNotNavigateUntilExplicitStart() {
        val session =
            VexNavigationSession()

        session.prepare(
            routeResult,
        )

        assertEquals(
            VexNavigationSessionPhase.READY,
            session.current.phase,
        )
        assertEquals(
            VexNavigationConvergenceAvailability.ROUTE_PREVIEW,
            session.current
                .convergence
                ?.availability,
        )

        session.acceptLocation(
            sample =
                sample(
                    ref = "before.start",
                    longitude =
                        -117.9995,
                    elapsed = 1_000L,
                ),
            nowElapsedRealtimeMillis =
                1_100L,
        )

        assertEquals(
            VexNavigationSessionPhase.READY,
            session.current.phase,
        )
    }

    @Test
    fun activeSessionTurnsMatchedCoordinatesIntoConvergenceProgress() {
        val session =
            VexNavigationSession()
        session.prepare(routeResult)
        session.start()

        session.acceptLocation(
            sample =
                sample(
                    ref = "active.1",
                    longitude =
                        -117.9995,
                    elapsed = 1_000L,
                ),
            nowElapsedRealtimeMillis =
                1_050L,
        )

        assertEquals(
            VexNavigationSessionPhase.ACTIVE,
            session.current.phase,
        )
        assertEquals(
            VexNavigationLocationDisposition.MATCHED,
            session.current
                .locationDisposition,
        )
        assertNotNull(
            session.current
                .convergence
                ?.progress,
        )
    }

    @Test
    fun offRouteCanRecoverWhenAUsableFixReturns() {
        val session =
            VexNavigationSession()
        session.prepare(routeResult)
        session.start()

        session.acceptLocation(
            sample =
                VexNavigationLocationSample(
                    sampleRef = "off.route",
                    latitude = 34.02,
                    longitude = -117.98,
                    accuracyMeters = 5f,
                    elapsedRealtimeMillis =
                        1_000L,
                ),
            nowElapsedRealtimeMillis =
                1_050L,
        )

        assertEquals(
            VexNavigationSessionPhase.OFF_ROUTE,
            session.current.phase,
        )

        session.acceptLocation(
            sample =
                sample(
                    ref = "back.on.route",
                    longitude =
                        -117.9995,
                    elapsed = 2_000L,
                ),
            nowElapsedRealtimeMillis =
                2_050L,
        )

        assertEquals(
            VexNavigationSessionPhase.ACTIVE,
            session.current.phase,
        )
    }

    @Test
    fun offRouteCreatesOneRerouteRequestAndCooldownPreventsSpam() {
        val session =
            VexNavigationSession(
                rerouteCooldownMillis =
                    10_000L,
            )
        session.prepare(routeResult)
        session.start()

        session.acceptLocation(
            sample =
                VexNavigationLocationSample(
                    sampleRef = "off.1",
                    latitude = 34.02,
                    longitude = -117.98,
                    accuracyMeters = 5f,
                    elapsedRealtimeMillis =
                        1_000L,
                ),
            nowElapsedRealtimeMillis =
                1_050L,
        )

        val first =
            requireNotNull(
                session.current
                    .pendingReroute,
            )

        assertEquals(
            routeResult
                .route
                .routeRevision,
            first.previousRouteRevision,
        )
        assertEquals(
            VexMapCoordinate(
                latitude = 34.02,
                longitude = -117.98,
            ),
            first.origin,
        )

        session.rerouteFailed(
            first.requestRef,
        )

        session.acceptLocation(
            sample =
                VexNavigationLocationSample(
                    sampleRef = "off.2",
                    latitude = 34.021,
                    longitude = -117.981,
                    accuracyMeters = 5f,
                    elapsedRealtimeMillis =
                        5_000L,
                ),
            nowElapsedRealtimeMillis =
                5_050L,
        )

        assertEquals(
            null,
            session.current
                .pendingReroute,
        )

        session.acceptLocation(
            sample =
                VexNavigationLocationSample(
                    sampleRef = "off.3",
                    latitude = 34.022,
                    longitude = -117.982,
                    accuracyMeters = 5f,
                    elapsedRealtimeMillis =
                        12_000L,
                ),
            nowElapsedRealtimeMillis =
                12_050L,
        )

        assertNotNull(
            session.current
                .pendingReroute,
        )
    }

    @Test
    fun offRouteReroutePreservesTravelMode() {
        val session =
            VexNavigationSession()
        val walkingRoute =
            routeResult.copy(
                travelMode =
                    VexMapsTravelMode.WALK,
            )

        session.prepare(
            walkingRoute,
        )
        session.start()

        session.acceptLocation(
            sample =
                VexNavigationLocationSample(
                    sampleRef =
                        "walking.off.route",
                    latitude = 34.02,
                    longitude = -117.98,
                    accuracyMeters = 5f,
                    elapsedRealtimeMillis =
                        1_000L,
                ),
            nowElapsedRealtimeMillis =
                1_050L,
        )

        assertEquals(
            VexMapsTravelMode.WALK,
            session.current
                .pendingReroute
                ?.travelMode,
        )
    }

    @Test
    fun activeRouteReplacementResetsOldRouteProgress() {
        val session =
            VexNavigationSession()
        session.prepare(routeResult)
        session.start()

        session.acceptLocation(
            sample =
                VexNavigationLocationSample(
                    sampleRef = "off.route",
                    latitude = 34.02,
                    longitude = -117.98,
                    accuracyMeters = 5f,
                    elapsedRealtimeMillis =
                        1_000L,
                ),
            nowElapsedRealtimeMillis =
                1_050L,
        )

        val rerouted =
            routeResult.copy(
                route =
                    routeResult.route.copy(
                        routeRevision = 2L,
                    ),
            )

        session.replaceActiveRoute(
            rerouted,
        )

        assertEquals(
            VexNavigationSessionPhase.ACTIVE,
            session.current.phase,
        )
        assertEquals(
            2L,
            session.current
                .routeResult
                ?.route
                ?.routeRevision,
        )
        assertEquals(
            null,
            session.current
                .pendingReroute,
        )
        assertEquals(
            VexNavigationConvergenceAvailability.ROUTE_PREVIEW,
            session.current
                .convergence
                ?.availability,
        )
    }

    @Test
    fun stopReleasesRouteAndReturnsToIdle() {
        val session =
            VexNavigationSession()
        session.prepare(routeResult)
        session.start()

        session.stop()

        assertEquals(
            VexNavigationSessionPhase.IDLE,
            session.current.phase,
        )
        assertEquals(
            null,
            session.current.routeResult,
        )
    }

    private fun sample(
        ref: String,
        longitude: Double,
        elapsed: Long,
    ) =
        VexNavigationLocationSample(
            sampleRef = ref,
            latitude = 34.0,
            longitude = longitude,
            accuracyMeters = 5f,
            elapsedRealtimeMillis =
                elapsed,
        )
}

// [VXG RealForever]
