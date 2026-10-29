package com.vextreme.vexlife.android.screens.shared.maps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class VexNavigationConvergenceRelayTest {
    private val guidancePolicy =
        VexNavigationGuidancePolicy(
            farWithinMeters = 200,
            approachingWithinMeters = 80,
            turnNowWithinMeters = 20,
        )

    private fun route(
        revision: Long = 1L,
        totalDistanceMeters: Int = 1_000,
    ): VexNavigationRoute =
        VexNavigationRoute(
            routeRef = "route.home",
            routeRevision = revision,
            destinationLabel = "Home",
            totalDistanceMeters = totalDistanceMeters,
            maneuvers =
                listOf(
                    VexNavigationManeuver(
                        maneuverRef = "maneuver.right-main",
                        instructionProjection =
                            "Turn right onto Main Street",
                        distanceFromRouteStartMeters = 300,
                    ),
                    VexNavigationManeuver(
                        maneuverRef = "maneuver.exit-home",
                        instructionProjection =
                            "Take the exit toward Home",
                        distanceFromRouteStartMeters = 800,
                    ),
                ),
        )

    private fun fix(
        distanceAlongRouteMeters: Int,
        revision: Long = 1L,
        fixRef: String =
            "fix.$revision.$distanceAlongRouteMeters",
    ): VexNavigationFix =
        VexNavigationFix(
            fixRef = fixRef,
            routeRef = "route.home",
            routeRevision = revision,
            distanceAlongRouteMeters =
                distanceAlongRouteMeters,
        )

    @Test
    fun routeCanExistBeforeLocationAndGuidance() {
        val relay =
            VexNavigationConvergenceRelay(
                guidancePolicy,
            )

        relay.routeChanged(route())

        assertEquals(
            VexNavigationConvergenceAvailability.ROUTE_PREVIEW,
            relay.currentOutput.availability,
        )
        assertNull(relay.currentOutput.progress)
        assertNull(relay.currentOutput.latestIssuedCue)
        assertEquals(
            emptySet<VexNavigationGuidanceCueKey>(),
            relay.currentOutput.issuedCueKeys,
        )
    }

    @Test
    fun mismatchedFixIsHeldUntilTheMatchingRouteRevisionArrives() {
        val relay =
            VexNavigationConvergenceRelay(
                guidancePolicy,
            )

        relay.routeChanged(
            route(revision = 2L),
        )
        relay.fixChanged(
            fix(
                distanceAlongRouteMeters = 100,
                revision = 1L,
            ),
        )

        assertEquals(
            VexNavigationConvergenceAvailability
                .HELD_FIX_ROUTE_MISMATCH,
            relay.currentOutput.availability,
        )
        assertNull(relay.currentOutput.progress)

        relay.fixChanged(
            fix(
                distanceAlongRouteMeters = 100,
                revision = 2L,
            ),
        )

        assertEquals(
            VexNavigationConvergenceAvailability.NAVIGATING,
            relay.currentOutput.availability,
        )
        assertEquals(
            200,
            relay.currentOutput
                .progress
                ?.nextManeuver
                ?.distanceToManeuverMeters,
        )
    }

    @Test
    fun progressAndGuidanceBandsAreDeterministicAndDeduplicated() {
        val relay =
            VexNavigationConvergenceRelay(
                guidancePolicy,
            )

        relay.routeChanged(route())

        relay.fixChanged(
            fix(50),
        )
        assertNull(
            relay.currentOutput.latestIssuedCue,
        )

        relay.fixChanged(
            fix(100),
        )
        assertEquals(
            VexNavigationCueBand.FAR,
            relay.currentOutput
                .latestIssuedCue
                ?.key
                ?.cueBand,
        )
        assertEquals(
            1,
            relay.currentOutput.issuedCueKeys.size,
        )

        relay.fixChanged(
            fix(150),
        )
        assertEquals(
            VexNavigationCueBand.FAR,
            relay.currentOutput
                .latestIssuedCue
                ?.key
                ?.cueBand,
        )
        assertEquals(
            1,
            relay.currentOutput.issuedCueKeys.size,
        )

        relay.fixChanged(
            fix(230),
        )
        assertEquals(
            VexNavigationCueBand.APPROACHING,
            relay.currentOutput
                .latestIssuedCue
                ?.key
                ?.cueBand,
        )
        assertEquals(
            2,
            relay.currentOutput.issuedCueKeys.size,
        )

        relay.fixChanged(
            fix(285),
        )
        assertEquals(
            VexNavigationCueBand.TURN_NOW,
            relay.currentOutput
                .latestIssuedCue
                ?.key
                ?.cueBand,
        )
        assertEquals(
            3,
            relay.currentOutput.issuedCueKeys.size,
        )

        relay.fixChanged(
            fix(301),
        )
        assertEquals(
            "maneuver.exit-home",
            relay.currentOutput
                .progress
                ?.nextManeuver
                ?.maneuver
                ?.maneuverRef,
        )
        assertEquals(
            499,
            relay.currentOutput
                .progress
                ?.nextManeuver
                ?.distanceToManeuverMeters,
        )

        val currentBeforeDuplicate =
            relay.current

        relay.fixChanged(
            fix(301),
        )

        assertEquals(
            currentBeforeDuplicate,
            relay.current,
        )
    }

    @Test
    fun rerouteInvalidatesOldFixAndResetsCueIdentity() {
        val relay =
            VexNavigationConvergenceRelay(
                guidancePolicy,
            )

        relay.routeChanged(route())
        relay.fixChanged(
            fix(100),
        )

        assertEquals(
            1,
            relay.currentOutput.issuedCueKeys.size,
        )

        relay.routeChanged(
            route(revision = 2L),
        )

        assertEquals(
            VexNavigationConvergenceAvailability
                .HELD_FIX_ROUTE_MISMATCH,
            relay.currentOutput.availability,
        )
        assertNull(
            relay.currentOutput.latestIssuedCue,
        )
        assertEquals(
            emptySet<VexNavigationGuidanceCueKey>(),
            relay.currentOutput.issuedCueKeys,
        )

        relay.fixChanged(
            fix(
                distanceAlongRouteMeters = 100,
                revision = 2L,
            ),
        )

        val cueKey =
            relay.currentOutput
                .latestIssuedCue
                ?.key

        assertEquals(
            2L,
            cueKey?.routeRevision,
        )
        assertEquals(
            VexNavigationCueBand.FAR,
            cueKey?.cueBand,
        )
        assertEquals(
            1,
            relay.currentOutput.issuedCueKeys.size,
        )
    }

    @Test
    fun overshootClampsToArrivalWithoutInventingAnotherManeuver() {
        val relay =
            VexNavigationConvergenceRelay(
                guidancePolicy,
            )

        relay.routeChanged(route())
        relay.fixChanged(
            fix(1_200),
        )

        assertEquals(
            VexNavigationConvergenceAvailability.ARRIVED,
            relay.currentOutput.availability,
        )
        assertEquals(
            0,
            relay.currentOutput.progress?.distanceRemainingMeters,
        )
        assertEquals(
            1_000,
            relay.currentOutput.progress?.distanceAlongRouteMeters,
        )
        assertNull(
            relay.currentOutput.progress?.nextManeuver,
        )
    }

    @Test
    fun arrivalDoesNotReopenFromLaterLocationJitter() {
        val relay =
            VexNavigationConvergenceRelay(
                guidancePolicy,
            )

        relay.routeChanged(route())
        relay.fixChanged(
            fix(1_000),
        )

        assertEquals(
            VexNavigationConvergenceAvailability.ARRIVED,
            relay.currentOutput.availability,
        )

        relay.fixChanged(
            fix(
                distanceAlongRouteMeters = 950,
                fixRef = "fix.jitter.after-arrival",
            ),
        )

        assertEquals(
            VexNavigationConvergenceAvailability.ARRIVED,
            relay.currentOutput.availability,
        )
        assertEquals(
            0,
            relay.currentOutput.progress?.distanceRemainingMeters,
        )
    }

    @Test
    fun sameRouteIdentityCannotSilentlyChangeMeaning() {
        val relay =
            VexNavigationConvergenceRelay(
                guidancePolicy,
            )

        relay.routeChanged(route())

        assertThrows(
            IllegalArgumentException::class.java,
        ) {
            relay.routeChanged(
                route().copy(
                    destinationLabel = "Different destination",
                ),
            )
        }
    }
}

// [VXG RealForever]
