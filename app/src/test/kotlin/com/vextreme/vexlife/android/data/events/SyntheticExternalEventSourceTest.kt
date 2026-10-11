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
}

// [VXG RealForever]
