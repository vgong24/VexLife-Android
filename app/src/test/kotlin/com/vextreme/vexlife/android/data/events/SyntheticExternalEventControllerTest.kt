package com.vextreme.vexlife.android.data.events

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SyntheticExternalEventControllerTest {
    @Test
    fun stopAndRestartContinuesTheSameProviderRevision() = runTest {
        val repository = RelayEventsRepository()
        var clock = 0L

        val controller =
            SyntheticExternalEventController(
                repository = repository,
                scope = backgroundScope,
                sourceFactory = { sink ->
                    SyntheticExternalEventSource(
                        sink = sink,
                        intervalMillis = 5_000L,
                        clock = {
                            clock += 1_000L
                            clock
                        },
                    )
                },
            )

        controller.start()
        advanceTimeBy(5_000L)
        runCurrent()

        assertEquals(
            1L,
            repository.current.sourceSnapshots
                .getValue(
                    SyntheticExternalEventSource.SOURCE_REF,
                )
                .sourceRevision,
        )

        controller.stop()

        // A restart attempted before cancellation completes must not create a second producer loop.
        controller.start()
        runCurrent()
        advanceTimeBy(10_000L)
        runCurrent()

        assertEquals(
            1L,
            repository.current.sourceSnapshots
                .getValue(
                    SyntheticExternalEventSource.SOURCE_REF,
                )
                .sourceRevision,
        )

        // Completion releases the single producer slot; the next Start resumes the same source.
        controller.start()
        advanceTimeBy(5_000L)
        runCurrent()

        assertEquals(
            2L,
            repository.current.sourceSnapshots
                .getValue(
                    SyntheticExternalEventSource.SOURCE_REF,
                )
                .sourceRevision,
        )
        assertEquals(
            2,
            repository.current.events.size,
        )
    }
    @Test
    fun resetDisablesTimedEmissionBeforePublishingEmptyProjection() = runTest {
        val repository = RelayEventsRepository()

        val controller =
            SyntheticExternalEventController(
                repository = repository,
                scope = backgroundScope,
                sourceFactory = { sink ->
                    SyntheticExternalEventSource(
                        sink = sink,
                        intervalMillis = 5_000L,
                        clock = { testScheduler.currentTime },
                    )
                },
            )

        controller.start()
        advanceTimeBy(4_999L)

        val reset =
            controller.resetCurrent()

        assertEquals(1L, reset.sourceRevision)
        assertEquals(0, reset.events.size)

        advanceTimeBy(10_000L)
        runCurrent()

        assertEquals(
            1L,
            repository.current.sourceSnapshots
                .getValue(
                    SyntheticExternalEventSource.SOURCE_REF,
                )
                .sourceRevision,
        )
        assertEquals(0, repository.current.events.size)
    }

}

// [VXG RealForever]
