package com.vextreme.vexlife.android.data.maps.distribution

import java.time.Instant
import java.util.UUID

class RoomVexMapsDistributionRepository(
    private val dao:
        VexMapsDistributionDao,
) : VexMapsDistributionRepository {
    override suspend fun loadDraft():
        VexMapsDistributionDraft =
        dao.readDraft()

    override suspend fun addStop(
        placeRef: String?,
        name: String,
        subtitle: String?,
        latitude: Double,
        longitude: Double,
    ): VexMapsDistributionDraft {
        require(name.isNotBlank())
        require(placeRef == null || placeRef.isNotBlank())

        return dao.addStop(
            stopRef =
                "vexmaps.distribution.stop." +
                    UUID.randomUUID(),
            placeRef =
                placeRef,
            name =
                name,
            subtitle =
                subtitle,
            latitude =
                latitude,
            longitude =
                longitude,
            updatedAtEpochMillis =
                Instant.now()
                    .toEpochMilli(),
        )
    }

    override suspend fun removeStop(
        stopRef: String,
    ): VexMapsDistributionDraft {
        require(stopRef.isNotBlank())

        return dao.removeStop(
            stopRef = stopRef,
            updatedAtEpochMillis =
                Instant.now()
                    .toEpochMilli(),
        )
    }

    override suspend fun setReturnToStart(
        enabled: Boolean,
    ): VexMapsDistributionDraft =
        dao.setReturnToStart(
            enabled = enabled,
            updatedAtEpochMillis =
                Instant.now()
                    .toEpochMilli(),
        )

    override suspend fun clearDraft():
        VexMapsDistributionDraft =
        dao.clearDraft(
            updatedAtEpochMillis =
                Instant.now()
                    .toEpochMilli(),
        )
}

// [VXG RealForever]
