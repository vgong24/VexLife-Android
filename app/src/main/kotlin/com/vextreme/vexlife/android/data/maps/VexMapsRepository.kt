package com.vextreme.vexlife.android.data.maps

import java.time.Instant

data class VexMapsStoredLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float?,
    val capturedAt: Instant,
) {
    init {
        require(
            latitude.isFinite() &&
                latitude in -90.0..90.0,
        ) {
            "latitude must be finite and within [-90, 90]"
        }
        require(
            longitude.isFinite() &&
                longitude in -180.0..180.0,
        ) {
            "longitude must be finite and within [-180, 180]"
        }
        require(
            accuracyMeters == null ||
                (
                    accuracyMeters.isFinite() &&
                        accuracyMeters >= 0f
                ),
        ) {
            "accuracyMeters must be null or finite/non-negative"
        }
    }
}

data class VexMapsStoredState(
    val lastLocation: VexMapsStoredLocation? = null,
    val onlineStyleRef: String? = null,
) {
    init {
        require(
            onlineStyleRef == null ||
                onlineStyleRef.isNotBlank(),
        ) {
            "onlineStyleRef must be null or non-blank"
        }
    }
}

/**
 * Durable Android-local Maps preference/state boundary.
 *
 * This answers only:
 * - what last accepted location may make Maps useful immediately on reopen?
 * - which online map style did the human choose?
 *
 * It is deliberately not a GPS history, navigation-session owner, map-pack catalog, or generic
 * preference store.
 */
interface VexMapsRepository {
    suspend fun load(): VexMapsStoredState

    suspend fun saveLastLocation(
        location: VexMapsStoredLocation,
    )

    suspend fun saveOnlineStyle(
        styleRef: String,
    )
}

// [VXG RealForever]
