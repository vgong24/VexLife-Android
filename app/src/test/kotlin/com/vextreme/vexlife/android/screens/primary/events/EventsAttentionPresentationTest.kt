package com.vextreme.vexlife.android.screens.primary.events

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EventsAttentionPresentationTest {
    @Test
    fun composedRowMayBridgeLaggingVisibilityProjection() {
        assertTrue(
            EventsConvergenceAvailability.READY
                .allowsAnchoredCandidateInComposedRow(),
        )
        assertTrue(
            EventsConvergenceAvailability.HELD_TARGET_UNREACHABLE
                .allowsAnchoredCandidateInComposedRow(),
        )
    }

    @Test
    fun unrelatedHeldStatesCannotRenderAnchoredAttention() {
        assertFalse(
            EventsConvergenceAvailability.UNOBSERVED
                .allowsAnchoredCandidateInComposedRow(),
        )
        assertFalse(
            EventsConvergenceAvailability.NONE
                .allowsAnchoredCandidateInComposedRow(),
        )
        assertFalse(
            EventsConvergenceAvailability.HELD_OTHER_SCREEN
                .allowsAnchoredCandidateInComposedRow(),
        )
        assertFalse(
            EventsConvergenceAvailability.HELD_TRANSIENT_OCCUPIED
                .allowsAnchoredCandidateInComposedRow(),
        )
    }
}

// [VXG RealForever][EVENTS][ATTENTION-PRESENTATION]
