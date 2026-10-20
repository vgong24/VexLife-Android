package com.vextreme.vexlife.android.testing.journey

import com.vextreme.vexlife.android.logging.VexLogKind
import com.vextreme.vexlife.android.logging.VexLogRecord
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VexJourneyHistoryVerifierTest {
    private val a = record("element.a", "Clicked")
    private val b = record("element.b", "Clicked")
    private val c = record("element.c", "Submitted")

    @Test
    fun identicalOrderedSequenceMatches() {
        val result = VexJourneyHistoryVerifier.compare(
            expected = listOf(a, b, c),
            actual = listOf(a, b, c),
        )

        assertTrue(result.matches)
        assertEquals(null, result.mismatch)
    }

    @Test
    fun missingRecordIsDetectedAtItsSequencePosition() {
        val result = VexJourneyHistoryVerifier.compare(
            expected = listOf(a, b, c),
            actual = listOf(a, c),
        )

        assertFalse(result.matches)
        assertEquals(1, result.mismatch?.index)
        assertEquals(
            VexJourneyHistoryMismatchKind.MISSING_ACTUAL_RECORD,
            result.mismatch?.kind,
        )
        assertEquals(b, result.mismatch?.expected)
        assertEquals(c, result.mismatch?.actual)
    }

    @Test
    fun reorderedRecordsFailAtTheFirstDivergence() {
        val result = VexJourneyHistoryVerifier.compare(
            expected = listOf(a, b, c),
            actual = listOf(a, c, b),
        )

        assertFalse(result.matches)
        assertEquals(1, result.mismatch?.index)
        assertEquals(
            VexJourneyHistoryMismatchKind.REORDERED_RECORD,
            result.mismatch?.kind,
        )
        assertEquals(b, result.mismatch?.expected)
        assertEquals(c, result.mismatch?.actual)
        assertTrue(
            result.render("negative-control")
                .contains("first divergent sequence index: 1"),
        )
    }

    @Test
    fun extraRecordIsDetected() {
        val result = VexJourneyHistoryVerifier.compare(
            expected = listOf(a, b),
            actual = listOf(a, b, c),
        )

        assertFalse(result.matches)
        assertEquals(2, result.mismatch?.index)
        assertEquals(
            VexJourneyHistoryMismatchKind.UNEXPECTED_ACTUAL_RECORD,
            result.mismatch?.kind,
        )
    }

    @Test
    fun projectionIgnoresTimestampDurationAndLoggerSequence() {
        val first = VexLogRecord(
            sequence = 1,
            timestamp = Instant.parse("2026-10-11T09:00:00Z"),
            durationSincePreviousSeconds = 1,
            kind = VexLogKind.Interaction,
            subjectRef = "element.profile.save",
            action = "Submitted",
            screenBeforeId = "screen.user-profile",
            screenAfterId = null,
            section = 0,
            detailsJson = "{\"name\":\"Victor\"}",
        )
        val laterSameMeaning = first.copy(
            sequence = 99,
            timestamp = Instant.parse("2026-10-11T09:03:42Z"),
            durationSincePreviousSeconds = 222,
            section = 7,
        )

        assertEquals(
            first.toJourneyRecord(),
            laterSameMeaning.toJourneyRecord(),
        )
    }

    private fun record(
        subjectRef: String,
        action: String,
    ): VexExpectedRecord =
        VexExpectedRecord(
            kind = VexLogKind.Interaction,
            subjectRef = subjectRef,
            action = action,
            screenBeforeId = "screen.test",
            screenAfterId = null,
        )
}
