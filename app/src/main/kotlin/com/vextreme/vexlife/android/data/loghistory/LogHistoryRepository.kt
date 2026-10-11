package com.vextreme.vexlife.android.data.loghistory

import com.vextreme.vexlife.android.logging.VexLogRecord
import kotlinx.coroutines.flow.Flow

/**
 * Domain-facing contract for readable diagnostic history.
 *
 * The UI should know this contract, not Room/DAO types. A future repository implementation may
 * combine local Room history with remote/exported history without changing the Screen API.
 */
interface LogHistoryRepository {
    val records: Flow<List<VexLogRecord>>

    suspend fun append(record: VexLogRecord)

    suspend fun clear()
}
