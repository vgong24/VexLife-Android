package com.vextreme.vexlife.android.data.events

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PartnerTransitAdvisoryConversionTest {
    @Test
    fun incompatiblePartnerShapeNormalizesBeforeEventsSemantics() {
        val batch =
            PartnerTransitAdvisoryBatch(
                partnerAccountId = "west account? 4",
                opaqueCursor = "cursor:abc/next?token=17",
                fetchedAtEpochSeconds = 100L,
                records =
                    listOf(
                        PartnerTransitAdvisoryRecord(
                            remoteId = "alert 42?north",
                            status =
                                PartnerTransitAdvisoryStatus.ACTIVE,
                            severityCode = 4,
                            headline = "North line delayed",
                            description = "Signal problem",
                            affectedRouteNames =
                                listOf("North", "Airport"),
                            startsAtEpochSeconds = 120L,
                        ),
                        PartnerTransitAdvisoryRecord(
                            remoteId = "cancelled/old",
                            status =
                                PartnerTransitAdvisoryStatus.CANCELLED,
                            severityCode = 1,
                            headline = "Old advisory",
                            description = "No longer active",
                            affectedRouteNames = listOf("South"),
                            startsAtEpochSeconds = 80L,
                        ),
                        PartnerTransitAdvisoryRecord(
                            remoteId = "fallback-title",
                            status =
                                PartnerTransitAdvisoryStatus.ACTIVE,
                            severityCode = 2,
                            headline = "   ",
                            description = "",
                            affectedRouteNames = emptyList(),
                            startsAtEpochSeconds = null,
                        ),
                    ),
            )

        val converted =
            batch.toVexEvents(
                canonicalRevision = 7L,
            )

        assertEquals(
            batch.opaqueCursor,
            converted.opaqueCursor,
        )
        assertEquals(
            7L,
            converted.snapshot.sourceRevision,
        )
        assertTrue(
            converted.snapshot.sourceRef
                .startsWith("source.partner.transit."),
        )
        assertFalse(
            converted.snapshot.sourceRef.contains("?"),
        )
        assertFalse(
            converted.snapshot.sourceRef.contains(" "),
        )

        assertEquals(
            2,
            converted.snapshot.events.size,
        )

        val first =
            converted.snapshot.events
                .first { event ->
                    event.title ==
                        "North line delayed"
                }
        assertEquals(
            VexExternalEventKind.MAP,
            first.kind,
        )
        assertEquals(
            120_000L,
            first.occurredAtEpochMillis,
        )
        assertEquals(
            "Severity 4 · Routes North, Airport · Signal problem",
            first.detail,
        )
        assertFalse(
            first.eventRef.contains("?"),
        )
        assertFalse(
            first.eventRef.contains(" "),
        )

        val fallback =
            converted.snapshot.events
                .first { event ->
                    event.title ==
                        "Transit advisory"
                }
        assertEquals(
            100_000L,
            fallback.occurredAtEpochMillis,
        )
        assertEquals(
            "Severity 2",
            fallback.detail,
        )

        assertTrue(
            converted.snapshot.events.none { event ->
                event.detail.contains("No longer active")
            },
        )
    }

    @Test
    fun opaquePartnerCursorDoesNotMasqueradeAsCanonicalRevision() {
        val first =
            PartnerTransitAdvisoryBatch(
                partnerAccountId = "account-A",
                opaqueCursor = "opaque:first",
                fetchedAtEpochSeconds = 10L,
                records = emptyList(),
            ).toVexEvents(
                canonicalRevision = 41L,
            )

        val second =
            PartnerTransitAdvisoryBatch(
                partnerAccountId = "account-A",
                opaqueCursor = "opaque:totally-different-format",
                fetchedAtEpochSeconds = 11L,
                records = emptyList(),
            ).toVexEvents(
                canonicalRevision = 42L,
            )

        assertEquals(
            "opaque:first",
            first.opaqueCursor,
        )
        assertEquals(
            "opaque:totally-different-format",
            second.opaqueCursor,
        )
        assertEquals(
            41L,
            first.snapshot.sourceRevision,
        )
        assertEquals(
            42L,
            second.snapshot.sourceRevision,
        )
        assertEquals(
            first.snapshot.sourceRef,
            second.snapshot.sourceRef,
        )
    }
}

// [VXG RealForever][EVENTS][INCOMPATIBLE-PARTNER-CONVERSION-TEST]
