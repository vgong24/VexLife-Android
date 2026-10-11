package com.vextreme.vexlife.android.data.loghistory

import com.vextreme.vexlife.android.logging.VexLogRecord
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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
        // One DAO transaction: the persisted append and retention invariant cannot partially diverge.
        dao.insertAndPrune(
            entity = record.toEntity(sessionRef),
            limit = retentionLimit,
        )
    }

    override suspend fun clear() {
        dao.clear()
    }
}
