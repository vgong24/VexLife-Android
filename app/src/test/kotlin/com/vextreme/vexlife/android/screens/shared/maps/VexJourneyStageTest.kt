package com.vextreme.vexlife.android.screens.shared.maps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The Map stage follows the route-matched position on the route's own geometry, never raw GPS. */
class VexJourneyStageTest {
    private val start = VexRouteGeometryPoint(34.0, -118.0)
    private val north = VexRouteGeometryPoint(34.001, -118.0)
    private val northEast = VexRouteGeometryPoint(34.001, -117.999)
    private val geometry = listOf(start, north, northEast)

    @Test
    fun positionInterpolatesWithinItsSegmentAndFacesAlongIt() {
        val firstLeg = cumulativeGeometryMeters(geometry)[1]

        val halfway = vexRoutePosition(geometry, firstLeg / 2)

        assertEquals(34.0005, halfway.latitude, 0.00002)
        assertEquals(-118.0, halfway.longitude, 1e-9)
        assertEquals(0.0, halfway.bearingDegrees, 0.5)

        val onSecondLeg = vexRoutePosition(geometry, firstLeg + 20)
        assertEquals(90.0, onSecondLeg.bearingDegrees, 0.5)
    }

    @Test
    fun positionIsClampedToTheRouteEnds() {
        val total = cumulativeGeometryMeters(geometry).last()

        val before = vexRoutePosition(geometry, -50)
        val after = vexRoutePosition(geometry, total + 500)

        assertEquals(start.latitude, before.latitude, 1e-9)
        assertEquals(start.longitude, before.longitude, 1e-9)
        assertEquals(northEast.latitude, after.latitude, 1e-9)
        assertEquals(northEast.longitude, after.longitude, 1e-9)
    }

    @Test
    fun aDuplicatedVertexBorrowsTheNextRealHeading() {
        // A zero-length segment's own bearing would be 0 (north); the real next leg heads east.
        val east = VexRouteGeometryPoint(34.0, -117.999)
        val withDuplicate = listOf(start, start, east)

        assertEquals(90.0, vexRoutePosition(withDuplicate, 0).bearingDegrees, 0.5)
    }

    @Test
    fun stageReadsTheSessionsRouteAndRouteMatchedDistance() {
        assertNull(VexNavigationSession().current.toVexJourneyStage())

        val route =
            VexOfflineRouteResult(
                route =
                    VexNavigationRoute(
                        routeRef = "route.stage",
                        routeRevision = 1L,
                        destinationLabel = "Sam's place",
                        totalDistanceMeters = cumulativeGeometryMeters(geometry).last(),
                        maneuvers =
                            listOf(
                                VexNavigationManeuver(
                                    maneuverRef = "turn.1",
                                    instructionProjection = "Turn right",
                                    distanceFromRouteStartMeters = cumulativeGeometryMeters(geometry)[1],
                                ),
                            ),
                    ),
                geometry = geometry,
            )
        val session = VexNavigationSession()
        session.prepare(route)

        val prepared = requireNotNull(session.current.toVexJourneyStage())
        assertEquals(0, prepared.distanceAlongMeters)
        assertEquals(geometry, prepared.geometry)

        session.start()
        session.acceptLocation(
            sample =
                VexNavigationLocationSample(
                    sampleRef = "stage.1",
                    latitude = 34.0005,
                    longitude = -118.0,
                    accuracyMeters = 5f,
                    elapsedRealtimeMillis = 1_000L,
                ),
            nowElapsedRealtimeMillis = 1_050L,
        )

        val active = requireNotNull(session.current.toVexJourneyStage())
        assertEquals(
            requireNotNull(session.current.convergence?.progress).distanceAlongRouteMeters,
            active.distanceAlongMeters,
        )
        assertEquals(0.0, active.position.bearingDegrees, 0.5)
    }
}
