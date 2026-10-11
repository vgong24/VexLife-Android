package com.vextreme.vexlife.android.screens.primary.events

import com.vextreme.vexlife.android.data.events.EventsAttentionPreferenceState
import com.vextreme.vexlife.android.data.events.EventsFeedState
import com.vextreme.vexlife.android.relay.VexCompoundRelay
import com.vextreme.vexlife.android.relay.VexCompoundState
import com.vextreme.vexlife.android.relay.VexInputState
import com.vextreme.vexlife.android.relay.VexOutputState

data class EventsAttentionInputState(
    val currentScreenId: String? = null,
    val feed: EventsFeedState? = null,
    val preferences: EventsAttentionPreferenceState? = null,
    val visibleEventRefs: Set<String>? = null,
    val transientBlocked: Boolean? = null,
) : VexInputState<EventsAttentionInputState> {
    override fun deepCopy(): EventsAttentionInputState =
        copy(
            feed = feed?.deepCopy(),
            preferences = preferences?.deepCopy(),
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
                visibleEventRefs = visibleEventRefs?.toSet(),
                transientBlocked = transientBlocked,
            )

        val candidateEvent =
            if (feed != null && preferences != null) {
                val unhandled =
                    feed.events.filter { event ->
                        event.eventRef !in
                            preferences.dispositionByEventRef
                    }

                val retainedCandidateRef =
                    currentState.outputState
                        .candidate
                        ?.eventRef

                unhandled.firstOrNull { event ->
                    event.eventRef == retainedCandidateRef
                } ?: unhandled
                    .asReversed()
                    .firstOrNull()
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

        return currentState.copy(
            inputState = coherentInput,
            outputState =
                EventsAttentionOutputState(
                    candidate = candidate,
                    availability = availability,
                ),
        )
    }
}

// [VXG RealForever]
