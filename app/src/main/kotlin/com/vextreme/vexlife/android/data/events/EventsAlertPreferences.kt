package com.vextreme.vexlife.android.data.events

import com.vextreme.vexlife.android.relay.VexAction
import com.vextreme.vexlife.android.relay.VexActionRelay
import com.vextreme.vexlife.android.relay.VexState
import kotlinx.coroutines.flow.Flow

enum class EventsAlertDisposition {
    ACKNOWLEDGED,
    DEFERRED,
    SUSPENDED,
    SUPPRESSED,
}

/**
 * Human alert action state is separate from canonical event truth.
 *
 * Deferred/suspended order maps capture the order the HUMAN queued those actions. Provider
 * occurrence time must not silently reorder "Later" or "Suspend" semantics.
 */
data class EventsAlertPreferenceState(
    val dispositionByEventRef: Map<String, EventsAlertDisposition> = emptyMap(),
    val deferredOrderByEventRef: Map<String, Long> = emptyMap(),
    val suspendedOrderByEventRef: Map<String, Long> = emptyMap(),
    val nextQueueOrdinal: Long = 1L,
) : VexState<EventsAlertPreferenceState> {
    init {
        require(nextQueueOrdinal > 0L)

        require(
            deferredOrderByEventRef.keys.all { eventRef ->
                dispositionByEventRef[eventRef] ==
                    EventsAlertDisposition.DEFERRED
            },
        ) {
            "deferred queue order must reference DEFERRED events only"
        }
        require(
            suspendedOrderByEventRef.keys.all { eventRef ->
                dispositionByEventRef[eventRef] ==
                    EventsAlertDisposition.SUSPENDED
            },
        ) {
            "suspended queue order must reference SUSPENDED events only"
        }
    }

    override fun deepCopy(): EventsAlertPreferenceState =
        copy(
            dispositionByEventRef =
                dispositionByEventRef.toMap(),
            deferredOrderByEventRef =
                deferredOrderByEventRef.toMap(),
            suspendedOrderByEventRef =
                suspendedOrderByEventRef.toMap(),
        )
}

interface EventsAlertPreferenceRepository {
    val states: Flow<EventsAlertPreferenceState>
    val current: EventsAlertPreferenceState

    fun acknowledge(eventRef: String)
    fun defer(eventRef: String)
    fun suspend(eventRef: String)
    fun suppress(eventRef: String)
    fun reset(eventRef: String)
    fun resetAll()
}

class RelayEventsAlertPreferenceRepository :
    EventsAlertPreferenceRepository {
    private val relay =
        EventsAlertPreferenceRelay()

    override val states: Flow<EventsAlertPreferenceState>
        get() = relay.states

    override val current: EventsAlertPreferenceState
        get() = relay.current

    override fun acknowledge(eventRef: String) {
        relay.terminal(
            eventRef,
            EventsAlertDisposition.ACKNOWLEDGED,
        )
    }

    override fun defer(eventRef: String) {
        relay.enqueue(
            eventRef,
            EventsAlertDisposition.DEFERRED,
        )
    }

    override fun suspend(eventRef: String) {
        relay.enqueue(
            eventRef,
            EventsAlertDisposition.SUSPENDED,
        )
    }

    override fun suppress(eventRef: String) {
        relay.terminal(
            eventRef,
            EventsAlertDisposition.SUPPRESSED,
        )
    }

    override fun reset(eventRef: String) {
        relay.reset(eventRef)
    }

    override fun resetAll() {
        relay.resetAll()
    }
}

private sealed interface EventsAlertPreferenceAction :
    VexAction {
    data class Terminal(
        val eventRef: String,
        val disposition: EventsAlertDisposition,
    ) : EventsAlertPreferenceAction {
        init {
            require(
                disposition ==
                    EventsAlertDisposition.ACKNOWLEDGED ||
                    disposition ==
                    EventsAlertDisposition.SUPPRESSED,
            )
            requireSemanticRef(
                eventRef,
                "eventRef",
            )
        }
    }

    data class Enqueue(
        val eventRef: String,
        val disposition: EventsAlertDisposition,
    ) : EventsAlertPreferenceAction {
        init {
            require(
                disposition ==
                    EventsAlertDisposition.DEFERRED ||
                    disposition ==
                    EventsAlertDisposition.SUSPENDED,
            )
            requireSemanticRef(
                eventRef,
                "eventRef",
            )
        }
    }

    data class Reset(
        val eventRef: String,
    ) : EventsAlertPreferenceAction {
        init {
            requireSemanticRef(
                eventRef,
                "eventRef",
            )
        }
    }

    data object ResetAll :
        EventsAlertPreferenceAction
}

/**
 * One action-driven state owner for human Events alert disposition.
 *
 * Public methods form named semantic actions only. [nextStateFor] is the one exhaustive location
 * where those actions become next preference state.
 */
private class EventsAlertPreferenceRelay :
    VexActionRelay<
        EventsAlertPreferenceAction,
        EventsAlertPreferenceState
    >(
        relayRef = "relay.events.alert-preference",
        initialState = EventsAlertPreferenceState(),
    ) {
    fun terminal(
        eventRef: String,
        disposition: EventsAlertDisposition,
    ) {
        sendAction(
            EventsAlertPreferenceAction.Terminal(
                eventRef = eventRef,
                disposition = disposition,
            ),
        )
    }

    fun enqueue(
        eventRef: String,
        disposition: EventsAlertDisposition,
    ) {
        sendAction(
            EventsAlertPreferenceAction.Enqueue(
                eventRef = eventRef,
                disposition = disposition,
            ),
        )
    }

    fun reset(
        eventRef: String,
    ) {
        sendAction(
            EventsAlertPreferenceAction.Reset(
                eventRef = eventRef,
            ),
        )
    }

    fun resetAll() {
        sendAction(
            EventsAlertPreferenceAction.ResetAll,
        )
    }

    override fun nextStateFor(
        action: EventsAlertPreferenceAction,
        currentState: EventsAlertPreferenceState,
    ): EventsAlertPreferenceState =
        when (action) {
            is EventsAlertPreferenceAction.Terminal ->
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

            is EventsAlertPreferenceAction.Enqueue -> {
                val ordinal =
                    currentState.nextQueueOrdinal

                currentState.copy(
                    dispositionByEventRef =
                        currentState.dispositionByEventRef +
                            (action.eventRef to action.disposition),
                    deferredOrderByEventRef =
                        if (
                            action.disposition ==
                                EventsAlertDisposition.DEFERRED
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
                                EventsAlertDisposition.SUSPENDED
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

            is EventsAlertPreferenceAction.Reset ->
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

            EventsAlertPreferenceAction.ResetAll ->
                EventsAlertPreferenceState()
        }
}

// [VXG RealForever]
