package com.vextreme.vexlife.android.data.events

import com.vextreme.vexlife.android.relay.VexRelay
import com.vextreme.vexlife.android.relay.VexState
import kotlinx.coroutines.flow.Flow

/**
 * Current availability of external sources whose events may request proactive attention.
 *
 * The Events list still reflects canonical event truth even when a source is unavailable. This
 * state answers only whether an event from a source may currently escalate into the attention
 * candidate relationship.
 *
 * This is intentionally not a partner registry, transport catalog, or universal adapter model.
 */
data class EventsSourceAvailabilityState(
    val unavailableSourceRefs: Set<String> = emptySet(),
) : VexState<EventsSourceAvailabilityState> {
    init {
        unavailableSourceRefs.forEach { sourceRef ->
            requireSemanticRef(sourceRef, "unavailableSourceRef")
        }
    }

    fun isAvailable(sourceRef: String): Boolean =
        sourceRef !in unavailableSourceRefs

    override fun deepCopy(): EventsSourceAvailabilityState =
        copy(
            unavailableSourceRefs =
                unavailableSourceRefs.toSet(),
        )
}

interface EventsSourceAvailabilityRepository {
    val states: Flow<EventsSourceAvailabilityState>
    val current: EventsSourceAvailabilityState

    fun setAvailable(
        sourceRef: String,
        available: Boolean,
    ): EventsSourceAvailabilityState
}

/**
 * Small current-state implementation for the architecture experiment.
 *
 * A future real source/partner owner may replace this projection without changing Events attention
 * semantics. Default state means every source is available until explicitly marked otherwise.
 */
class RelayEventsSourceAvailabilityRepository :
    EventsSourceAvailabilityRepository {
    private val relay =
        EventsSourceAvailabilityRelay()

    override val states: Flow<EventsSourceAvailabilityState>
        get() = relay.states

    override val current: EventsSourceAvailabilityState
        get() = relay.current

    override fun setAvailable(
        sourceRef: String,
        available: Boolean,
    ): EventsSourceAvailabilityState =
        relay.setAvailable(
            sourceRef = sourceRef,
            available = available,
        )
}

private class EventsSourceAvailabilityRelay :
    VexRelay<EventsSourceAvailabilityState>(
        relayRef = "relay.events.source-availability",
        initialState = EventsSourceAvailabilityState(),
    ) {
    override fun processState(
        inputState: EventsSourceAvailabilityState,
        currentState: EventsSourceAvailabilityState,
    ): EventsSourceAvailabilityState =
        inputState.deepCopy()

    fun setAvailable(
        sourceRef: String,
        available: Boolean,
    ): EventsSourceAvailabilityState {
        requireSemanticRef(sourceRef, "sourceRef")

        val unavailable =
            current
                .unavailableSourceRefs
                .toMutableSet()
                .apply {
                    if (available) {
                        remove(sourceRef)
                    } else {
                        add(sourceRef)
                    }
                }

        return send(
            EventsSourceAvailabilityState(
                unavailableSourceRefs =
                    unavailable.toSet(),
            ),
        )
    }
}

// [VXG RealForever][EVENTS][SOURCE-AVAILABILITY-PRESSURE]
