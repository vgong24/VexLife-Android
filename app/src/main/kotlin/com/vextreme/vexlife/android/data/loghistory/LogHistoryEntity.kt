package com.vextreme.vexlife.android.data.loghistory

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.vextreme.vexlife.android.logging.VexLogKind
import com.vextreme.vexlife.android.logging.VexLogRecord
import java.time.Instant

/**
 * SQLite/Room representation only.
 *
 * SQL names are explicit so Kotlin refactors/obfuscation are not accidentally treated as database
 * migrations. Network serialization names are a different contract and do not belong on this entity.
 */
@Entity(
    tableName = "log_history",
    indices = [
        Index(value = ["session_ref", "sequence"]),
        Index(value = ["timestamp_epoch_millis"]),
    ],
)
data class LogHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "database_id")
    val databaseId: Long = 0,

    @ColumnInfo(name = "session_ref")
    val sessionRef: String,

    @ColumnInfo(name = "sequence")
    val sequence: Long,

    @ColumnInfo(name = "timestamp_epoch_millis")
    val timestampEpochMillis: Long,

    @ColumnInfo(name = "duration_since_previous_seconds")
    val durationSincePreviousSeconds: Long?,

    @ColumnInfo(name = "kind")
    val kind: String,

    @ColumnInfo(name = "subject_ref")
    val subjectRef: String,

    @ColumnInfo(name = "action")
    val action: String,

    @ColumnInfo(name = "screen_before_id")
    val screenBeforeId: String?,

    @ColumnInfo(name = "screen_after_id")
    val screenAfterId: String?,

    @ColumnInfo(name = "section")
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
