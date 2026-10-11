package com.vextreme.vexlife.android.data.loghistory

import com.vextreme.vexlife.android.elements.ScreenElement
import com.vextreme.vexlife.android.elements.UIElement
import com.vextreme.vexlife.android.logging.VexLogKind
import com.vextreme.vexlife.android.logging.VexLogRecord
import java.time.Duration
import java.time.Instant

/**
 * Chain-buildable deterministic data for previews/unit tests.
 *
 * Example:
 *
 * val repeatedReset = LogHistoryFixture.build(startAt) {
 *     appOpened(HomePage)
 *     afterSeconds(120)
 *     click(DebugButton, from = HomePage, to = DebugMenuPage)
 * }.append {
 *     afterSeconds(30)
 *     click(ResetButton, from = DebugMenuPage)
 * }
 *
 * The fixture creates app/domain records, not Room entities. The same fixture can seed an
 * InMemoryLogHistoryRepository for a Compose preview or drive repository tests.
 */
data class LogHistoryFixture(
    val records: List<VexLogRecord>,
) {
    fun append(
        block: LogHistoryFixtureBuilder.() -> Unit,
    ): LogHistoryFixture =
        LogHistoryFixtureBuilder(records)
            .apply(block)
            .build()

    companion object {
        fun build(
            startAt: Instant = Instant.parse("2026-10-11T01:51:00Z"),
            block: LogHistoryFixtureBuilder.() -> Unit = {},
        ): LogHistoryFixture =
            LogHistoryFixtureBuilder(
                initialRecords = emptyList(),
                initialTime = startAt,
            ).apply(block).build()
    }
}

class LogHistoryFixtureBuilder internal constructor(
    initialRecords: List<VexLogRecord>,
    initialTime: Instant = initialRecords.lastOrNull()?.timestamp
        ?: Instant.parse("2026-10-11T01:51:00Z"),
) {
    private val records = initialRecords.toMutableList()
    private var now: Instant = initialTime
    private var section: Long = initialRecords.lastOrNull()?.section ?: 0L

    fun at(instant: Instant) {
        now = instant
    }

    fun afterSeconds(seconds: Long) {
        require(seconds >= 0) { "seconds must not be negative" }
        now = now.plusSeconds(seconds)
    }

    fun newSection() {
        section += 1
    }

    fun appOpened(screen: ScreenElement) {
        appendRecord(
            kind = VexLogKind.AppOpened,
            subjectRef = "app",
            action = "App Opened",
            screenBeforeId = null,
            screenAfterId = screen.screenId,
        )
    }

    fun click(
        element: UIElement,
        from: ScreenElement,
        to: ScreenElement? = null,
        action: String = "Clicked",
    ) {
        appendRecord(
            kind = VexLogKind.Interaction,
            subjectRef = element.elementId,
            action = action,
            screenBeforeId = from.screenId,
            screenAfterId = to?.screenId,
        )
    }

    fun back(
        from: ScreenElement,
        to: ScreenElement? = null,
    ) {
        appendRecord(
            kind = VexLogKind.Gesture,
            subjectRef = "system.back",
            action = "Back",
            screenBeforeId = from.screenId,
            screenAfterId = to?.screenId,
        )
    }

    fun append(record: VexLogRecord) {
        records += record
        now = record.timestamp
        section = record.section
    }

    fun build(): LogHistoryFixture =
        LogHistoryFixture(records.toList())

    private fun appendRecord(
        kind: VexLogKind,
        subjectRef: String,
        action: String,
        screenBeforeId: String?,
        screenAfterId: String?,
    ) {
        val previous = records.lastOrNull()
        val duration = previous?.timestamp?.let {
            Duration.between(it, now).seconds.coerceAtLeast(0L)
        }

        records += VexLogRecord(
            sequence = (previous?.sequence ?: 0L) + 1L,
            timestamp = now,
            durationSincePreviousSeconds = duration,
            kind = kind,
            subjectRef = subjectRef,
            action = action,
            screenBeforeId = screenBeforeId,
            screenAfterId = screenAfterId,
            section = section,
        )
    }
}
