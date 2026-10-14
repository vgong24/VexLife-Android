package com.vextreme.vexlife.android.data.events

/**
 * Deliberately incompatible partner model used to pressure the Events source boundary.
 *
 * The partner speaks in:
 * - opaque cursors rather than monotonic numeric revisions;
 * - raw remote IDs that are not semantic refs;
 * - ACTIVE / CANCELLED record status;
 * - severity codes and affected transit routes;
 * - optional start time in epoch seconds.
 *
 * None of those partner-specific facts belong in EventsConvergenceRelay or a presentation consumer.
 */
data class PartnerTransitAdvisoryBatch(
    val partnerAccountId: String,
    val opaqueCursor: String,
    val fetchedAtEpochSeconds: Long,
    val records: List<PartnerTransitAdvisoryRecord>,
) {
    init {
        require(partnerAccountId.isNotBlank()) {
            "partnerAccountId must not be blank"
        }
        require(opaqueCursor.isNotBlank()) {
            "opaqueCursor must not be blank"
        }
        require(fetchedAtEpochSeconds >= 0L) {
            "fetchedAtEpochSeconds must be non-negative"
        }
    }
}

data class PartnerTransitAdvisoryRecord(
    val remoteId: String,
    val status: PartnerTransitAdvisoryStatus,
    val severityCode: Int,
    val headline: String?,
    val description: String,
    val affectedRouteNames: List<String>,
    val startsAtEpochSeconds: Long?,
) {
    init {
        require(remoteId.isNotBlank()) {
            "partner remoteId must not be blank"
        }
        require(severityCode in 0..5) {
            "partner severityCode must be between 0 and 5"
        }
        require(startsAtEpochSeconds == null || startsAtEpochSeconds >= 0L) {
            "partner startsAtEpochSeconds must be non-negative when present"
        }
    }
}

enum class PartnerTransitAdvisoryStatus {
    ACTIVE,
    CANCELLED,
}

/**
 * Result of source-bound conversion.
 *
 * [opaqueCursor] remains acquisition metadata. The canonical Events repository intentionally does
 * not pretend the partner cursor is a numeric source revision; the acquisition owner supplies its
 * own monotonic [canonicalRevision] when admitting a new partner snapshot.
 */
data class PartnerTransitEventsConversion(
    val opaqueCursor: String,
    val snapshot: VexExternalEventSourceSnapshot,
)

fun PartnerTransitAdvisoryBatch.toVexEvents(
    canonicalRevision: Long,
): PartnerTransitEventsConversion {
    require(canonicalRevision >= 0L) {
        "canonicalRevision must be non-negative"
    }

    val sourceRef =
        "source.partner.transit." +
            partnerAccountId.toSemanticHex()

    val events =
        records
            .asSequence()
            .filter { record ->
                record.status ==
                    PartnerTransitAdvisoryStatus.ACTIVE
            }
            .map { record ->
                val occurredAtSeconds =
                    record.startsAtEpochSeconds
                        ?: fetchedAtEpochSeconds

                VexExternalEvent(
                    eventRef =
                        "event.partner.transit." +
                            partnerAccountId.toSemanticHex() +
                            "." +
                            record.remoteId.toSemanticHex(),
                    sourceRef = sourceRef,
                    kind = VexExternalEventKind.MAP,
                    title =
                        record.headline
                            ?.trim()
                            ?.takeIf(String::isNotEmpty)
                            ?: "Transit advisory",
                    detail =
                        buildString {
                            append("Severity ")
                            append(record.severityCode)
                            if (record.affectedRouteNames.isNotEmpty()) {
                                append(" · Routes ")
                                append(
                                    record.affectedRouteNames
                                        .joinToString(", "),
                                )
                            }
                            record.description
                                .trim()
                                .takeIf(String::isNotEmpty)
                                ?.let { detail ->
                                    append(" · ")
                                    append(detail)
                                }
                        },
                    occurredAtEpochMillis =
                        Math.multiplyExact(
                            occurredAtSeconds,
                            1_000L,
                        ),
                )
            }
            .toList()

    return PartnerTransitEventsConversion(
        opaqueCursor = opaqueCursor,
        snapshot =
            VexExternalEventSourceSnapshot(
                sourceRef = sourceRef,
                sourceRevision = canonicalRevision,
                events = events,
            ),
    )
}

/**
 * Partner IDs are explicitly not required to satisfy Vex semantic-ref syntax. Hex encoding keeps
 * identity deterministic and reversible without leaking partner punctuation into semantic refs.
 */
private fun String.toSemanticHex(): String =
    encodeToByteArray()
        .joinToString(separator = "") { byte ->
            "%02x".format(byte.toInt() and 0xff)
        }

// [VXG RealForever][EVENTS][INCOMPATIBLE-PARTNER-PRESSURE]
