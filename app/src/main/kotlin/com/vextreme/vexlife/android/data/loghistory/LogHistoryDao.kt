package com.vextreme.vexlife.android.data.loghistory

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LogHistoryDao {
    /**
     * Keep only the most recent bounded window for UI/debug inspection while returning it oldest ->
     * newest so readable chronology remains natural.
     */
    @Query(
        """
        SELECT * FROM (
            SELECT * FROM log_history
            ORDER BY databaseId DESC
            LIMIT :limit
        )
        ORDER BY databaseId ASC
        """
    )
    fun observeRecent(limit: Int): Flow<List<LogHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: LogHistoryEntity)

    @Query(
        """
        DELETE FROM log_history
        WHERE databaseId NOT IN (
            SELECT databaseId FROM log_history
            ORDER BY databaseId DESC
            LIMIT :limit
        )
        """
    )
    suspend fun pruneTo(limit: Int)

    @Query("DELETE FROM log_history")
    suspend fun clear()
}
