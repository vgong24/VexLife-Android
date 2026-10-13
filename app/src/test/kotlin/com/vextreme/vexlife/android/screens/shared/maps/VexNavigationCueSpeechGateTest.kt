package com.vextreme.vexlife.android.screens.shared.maps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VexNavigationCueSpeechGateTest {
    @Test
    fun activeCueSpeaksOnceAcrossProgressOnlyUpdates() {
        val gate =
            VexNavigationCueSpeechGate()
        val cue =
            cue(
                VexNavigationCueBand.APPROACHING,
            )

        assertEquals(
            cue,
            gate.nextCue(
                state(cue),
            ),
        )

        assertNull(
            gate.nextCue(
                state(
                    cue.copy(
                        distanceToManeuverMeters =
                            60,
                    ),
                ),
            ),
        )
    }

    @Test
    fun aNewCueBandSpeaksAsANewSemanticCue() {
        val gate =
            VexNavigationCueSpeechGate()

        assertEquals(
            VexNavigationCueBand.FAR,
            gate.nextCue(
                state(
                    cue(
                        VexNavigationCueBand.FAR,
                    ),
                ),
            )?.key?.cueBand,
        )

        assertEquals(
            VexNavigationCueBand.TURN_NOW,
            gate.nextCue(
                state(
                    cue(
                        VexNavigationCueBand.TURN_NOW,
                    ),
                ),
            )?.key?.cueBand,
        )
    }

    @Test
    fun nonActiveNavigationDoesNotSpeak() {
        val gate =
            VexNavigationCueSpeechGate()

        assertNull(
            gate.nextCue(
                state(
                    cue(
                        VexNavigationCueBand.FAR,
                    ),
                    phase =
                        VexNavigationSessionPhase.READY,
                ),
            ),
        )
    }

    private fun state(
        cue: VexNavigationGuidanceCue,
        phase:
            VexNavigationSessionPhase =
            VexNavigationSessionPhase.ACTIVE,
    ) =
        VexNavigationSessionState(
            phase = phase,
            convergence =
                VexNavigationConvergenceOutputState(
                    availability =
                        VexNavigationConvergenceAvailability.NAVIGATING,
                    latestIssuedCue =
                        cue,
                    issuedCueKeys =
                        setOf(
                            cue.key,
                        ),
                ),
        )

    private fun cue(
        band: VexNavigationCueBand,
    ): VexNavigationGuidanceCue {
        val key =
            VexNavigationGuidanceCueKey(
                routeRef =
                    "route.test",
                routeRevision = 1L,
                maneuverRef =
                    "maneuver.test",
                cueBand = band,
            )

        return VexNavigationGuidanceCue(
            key = key,
            instructionProjection =
                "Turn right",
            distanceToManeuverMeters =
                100,
        )
    }
}

// [VXG RealForever]
