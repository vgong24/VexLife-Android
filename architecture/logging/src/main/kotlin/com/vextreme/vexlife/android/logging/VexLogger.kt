package com.vextreme.vexlife.android.logging

import androidx.compose.runtime.State
import com.vextreme.vexlife.android.behavior.VexFamilyLogEvent
import com.vextreme.vexlife.android.behavior.VexFamilyLogger
import androidx.compose.runtime.mutableStateOf
import com.vextreme.vexlife.android.elements.ScreenElement
import com.vextreme.vexlife.android.elements.UIElement
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class VexLogKind {
    AppOpened,
    Interaction,
    Gesture,
    Family,
}

/**
 * Canonical readable diagnostic fact.
 *
 * [detailsJson] is optional structured detail captured only at an explicit semantic boundary such
 * as form Submit. Draft field edits are never emitted as records.
 */
data class VexLogRecord(
    val sequence: Long,
    val timestamp: Instant,
    val durationSincePreviousSeconds: Long?,
    val kind: VexLogKind,
    val subjectRef: String,
    val action: String,
    val screenBeforeId: String?,
    val screenAfterId: String?,
    val section: Long,
    val detailsJson: String? = null,
)

/**
 * Derived readable scope: one semantic Screen visit and the events that happened inside it.
 *
 * Screen duration is computed from entry -> transition time, not by summing per-event gaps.
 */
data class VexLogScreenScope(
    val screenId: String,
    val enteredAt: Instant,
    val exitedAt: Instant?,
    val records: List<VexLogRecord>,
) {
    val durationSeconds: Long?
        get() = exitedAt?.let { exit ->
            Duration.between(enteredAt, exit).seconds.coerceAtLeast(0L)
        }
}

class VexLogger(
    private val clock: () -> Instant = { Instant.now() },
    private val zoneId: () -> ZoneId = { ZoneId.systemDefault() },
    private val maxRecords: Int = 256,
    private val onRecord: (VexLogRecord) -> Unit = {},
) :
    VexFamilyLogger {
    private val lock = Any()
    private val _history = mutableStateOf<List<VexLogRecord>>(emptyList())
    private var nextSequence = 1L
    private var lastTimestamp: Instant? = null
    private var currentSection = 0L

    val history: State<List<VexLogRecord>>
        get() = _history

    init {
        require(maxRecords > 0) { "maxRecords must be positive" }
    }

    /**
     * Family accountant projection. The Family remains the truth owner; this logger only witnesses
     * the accepted fact in sequence.
     */
    override fun record(
        event: VexFamilyLogEvent,
    ) {
        append(
            kind =
                VexLogKind.Family,
            subjectRef =
                familySubjectRef(
                    event,
                ),
            action =
                familyAction(
                    event,
                ),
            screenBeforeId =
                null,
            screenAfterId =
                null,
        )
    }

    fun appOpened(screen: ScreenElement) {
        append(
            kind = VexLogKind.AppOpened,
            subjectRef = "app",
            action = "App Opened",
            screenBeforeId = null,
            screenAfterId = screen.screenId,
        )
    }

    fun recordInteraction(
        element: UIElement,
        screenBefore: ScreenElement,
        screenAfter: ScreenElement? = null,
        action: String = "Clicked",
        detailsJson: String? = null,
    ) {
        recordInteractionRef(
            subjectRef = element.elementId,
            screenBefore = screenBefore,
            screenAfter = screenAfter,
            action = action,
            detailsJson = detailsJson,
        )
    }

    /**
     * Dynamic repeated elements use a stable semantic ref even though they cannot all be members of
     * the static UI registry. This records that already-formed identity without creating a second
     * logger or pretending list position is identity.
     */
    fun recordInteractionRef(
        subjectRef: String,
        screenBefore: ScreenElement,
        screenAfter: ScreenElement? = null,
        action: String = "Clicked",
        detailsJson: String? = null,
    ) {
        require(subjectRef.isNotBlank()) {
            "interaction subjectRef must not be blank"
        }

        append(
            kind = VexLogKind.Interaction,
            subjectRef = subjectRef,
            action = action,
            screenBeforeId = screenBefore.screenId,
            screenAfterId = screenAfter?.screenId,
            detailsJson = detailsJson,
        )
    }

    fun recordBack(
        screenBefore: ScreenElement,
        screenAfter: ScreenElement? = null,
    ) {
        append(
            kind = VexLogKind.Gesture,
            subjectRef = "system.back",
            action = "Back",
            screenBeforeId = screenBefore.screenId,
            screenAfterId = screenAfter?.screenId,
        )
    }

    fun recordScreenGesture(
        screenBefore: ScreenElement,
        screenAfter: ScreenElement,
    ) {
        append(
            kind = VexLogKind.Gesture,
            subjectRef = "presentation.screen-pager",
            action = "Swiped",
            screenBeforeId = screenBefore.screenId,
            screenAfterId = screenAfter.screenId,
        )
    }

    fun startNewSection() {
        synchronized(lock) {
            currentSection += 1
        }
    }

    fun clearHistory() {
        synchronized(lock) {
            _history.value = emptyList()
            nextSequence = 1L
            lastTimestamp = null
            currentSection = 0L
        }

        try {
            VexLogcat.historyCleared()
        } catch (_: Throwable) {
            // Logcat is a non-authoritative developer projection.
        }
    }

    fun renderHistory(): String = formatHistory(
        records = history.value,
        zoneId = zoneId(),
    )

    fun renderFamilyHistory(): String =
        formatFamilyHistory(
            records =
                history.value,
            zoneId =
                zoneId(),
        )

    private fun append(
        kind: VexLogKind,
        subjectRef: String,
        action: String,
        screenBeforeId: String?,
        screenAfterId: String?,
        detailsJson: String? = null,
    ) {
        val record = synchronized(lock) {
            val now = clock()
            val duration = lastTimestamp
                ?.let { Duration.between(it, now).seconds.coerceAtLeast(0L) }

            VexLogRecord(
                sequence = nextSequence++,
                timestamp = now,
                durationSincePreviousSeconds = duration,
                kind = kind,
                subjectRef = subjectRef,
                action = action,
                screenBeforeId = screenBeforeId,
                screenAfterId = screenAfterId,
                section = currentSection,
                detailsJson = detailsJson,
            ).also {
                _history.value = (_history.value + it).takeLast(maxRecords)
                lastTimestamp = now
            }
        }

        try {
            VexLogcat.record(record)
        } catch (_: Throwable) {
            // Logcat is a non-authoritative developer projection.
        }

        try {
            onRecord(record)
        } catch (_: Throwable) {
            // Downstream diagnostics are deliberately non-authoritative.
        }
    }

    companion object {
        /**
         * Groups canonical records into semantic Screen visits.
         *
         * A transition record belongs to its source Screen. The destination scope begins at that
         * same timestamp. A later AppOpened starts a new session scope rather than pretending the
         * app spent the offline interval on the previous Screen.
         */
        fun screenScopes(records: List<VexLogRecord>): List<VexLogScreenScope> {
            if (records.isEmpty()) return emptyList()

            val result = mutableListOf<VexLogScreenScope>()
            var currentScreenId: String? = null
            var enteredAt: Instant? = null
            var currentRecords = mutableListOf<VexLogRecord>()

            fun finish(exitedAt: Instant?) {
                val screenId = currentScreenId ?: return
                val entry = enteredAt ?: return
                result += VexLogScreenScope(
                    screenId = screenId,
                    enteredAt = entry,
                    exitedAt = exitedAt,
                    records = currentRecords.toList(),
                )
                currentScreenId = null
                enteredAt = null
                currentRecords = mutableListOf()
            }

            fun begin(screenId: String, timestamp: Instant) {
                currentScreenId = screenId
                enteredAt = timestamp
                currentRecords = mutableListOf()
            }

            records.forEach { record ->
                if (
                    record.kind ==
                    VexLogKind.Family
                ) {
                    return@forEach
                }

                val eventScreenId = when {
                    record.kind == VexLogKind.AppOpened ->
                        record.screenAfterId ?: record.screenBeforeId

                    record.screenBeforeId != null ->
                        record.screenBeforeId

                    else ->
                        record.screenAfterId
                } ?: "screen.unknown"

                if (record.kind == VexLogKind.AppOpened) {
                    if (currentScreenId != null) {
                        // A new app session does not establish when the prior process actually ended.
                        finish(exitedAt = null)
                    }
                    begin(eventScreenId, record.timestamp)
                    currentRecords += record
                    return@forEach
                }

                if (currentScreenId == null) {
                    begin(eventScreenId, record.timestamp)
                } else if (currentScreenId != eventScreenId) {
                    // Handles truncated histories whose first observed record begins mid-session.
                    finish(exitedAt = record.timestamp)
                    begin(eventScreenId, record.timestamp)
                }

                currentRecords += record

                val destination = record.screenAfterId
                if (
                    record.screenBeforeId != null &&
                    destination != null &&
                    record.screenBeforeId != destination
                ) {
                    finish(exitedAt = record.timestamp)
                    begin(destination, record.timestamp)
                }
            }

            if (currentScreenId != null) {
                finish(exitedAt = null)
            }

            return result
        }

        fun formatHistory(
            records: List<VexLogRecord>,
            zoneId: ZoneId,
        ): String {
            if (records.isEmpty()) return "No actions recorded yet."

            val formatter = DateTimeFormatter
                .ofPattern("h:mm:ssa MMM d, yyyy", Locale.US)
                .withZone(zoneId)

            return buildString {
                screenScopes(records).forEachIndexed { scopeIndex, scope ->
                    if (scopeIndex > 0) appendLine()
                    appendLine("---${screenName(scope.screenId)}---")

                    scope.records.forEachIndexed { recordIndex, record ->
                        if (recordIndex > 0) appendLine()
                        appendLine("[${formatter.format(record.timestamp)}]")

                        when (record.kind) {
                            VexLogKind.AppOpened ->
                                appendLine("App Opened")

                            else -> {
                                val subject = eventName(record.subjectRef)
                                if (subject.equals(record.action, ignoreCase = true)) {
                                    appendLine(record.action)
                                } else {
                                    appendLine("$subject ${record.action}")
                                }

                                record.detailsJson
                                    ?.takeIf { it.isNotBlank() }
                                    ?.let { details ->
                                        appendLine(details)
                                    }
                            }
                        }
                    }

                    scope.durationSeconds?.let { seconds ->
                        if (scope.records.isNotEmpty()) appendLine()
                        appendLine("[Duration: $seconds seconds]")
                    }
                }
            }.trimEnd()
        }

        fun screenName(screenId: String?): String {
            if (screenId == null) return "UnknownScreen"
            val body = screenId.removePrefix("screen.")
            return body.split('.')
                .joinToString("") { pascal(it) } + "Screen"
        }

        /**
         * Compact event label used inside a Screen scope. The full stable ref remains in the record.
         */
        fun eventName(ref: String): String {
            if (ref == "system.back") return "Back"
            val body = ref
                .removePrefix("element.")
                .removePrefix("presentation.")
            return pascal(body.substringAfterLast('.'))
        }

        fun subjectName(ref: String): String {
            val body = ref
                .removePrefix("element.")
                .removePrefix("presentation.")
            return body.split('.')
                .joinToString(".") { pascal(it) }
        }

        fun formatFamilyHistory(
            records: List<VexLogRecord>,
            zoneId: ZoneId,
        ): String {
            val familyRecords =
                records.filter { record ->
                    record.kind ==
                        VexLogKind.Family
                }

            if (
                familyRecords.isEmpty()
            ) {
                return "No Family actions recorded yet."
            }

            val formatter =
                DateTimeFormatter
                    .ofPattern(
                        "h:mm:ssa MMM d, yyyy",
                        Locale.US,
                    )
                    .withZone(
                        zoneId,
                    )

            return buildString {
                familyRecords.forEachIndexed { index, record ->
                    if (
                        index >
                        0
                    ) {
                        appendLine()
                    }

                    append(
                        "[",
                    )
                    append(
                        formatter.format(
                            record.timestamp,
                        ),
                    )
                    appendLine(
                        "]",
                    )
                    append(
                        record.subjectRef
                            .substringAfterLast(
                                '.',
                            ),
                    )
                    append(
                        " ",
                    )
                    appendLine(
                        record.action,
                    )
                }
            }.trimEnd()
        }

        private fun familySubjectRef(
            event: VexFamilyLogEvent,
        ): String =
            event.family.qualifiedName
                ?: event.family.simpleName
                ?: "Family"

        private fun familyAction(
            event: VexFamilyLogEvent,
        ): String =
            when (
                event
            ) {
                is VexFamilyLogEvent.ControlAccepted ->
                    readableSourceName(
                        event.control,
                    ) +
                        " Accepted @ " +
                        readableSourceName(
                            event.currentState,
                        )

                is VexFamilyLogEvent.StateAdmitted ->
                    readableSourceName(
                        event.fromState,
                    ) +
                        " -> " +
                        readableSourceName(
                            event.toState,
                        ) +
                        " Admitted"

                is VexFamilyLogEvent.OperationBegan ->
                    pascal(
                        event.operation.name,
                    ) +
                        operationStateSuffix(
                            event.state,
                        ) +
                        " Began"

                is VexFamilyLogEvent.OperationSuccess ->
                    pascal(
                        event.operation.name,
                    ) +
                        operationStateSuffix(
                            event.state,
                        ) +
                        " Success"

                is VexFamilyLogEvent.OperationFailure ->
                    buildString {
                        append(
                            pascal(
                                event.operation.name,
                            ),
                        )
                        append(
                            operationStateSuffix(
                                event.state,
                            ),
                        )
                        append(
                            " Failure",
                        )
                        event.failure
                            ?.let { failure ->
                                append(
                                    " [",
                                )
                                append(
                                    readableSourceName(
                                        failure,
                                    ),
                                )
                                append(
                                    "]",
                                )
                            }
                    }
            }

        private fun operationStateSuffix(
            state:
                kotlin.reflect.KClass<*>?,
        ): String =
            state
                ?.let { current ->
                    "(" +
                        readableSourceName(
                            current,
                        ) +
                        ")"
                }
                ?: ""

        private fun readableSourceName(
            source:
                kotlin.reflect.KClass<*>,
        ): String =
            source.simpleName
                ?: source.qualifiedName
                ?: "Unknown"

        private fun pascal(value: String): String =
            value.split('-', '_')
                .filter { it.isNotBlank() }
                .joinToString("") { part ->
                    part.replaceFirstChar { ch ->
                        if (ch.isLowerCase()) ch.titlecase(Locale.US) else ch.toString()
                    }
                }
    }
}
