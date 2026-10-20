package com.vextreme.vexlife.android.data.maps

import java.time.Instant

class RoomVexMapsRepository(
    private val dao: VexMapsDao,
) : VexMapsRepository {
    override suspend fun load(): VexMapsStoredState =
        dao.read(
            stateRef =
                VEX_MAPS_STATE_REF,
        )
            ?.toStoredState()
            ?: VexMapsStoredState()

    override suspend fun saveLastLocation(
        location: VexMapsStoredLocation,
    ) {
        ensureRow()
        val updated =
            dao.updateLastLocation(
                latitude =
                    location.latitude,
                longitude =
                    location.longitude,
                accuracyMeters =
                    location.accuracyMeters,
                capturedAtEpochMillis =
                    location
                        .capturedAt
                        .toEpochMilli(),
                updatedAtEpochMillis =
                    Instant.now()
                        .toEpochMilli(),
                stateRef =
                    VEX_MAPS_STATE_REF,
            )
        check(updated == 1) {
            "VexMaps last-location row disappeared during update"
        }
    }

    override suspend fun saveAppearancePreference(
        preference:
            VexMapsAppearancePreference,
    ) {
        ensureRow()
        val updated =
            dao.updateAppearancePreference(
                storedValue =
                    preference
                        .toStoredAppearanceValue(),
                updatedAtEpochMillis =
                    Instant.now()
                        .toEpochMilli(),
                stateRef =
                    VEX_MAPS_STATE_REF,
            )
        check(updated == 1) {
            "VexMaps appearance row disappeared during update"
        }
    }

    private suspend fun ensureRow() {
        dao.insertIfAbsent(
            VexMapsStateEntity(
                updatedAtEpochMillis =
                    Instant.now()
                        .toEpochMilli(),
            ),
        )
    }
}

// [VXG RealForever]
