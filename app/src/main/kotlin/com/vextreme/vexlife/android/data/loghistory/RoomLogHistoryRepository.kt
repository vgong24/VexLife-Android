package com.vextreme.vexlife.android.data.loghistory

import com.vextreme.vexlife.android.logging.VexLogRecord
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Room-backed implementation of [LogHistoryRepository].
 *
 * The repository owns mapping between storage rows and VexLogRecord. Pages never see DAO/entity
 * types, which leaves room for future local+remote/sync behavior behind this contract.
 */
class RoomLogHistoryRepository(
    private val dao: LogHistoryDao,
    private val recentLimit: Int = 256,
    private val retentionLimit: Int = 4096,
    private val sessionRef: String = UUID.randomUUID().toString(),
) : LogHistoryRepository {
    init {
        require(recentLimit > 0) { "recentLimit must be positive" }
        require(retentionLimit >= recentLimit) {
            "retentionLimit must be >= recentLimit"
        }
    }

    override val records: Flow<List<VexLogRecord>> =
        dao.observeRecent(recentLimit).map { entities ->
            entities.map { it.toRecord() }
        }

    override suspend fun append(record: VexLogRecord) {
        dao.insert(record.toEntity(sessionRef))
        dao.pruneTo(retentionLimit)
    }

    override suspend fun clear() {
        dao.clear()
    }
}
