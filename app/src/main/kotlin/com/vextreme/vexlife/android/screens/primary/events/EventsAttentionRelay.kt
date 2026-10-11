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

data class EventsAttentionOutputState(
    val candidate: EventsAttentionCandidate? = null,
) : VexOutputState<EventsAttentionOutputState> {
    override fun deepCopy(): EventsAttentionOutputState =
        copy(candidate = candidate?.copy())
}

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
            if (
                screenId == EventsScreen.screenId &&
                transientBlocked == false &&
                visibleEventRefs != null
            ) {
                feed
                    ?.events
                    .orEmpty()
                    .asReversed()
                    .firstOrNull { event ->
                        event.eventRef in visibleEventRefs &&
                            preferences
                                ?.dispositionByEventRef
                                ?.containsKey(event.eventRef)
                                != true
                    }
            } else {
                null
            }

        val output =
            EventsAttentionOutputState(
                candidate =
                    candidateEvent?.let { event ->
                        EventsAttentionCandidate(
                            eventRef = event.eventRef,
                            targetElementRef =
                                EventsPresentationRefs.row(
                                    event.eventRef,
                                ),
                        )
                    },
            )

        return currentState.copy(
            inputState = coherentInput,
            outputState = output,
        )
    }
}

// [VXG RealForever]
