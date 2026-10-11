package com.vextreme.vexlife.android.data.loghistory

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.vextreme.vexlife.android.logging.VexLogKind
import com.vextreme.vexlife.android.logging.VexLogRecord
import java.time.Instant

/**
 * SQLite/Room representation only.
 *
 * The database primary key is intentionally different from VexLogRecord.sequence: logger sequence is
 * session-local and may restart after process restart/clear. [sessionRef] preserves that distinction.
 */
@Entity(
    tableName = "log_history",
    indices = [
        Index(value = ["sessionRef", "sequence"]),
        Index(value = ["timestampEpochMillis"]),
    ],
)
data class LogHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val databaseId: Long = 0,
    val sessionRef: String,
    val sequence: Long,
    val timestampEpochMillis: Long,
    val durationSincePreviousSeconds: Long?,
    val kind: String,
    val subjectRef: String,
    val action: String,
    val screenBeforeId: String?,
    val screenAfterId: String?,
    val section: Long,
)

internal fun VexLogRecord.toEntity(
    sessionRef: String,
): LogHistoryEntity =
    LogHistoryEntity(
        sessionRef = sessionRef,
        sequence = sequence,
        timestampEpochMillis = timestamp.toEpochMilli(),
        durationSincePreviousSeconds = durationSincePreviousSeconds,
        kind = kind.name,
        subjectRef = subjectRef,
        action = action,
        screenBeforeId = screenBeforeId,
        screenAfterId = screenAfterId,
        section = section,
    )

internal fun LogHistoryEntity.toRecord(): VexLogRecord =
    VexLogRecord(
        sequence = sequence,
        timestamp = Instant.ofEpochMilli(timestampEpochMillis),
        durationSincePreviousSeconds = durationSincePreviousSeconds,
        kind = VexLogKind.valueOf(kind),
        subjectRef = subjectRef,
        action = action,
        screenBeforeId = screenBeforeId,
        screenAfterId = screenAfterId,
        section = section,
    )
