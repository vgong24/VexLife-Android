package com.vextreme.vexlife.android.screens.shared.maps

import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng

/**
 * Restorable map-camera meaning independent of the MapLibre view instance.
 *
 * The screen owns when a viewport should be retained. The MapLibre adapter consumes this projection
 * only when it needs to realize or report camera state.
 */
internal data class VexMapsViewportSnapshot(
    val center: VexMapCoordinate,
    val zoom: Double,
    val bearing: Double,
    val tilt: Double,
) {
    init {
        require(zoom.isFinite() && zoom >= 0.0) {
            "zoom must be finite and non-negative"
        }
        require(bearing.isFinite()) {
            "bearing must be finite"
        }
        require(
            tilt.isFinite() &&
                tilt in 0.0..60.0,
        ) {
            "tilt must be finite and in [0, 60]"
        }
    }
}

internal fun restoreVexMapsViewportSnapshot(
    latitude: Double?,
    longitude: Double?,
    zoom: Double?,
    bearing: Double?,
    tilt: Double?,
): VexMapsViewportSnapshot? {
    if (
        latitude == null ||
        longitude == null ||
        zoom == null ||
        bearing == null ||
        tilt == null
    ) {
        return null
    }

    return runCatching {
        VexMapsViewportSnapshot(
            center =
                VexMapCoordinate(
                    latitude = latitude,
                    longitude = longitude,
                ),
            zoom = zoom,
            bearing = bearing,
            tilt = tilt,
        )
    }.getOrNull()
}

internal fun VexMapsViewportSnapshot
    .toCameraPosition(): CameraPosition =
    CameraPosition.Builder()
        .target(
            LatLng(
                center.latitude,
                center.longitude,
            ),
        )
        .zoom(zoom)
        .bearing(bearing)
        .tilt(tilt)
        .build()

internal fun VexOfflineRouteResult
    .vexMapsRouteKey(): String =
    route.routeRef +
        ":" +
        route.routeRevision

internal fun resolveVexMapsRestoredRouteFitKey(
    hasRestoredViewport: Boolean,
    savedViewportRouteKey: String?,
    currentRouteKey: String?,
): String? =
    currentRouteKey
        ?.takeIf { routeKey ->
            hasRestoredViewport &&
                savedViewportRouteKey ==
                routeKey
        }

// [VXG RealForever]
