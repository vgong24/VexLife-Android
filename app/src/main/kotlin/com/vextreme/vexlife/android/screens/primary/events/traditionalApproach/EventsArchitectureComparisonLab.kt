package com.vextreme.vexlife.android.screens.primary.events.traditionalApproach

import com.vextreme.vexlife.android.screens.primary.events.EventsConvergenceRelay
import com.vextreme.vexlife.android.screens.primary.events.EventsPresentationState
import com.vextreme.vexlife.android.screens.primary.events.toPresentationState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class EventsArchitectureMode {
    VEXTREME_RELAY,
    TRADITIONAL_CONTROLLER,
}

enum class EventsArchitectureComparisonStatus {
    PARITY,
    DIVERGENCE,
}

enum class EventsArchitectureDifference(
    val diagnosticRef: String,
) {
    LIST("list"),
    CANDIDATE_EVENT_REF("candidate-event-ref"),
    CANDIDATE_READY("candidate-ready"),
    DISPOSITIONS("dispositions"),
    SCENARIO("scenario"),
    UNHANDLED_COUNT("unhandled-count"),
    CURRENT_QUEUE_COUNT("current-queue-count"),
    NEXT_SEQUENCE_COUNT("next-sequence-count"),
}

data class EventsArchitectureComparisonSnapshot(
    val status: EventsArchitectureComparisonStatus,
    val differences: List<EventsArchitectureDifference>,
) {
    val diagnosticSummary: String
        get() =
            when (status) {
                EventsArchitectureComparisonStatus.PARITY ->
                    "PARITY"

                EventsArchitectureComparisonStatus.DIVERGENCE ->
                    "DIVERGENCE · " +
                        differences.joinToString(", ") {
                            it.diagnosticRef
                        }
            }
}

/**
 * Debug-only comparison selection and observation.
 *
 * This object does not decide Events semantics. It selects which independent implementation is
 * projected into the shared presentation and, separately, compares both presentation projections
 * while they continue observing the same live inputs.
 *
 * The comparison is diagnostic-only:
 * - neither implementation reads it;
 * - it cannot change Events meaning;
 * - it stays in this removable experiment package;
 * - it compares presentation truth rather than making either implementation reuse the other's
 *   internal reducer/state contract.
 */
class EventsArchitectureComparisonLab(
    relay: EventsConvergenceRelay,
    val traditionalController: TraditionalEventsController,
    scope: CoroutineScope,
) {
    private val mutableMode =
        MutableStateFlow(EventsArchitectureMode.VEXTREME_RELAY)

    val mode: StateFlow<EventsArchitectureMode> =
        mutableMode.asStateFlow()

    val currentMode: EventsArchitectureMode
        get() = mutableMode.value

    val comparisonStates: StateFlow<EventsArchitectureComparisonSnapshot> =
        combine(
            relay.outputStates,
            traditionalController.uiStates,
        ) { relayState, traditionalState ->
            compareEventsPresentationStates(
                relay = relayState.toPresentationState(),
                traditional = traditionalState.toPresentationState(),
            )
        }.stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue =
                compareEventsPresentationStates(
                    relay = relay.currentOutput.toPresentationState(),
                    traditional = traditionalController.current.toPresentationState(),
                ),
        )

    fun useVextremeRelay() {
        mutableMode.value =
            EventsArchitectureMode.VEXTREME_RELAY
    }

    fun useTraditionalController() {
        mutableMode.value =
            EventsArchitectureMode.TRADITIONAL_CONTROLLER
    }
}

internal fun compareEventsPresentationStates(
    relay: EventsPresentationState,
    traditional: EventsPresentationState,
): EventsArchitectureComparisonSnapshot {
    val differences =
        buildList {
            if (relay.list != traditional.list) {
                add(EventsArchitectureDifference.LIST)
            }
            if (relay.candidateEventRef != traditional.candidateEventRef) {
                add(EventsArchitectureDifference.CANDIDATE_EVENT_REF)
            }
            if (relay.candidateReady != traditional.candidateReady) {
                add(EventsArchitectureDifference.CANDIDATE_READY)
            }
            if (relay.dispositionByEventRef != traditional.dispositionByEventRef) {
                add(EventsArchitectureDifference.DISPOSITIONS)
            }
            if (relay.scenario != traditional.scenario) {
                add(EventsArchitectureDifference.SCENARIO)
            }
            if (relay.unhandledCount != traditional.unhandledCount) {
                add(EventsArchitectureDifference.UNHANDLED_COUNT)
            }
            if (relay.currentQueueCount != traditional.currentQueueCount) {
                add(EventsArchitectureDifference.CURRENT_QUEUE_COUNT)
            }
            if (relay.nextSequenceCount != traditional.nextSequenceCount) {
                add(EventsArchitectureDifference.NEXT_SEQUENCE_COUNT)
            }
        }

    return EventsArchitectureComparisonSnapshot(
        status =
            if (differences.isEmpty()) {
                EventsArchitectureComparisonStatus.PARITY
            } else {
                EventsArchitectureComparisonStatus.DIVERGENCE
            },
        differences = differences,
    )
}

// [VXG RealForever][EVENTS][ARCHITECTURE-COMPARISON-LAB]
