package com.vextreme.vexlife.android.screens.primary.events

import com.vextreme.vexlife.android.data.events.EventsAttentionLabState
import com.vextreme.vexlife.android.data.events.EventsAttentionPreferenceState
import com.vextreme.vexlife.android.data.events.EventsFeedState
import com.vextreme.vexlife.android.relay.VexCompoundRelay
import com.vextreme.vexlife.android.relay.VexCompoundState
import com.vextreme.vexlife.android.relay.VexInputState
import com.vextreme.vexlife.android.relay.VexOutputState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

data class EventsAttentionInputState(
    val currentScreenId: String? = null,
    val feed: EventsFeedState? = null,
    val preferences: EventsAttentionPreferenceState? = null,
    val lab: EventsAttentionLabState? = null,
    val visibleEventRefs: Set<String>? = null,
    val transientBlocked: Boolean? = null,
) : VexInputState<EventsAttentionInputState> {
    override fun deepCopy(): EventsAttentionInputState =
        copy(
            feed = feed?.deepCopy(),
            preferences = preferences?.deepCopy(),
            lab = lab?.deepCopy(),
            visibleEventRefs = visibleEventRefs?.toSet(),
        )
}

data class EventsAttentionCandidate(
    val eventRef: String,
    val targetElementRef: String,
)

enum class EventsAttentionAvailability {
    /**
     * One or more dependencies have not yet been observed by this presentation relationship.
     */
    UNOBSERVED,

    /** Current feed contains no unhandled event candidate. */
    NONE,

    /** Candidate exists, but Events is not the current semantic Screen. */
    HELD_OTHER_SCREEN,

    /** Candidate exists and Events is current, but its exact row is not presently reachable. */
    HELD_TARGET_UNREACHABLE,

    /** Candidate exists, but another transient surface currently owns presentation attention. */
    HELD_TRANSIENT_OCCUPIED,

    /** Candidate exists and the exact target is currently eligible for anchored presentation. */
    READY,
}

data class EventsAttentionOutputState(
    val candidate: EventsAttentionCandidate? = null,
    val availability: EventsAttentionAvailability =
        EventsAttentionAvailability.UNOBSERVED,

    /**
     * Human/debug-facing queue metadata derived from the same Relay decision.
     *
     * These counts are intentionally not recomputed by Compose. The popup can therefore explain
     * exactly what the Relay believes is current/queued without creating a second queue model.
     */
    val unhandledCount: Int = 0,
    val queuedAfterCurrentCount: Int = 0,
    val acknowledgedCount: Int = 0,
    val deferredCount: Int = 0,
    val suspendedCount: Int = 0,
    val suppressedCount: Int = 0,
    val currentQueueCount: Int = 0,
    val nextSequenceCount: Int = 0,
) : VexOutputState<EventsAttentionOutputState> {
    override fun deepCopy(): EventsAttentionOutputState =
        copy(candidate = candidate?.copy())
}

/**
 * One current attention relationship over canonical Events truth.
 *
 * Candidate identity is resolved from event truth + acknowledgement state first. Current Screen,
 * target reachability and transient occupancy then classify presentation availability without
 * silently switching to a different event merely because the candidate scrolled off-screen.
 */
class EventsAttentionRelay :
    VexCompoundRelay<EventsAttentionInputState, EventsAttentionOutputState>(
        relayRef = "relay.events.attention",
        initialInputState = EventsAttentionInputState(),
        initialOutputState = EventsAttentionOutputState(),
    ) {
    /**
     * Presentation consumers should observe only the derived output, not every retained input
     * revision. External provider snapshots may change while the candidate/availability/queue
     * projection remains semantically identical.
     */
    val outputStates: Flow<EventsAttentionOutputState> =
        states
            .map { state ->
                state.outputState
            }
            .distinctUntilChanged()

    fun screenChanged(screenId: String) {
        require(screenId.isNotBlank())
        send(EventsAttentionInputState(currentScreenId = screenId))
    }

    fun feedChanged(feed: EventsFeedState) {
        send(EventsAttentionInputState(feed = feed.deepCopy()))
    }

    fun preferencesChanged(
        preferences: EventsAttentionPreferenceState,
    ) {
        send(
            EventsAttentionInputState(
                preferences = preferences.deepCopy(),
            ),
        )
    }

    fun labChanged(lab: EventsAttentionLabState) {
        send(
            EventsAttentionInputState(
                lab = lab.deepCopy(),
            ),
        )
    }

    fun visibleTargetsChanged(eventRefs: Set<String>) {
        send(
            EventsAttentionInputState(
                visibleEventRefs = eventRefs.toSet(),
            ),
        )
    }

    fun transientBlockedChanged(blocked: Boolean) {
        send(
            EventsAttentionInputState(
                transientBlocked = blocked,
            ),
        )
    }

    override fun processStates(
        inputState: EventsAttentionInputState,
        currentState: VexCompoundState<
            EventsAttentionInputState,
            EventsAttentionOutputState
        >,
    ): VexCompoundState<
        EventsAttentionInputState,
        EventsAttentionOutputState
    > {
        val previous =
            currentState.inputState

        val screenId =
            inputState.currentScreenId
                ?: previous.currentScreenId
        val feed =
            inputState.feed
                ?: previous.feed
        val preferences =
            inputState.preferences
                ?: previous.preferences
        val lab =
            inputState.lab
                ?: previous.lab
        val visibleEventRefs =
            inputState.visibleEventRefs
                ?: previous.visibleEventRefs
        val transientBlocked =
            inputState.transientBlocked
                ?: previous.transientBlocked

        val coherentInput =
            EventsAttentionInputState(
                currentScreenId = screenId,
                feed = feed?.deepCopy(),
                preferences = preferences?.deepCopy(),
                lab = lab?.deepCopy(),
                visibleEventRefs = visibleEventRefs?.toSet(),
                transientBlocked = transientBlocked,
            )

        val dispositions =
            preferences
                ?.dispositionByEventRef
                .orEmpty()

        /*
         * Queue law:
         *
         * NORMAL      -> current queue
         * SUSPENDED   -> back of current queue
         * DEFERRED    -> next sequence, only after current queue drains
         * ACK/SUPPRESS-> terminal for proactive attention this lab/session
         *
         * feed.events is newest-first; reversing each pool makes the attention queue oldest-first.
         */
        val normalQueue =
            feed
                ?.events
                .orEmpty()
                .filter { event ->
                    event.eventRef !in dispositions
                }
                .asReversed()

        val suspendedQueue =
            feed
                ?.events
                .orEmpty()
                .filter { event ->
                    dispositions[event.eventRef] ==
                        com.vextreme.vexlife.android.data.events.EventsAttentionDisposition.SUSPENDED
                }
                .sortedBy { event ->
                    preferences
                        ?.suspendedOrderByEventRef
                        ?.get(event.eventRef)
                        ?: Long.MAX_VALUE
                }

        val deferredQueue =
            feed
                ?.events
                .orEmpty()
                .filter { event ->
                    dispositions[event.eventRef] ==
                        com.vextreme.vexlife.android.data.events.EventsAttentionDisposition.DEFERRED
                }
                .sortedBy { event ->
                    preferences
                        ?.deferredOrderByEventRef
                        ?.get(event.eventRef)
                        ?: Long.MAX_VALUE
                }

        val activePool =
            when {
                normalQueue.isNotEmpty() ->
                    normalQueue

                suspendedQueue.isNotEmpty() ->
                    suspendedQueue

                else ->
                    deferredQueue
            }

        val candidateEvent =
            if (feed != null && preferences != null) {
                val labTargetRef =
                    lab
                        ?.currentScenario
                        ?.targetEventRef

                val retainedCandidateRef =
                    currentState.outputState
                        .candidate
                        ?.eventRef

                /*
                 * A live lab scenario may intentionally target a specific stable eventRef to prove
                 * list-position/geometry conditions. Outside the lab, retain the current candidate
                 * while it remains in the active queue, then advance normally.
                 */
                activePool.firstOrNull { event ->
                    event.eventRef == labTargetRef
                } ?: activePool.firstOrNull { event ->
                    event.eventRef == retainedCandidateRef
                } ?: activePool.firstOrNull()
            } else {
                null
            }

        val candidate =
            candidateEvent?.let { event ->
                EventsAttentionCandidate(
                    eventRef = event.eventRef,
                    targetElementRef =
                        EventsPresentationRefs.row(
                            event.eventRef,
                        ),
                )
            }

        val availability =
            when {
                feed == null ||
                    preferences == null ||
                    screenId == null ||
                    visibleEventRefs == null ||
                    transientBlocked == null ->
                    EventsAttentionAvailability.UNOBSERVED

                candidate == null ->
                    EventsAttentionAvailability.NONE

                screenId != EventsScreen.screenId ->
                    EventsAttentionAvailability.HELD_OTHER_SCREEN

                transientBlocked ->
                    EventsAttentionAvailability.HELD_TRANSIENT_OCCUPIED

                candidate.eventRef !in visibleEventRefs ->
                    EventsAttentionAvailability.HELD_TARGET_UNREACHABLE

                else ->
                    EventsAttentionAvailability.READY
            }

        val currentQueueCount =
            normalQueue.size +
                suspendedQueue.size
        val nextSequenceCount =
            deferredQueue.size
        val unhandledCount =
            currentQueueCount +
                nextSequenceCount

        val activeQueueCount =
            when {
                normalQueue.isNotEmpty() ->
                    currentQueueCount

                suspendedQueue.isNotEmpty() ->
                    suspendedQueue.size

                else ->
                    deferredQueue.size
            }

        return currentState.copy(
            inputState = coherentInput,
            outputState =
                EventsAttentionOutputState(
                    candidate = candidate,
                    availability = availability,
                    unhandledCount = unhandledCount,
                    queuedAfterCurrentCount =
                        if (candidate == null) {
                            0
                        } else {
                            (activeQueueCount - 1)
                                .coerceAtLeast(0)
                        },
                    acknowledgedCount =
                        dispositions.values.count { disposition ->
                            disposition ==
                                com.vextreme.vexlife.android.data.events.EventsAttentionDisposition.ACKNOWLEDGED
                        },
                    deferredCount =
                        deferredQueue.size,
                    suspendedCount =
                        suspendedQueue.size,
                    suppressedCount =
                        dispositions.values.count { disposition ->
                            disposition ==
                                com.vextreme.vexlife.android.data.events.EventsAttentionDisposition.SUPPRESSED
                        },
                    currentQueueCount =
                        currentQueueCount,
                    nextSequenceCount =
                        nextSequenceCount,
                ),
        )
    }
}

// [VXG RealForever]
