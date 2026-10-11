package com.vextreme.vexlife.android.data.events

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class EventsRepositoryTest {
    private fun event(ref: String, source: String, at: Long) =
        VexExternalEvent(
            eventRef = ref,
            sourceRef = source,
            kind = VexExternalEventKind.SYSTEM,
            title = ref,
            detail = "fixture",
            occurredAtEpochMillis = at,
        )

    private fun snapshot(
        source: String,
        revision: Long,
        vararg events: VexExternalEvent,
    ) =
        VexExternalEventSourceSnapshot(
            sourceRef = source,
            sourceRevision = revision,
            events = events.toList(),
        )

    @Test
    fun sourceFreshnessIsIndependentPerProvider() {
        val repository = RelayEventsRepository()

        repository.accept(
            snapshot(
                "source.messaging",
                12,
                event("event.message.new", "source.messaging", 120),
            ),
        )
        repository.accept(
            snapshot(
                "source.maps",
                4,
                event("event.map.current", "source.maps", 130),
            ),
        )
        repository.accept(
            snapshot(
                "source.messaging",
                7,
                event("event.message.old", "source.messaging", 90),
            ),
        )

        assertEquals(12L, repository.current.sourceSnapshots.getValue("source.messaging").sourceRevision)
        assertEquals(4L, repository.current.sourceSnapshots.getValue("source.maps").sourceRevision)
        assertEquals(
            listOf("event.map.current", "event.message.new"),
            repository.current.events.map(VexExternalEvent::eventRef),
        )
    }

    @Test
    fun sameProviderRevisionCannotBeReusedForDifferentTruth() {
        val repository = RelayEventsRepository()
        val source = "source.messaging"

        repository.accept(
            snapshot(source, 3, event("event.message.a", source, 10)),
        )

        try {
            repository.accept(
                snapshot(source, 3, event("event.message.b", source, 11)),
            )
            fail("same source revision with different content must fail")
        } catch (failure: IllegalStateException) {
            assertTrue(failure.message.orEmpty().contains("reused revision"))
        }
    }

    @Test
    fun eventIdentityCannotCollideAcrossProviders() {
        val repository = RelayEventsRepository()

        repository.accept(
            snapshot(
                "source.messaging",
                1,
                event("event.shared", "source.messaging", 10),
            ),
        )

        try {
            repository.accept(
                snapshot(
                    "source.maps",
                    1,
                    event("event.shared", "source.maps", 11),
                ),
            )
            fail("eventRef collision across providers must fail")
        } catch (failure: IllegalArgumentException) {
            assertTrue(failure.message.orEmpty().contains("globally unique"))
        }
    }
}

// [VXG RealForever]
