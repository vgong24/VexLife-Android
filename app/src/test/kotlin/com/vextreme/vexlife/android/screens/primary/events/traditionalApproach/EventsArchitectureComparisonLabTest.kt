package com.vextreme.vexlife.android.screens.primary.events.traditionalApproach

import com.vextreme.vexlife.android.screens.primary.events.EventsPresentationState
import org.junit.Assert.assertEquals
import org.junit.Test

class EventsArchitectureComparisonLabTest {
    @Test
    fun identicalPresentationStatesReportParity() {
        val state =
            EventsPresentationState(
                candidateEventRef = "event.alpha",
                candidateReady = true,
                unhandledCount = 2,
                currentQueueCount = 1,
                nextSequenceCount = 1,
            )

        val comparison =
            compareEventsPresentationStates(
                relay = state,
                traditional = state.copy(),
            )

        assertEquals(
            EventsArchitectureComparisonStatus.PARITY,
            comparison.status,
        )
        assertEquals(
            emptyList<EventsArchitectureDifference>(),
            comparison.differences,
        )
        assertEquals(
            "PARITY",
            comparison.diagnosticSummary,
        )
    }

    @Test
    fun divergenceNamesOnlyThePresentationFieldsThatDiffer() {
        val relay =
            EventsPresentationState(
                candidateEventRef = "event.alpha",
                candidateReady = true,
                unhandledCount = 2,
                currentQueueCount = 1,
                nextSequenceCount = 1,
            )
        val traditional =
            relay.copy(
                candidateEventRef = "event.beta",
                currentQueueCount = 2,
            )

        val comparison =
            compareEventsPresentationStates(
                relay = relay,
                traditional = traditional,
            )

        assertEquals(
            EventsArchitectureComparisonStatus.DIVERGENCE,
            comparison.status,
        )
        assertEquals(
            listOf(
                EventsArchitectureDifference.CANDIDATE_EVENT_REF,
                EventsArchitectureDifference.CURRENT_QUEUE_COUNT,
            ),
            comparison.differences,
        )
        assertEquals(
            "DIVERGENCE · candidate-event-ref, current-queue-count",
            comparison.diagnosticSummary,
        )
    }
}

// [VXG RealForever][EVENTS][ARCHITECTURE-COMPARISON-LAB-TEST]
