package com.vextreme.vexlife.android.screens.primary.events.traditionalApproach

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class EventsArchitectureMode {
    VEXTREME_RELAY,
    TRADITIONAL_CONTROLLER,
}

/**
 * Debug-only comparison selection.
 *
 * This object does not decide Events semantics. It only selects which independent implementation is
 * projected into the shared presentation while both implementations may continue observing the same
 * source repositories for parity diagnostics.
 */
class EventsArchitectureComparisonLab(
    val traditionalController: TraditionalEventsController,
) {
    private val mutableMode =
        MutableStateFlow(EventsArchitectureMode.VEXTREME_RELAY)

    val mode: StateFlow<EventsArchitectureMode> =
        mutableMode.asStateFlow()

    val currentMode: EventsArchitectureMode
        get() = mutableMode.value

    fun useVextremeRelay() {
        mutableMode.value =
            EventsArchitectureMode.VEXTREME_RELAY
    }

    fun useTraditionalController() {
        mutableMode.value =
            EventsArchitectureMode.TRADITIONAL_CONTROLLER
    }
}

// [VXG RealForever][EVENTS][ARCHITECTURE-COMPARISON-LAB]
