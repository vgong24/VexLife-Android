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
    fun currentContextReachabilityAndPreferencesProduceOneCandidate() {
        val relay = EventsAttentionRelay()

        relay.feedChanged(feed())
        relay.preferencesChanged(
            EventsAttentionPreferenceState(),
        )
        relay.visibleTargetsChanged(
            setOf("event.first", "event.second"),
        )
        relay.transientBlockedChanged(false)
        relay.screenChanged("screen.home")

        assertNull(relay.current.outputState.candidate)

        relay.screenChanged(EventsScreen.screenId)

        assertEquals(
            "event.first",
            relay.current.outputState.candidate?.eventRef,
        )

        relay.transientBlockedChanged(true)
        assertNull(relay.current.outputState.candidate)

        relay.transientBlockedChanged(false)
        assertEquals(
            "event.first",
            relay.current.outputState.candidate?.eventRef,
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

        relay.visibleTargetsChanged(
            setOf("event.first"),
        )
        assertNull(relay.current.outputState.candidate)

        relay.visibleTargetsChanged(
            setOf("event.second"),
        )
        assertEquals(
            "event.second",
            relay.current.outputState.candidate?.eventRef,
        )
    }
}

// [VXG RealForever]
