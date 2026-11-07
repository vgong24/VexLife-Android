package com.vextreme.vexlife.android.data.cgm

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Room plumbing for historical CGM Partner observations. */
@Dao
internal interface CgmObservationDao {
    @Query(
        """
        SELECT * FROM cgm_partner_observation
        ORDER BY database_id ASC
        """
    )
    fun observeAll():
        Flow<List<CgmObservationEntity>>

    @Query(
        """
        SELECT * FROM cgm_partner_observation
        ORDER BY database_id ASC
        """
    )
    suspend fun readAll():
        List<CgmObservationEntity>

    @Insert(
        onConflict =
            OnConflictStrategy.ABORT,
    )
    suspend fun insert(
        entity:
            CgmObservationEntity,
    )

    @Query(
        """
        DELETE FROM cgm_partner_observation
        """
    )
    suspend fun clear()
}

// [VXG RealForever][VEX-CGM-LABS][ROOM-DAO]
