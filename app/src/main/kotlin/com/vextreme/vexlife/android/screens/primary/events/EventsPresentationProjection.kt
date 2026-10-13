package com.vextreme.vexlife.android.screens.primary.events

import com.vextreme.vexlife.android.data.events.EventsAttentionDisposition
import com.vextreme.vexlife.android.data.events.EventsAttentionLabScenario
import com.vextreme.vexlife.android.data.events.VexExternalEvent
import com.vextreme.vexlife.android.screens.primary.events.traditionalApproach.TraditionalEventsAvailability
import com.vextreme.vexlife.android.screens.primary.events.traditionalApproach.TraditionalEventsListState
import com.vextreme.vexlife.android.screens.primary.events.traditionalApproach.TraditionalEventsUiState

/**
 * Presentation-only projection shared by the architecture comparison.
 *
 * This is deliberately smaller than either implementation's native state. It contains only what the
 * existing Events renderer needs, so parity does not make the traditional controller reuse the
 * Convergence Relay's reducer/state contract.
 */
internal sealed interface EventsPresentationListState {
    data object Unobserved : EventsPresentationListState
    data object Empty : EventsPresentationListState

    data class Content(
        val events: List<VexExternalEvent>,
    ) : EventsPresentationListState
}

internal data class EventsPresentationState(
    val list: EventsPresentationListState =
        EventsPresentationListState.Unobserved,
    val candidateEventRef: String? = null,
    val candidateReady: Boolean = false,
    val dispositionByEventRef: Map<String, EventsAttentionDisposition> = emptyMap(),
    val scenario: EventsAttentionLabScenario? = null,
    val unhandledCount: Int = 0,
    val currentQueueCount: Int = 0,
    val nextSequenceCount: Int = 0,
) {
    val events: List<VexExternalEvent>
        get() =
            when (val current = list) {
                EventsPresentationListState.Unobserved,
                EventsPresentationListState.Empty,
                ->
                    emptyList()

                is EventsPresentationListState.Content ->
                    current.events.map(VexExternalEvent::deepCopy)
            }
}

internal fun EventsConvergenceOutputState.toPresentationState():
    EventsPresentationState =
    EventsPresentationState(
        list =
            when (val current = list) {
                EventsConvergenceListState.Unobserved ->
                    EventsPresentationListState.Unobserved

                EventsConvergenceListState.Empty ->
                    EventsPresentationListState.Empty

                is EventsConvergenceListState.Content ->
                    EventsPresentationListState.Content(
                        current.events.map(VexExternalEvent::deepCopy),
                    )
            },
        candidateEventRef = candidate?.eventRef,
        candidateReady =
            availability ==
                EventsConvergenceAvailability.READY,
        dispositionByEventRef = dispositionByEventRef.toMap(),
        scenario = scenario?.copy(),
        unhandledCount = unhandledCount,
        currentQueueCount = currentQueueCount,
        nextSequenceCount = nextSequenceCount,
    )

internal fun TraditionalEventsUiState.toPresentationState():
    EventsPresentationState =
    EventsPresentationState(
        list =
            when (val current = list) {
                TraditionalEventsListState.Unobserved ->
                    EventsPresentationListState.Unobserved

                TraditionalEventsListState.Empty ->
                    EventsPresentationListState.Empty

                is TraditionalEventsListState.Content ->
                    EventsPresentationListState.Content(
                        current.events.map(VexExternalEvent::deepCopy),
                    )
            },
        candidateEventRef = candidate?.eventRef,
        candidateReady =
            availability ==
                TraditionalEventsAvailability.READY,
        dispositionByEventRef = dispositionByEventRef.toMap(),
        scenario = scenario?.copy(),
        unhandledCount = unhandledCount,
        currentQueueCount = currentQueueCount,
        nextSequenceCount = nextSequenceCount,
    )

// [VXG RealForever][EVENTS][ARCHITECTURE-COMPARISON-PRESENTATION]
