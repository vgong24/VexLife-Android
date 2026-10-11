package com.vextreme.vexlife.android.data.loghistory

import com.vextreme.vexlife.android.pages.primary.home.HomePage
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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
            appOpened(HomePage)
        }
        val afterReset = fixture.append {
            afterSeconds(10)
            back(from = HomePage)
        }.records.last()

        val queue = LogHistoryPersistenceQueue(
            repository = repository,
            scope = this,
            dispatcher = dispatcher,
            capacity = 8,
        )

        queue.record(fixture.records.single())
        queue.clear()
        queue.record(afterReset)

        advanceUntilIdle()

        assertEquals(listOf(afterReset), repository.records.value)
    }
}
