package com.vextreme.vexlife.android.data.maps

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

internal const val VEX_MAPS_STATE_REF =
    "vexmaps.local"

/**
 * One durable Maps state row.
 *
 * Location is a last-known convenience checkpoint, not a movement log. The schema intentionally has
 * no repeated location table.
 */
@Entity(tableName = "vex_maps_state")
data class VexMapsStateEntity(
    @PrimaryKey
    @ColumnInfo(name = "state_ref")
    val stateRef: String =
        VEX_MAPS_STATE_REF,

    @ColumnInfo(name = "last_location_latitude")
    val lastLocationLatitude: Double? = null,

    @ColumnInfo(name = "last_location_longitude")
    val lastLocationLongitude: Double? = null,

    @ColumnInfo(name = "last_location_accuracy_meters")
    val lastLocationAccuracyMeters: Float? = null,

    @ColumnInfo(name = "last_location_captured_at_epoch_millis")
    val lastLocationCapturedAtEpochMillis: Long? = null,

    @ColumnInfo(name = "online_style_ref")
    val appearanceStorageValue: String? = null,

    @ColumnInfo(name = "updated_at_epoch_millis")
    val updatedAtEpochMillis: Long = 0L,
)

internal fun VexMapsStateEntity.toStoredState():
    VexMapsStoredState {
    val location =
        if (
            lastLocationLatitude != null &&
            lastLocationLongitude != null &&
            lastLocationCapturedAtEpochMillis != null
        ) {
            VexMapsStoredLocation(
                latitude =
                    lastLocationLatitude,
                longitude =
                    lastLocationLongitude,
                accuracyMeters =
                    lastLocationAccuracyMeters,
                capturedAt =
                    Instant.ofEpochMilli(
                        lastLocationCapturedAtEpochMillis,
                    ),
            )
        } else {
            null
        }

    return VexMapsStoredState(
        lastLocation = location,
        appearancePreference =
            vexMapsAppearancePreferenceFromStoredValue(
                appearanceStorageValue,
            ),
    )
}

// [VXG RealForever]
