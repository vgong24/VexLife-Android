package com.vextreme.vexlife.android.data.loghistory

import com.vextreme.vexlife.android.pages.primary.home.HomePage
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class InMemoryLogHistoryRepositoryTest {
    @Test
    fun appendAndClearMatchRepositoryContract() = runTest {
        val fixture = LogHistoryFixture.build(
            startAt = Instant.parse("2026-10-11T01:51:00Z"),
        ) {
            appOpened(HomePage)
        }
        val repository = InMemoryLogHistoryRepository(fixture.records)

        assertEquals(1, repository.records.value.size)

        repository.append(
            fixture.append {
                afterSeconds(10)
                back(from = HomePage)
            }.records.last(),
        )
        assertEquals(2, repository.records.value.size)

        repository.clear()
        assertEquals(0, repository.records.value.size)
    }
}
