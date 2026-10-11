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
                eventRef = "event.first",
                sourceRef = source,
                kind = VexExternalEventKind.MESSAGE,
                title = "First",
                detail = "First",
                occurredAtEpochMillis = 10L,
            )
        val second =
            VexExternalEvent(
                eventRef = "event.second",
                sourceRef = source,
                kind = VexExternalEventKind.CALL,
                title = "Second",
                detail = "Second",
                occurredAtEpochMillis = 20L,
            )

        return EventsFeedState(
            sourceSnapshots =
                mapOf(
                    source to
                        VexExternalEventSourceSnapshot(
                            sourceRef = source,
                            sourceRevision = 1L,
                            events = listOf(first, second),
                        ),
                ),
        )
    }

    @Test
    fun currentScreenAndPreferencesRecomputeOneStableCandidate() {
        val relay =
            EventsAttentionRelay()

        relay.feedChanged(feed())
        relay.preferencesChanged(
            EventsAttentionPreferenceState(),
        )
        relay.screenChanged("screen.home")

        assertNull(relay.current.outputState.candidate)

        relay.screenChanged(EventsScreen.screenId)

        assertEquals(
            "event.first",
            relay.current.outputState.candidate?.eventRef,
        )
        assertEquals(
            EventsPresentationRefs.row("event.first"),
            relay.current.outputState.candidate
                ?.targetElementRef,
        )

        relay.preferencesChanged(
            EventsAttentionPreferenceState(
                dispositionByEventRef =
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

        relay.screenChanged("screen.settings")
        assertNull(relay.current.outputState.candidate)

        relay.screenChanged(EventsScreen.screenId)
        assertEquals(
            "event.second",
            relay.current.outputState.candidate?.eventRef,
        )
    }
}

// [VXG RealForever]
