package com.vextreme.vexlife.android.logging

import androidx.compose.runtime.State
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
}

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
)

class VexLogger(
    private val clock: () -> Instant = { Instant.now() },
    private val zoneId: () -> ZoneId = { ZoneId.systemDefault() },
    private val maxRecords: Int = 256,
) {
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
    ) {
        append(
            kind = VexLogKind.Interaction,
            subjectRef = element.elementId,
            action = action,
            screenBeforeId = screenBefore.screenId,
            screenAfterId = screenAfter?.screenId,
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
    }

    fun renderHistory(): String = formatHistory(
        records = history.value,
        zoneId = zoneId(),
    )

    private fun append(
        kind: VexLogKind,
        subjectRef: String,
        action: String,
        screenBeforeId: String?,
        screenAfterId: String?,
    ) {
        synchronized(lock) {
            val now = clock()
            val duration = lastTimestamp
                ?.let { Duration.between(it, now).seconds.coerceAtLeast(0L) }

            val record = VexLogRecord(
                sequence = nextSequence++,
                timestamp = now,
                durationSincePreviousSeconds = duration,
                kind = kind,
                subjectRef = subjectRef,
                action = action,
                screenBeforeId = screenBeforeId,
                screenAfterId = screenAfterId,
                section = currentSection,
            )

            _history.value = (_history.value + record).takeLast(maxRecords)
            lastTimestamp = now
        }
    }

    companion object {
        fun formatHistory(
            records: List<VexLogRecord>,
            zoneId: ZoneId,
        ): String {
            if (records.isEmpty()) return "No actions recorded yet."

            val formatter = DateTimeFormatter
                .ofPattern("h:mm:ssa MMM d, yyyy", Locale.US)
                .withZone(zoneId)

            return buildString {
                var previousSection: Long? = null

                records.forEachIndexed { index, record ->
                    val screenTransition =
                        record.screenBeforeId != null &&
                            record.screenAfterId != null &&
                            record.screenBeforeId != record.screenAfterId

                    if (
                        index > 0 &&
                        (record.section != previousSection || screenTransition)
                    ) {
                        appendLine("---")
                    }

                    val duration = record.durationSincePreviousSeconds
                        ?.let { " | $it sec" }
                        .orEmpty()

                    append("[")
                    append(formatter.format(record.timestamp))
                    append(duration)
                    append("]")

                    when (record.kind) {
                        VexLogKind.AppOpened -> {
                            append("[App Opened]")
                            append("[")
                            append(screenName(record.screenAfterId))
                            append(" ScreenAppeared]")
                        }

                        else -> {
                            append("[")
                            append(subjectName(record.subjectRef))
                            append(" | ")
                            append(record.action)
                            append("]")

                            append("[")
                            if (screenTransition) {
                                append(screenName(record.screenBeforeId))
                                append(" ScreenDisappeared -> ")
                                append(screenName(record.screenAfterId))
                                append(" ScreenAppeared")
                            } else {
                                append(screenName(record.screenBeforeId ?: record.screenAfterId))
                            }
                            append("]")
                        }
                    }

                    if (index < records.lastIndex) appendLine()
                    previousSection = record.section
                }
            }
        }

        fun screenName(screenId: String?): String {
            if (screenId == null) return "UnknownScreen"
            val body = screenId.removePrefix("screen.")
            return body.split('.')
                .joinToString("") { pascal(it) } + "Screen"
        }

        fun subjectName(ref: String): String {
            val body = ref
                .removePrefix("element.")
                .removePrefix("presentation.")
            return body.split('.')
                .joinToString(".") { pascal(it) }
        }

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
