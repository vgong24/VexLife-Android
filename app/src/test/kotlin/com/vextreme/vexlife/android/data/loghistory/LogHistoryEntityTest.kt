package com.vextreme.vexlife.android.data.loghistory

import com.vextreme.vexlife.android.logging.VexLogKind
import com.vextreme.vexlife.android.logging.VexLogRecord
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class LogHistoryEntityTest {
    @Test
    fun roomMappingPreservesDomainRecordWithoutUsingDatabaseIdAsSequence() {
        val record = VexLogRecord(
            sequence = 7,
            timestamp = Instant.parse("2026-10-11T01:54:30Z"),
            durationSincePreviousSeconds = 30,
            kind = VexLogKind.Interaction,
            subjectRef = "element.debug-menu.log-history.reset",
            action = "Clicked",
            screenBeforeId = "screen.debug-menu",
            screenAfterId = null,
            section = 2,
        )

        val entity = record.toEntity(sessionRef = "session-a")
        val roundTrip = entity.copy(databaseId = 99).toRecord()

        assertEquals("session-a", entity.sessionRef)
        assertEquals(99L, entity.copy(databaseId = 99).databaseId)
        assertEquals(record, roundTrip)
    }
}
