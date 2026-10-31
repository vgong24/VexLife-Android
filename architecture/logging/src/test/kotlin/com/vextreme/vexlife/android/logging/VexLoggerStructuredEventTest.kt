package com.vextreme.vexlife.android.logging

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VexLoggerStructuredEventTest {
    private class ExampleFamily
    private class ExampleControl
    private class ExampleState

    @Test
    fun structuredSinkFeedsTheSameChronologyWithoutAnotherLoggerRoot() {
        val logger =
            VexLogger()

        logger.sink.record(
            VexLogger.Event.Family.ControlAccepted(
                family =
                    ExampleFamily::class,
                control =
                    ExampleControl::class,
                currentState =
                    ExampleState::class,
            ),
        )

        val record =
            logger.history.value
                .single()

        assertEquals(
            VexLogKind.Semantic,
            record.kind,
        )
        assertEquals(
            "ControlAccepted",
            record.action,
        )
        assertTrue(
            record.subjectRef
                .startsWith(
                    "family.",
                ),
        )
        assertTrue(
            record.detailsJson
                .orEmpty()
                .contains(
                    "ExampleControl",
                ),
        )
    }
}

// [VXG RealForever]
