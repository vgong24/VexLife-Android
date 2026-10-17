package com.vextreme.vexlife.android.data.maps

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Storage plumbing for the one Maps-local state row.
 *
 * Field-specific updates avoid read-modify-write races between an appearance preference and a
 * location refresh.
 */
@Dao
interface VexMapsDao {
    @Query(
        """
        SELECT * FROM vex_maps_state
        WHERE state_ref = :stateRef
        LIMIT 1
        """
    )
    suspend fun read(
        stateRef: String,
    ): VexMapsStateEntity?

    @Insert(
        onConflict = OnConflictStrategy.IGNORE,
    )
    suspend fun insertIfAbsent(
        entity: VexMapsStateEntity,
    ): Long

    @Query(
        """
        UPDATE vex_maps_state
        SET last_location_latitude = :latitude,
            last_location_longitude = :longitude,
            last_location_accuracy_meters = :accuracyMeters,
            last_location_captured_at_epoch_millis = :capturedAtEpochMillis,
            updated_at_epoch_millis = :updatedAtEpochMillis
        WHERE state_ref = :stateRef
        """
    )
    suspend fun updateLastLocation(
        latitude: Double,
        longitude: Double,
        accuracyMeters: Float?,
        capturedAtEpochMillis: Long,
        updatedAtEpochMillis: Long,
        stateRef: String,
    ): Int

    @Query(
        """
        UPDATE vex_maps_state
        SET online_style_ref = :storedValue,
            updated_at_epoch_millis = :updatedAtEpochMillis
        WHERE state_ref = :stateRef
        """
    )
    suspend fun updateAppearancePreference(
        storedValue: String?,
        updatedAtEpochMillis: Long,
        stateRef: String,
    ): Int
}

// [VXG RealForever]
