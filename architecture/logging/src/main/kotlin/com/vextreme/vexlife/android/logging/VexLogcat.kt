package com.vextreme.vexlife.android.logging

import android.util.Log

/**
 * Android Logcat projection of canonical [VexLogRecord] diagnostics.
 *
 * Logcat is a developer/debugging surface only. [VexLogRecord] remains canonical truth and the
 * durable LogHistory repository remains a separate downstream projection.
 */
object VexLogcat {
    const val TAG: String = "VexLogger"

    fun record(record: VexLogRecord) {
        Log.i(TAG, message(record))
    }

    fun historyCleared() {
        Log.i(TAG, "history=cleared")
    }

    /**
     * Pure readable projection so the shape is unit-testable without Android Logcat.
     */
    fun message(record: VexLogRecord): String =
        buildString {
            append("seq=")
            append(record.sequence)
            append(" kind=")
            append(record.kind)
            append(" subject=")
            append(record.subjectRef)
            append(" action=")
            append(record.action)
            append(" before=")
            append(record.screenBeforeId ?: "-")
            append(" after=")
            append(record.screenAfterId ?: "-")
            append(" section=")
            append(record.section)
            record.durationSincePreviousSeconds?.let { seconds ->
                append(" delta=")
                append(seconds)
                append("s")
            }
            record.detailsJson
                ?.takeIf { it.isNotBlank() }
                ?.let { details ->
                    append(" details=")
                    append(details.replace('\n', ' '))
                }
        }
}
