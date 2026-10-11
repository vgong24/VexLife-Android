package com.vextreme.vexlife.android.screens.primary.events

import com.vextreme.vexlife.android.data.events.EventsAttentionDisposition
import com.vextreme.vexlife.android.data.events.EventsAttentionPreferenceState
import com.vextreme.vexlife.android.data.events.EventsFeedState
import com.vextreme.vexlife.android.data.events.VexExternalEvent
import com.vextreme.vexlife.android.data.events.VexExternalEventKind
import com.vextreme.vexlife.android.data.events.VexExternalEventSourceSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EventsAttentionRelayTest {
    private fun feed(): EventsFeedState {
        val source = "source.test"
        val first =
            VexExternalEvent(
                "event.first",
                source,
                VexExternalEventKind.MESSAGE,
                "First",
                "First",
                10L,
            )
        val second =
            VexExternalEvent(
                "event.second",
                source,
                VexExternalEventKind.CALL,
                "Second",
                "Second",
                20L,
            )

        return EventsFeedState(
            sourceSnapshots =
                mapOf(
                    source to
                        VexExternalEventSourceSnapshot(
                            source,
                            1L,
                            listOf(first, second),
                        ),
                ),
        )
    }

    @Test
    fun candidateIdentitySurvivesScreenReachabilityAndTransientChanges() {
        val relay = EventsAttentionRelay()

        assertNull(relay.current.outputState.candidate)
        assertEquals(
            EventsAttentionAvailability.UNOBSERVED,
            relay.current.outputState.availability,
        )

        relay.feedChanged(feed())
        relay.preferencesChanged(
            EventsAttentionPreferenceState(),
        )
        relay.visibleTargetsChanged(
            setOf("event.first", "event.second"),
        )
        relay.transientBlockedChanged(false)
        relay.screenChanged("screen.home")

        assertEquals(
            "event.first",
            relay.current.outputState.candidate?.eventRef,
        )
        assertEquals(
            EventsAttentionAvailability.HELD_OTHER_SCREEN,
            relay.current.outputState.availability,
        )

        relay.screenChanged(EventsScreen.screenId)

        assertEquals(
            "event.first",
            relay.current.outputState.candidate?.eventRef,
        )
        assertEquals(
            EventsAttentionAvailability.READY,
            relay.current.outputState.availability,
        )
        assertEquals(
            2,
            relay.current.outputState.unhandledCount,
        )
        assertEquals(
            1,
            relay.current.outputState.queuedAfterCurrentCount,
        )

        relay.visibleTargetsChanged(
            setOf("event.second"),
        )

        assertEquals(
            "event.first",
            relay.current.outputState.candidate?.eventRef,
        )
        assertEquals(
            EventsAttentionAvailability.HELD_TARGET_UNREACHABLE,
            relay.current.outputState.availability,
        )

        relay.visibleTargetsChanged(
            setOf("event.first", "event.second"),
        )
        relay.transientBlockedChanged(true)

        assertEquals(
            "event.first",
            relay.current.outputState.candidate?.eventRef,
        )
        assertEquals(
            EventsAttentionAvailability.HELD_TRANSIENT_OCCUPIED,
            relay.current.outputState.availability,
        )

        relay.transientBlockedChanged(false)
        assertEquals(
            EventsAttentionAvailability.READY,
            relay.current.outputState.availability,
        )

        relay.preferencesChanged(
            EventsAttentionPreferenceState(
                mapOf(
                    "event.first" to
                        EventsAttentionDisposition.ACKNOWLEDGED,
                ),
            ),
        )

        assertEquals(
            "event.second",
            relay.current.outputState.candidate?.eventRef,
        )
        assertEquals(
            EventsAttentionAvailability.READY,
            relay.current.outputState.availability,
        )
        assertEquals(
            1,
            relay.current.outputState.unhandledCount,
        )
        assertEquals(
            0,
            relay.current.outputState.queuedAfterCurrentCount,
        )
        assertEquals(
            1,
            relay.current.outputState.acknowledgedCount,
        )
        assertEquals(
            0,
            relay.current.outputState.deferredCount,
        )
        assertEquals(
            0,
            relay.current.outputState.suppressedCount,
        )
    }

    @Test
    fun candidateIdentitySurvivesProviderInsertReorderAndRemoval() {
        val source = "source.reorder"
        val first =
            VexExternalEvent(
                "event.first",
                source,
                VexExternalEventKind.MESSAGE,
                "First",
                "First",
                10L,
            )
        val second =
            VexExternalEvent(
                "event.second",
                source,
                VexExternalEventKind.CALL,
                "Second",
                "Second",
                20L,
            )
        val insertedOlder =
            VexExternalEvent(
                "event.inserted-older",
                source,
                VexExternalEventKind.MAP,
                "Inserted older",
                "Inserted older",
                5L,
            )

        fun feed(
            revision: Long,
            events: List<VexExternalEvent>,
        ) =
            EventsFeedState(
                sourceSnapshots =
                    mapOf(
                        source to
                            VexExternalEventSourceSnapshot(
                                source,
                                revision,
                                events,
                            ),
                    ),
            )

        val relay = EventsAttentionRelay()

        relay.feedChanged(feed(1L, listOf(first, second)))
        relay.preferencesChanged(
            EventsAttentionPreferenceState(),
        )
        relay.visibleTargetsChanged(
            setOf(
                first.eventRef,
                second.eventRef,
                insertedOlder.eventRef,
            ),
        )
        relay.transientBlockedChanged(false)
        relay.screenChanged(EventsScreen.screenId)

        assertEquals(
            first.eventRef,
            relay.current.outputState.candidate?.eventRef,
        )

        // A later provider revision inserts an even older item and reorders the list.
        // Existing candidate identity must remain stable while it is still present/unhandled.
        relay.feedChanged(
            feed(
                2L,
                listOf(
                    second.copy(
                        occurredAtEpochMillis = 30L,
                    ),
                    insertedOlder,
                    first,
                ),
            ),
        )

        assertEquals(
            first.eventRef,
            relay.current.outputState.candidate?.eventRef,
        )

        // If the provider removes the retained candidate from its current projection,
        // the Relay may advance to the next unhandled event.
        relay.feedChanged(
            feed(
                3L,
                listOf(
                    second,
                    insertedOlder,
                ),
            ),
        )

        assertEquals(
            insertedOlder.eventRef,
            relay.current.outputState.candidate?.eventRef,
        )
    }

    @Test
    fun handledFeedExposesNoneInsteadOfUnobserved() {
        val relay = EventsAttentionRelay()

        relay.feedChanged(feed())
        relay.preferencesChanged(
            EventsAttentionPreferenceState(
                mapOf(
                    "event.first" to
                        EventsAttentionDisposition.ACKNOWLEDGED,
                    "event.second" to
                        EventsAttentionDisposition.SUPPRESSED,
                ),
            ),
        )
        relay.visibleTargetsChanged(
            setOf("event.first", "event.second"),
        )
        relay.transientBlockedChanged(false)
        relay.screenChanged(EventsScreen.screenId)

        assertNull(relay.current.outputState.candidate)
        assertEquals(
            EventsAttentionAvailability.NONE,
            relay.current.outputState.availability,
        )
        assertEquals(0, relay.current.outputState.unhandledCount)
        assertEquals(0, relay.current.outputState.queuedAfterCurrentCount)
        assertEquals(1, relay.current.outputState.acknowledgedCount)
        assertEquals(0, relay.current.outputState.deferredCount)
        assertEquals(1, relay.current.outputState.suppressedCount)
    }
}

// [VXG RealForever]
