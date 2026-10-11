package com.vextreme.vexlife.android.screens.shared.maps

/**
 * Feature-local raw-location projection used only by VexMaps presentation.
 *
 * This is intentionally separate from [VexNavigationFix] and the convergence relay. High-frequency
 * device coordinates may move the visible map dot, but they do not become semantic guidance/model
 * context merely because the foreground navigation service observed them.
 */
internal data class VexMapsNavigationVisualLocation(
    val sampleRef: String,
    val coordinate: VexMapCoordinate,
    val accuracyMeters: Float?,
    val elapsedRealtimeMillis: Long,
)

internal fun VexNavigationLocationSample
    .toVexMapsNavigationVisualLocation():
    VexMapsNavigationVisualLocation =
    VexMapsNavigationVisualLocation(
        sampleRef = sampleRef,
        coordinate =
            VexMapCoordinate(
                latitude = latitude,
                longitude = longitude,
            ),
        accuracyMeters = accuracyMeters,
        elapsedRealtimeMillis =
            elapsedRealtimeMillis,
    )

// [VXG RealForever]
