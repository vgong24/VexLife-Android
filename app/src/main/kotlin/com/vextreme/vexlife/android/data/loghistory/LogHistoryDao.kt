package com.vextreme.vexlife.android.data.loghistory

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Room access for persisted Log History.
 *
 * insertAndPrune() is one transaction because "persist this record" and "restore the retention
 * invariant" are one database effect. If the transaction is interrupted, SQLite/Room does not leave
 * the database halfway between those two statements.
 */
@Dao
abstract class LogHistoryDao {
    @Query(
        """
        SELECT * FROM (
            SELECT * FROM log_history
            ORDER BY database_id DESC
            LIMIT :limit
        )
        ORDER BY database_id ASC
        """
    )
    abstract fun observeRecent(limit: Int): Flow<List<LogHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun insertInternal(entity: LogHistoryEntity)

    @Query(
        """
        DELETE FROM log_history
        WHERE database_id NOT IN (
            SELECT database_id FROM log_history
            ORDER BY database_id DESC
            LIMIT :limit
        )
        """
    )
    protected abstract suspend fun pruneToInternal(limit: Int)

    /**
     * Multi-statement write boundary.
     *
     * Do not replace this with repository-side "insert(); prune()" sequencing: that would reopen the
     * partial-update window this transaction is intended to close.
     */
    @Transaction
    open suspend fun insertAndPrune(
        entity: LogHistoryEntity,
        limit: Int,
    ) {
        insertInternal(entity)
        pruneToInternal(limit)
    }

    @Query("DELETE FROM log_history")
    abstract suspend fun clear()
}
