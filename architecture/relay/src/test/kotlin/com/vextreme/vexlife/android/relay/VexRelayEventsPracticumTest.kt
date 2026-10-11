package com.vextreme.vexlife.android.relay

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * EAP-00 uses Events as the real architectural question:
 *
 * external information may arrive while the human is somewhere else in the app, the canonical feed
 * must remain trustworthy, and presentation eligibility must be recomputed from current dependencies
 * rather than from where the information originally arrived.
 *
 * This is intentionally a pure JVM practicum. Timers, Android rendering, tooltip geometry and
 * Journey/ScreenCrawler realization belong to later Events slices.
 */
class VexRelayEventsPracticumTest {
    private data class EventItem(
        val eventRef: String,
        val summary: String,
        val tags: MutableList<String> = mutableListOf(),
    ) : VexDeepCopyable<EventItem> {
        override fun deepCopy(): EventItem =
            copy(tags = tags.toMutableList())
    }

    private data class ExternalFeedState(
        val sourceRevision: Long = 0L,
        val items: List<EventItem> = emptyList(),
    ) : VexState<ExternalFeedState> {
        override fun deepCopy(): ExternalFeedState =
            copy(items = items.map(EventItem::deepCopy))
    }

    private class ExternalFeedRelay :
        VexRelay<ExternalFeedState>(
            relayRef = "relay.events.external-feed",
            initialState = ExternalFeedState(),
        ) {
        override fun processState(
            inputState: ExternalFeedState,
            currentState: ExternalFeedState,
        ): ExternalFeedState =
            if (inputState.sourceRevision <= currentState.sourceRevision) {
                currentState.deepCopy()
            } else {
                inputState.deepCopy()
            }

        fun feedArrived(
            sourceRevision: Long,
            items: List<EventItem>,
        ): ExternalFeedState =
            send(
                ExternalFeedState(
                    sourceRevision = sourceRevision,
                    items = items,
                ),
            )
    }

    private data class ScreenState(
        val currentScreenId: String = "home",
    ) : VexState<ScreenState> {
        override fun deepCopy(): ScreenState = copy()
    }

    private data class AttentionPreferenceState(
        val acknowledgedEventRefs: Set<String> = emptySet(),
    ) : VexState<AttentionPreferenceState> {
        override fun deepCopy(): AttentionPreferenceState =
            copy(acknowledgedEventRefs = acknowledgedEventRefs.toSet())
    }

    private data class EventsInputState(
        val screen: ScreenState? = null,
        val feed: ExternalFeedState? = null,
        val preference: AttentionPreferenceState? = null,
    ) : VexInputState<EventsInputState> {
        override fun deepCopy(): EventsInputState =
            copy(
                screen = screen?.deepCopy(),
                feed = feed?.deepCopy(),
                preference = preference?.deepCopy(),
            )
    }

    private data class EventsOutputState(
        val eventRefs: List<String> = emptyList(),
        val attentionCandidateEventRef: String? = null,
    ) : VexOutputState<EventsOutputState> {
        override fun deepCopy(): EventsOutputState =
            copy(eventRefs = eventRefs.toList())
    }

    private class EventsAttentionRelay :
        VexCompoundRelay<EventsInputState, EventsOutputState>(
            relayRef = "relay.events.attention",
            initialInputState = EventsInputState(),
            initialOutputState = EventsOutputState(),
        ) {
        fun screenChanged(state: ScreenState) {
            send(EventsInputState(screen = state))
        }

        fun feedChanged(state: ExternalFeedState) {
            send(EventsInputState(feed = state))
        }

        fun preferenceChanged(state: AttentionPreferenceState) {
            send(EventsInputState(preference = state))
        }

        override fun processStates(
            inputState: EventsInputState,
            currentState: VexCompoundState<EventsInputState, EventsOutputState>,
        ): VexCompoundState<EventsInputState, EventsOutputState> {
            val previous =
                currentState.inputState

            val screen =
                inputState.screen ?: previous.screen
            val feed =
                inputState.feed ?: previous.feed
            val preference =
                inputState.preference ?: previous.preference

            val coherentInput =
                EventsInputState(
                    screen = screen?.deepCopy(),
                    feed = feed?.deepCopy(),
                    preference = preference?.deepCopy(),
                )

            val items =
                feed?.items.orEmpty()

            val acknowledged =
                preference?.acknowledgedEventRefs.orEmpty()

            val attentionCandidate =
                if (screen?.currentScreenId == "events") {
                    items.firstOrNull { item ->
                        item.eventRef !in acknowledged
                    }?.eventRef
                } else {
                    null
                }

            return currentState.copy(
                inputState = coherentInput,
                outputState =
                    EventsOutputState(
                        eventRefs = items.map(EventItem::eventRef),
                        attentionCandidateEventRef = attentionCandidate,
                    ),
            )
        }
    }

    @Test
    fun relayDetachesMutableCallerAndConsumerOwnership() {
        val tags = mutableListOf("incoming")
        val callerItems =
            mutableListOf(
                EventItem(
                    eventRef = "event.synthetic.001",
                    summary = "Synthetic message",
                    tags = tags,
                ),
            )
        val relay = ExternalFeedRelay()

        relay.feedArrived(
            sourceRevision = 1L,
            items = callerItems,
        )

        callerItems +=
            EventItem(
                eventRef = "event.caller-mutation",
                summary = "Must not leak",
            )
        tags += "caller-mutated"

        assertEquals(
            listOf("event.synthetic.001"),
            relay.current.items.map(EventItem::eventRef),
        )
        assertEquals(
            listOf("incoming"),
            relay.current.items.single().tags,
        )

        val exposed = relay.current.items.single()
        val reread = relay.current.items.single()
        assertNotSame(exposed.tags, reread.tags)

        exposed.tags += "consumer-mutated"
        assertEquals(
            listOf("incoming"),
            relay.current.items.single().tags,
        )
    }

    @Test
    fun staleExternalSourceRevisionCannotOverwriteNewerTruth() {
        val relay = ExternalFeedRelay()

        relay.feedArrived(
            sourceRevision = 20L,
            items = listOf(EventItem("event.new", "New")),
        )
        relay.feedArrived(
            sourceRevision = 10L,
            items = listOf(EventItem("event.old", "Old")),
        )

        assertEquals(20L, relay.current.sourceRevision)
        assertEquals(
            listOf("event.new"),
            relay.current.items.map(EventItem::eventRef),
        )
    }

    @Test
    fun eventCanExistWhilePresentationIsIneligibleThenBecomeEligibleOnCurrentScreen() {
        val feed = ExternalFeedRelay()
        val events = EventsAttentionRelay()

        events.screenChanged(ScreenState(currentScreenId = "home"))

        feed.feedArrived(
            sourceRevision = 1L,
            items =
                listOf(
                    EventItem(
                        eventRef = "event.synthetic.message.001",
                        summary = "Message arrived while Home was current",
                    ),
                ),
        )
        events.feedChanged(feed.current)

        assertEquals(
            listOf("event.synthetic.message.001"),
            events.current.outputState.eventRefs,
        )
        assertNull(events.current.outputState.attentionCandidateEventRef)

        events.screenChanged(ScreenState(currentScreenId = "events"))

        assertEquals(
            "event.synthetic.message.001",
            events.current.outputState.attentionCandidateEventRef,
        )
    }

    @Test
    fun acknowledgementRecomputesTheNextCurrentCandidateWithoutDeletingEventTruth() {
        val feed = ExternalFeedRelay()
        val events = EventsAttentionRelay()

        feed.feedArrived(
            sourceRevision = 1L,
            items =
                listOf(
                    EventItem("event.synthetic.001", "First"),
                    EventItem("event.synthetic.002", "Second"),
                ),
        )

        events.feedChanged(feed.current)
        events.screenChanged(ScreenState(currentScreenId = "events"))

        assertEquals(
            "event.synthetic.001",
            events.current.outputState.attentionCandidateEventRef,
        )

        events.preferenceChanged(
            AttentionPreferenceState(
                acknowledgedEventRefs = setOf("event.synthetic.001"),
            ),
        )

        assertEquals(
            listOf(
                "event.synthetic.001",
                "event.synthetic.002",
            ),
            events.current.outputState.eventRefs,
        )
        assertEquals(
            "event.synthetic.002",
            events.current.outputState.attentionCandidateEventRef,
        )
    }

    @Test
    fun concurrentDependencyPatchesCannotEraseSiblingTruth() {
        val feed = ExternalFeedRelay()
        val events = EventsAttentionRelay()

        feed.feedArrived(
            sourceRevision = 7L,
            items =
                listOf(
                    EventItem(
                        "event.synthetic.concurrent",
                        "Concurrent external information",
                    ),
                ),
        )

        val executor = Executors.newFixedThreadPool(3)
        val ready = CountDownLatch(3)
        val go = CountDownLatch(1)

        try {
            val futures =
                listOf(
                    executor.submit {
                        ready.countDown()
                        go.await()
                        events.screenChanged(
                            ScreenState(currentScreenId = "events"),
                        )
                    },
                    executor.submit {
                        ready.countDown()
                        go.await()
                        events.feedChanged(feed.current)
                    },
                    executor.submit {
                        ready.countDown()
                        go.await()
                        events.preferenceChanged(
                            AttentionPreferenceState(),
                        )
                    },
                )

            assertTrue(
                ready.await(
                    2L,
                    TimeUnit.SECONDS,
                ),
            )
            go.countDown()

            futures.forEach { future ->
                future.get(
                    2L,
                    TimeUnit.SECONDS,
                )
            }
        } finally {
            executor.shutdownNow()
        }

        assertEquals(
            "events",
            events.current.inputState.screen?.currentScreenId,
        )
        assertEquals(
            7L,
            events.current.inputState.feed?.sourceRevision,
        )
        assertEquals(
            listOf("event.synthetic.concurrent"),
            events.current.outputState.eventRefs,
        )
        assertEquals(
            "event.synthetic.concurrent",
            events.current.outputState.attentionCandidateEventRef,
        )
    }

    @Test
    fun compoundInputStatesExposeCoherentDependencyProjection() = runBlocking {
        val events =
            EventsAttentionRelay()

        val observed =
            mutableListOf<EventsInputState>()

        val collection =
            async(
                start = CoroutineStart.UNDISPATCHED,
            ) {
                events.inputStates
                    .take(3)
                    .toList(observed)
            }

        events.screenChanged(
            ScreenState(
                currentScreenId = "home",
            ),
        )
        events.preferenceChanged(
            AttentionPreferenceState(
                acknowledgedEventRefs =
                    setOf("event.input-proof"),
            ),
        )

        withTimeout(1_000L) {
            collection.await()
        }

        assertEquals(
            EventsInputState(),
            observed[0],
        )
        assertEquals(
            "home",
            observed[1].screen?.currentScreenId,
        )
        assertEquals(
            setOf("event.input-proof"),
            observed[2]
                .preference
                ?.acknowledgedEventRefs,
        )
        assertEquals(
            observed.last(),
            events.currentInput,
        )
    }

    @Test
    fun compoundOutputStatesSuppressInputOnlyRevisions() = runBlocking {
        val feed =
            ExternalFeedRelay()
        val events =
            EventsAttentionRelay()

        val observed =
            mutableListOf<EventsOutputState>()

        val collection =
            async(
                start = CoroutineStart.UNDISPATCHED,
            ) {
                events.outputStates
                    .take(2)
                    .toList(observed)
            }

        /*
         * Both mutations advance retained Compound input/currentness but derive the exact same empty
         * output. A downstream consumer should not see either revision.
         */
        events.screenChanged(
            ScreenState(
                currentScreenId = "home",
            ),
        )
        events.preferenceChanged(
            AttentionPreferenceState(),
        )

        feed.feedArrived(
            sourceRevision = 1L,
            items =
                listOf(
                    EventItem(
                        eventRef = "event.output-change",
                        summary = "Actual derived output change",
                    ),
                ),
        )
        events.feedChanged(
            feed.current,
        )

        yield()

        withTimeout(1_000L) {
            collection.await()
        }

        assertEquals(
            2,
            observed.size,
        )
        assertEquals(
            EventsOutputState(),
            observed.first(),
        )
        assertEquals(
            listOf("event.output-change"),
            observed.last().eventRefs,
        )
    }

    @Test
    fun semanticNoOpDoesNotAdvanceCompoundRevision() {
        val events = EventsAttentionRelay()

        events.screenChanged(ScreenState(currentScreenId = "events"))
        val afterFirst = events.current.revision

        events.screenChanged(ScreenState(currentScreenId = "events"))

        assertEquals(afterFirst, events.current.revision)
    }
}

// [VXG RealForever]
