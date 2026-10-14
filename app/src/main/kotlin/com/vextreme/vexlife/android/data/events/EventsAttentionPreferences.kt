package com.vextreme.vexlife.android.data.events

import com.vextreme.vexlife.android.relay.VexAction
import com.vextreme.vexlife.android.relay.VexActionRelay
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

private sealed interface EventsAttentionPreferenceAction :
    VexAction {
    data class Terminal(
        val eventRef: String,
        val disposition: EventsAttentionDisposition,
    ) : EventsAttentionPreferenceAction {
        init {
            require(
                disposition ==
                    EventsAttentionDisposition.ACKNOWLEDGED ||
                    disposition ==
                    EventsAttentionDisposition.SUPPRESSED,
            )
            requireSemanticRef(
                eventRef,
                "eventRef",
            )
        }
    }

    data class Enqueue(
        val eventRef: String,
        val disposition: EventsAttentionDisposition,
    ) : EventsAttentionPreferenceAction {
        init {
            require(
                disposition ==
                    EventsAttentionDisposition.DEFERRED ||
                    disposition ==
                    EventsAttentionDisposition.SUSPENDED,
            )
            requireSemanticRef(
                eventRef,
                "eventRef",
            )
        }
    }

    data class Reset(
        val eventRef: String,
    ) : EventsAttentionPreferenceAction {
        init {
            requireSemanticRef(
                eventRef,
                "eventRef",
            )
        }
    }

    data object ResetAll :
        EventsAttentionPreferenceAction
}

/**
 * One action-driven state owner for human Events attention disposition.
 *
 * Public methods form named semantic actions only. [nextStateFor] is the one exhaustive location
 * where those actions become next preference state.
 */
private class EventsAttentionPreferenceRelay :
    VexActionRelay<
        EventsAttentionPreferenceAction,
        EventsAttentionPreferenceState
    >(
        relayRef = "relay.events.attention-preference",
        initialState = EventsAttentionPreferenceState(),
    ) {
    fun terminal(
        eventRef: String,
        disposition: EventsAttentionDisposition,
    ) {
        sendAction(
            EventsAttentionPreferenceAction.Terminal(
                eventRef = eventRef,
                disposition = disposition,
            ),
        )
    }

    fun enqueue(
        eventRef: String,
        disposition: EventsAttentionDisposition,
    ) {
        sendAction(
            EventsAttentionPreferenceAction.Enqueue(
                eventRef = eventRef,
                disposition = disposition,
            ),
        )
    }

    fun reset(
        eventRef: String,
    ) {
        sendAction(
            EventsAttentionPreferenceAction.Reset(
                eventRef = eventRef,
            ),
        )
    }

    fun resetAll() {
        sendAction(
            EventsAttentionPreferenceAction.ResetAll,
        )
    }

    override fun nextStateFor(
        action: EventsAttentionPreferenceAction,
        currentState: EventsAttentionPreferenceState,
    ): EventsAttentionPreferenceState =
        when (action) {
            is EventsAttentionPreferenceAction.Terminal ->
                currentState.copy(
                    dispositionByEventRef =
                        currentState.dispositionByEventRef +
                            (action.eventRef to action.disposition),
                    deferredOrderByEventRef =
                        currentState.deferredOrderByEventRef -
                            action.eventRef,
                    suspendedOrderByEventRef =
                        currentState.suspendedOrderByEventRef -
                            action.eventRef,
                )

            is EventsAttentionPreferenceAction.Enqueue -> {
                val ordinal =
                    currentState.nextQueueOrdinal

                currentState.copy(
                    dispositionByEventRef =
                        currentState.dispositionByEventRef +
                            (action.eventRef to action.disposition),
                    deferredOrderByEventRef =
                        if (
                            action.disposition ==
                                EventsAttentionDisposition.DEFERRED
                        ) {
                            currentState.deferredOrderByEventRef +
                                (action.eventRef to ordinal)
                        } else {
                            currentState.deferredOrderByEventRef -
                                action.eventRef
                        },
                    suspendedOrderByEventRef =
                        if (
                            action.disposition ==
                                EventsAttentionDisposition.SUSPENDED
                        ) {
                            currentState.suspendedOrderByEventRef +
                                (action.eventRef to ordinal)
                        } else {
                            currentState.suspendedOrderByEventRef -
                                action.eventRef
                        },
                    nextQueueOrdinal =
                        ordinal + 1L,
                )
            }

            is EventsAttentionPreferenceAction.Reset ->
                currentState.copy(
                    dispositionByEventRef =
                        currentState.dispositionByEventRef -
                            action.eventRef,
                    deferredOrderByEventRef =
                        currentState.deferredOrderByEventRef -
                            action.eventRef,
                    suspendedOrderByEventRef =
                        currentState.suspendedOrderByEventRef -
                            action.eventRef,
                )

            EventsAttentionPreferenceAction.ResetAll ->
                EventsAttentionPreferenceState()
        }
}

// [VXG RealForever]
