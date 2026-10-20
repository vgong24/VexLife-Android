package com.vextreme.vexlife.android.data.loghistory

import com.vextreme.vexlife.android.screens.primary.home.HomeScreen
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LogHistoryPersistenceQueueTest {
    @Test
    fun clearIsOrderedBetweenEarlierAndLaterRecords() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val repository = InMemoryLogHistoryRepository()
        val fixture = LogHistoryFixture.build(
            startAt = Instant.parse("2026-10-11T01:51:00Z"),
        ) {
            appOpened(HomeScreen)
        }
        val afterReset = fixture.append {
            afterSeconds(10)
            back(from = HomeScreen)
        }.records.last()

        val queue = LogHistoryPersistenceQueue(
            repository = repository,
            scope = backgroundScope,
            dispatcher = dispatcher,
            capacity = 8,
        )

        queue.record(fixture.records.single())
        queue.clear()
        queue.record(afterReset)

        runCurrent()

        assertEquals(listOf(afterReset), repository.records.value)
    }
}
