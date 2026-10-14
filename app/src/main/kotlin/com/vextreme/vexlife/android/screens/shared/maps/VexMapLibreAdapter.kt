package com.vextreme.vexlife.android.screens.shared.maps

import android.graphics.Color
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineOpacity
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

/**
 * Feature-local adapter from VexMaps meaning into the MapLibre SDK.
 *
 * This owns provider mechanics: camera movement, MapLibre sources/layers, style attachment and
 * provider listeners. It does not own route meaning, location permission, persistence, navigation
 * lifetime or Relay state.
 */
internal class VexMapLibreAdapter {
    private var map: MapLibreMap? = null
    private var style: Style? = null
    private var pendingLocation:
        VexDeviceLocationResult.Located? = null
    private var pendingRoute:
        VexOfflineRouteResult? = null
    private var pendingOrigin:
        VexMapCoordinate? = null
    private var pendingDestination:
        VexMapCoordinate? = null
    private var lastFittedRouteKey:
        String? = null
    private var onMapLongPress:
        ((VexMapCoordinate) -> Unit)? = null
    private var onViewportChanged:
        ((VexMapsViewportSnapshot) -> Unit)? =
        null

    private val longClickListener =
        MapLibreMap.OnMapLongClickListener {
            point ->
            val callback =
                onMapLongPress
                    ?: return@OnMapLongClickListener false

            callback(
                VexMapCoordinate(
                    latitude =
                        point.latitude,
                    longitude =
                        point.longitude,
                ),
            )
            true
        }

    private val cameraIdleListener =
        MapLibreMap.OnCameraIdleListener {
            val position =
                map?.cameraPosition
                    ?: return@OnCameraIdleListener
            val target =
                position.target
                    ?: return@OnCameraIdleListener
            val snapshot =
                restoreVexMapsViewportSnapshot(
                    latitude = target.latitude,
                    longitude = target.longitude,
                    zoom = position.zoom,
                    bearing = position.bearing,
                    tilt = position.tilt,
                )
                    ?: return@OnCameraIdleListener

            onViewportChanged
                ?.invoke(snapshot)
        }

    fun setOnMapLongPress(
        callback:
            ((VexMapCoordinate) -> Unit)?,
    ) {
        onMapLongPress =
            callback
    }

    fun setOnViewportChanged(
        callback:
            ((VexMapsViewportSnapshot) -> Unit)?,
    ) {
        onViewportChanged =
            callback
    }

    fun attach(
        map: MapLibreMap,
        style: Style,
        restoredRouteKey: String?,
    ) {
        this.map
            ?.removeOnMapLongClickListener(
                longClickListener,
            )
        this.map
            ?.removeOnCameraIdleListener(
                cameraIdleListener,
            )

        this.map = map
        this.style = style
        lastFittedRouteKey =
            restoredRouteKey
        map.addOnMapLongClickListener(
            longClickListener,
        )
        map.addOnCameraIdleListener(
            cameraIdleListener,
        )

        pendingRoute?.let { route ->
            renderRoute(
                route = route,
                fitCamera = true,
            )
        }
        pendingOrigin?.let(
            ::renderOrigin,
        )
        pendingDestination?.let(
            ::renderDestination,
        )
        pendingLocation?.let(
            ::renderDeviceLocation,
        )
    }

    fun detach() {
        map?.removeOnMapLongClickListener(
            longClickListener,
        )
        map?.removeOnCameraIdleListener(
            cameraIdleListener,
        )
        map = null
        style = null
        lastFittedRouteKey = null
    }

    fun focusOn(
        coordinate: VexMapCoordinate,
    ) {
        map?.animateCamera(
            CameraUpdateFactory
                .newLatLngZoom(
                    LatLng(
                        coordinate.latitude,
                        coordinate.longitude,
                    ),
                    15.5,
                ),
        )
    }

    fun showOrigin(
        origin: VexMapCoordinate,
    ) {
        pendingOrigin = origin
        renderOrigin(origin)
    }

    fun clearOrigin() {
        pendingOrigin = null

        val style = style ?: return
        if (
            style.getLayer(
                ORIGIN_LAYER_ID,
            ) != null
        ) {
            style.removeLayer(
                ORIGIN_LAYER_ID,
            )
        }
        if (
            style.getSource(
                ORIGIN_SOURCE_ID,
            ) != null
        ) {
            style.removeSource(
                ORIGIN_SOURCE_ID,
            )
        }
    }

    fun showDestination(
        destination: VexMapCoordinate,
    ) {
        pendingDestination = destination
        renderDestination(destination)
    }

    fun clearDestination() {
        pendingDestination = null

        val style = style ?: return
        if (
            style.getLayer(
                DESTINATION_LAYER_ID,
            ) != null
        ) {
            style.removeLayer(
                DESTINATION_LAYER_ID,
            )
        }
        if (
            style.getSource(
                DESTINATION_SOURCE_ID,
            ) != null
        ) {
            style.removeSource(
                DESTINATION_SOURCE_ID,
            )
        }
    }

    fun showRoute(
        route: VexOfflineRouteResult,
    ) {
        pendingRoute = route
        renderRoute(
            route = route,
            fitCamera = true,
        )
    }

    fun clearRoute() {
        pendingRoute = null
        lastFittedRouteKey = null

        val style = style ?: return

        if (
            style.getLayer(
                ROUTE_LAYER_ID,
            ) != null
        ) {
            style.removeLayer(
                ROUTE_LAYER_ID,
            )
        }
        if (
            style.getSource(
                ROUTE_SOURCE_ID,
            ) != null
        ) {
            style.removeSource(
                ROUTE_SOURCE_ID,
            )
        }
    }

    fun showDeviceLocation(
        location: VexDeviceLocationResult.Located,
    ) {
        pendingLocation = location
        renderDeviceLocation(location)
    }

    fun clearDeviceLocation() {
        pendingLocation = null

        val style = style ?: return
        if (
            style.getLayer(
                DEVICE_LOCATION_LAYER_ID,
            ) != null
        ) {
            style.removeLayer(
                DEVICE_LOCATION_LAYER_ID,
            )
        }
        if (
            style.getSource(
                DEVICE_LOCATION_SOURCE_ID,
            ) != null
        ) {
            style.removeSource(
                DEVICE_LOCATION_SOURCE_ID,
            )
        }
    }

    private fun renderOrigin(
        origin: VexMapCoordinate,
    ) {
        val style = style ?: return
        val point =
            Point.fromLngLat(
                origin.longitude,
                origin.latitude,
            )
        val existing =
            style.getSource(
                ORIGIN_SOURCE_ID,
            ) as? GeoJsonSource

        if (existing == null) {
            style.addSource(
                GeoJsonSource(
                    ORIGIN_SOURCE_ID,
                    point,
                ),
            )
            style.addLayer(
                CircleLayer(
                    ORIGIN_LAYER_ID,
                    ORIGIN_SOURCE_ID,
                ).withProperties(
                    circleRadius(9f),
                    circleColor(
                        Color.parseColor(
                            "#146C2E",
                        ),
                    ),
                    circleStrokeWidth(3f),
                    circleStrokeColor(
                        Color.WHITE,
                    ),
                ),
            )
        } else {
            existing.setGeoJson(
                point,
            )
        }
    }

    private fun renderDestination(
        destination: VexMapCoordinate,
    ) {
        val style = style ?: return
        val point =
            Point.fromLngLat(
                destination.longitude,
                destination.latitude,
            )
        val existing =
            style.getSource(
                DESTINATION_SOURCE_ID,
            ) as? GeoJsonSource

        if (existing == null) {
            style.addSource(
                GeoJsonSource(
                    DESTINATION_SOURCE_ID,
                    point,
                ),
            )
            style.addLayer(
                CircleLayer(
                    DESTINATION_LAYER_ID,
                    DESTINATION_SOURCE_ID,
                ).withProperties(
                    circleRadius(9f),
                    circleColor(
                        Color.parseColor(
                            "#B3261E",
                        ),
                    ),
                    circleStrokeWidth(3f),
                    circleStrokeColor(
                        Color.WHITE,
                    ),
                ),
            )
        } else {
            existing.setGeoJson(
                point,
            )
        }
    }

    private fun renderRoute(
        route: VexOfflineRouteResult,
        fitCamera: Boolean,
    ) {
        val map = map ?: return
        val style = style ?: return

        val points =
            route.geometry.map { coordinate ->
                Point.fromLngLat(
                    coordinate.longitude,
                    coordinate.latitude,
                )
            }

        val line =
            LineString.fromLngLats(
                points,
            )

        val existingSource =
            style.getSource(
                ROUTE_SOURCE_ID,
            ) as? GeoJsonSource

        if (existingSource == null) {
            style.addSource(
                GeoJsonSource(
                    ROUTE_SOURCE_ID,
                    line,
                ),
            )
            style.addLayer(
                LineLayer(
                    ROUTE_LAYER_ID,
                    ROUTE_SOURCE_ID,
                ).withProperties(
                    lineColor(
                        Color.parseColor(
                            "#6750A4",
                        ),
                    ),
                    lineWidth(7f),
                    lineOpacity(0.92f),
                ),
            )
        } else {
            existingSource.setGeoJson(
                line,
            )
        }

        val routeKey =
            route.vexMapsRouteKey()

        if (
            fitCamera &&
            lastFittedRouteKey != routeKey
        ) {
            val bounds =
                LatLngBounds.Builder()
                    .includes(
                        route.geometry.map { coordinate ->
                            LatLng(
                                coordinate.latitude,
                                coordinate.longitude,
                            )
                        },
                    )
                    .build()

            map.animateCamera(
                CameraUpdateFactory
                    .newLatLngBounds(
                        bounds,
                        96,
                    ),
            )
            lastFittedRouteKey = routeKey
        }
    }

    private fun renderDeviceLocation(
        location: VexDeviceLocationResult.Located,
    ) {
        val map = map ?: return
        val style = style ?: return

        val point =
            Point.fromLngLat(
                location.longitude,
                location.latitude,
            )

        val existingSource =
            style.getSource(
                DEVICE_LOCATION_SOURCE_ID,
            ) as? GeoJsonSource

        if (existingSource == null) {
            style.addSource(
                GeoJsonSource(
                    DEVICE_LOCATION_SOURCE_ID,
                    point,
                ),
            )

            style.addLayer(
                CircleLayer(
                    DEVICE_LOCATION_LAYER_ID,
                    DEVICE_LOCATION_SOURCE_ID,
                ).withProperties(
                    circleRadius(8f),
                    circleColor(
                        Color.parseColor(
                            "#6750A4",
                        ),
                    ),
                    circleStrokeWidth(3f),
                    circleStrokeColor(
                        Color.WHITE,
                    ),
                ),
            )
        } else {
            existingSource.setGeoJson(
                point,
            )
        }

        if (
            pendingRoute == null
        ) {
            map.animateCamera(
                CameraUpdateFactory
                    .newLatLngZoom(
                        LatLng(
                            location.latitude,
                            location.longitude,
                        ),
                        15.5,
                    ),
            )
        }
    }
}

private const val ROUTE_SOURCE_ID =
    "vexmaps-route"

private const val ROUTE_LAYER_ID =
    "vexmaps-route-layer"

private const val ORIGIN_SOURCE_ID =
    "vexmaps-origin"

private const val ORIGIN_LAYER_ID =
    "vexmaps-origin-layer"

private const val DESTINATION_SOURCE_ID =
    "vexmaps-destination"

private const val DESTINATION_LAYER_ID =
    "vexmaps-destination-layer"

private const val DEVICE_LOCATION_SOURCE_ID =
    "vexmaps-device-location"

private const val DEVICE_LOCATION_LAYER_ID =
    "vexmaps-device-location-layer"

// [VXG RealForever]
