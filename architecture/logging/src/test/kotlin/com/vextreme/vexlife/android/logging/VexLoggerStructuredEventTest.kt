package com.vextreme.vexlife.android.logging

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VexLoggerStructuredEventTest {
    @Test
    fun semanticFactFeedsTheSameChronologyWithoutAnotherLoggerRoot() {
        val logger =
            VexLogger()

        logger.sink.record(
            VexLogger.Event.Semantic(
                subjectRef =
                    "family.example",
                action =
                    "ControlAccepted",
                detailsJson =
                    "{\"control\":\"Select\",\"mode\":\"Dark\"}",
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
            "family.example",
            record.subjectRef,
        )
        assertEquals(
            "ControlAccepted",
            record.action,
        )
        assertTrue(
            record.detailsJson
                .orEmpty()
                .contains(
                    "\"mode\":\"Dark\"",
                ),
        )
    }

    @Test
    fun runtimeFactCarriesMeaningfulOwnerDetailInsteadOfCallableIdentity() {
        val logger =
            VexLogger()

        logger.sink.record(
            VexLogger.Event.Runtime(
                subjectRef =
                    "family.example",
                action =
                    "EffectRequested",
                detailsJson =
                    "{\"effect\":\"Save\",\"mode\":\"Dark\"}",
            ),
        )

        val record =
            logger.history.value
                .single()

        assertEquals(
            VexLogKind.Runtime,
            record.kind,
        )
        assertEquals(
            "EffectRequested",
            record.action,
        )
        assertTrue(
            record.detailsJson
                .orEmpty()
                .contains(
                    "\"effect\":\"Save\"",
                ),
        )
        assertTrue(
            record.detailsJson
                .orEmpty()
                .contains(
                    "\"mode\":\"Dark\"",
                ),
        )
    }
}

// [VXG RealForever]
