package com.vextreme.vexlife.android.data.maps

import java.time.Instant

/**
 * Durable checkpoint of the last accepted precise-device observation.
 *
 * This is not current device truth. Reopen logic may project it only into non-authoritative map
 * context while permission remains active; navigation/readiness must wait for a fresh Android fix.
 * Accuracy and capture time are retained so future restoration policy can make an explicit decision.
 */
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
    val appearancePreference:
        VexMapsAppearancePreference =
        VexMapsAppearancePreference.AUTOMATIC,
)

/**
 * Durable Android-local Maps preference/state boundary.
 *
 * This answers only:
 * - what last accepted precise-location checkpoint may seed non-authoritative map context?
 * - which map appearance preference did the human choose?
 *
 * The checkpoint never becomes a current fix merely because the app reopened. It is deliberately
 * not a GPS history, navigation-session owner, map-pack catalog, or generic preference store.
 */
interface VexMapsRepository {
    suspend fun load(): VexMapsStoredState

    suspend fun saveLastLocation(
        location: VexMapsStoredLocation,
    )

    suspend fun saveAppearancePreference(
        preference:
            VexMapsAppearancePreference,
    )
}

// [VXG RealForever]
