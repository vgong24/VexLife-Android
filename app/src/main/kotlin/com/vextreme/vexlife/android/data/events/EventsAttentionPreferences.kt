package com.vextreme.vexlife.android.data.events

import com.vextreme.vexlife.android.relay.VexRelay
import com.vextreme.vexlife.android.relay.VexState
import kotlinx.coroutines.flow.Flow

enum class EventsAttentionDisposition {
    ACKNOWLEDGED,
    DEFERRED,
    SUSPENDED,
    SUPPRESSED,
}

/**
 * Human attention action state is separate from canonical event truth.
 *
 * Deferred/suspended order maps capture the order the HUMAN queued those actions. Provider
 * occurrence time must not silently reorder "Later" or "Suspend" semantics.
 */
data class EventsAttentionPreferenceState(
    val dispositionByEventRef: Map<String, EventsAttentionDisposition> = emptyMap(),
    val deferredOrderByEventRef: Map<String, Long> = emptyMap(),
    val suspendedOrderByEventRef: Map<String, Long> = emptyMap(),
    val nextQueueOrdinal: Long = 1L,
) : VexState<EventsAttentionPreferenceState> {
    init {
        require(nextQueueOrdinal > 0L)

        require(
            deferredOrderByEventRef.keys.all { eventRef ->
                dispositionByEventRef[eventRef] ==
                    EventsAttentionDisposition.DEFERRED
            },
        ) {
            "deferred queue order must reference DEFERRED events only"
        }
        require(
            suspendedOrderByEventRef.keys.all { eventRef ->
                dispositionByEventRef[eventRef] ==
                    EventsAttentionDisposition.SUSPENDED
            },
        ) {
            "suspended queue order must reference SUSPENDED events only"
        }
    }

    override fun deepCopy(): EventsAttentionPreferenceState =
        copy(
            dispositionByEventRef =
                dispositionByEventRef.toMap(),
            deferredOrderByEventRef =
                deferredOrderByEventRef.toMap(),
            suspendedOrderByEventRef =
                suspendedOrderByEventRef.toMap(),
        )
}

interface EventsAttentionPreferenceRepository {
    val states: Flow<EventsAttentionPreferenceState>
    val current: EventsAttentionPreferenceState

    fun acknowledge(eventRef: String)
    fun defer(eventRef: String)
    fun suspend(eventRef: String)
    fun suppress(eventRef: String)
    fun reset(eventRef: String)
    fun resetAll()
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
        relay.terminal(
            eventRef,
            EventsAttentionDisposition.ACKNOWLEDGED,
        )
    }

    override fun defer(eventRef: String) {
        relay.enqueue(
            eventRef,
            EventsAttentionDisposition.DEFERRED,
        )
    }

    override fun suspend(eventRef: String) {
        relay.enqueue(
            eventRef,
            EventsAttentionDisposition.SUSPENDED,
        )
    }

    override fun suppress(eventRef: String) {
        relay.terminal(
            eventRef,
            EventsAttentionDisposition.SUPPRESSED,
        )
    }

    override fun reset(eventRef: String) {
        relay.reset(eventRef)
    }

    override fun resetAll() {
        relay.resetAll()
    }
}

private class EventsAttentionPreferenceRelay :
    VexRelay<EventsAttentionPreferenceState>(
        relayRef = "relay.events.attention-preference",
        initialState = EventsAttentionPreferenceState(),
    ) {
    fun terminal(
        eventRef: String,
        disposition: EventsAttentionDisposition,
    ) {
        require(
            disposition ==
                EventsAttentionDisposition.ACKNOWLEDGED ||
                disposition ==
                EventsAttentionDisposition.SUPPRESSED,
        )
        requireSemanticRef(eventRef, "eventRef")

        update { current ->
            current.copy(
                dispositionByEventRef =
                    current.dispositionByEventRef +
                        (eventRef to disposition),
                deferredOrderByEventRef =
                    current.deferredOrderByEventRef - eventRef,
                suspendedOrderByEventRef =
                    current.suspendedOrderByEventRef - eventRef,
            )
        }
    }

    fun enqueue(
        eventRef: String,
        disposition: EventsAttentionDisposition,
    ) {
        require(
            disposition ==
                EventsAttentionDisposition.DEFERRED ||
                disposition ==
                EventsAttentionDisposition.SUSPENDED,
        )
        requireSemanticRef(eventRef, "eventRef")

        update { current ->
            val ordinal =
                current.nextQueueOrdinal

            current.copy(
                dispositionByEventRef =
                    current.dispositionByEventRef +
                        (eventRef to disposition),
                deferredOrderByEventRef =
                    if (
                        disposition ==
                            EventsAttentionDisposition.DEFERRED
                    ) {
                        current.deferredOrderByEventRef +
                            (eventRef to ordinal)
                    } else {
                        current.deferredOrderByEventRef - eventRef
                    },
                suspendedOrderByEventRef =
                    if (
                        disposition ==
                            EventsAttentionDisposition.SUSPENDED
                    ) {
                        current.suspendedOrderByEventRef +
                            (eventRef to ordinal)
                    } else {
                        current.suspendedOrderByEventRef - eventRef
                    },
                nextQueueOrdinal = ordinal + 1L,
            )
        }
    }

    fun reset(eventRef: String) {
        requireSemanticRef(eventRef, "eventRef")

        update { current ->
            current.copy(
                dispositionByEventRef =
                    current.dispositionByEventRef - eventRef,
                deferredOrderByEventRef =
                    current.deferredOrderByEventRef - eventRef,
                suspendedOrderByEventRef =
                    current.suspendedOrderByEventRef - eventRef,
            )
        }
    }

    fun resetAll() {
        update {
            EventsAttentionPreferenceState()
        }
    }
}

// [VXG RealForever]
