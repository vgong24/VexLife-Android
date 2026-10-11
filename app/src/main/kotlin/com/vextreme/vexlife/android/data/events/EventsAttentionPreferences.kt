package com.vextreme.vexlife.android.data.events

import com.vextreme.vexlife.android.relay.VexRelay
import com.vextreme.vexlife.android.relay.VexState
import kotlinx.coroutines.flow.Flow

enum class EventsAttentionDisposition {
    ACKNOWLEDGED,
    DEFERRED,
    SUPPRESSED,
}

data class EventsAttentionPreferenceState(
    val dispositionByEventRef: Map<String, EventsAttentionDisposition> = emptyMap(),
) : VexState<EventsAttentionPreferenceState> {
    override fun deepCopy(): EventsAttentionPreferenceState =
        copy(
            dispositionByEventRef =
                dispositionByEventRef.toMap(),
        )
}

interface EventsAttentionPreferenceRepository {
    val states: Flow<EventsAttentionPreferenceState>
    val current: EventsAttentionPreferenceState

    fun acknowledge(eventRef: String)
    fun defer(eventRef: String)
    fun suppress(eventRef: String)
    fun reset(eventRef: String)
}

class RelayEventsAttentionPreferenceRepository :
    EventsAttentionPreferenceRepository {
    private val relay =
        EventsAttentionPreferenceRelay()

    override val states: Flow<EventsAttentionPreferenceState>
        get() = relay.states

    override val current: EventsAttentionPreferenceState
        get() = relay.current

    override fun acknowledge(eventRef: String) {
        relay.set(
            eventRef,
            EventsAttentionDisposition.ACKNOWLEDGED,
        )
    }

    override fun defer(eventRef: String) {
        relay.set(
            eventRef,
            EventsAttentionDisposition.DEFERRED,
        )
    }

    override fun suppress(eventRef: String) {
        relay.set(
            eventRef,
            EventsAttentionDisposition.SUPPRESSED,
        )
    }

    override fun reset(eventRef: String) {
        relay.reset(eventRef)
    }
}

private class EventsAttentionPreferenceRelay :
    VexRelay<EventsAttentionPreferenceState>(
        relayRef = "relay.events.attention-preference",
        initialState = EventsAttentionPreferenceState(),
    ) {
    fun set(
        eventRef: String,
        disposition: EventsAttentionDisposition,
    ) {
        requireSemanticRef(eventRef, "eventRef")

        update { current ->
            current.copy(
                dispositionByEventRef =
                    current.dispositionByEventRef +
                        (eventRef to disposition),
            )
        }
    }

    fun reset(eventRef: String) {
        requireSemanticRef(eventRef, "eventRef")

        update { current ->
            current.copy(
                dispositionByEventRef =
                    current.dispositionByEventRef - eventRef,
            )
        }
    }
}

// [VXG RealForever]
