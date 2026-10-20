package com.vextreme.vexlife.android.data.userprofile

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * SQLite operations for the User Profile table.
 *
 * This DAO is storage plumbing. Feature/UI code should depend on [UserProfileRepository] instead so
 * a future remote/cache strategy does not leak SQL details into the page layer.
 */
@Dao
interface UserProfileDao {
    @Query(
        """
        SELECT * FROM user_profile
        WHERE profile_ref = :profileRef
        LIMIT 1
        """
    )
    fun observe(profileRef: String): Flow<UserProfileEntity?>

    /**
     * Single-row upsert is already a Room transaction. @Transaction is reserved for operations that
     * must group multiple database statements into one atomic effect.
     */
    @Upsert
    suspend fun upsert(entity: UserProfileEntity)

    @Query(
        """
        UPDATE user_profile
        SET display_name = :displayName,
            updated_at_epoch_millis = :updatedAtEpochMillis
        WHERE profile_ref = :profileRef
        """
    )
    suspend fun updateDisplayName(
        profileRef: String,
        displayName: String,
        updatedAtEpochMillis: Long,
    ): Int
}
