package com.vextreme.vexlife.android.data.loghistory

import com.vextreme.vexlife.android.logging.VexLogRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Small repository implementation for unit tests, previews, and isolated feature work.
 *
 * This is intentionally a real implementation of the same repository contract rather than a mock
 * framework proxy. Seed it with [LogHistoryFixture.records] to reproduce a deterministic state.
 */
class InMemoryLogHistoryRepository(
    initialRecords: List<VexLogRecord> = emptyList(),
) : LogHistoryRepository {
    private val _records = MutableStateFlow(initialRecords.toList())

    override val records: StateFlow<List<VexLogRecord>> = _records.asStateFlow()

    override suspend fun append(record: VexLogRecord) {
        _records.value = _records.value + record
    }

    override suspend fun clear() {
        _records.value = emptyList()
    }
}
