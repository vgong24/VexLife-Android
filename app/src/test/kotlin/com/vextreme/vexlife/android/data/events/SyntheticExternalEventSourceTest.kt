package com.vextreme.vexlife.android.data.events

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SyntheticExternalEventSourceTest {
    @Test
    fun fiveSecondTicksProduceDeterministicProviderSnapshots() = runTest {
        val repository = RelayEventsRepository()
        var time = 10_000L

        val source =
            SyntheticExternalEventSource(
                sink = repository::accept,
                intervalMillis = 5_000L,
                clock = {
                    time += 1_000L
                    time
                },
            )

        backgroundScope.launch {
            source.runForever()
        }

        advanceTimeBy(4_999L)
        runCurrent()
        assertTrue(repository.current.events.isEmpty())

        advanceTimeBy(1L)
        runCurrent()
        assertEquals(
            VexExternalEventKind.MESSAGE,
            repository.current.events.single().kind,
        )

        advanceTimeBy(5_000L)
        runCurrent()
        assertEquals(
            listOf(
                VexExternalEventKind.CALL,
                VexExternalEventKind.MESSAGE,
            ),
            repository.current.events.map(VexExternalEvent::kind),
        )
        assertEquals(
            2L,
            repository.current.sourceSnapshots
                .getValue(SyntheticExternalEventSource.SOURCE_REF)
                .sourceRevision,
        )
    }

    @Test
    fun manualControlsExposeDeterministicListTortureConditions() {
        val repository = RelayEventsRepository()
        var now = 10_000L

        val source =
            SyntheticExternalEventSource(
                sink = repository::accept,
                clock = {
                    now += 1_000L
                    now
                },
            )

        source.emitBurst(count = 4)

        val beforeInsert =
            repository.current.events
        assertEquals(4, beforeInsert.size)

        val retainedRefs =
            beforeInsert
                .map(VexExternalEvent::eventRef)
                .toSet()

        source.insertHistorical()

        val afterInsert =
            repository.current.events
        assertEquals(5, afterInsert.size)
        assertTrue(
            afterInsert
                .map(VexExternalEvent::eventRef)
                .toSet()
                .containsAll(retainedRefs),
        )
        assertEquals(
            "event.synthetic.external.historical.0005",
            afterInsert.last().eventRef,
        )

        source.removeOldest()

        val afterRemoval =
            repository.current.events
        assertEquals(4, afterRemoval.size)
        assertTrue(
            afterRemoval.none { event ->
                event.eventRef ==
                    "event.synthetic.external.historical.0005"
            },
        )
        assertEquals(
            6L,
            repository.current.sourceSnapshots
                .getValue(
                    SyntheticExternalEventSource.SOURCE_REF,
                )
                .sourceRevision,
        )
    }

    @Test
    fun longRunningFixtureKeepsABoundedCurrentProjection() = runTest {
        val repository = RelayEventsRepository()
        val source =
            SyntheticExternalEventSource(
                sink = repository::accept,
                intervalMillis = 1L,
                clock = { testScheduler.currentTime },
                maxEvents = 3,
            )

        backgroundScope.launch {
            source.runForever()
        }

        advanceTimeBy(6L)
        runCurrent()

        val snapshot =
            repository.current.sourceSnapshots
                .getValue(SyntheticExternalEventSource.SOURCE_REF)

        assertEquals(6L, snapshot.sourceRevision)
        assertEquals(3, snapshot.events.size)
        assertEquals(
            listOf(
                "event.synthetic.external.0004",
                "event.synthetic.external.0005",
                "event.synthetic.external.0006",
            ),
            snapshot.events.map(VexExternalEvent::eventRef),
        )
    }
}

// [VXG RealForever]
