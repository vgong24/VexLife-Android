package com.vextreme.vexlife.android.data.maps.distribution

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

internal const val VEX_MAPS_DISTRIBUTION_DRAFT_REF =
    "vexmaps.distribution.current"

@Entity(
    tableName =
        "vex_maps_distribution_draft",
)
data class VexMapsDistributionDraftEntity(
    @PrimaryKey
    @ColumnInfo(name = "draft_ref")
    val draftRef: String =
        VEX_MAPS_DISTRIBUTION_DRAFT_REF,

    @ColumnInfo(name = "revision")
    val revision: Long = 0L,

    @ColumnInfo(name = "return_to_start")
    val returnToStart: Boolean = true,

    @ColumnInfo(name = "updated_at_epoch_millis")
    val updatedAtEpochMillis: Long = 0L,
)

@Entity(
    tableName =
        "vex_maps_distribution_stop",
    foreignKeys = [
        ForeignKey(
            entity =
                VexMapsDistributionDraftEntity::class,
            parentColumns =
                ["draft_ref"],
            childColumns =
                ["draft_ref"],
            onDelete =
                ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(
            value = ["draft_ref"],
        ),
        Index(
            value = [
                "draft_ref",
                "input_order",
            ],
            unique = true,
        ),
    ],
)
data class VexMapsDistributionStopEntity(
    @PrimaryKey
    @ColumnInfo(name = "stop_ref")
    val stopRef: String,

    @ColumnInfo(name = "draft_ref")
    val draftRef: String =
        VEX_MAPS_DISTRIBUTION_DRAFT_REF,

    @ColumnInfo(name = "place_ref")
    val placeRef: String?,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "subtitle")
    val subtitle: String?,

    @ColumnInfo(name = "latitude")
    val latitude: Double,

    @ColumnInfo(name = "longitude")
    val longitude: Double,

    @ColumnInfo(name = "input_order")
    val inputOrder: Int,
)

internal fun VexMapsDistributionStopEntity.toDomain():
    VexMapsDistributionStop =
    VexMapsDistributionStop(
        stopRef = stopRef,
        placeRef = placeRef,
        name = name,
        subtitle = subtitle,
        latitude = latitude,
        longitude = longitude,
        inputOrder = inputOrder,
    )

// [VXG RealForever]
