package com.vextreme.vexlife.android.screens.shared.maps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** The Navigation glance reads real [VexNavigationSession] truth and never invents a live ETA. */
class VexNavigationGlanceTest {
    private val geometry =
        listOf(
            VexRouteGeometryPoint(34.0, -118.0),
            VexRouteGeometryPoint(34.0, -117.999),
            VexRouteGeometryPoint(34.001, -117.999),
        )

    private val routeResult =
        VexOfflineRouteResult(
            route =
                VexNavigationRoute(
                    routeRef = "route.glance",
                    routeRevision = 1L,
                    destinationLabel = "Sam's place",
                    totalDistanceMeters =
                        cumulativeGeometryMeters(geometry).last(),
                    maneuvers =
                        listOf(
                            VexNavigationManeuver(
                                maneuverRef = "turn.1",
                                instructionProjection = "Turn left onto Main St",
                                distanceFromRouteStartMeters =
                                    cumulativeGeometryMeters(geometry)[1],
                            ),
                        ),
                ),
            geometry = geometry,
            estimatedDurationSeconds = 22 * 60,
        )

    @Test
    fun idleSessionHasNoGlance() {
        assertNull(
            VexNavigationSession().current.toVexNavigationGlance(),
        )
    }

    @Test
    fun preparedRouteIsReadyWithDestinationButNoInstructionYet() {
        val session =
            VexNavigationSession()
        session.prepare(routeResult)

        val glance =
            requireNotNull(
                session.current.toVexNavigationGlance(),
            )

        assertEquals(VexNavigationSessionPhase.READY, glance.phase)
        assertEquals("Sam's place", glance.destinationLabel)
        assertEquals(22 * 60, glance.plannedDurationSeconds)
    }

    @Test
    fun activeMatchedSessionProjectsNextManeuverAndRemainingDistance() {
        val session =
            VexNavigationSession()
        session.prepare(routeResult)
        session.start()
        session.acceptLocation(
            sample =
                VexNavigationLocationSample(
                    sampleRef = "glance.1",
                    latitude = 34.0,
                    longitude = -117.9995,
                    accuracyMeters = 5f,
                    elapsedRealtimeMillis = 1_000L,
                ),
            nowElapsedRealtimeMillis = 1_050L,
        )

        val glance =
            requireNotNull(
                session.current.toVexNavigationGlance(),
            )
        val progress =
            requireNotNull(
                session.current.convergence?.progress,
            )

        assertEquals(VexNavigationSessionPhase.ACTIVE, glance.phase)
        assertEquals("Turn left onto Main St", glance.nextInstruction)
        assertEquals(progress.distanceRemainingMeters, glance.distanceRemainingMeters)
        assertNotNull(glance.distanceToNextManeuverMeters)
        // Still the planning-time estimate: the glance never claims a live time-remaining figure.
        assertEquals(22 * 60, glance.plannedDurationSeconds)
    }

    @Test
    fun stoppedSessionHasNoGlance() {
        val session =
            VexNavigationSession()
        session.prepare(routeResult)
        session.start()
        session.stop()

        assertNull(
            session.current.toVexNavigationGlance(),
        )
    }
}

// [VXG RealForever][VEXMAPS][NAVIGATION][GLANCE][PROOF]
