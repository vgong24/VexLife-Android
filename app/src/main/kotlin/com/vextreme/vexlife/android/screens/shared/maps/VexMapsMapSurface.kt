package com.vextreme.vexlife.android.screens.shared.maps

import android.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.locationbutton.compose.LocationButton
import androidx.core.locationbutton.compose.LocationButtonTextType
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.vextreme.vexlife.android.R
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.CameraPosition
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Point

/**
 * Android-native realization of the VexMaps basemap + explicit current-location capability.
 *
 * MapLibre owns rendering. OpenFreeMap is the current zero-key online OSM tile/style source.
 * Android owns the one-session precise-location grant. Neither becomes the owner of route,
 * navigation, or guidance meaning.
 */
@Composable
internal fun VexMapsMapSurface(
    darkTheme: Boolean,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val scope = rememberCoroutineScope()
    val mapController =
        remember {
            VexMapsMapController()
        }
    val locationSource =
        remember(context) {
            VexDeviceLocationSource(
                context,
            )
        }

    var locationState by
        remember {
            mutableStateOf<VexMapsLocationState>(
                VexMapsLocationState.Idle,
            )
        }

    val onlineStyleUri =
        if (darkTheme) {
            OPEN_FREE_MAP_DARK_STYLE
        } else {
            OPEN_FREE_MAP_LIBERTY_STYLE
        }

    val mapView =
        remember(
            context,
            onlineStyleUri,
            mapController,
        ) {
            MapLibre.getInstance(
                context.applicationContext,
            )

            MapView(context).apply {
                onCreate(null)
                getMapAsync { map ->
                    map.cameraPosition =
                        CameraPosition.Builder()
                            .target(
                                LatLng(
                                    0.0,
                                    0.0,
                                ),
                            )
                            .zoom(1.25)
                            .build()

                    map.setStyle(
                        onlineStyleUri,
                    ) { style ->
                        mapController.attach(
                            map = map,
                            style = style,
                        )
                    }
                }
            }
        }

    DisposableEffect(
        lifecycle,
        mapView,
        mapController,
    ) {
        val lifecycleBridge =
            VexMapsMapViewLifecycle(
                mapView = mapView,
            )

        lifecycleBridge.synchronize(
            lifecycle.currentState,
        )

        val observer =
            LifecycleEventObserver { _, event ->
                lifecycleBridge.onEvent(event)
            }

        lifecycle.addObserver(observer)

        onDispose {
            lifecycle.removeObserver(observer)
            mapController.detach()
            lifecycleBridge.dispose()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
        )

        Column(
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
            horizontalAlignment = Alignment.End,
        ) {
            LocationButton(
                textType =
                    LocationButtonTextType
                        .UsePreciseLocation,
                onPermissionResult = { granted ->
                    if (!granted) {
                        locationState =
                            VexMapsLocationState.Denied
                        return@LocationButton
                    }

                    locationState =
                        VexMapsLocationState.Finding

                    scope.launch {
                        locationState =
                            when (
                                val result =
                                    locationSource
                                        .currentPreciseLocation()
                            ) {
                                is VexDeviceLocationResult.Located -> {
                                    mapController
                                        .showDeviceLocation(
                                            result,
                                        )
                                    VexMapsLocationState.Ready
                                }

                                VexDeviceLocationResult.PermissionMissing ->
                                    VexMapsLocationState.Denied

                                VexDeviceLocationResult.LocationDisabled ->
                                    VexMapsLocationState.Disabled

                                VexDeviceLocationResult.Unavailable ->
                                    VexMapsLocationState.Unavailable
                            }
                    }
                },
                onError = {
                    locationState =
                        VexMapsLocationState.Error
                },
            )

            locationState.stringResOrNull
                ?.let { stringRes ->
                    Text(
                        text =
                            stringResource(
                                stringRes,
                            ),
                        style =
                            MaterialTheme
                                .typography
                                .labelMedium,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurface,
                        modifier =
                            Modifier
                                .padding(top = 8.dp)
                                .background(
                                    MaterialTheme
                                        .colorScheme
                                        .surface
                                        .copy(
                                            alpha = 0.88f,
                                        ),
                                )
                                .padding(
                                    horizontal = 8.dp,
                                    vertical = 5.dp,
                                ),
                    )
                }
        }

        Text(
            text = stringResource(
                R.string.vexmaps_attribution,
            ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .background(
                        MaterialTheme.colorScheme
                            .surface
                            .copy(alpha = 0.82f),
                    )
                    .padding(
                        horizontal = 6.dp,
                        vertical = 3.dp,
                    ),
        )
    }
}

private class VexMapsMapController {
    private var map: MapLibreMap? = null
    private var style: Style? = null
    private var pendingLocation:
        VexDeviceLocationResult.Located? = null

    fun attach(
        map: MapLibreMap,
        style: Style,
    ) {
        this.map = map
        this.style = style
        pendingLocation?.let(
            ::renderDeviceLocation,
        )
    }

    fun detach() {
        map = null
        style = null
    }

    fun showDeviceLocation(
        location: VexDeviceLocationResult.Located,
    ) {
        pendingLocation = location
        renderDeviceLocation(location)
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

private sealed interface VexMapsLocationState {
    val stringResOrNull: Int?

    data object Idle :
        VexMapsLocationState {
        override val stringResOrNull:
            Int? = null
    }

    data object Finding :
        VexMapsLocationState {
        override val stringResOrNull =
            R.string.vexmaps_location_finding
    }

    data object Ready :
        VexMapsLocationState {
        override val stringResOrNull =
            R.string.vexmaps_location_ready
    }

    data object Denied :
        VexMapsLocationState {
        override val stringResOrNull =
            R.string.vexmaps_location_denied
    }

    data object Disabled :
        VexMapsLocationState {
        override val stringResOrNull =
            R.string.vexmaps_location_disabled
    }

    data object Unavailable :
        VexMapsLocationState {
        override val stringResOrNull =
            R.string.vexmaps_location_unavailable
    }

    data object Error :
        VexMapsLocationState {
        override val stringResOrNull =
            R.string.vexmaps_location_error
    }
}

/**
 * Keeps MapView's imperative Android lifecycle behind the screen-local realization boundary.
 */
private class VexMapsMapViewLifecycle(
    private val mapView: MapView,
) {
    private var started = false
    private var resumed = false
    private var disposed = false

    fun synchronize(
        state: Lifecycle.State,
    ) {
        if (
            state.isAtLeast(
                Lifecycle.State.STARTED,
            )
        ) {
            start()
        }

        if (
            state.isAtLeast(
                Lifecycle.State.RESUMED,
            )
        ) {
            resume()
        }
    }

    fun onEvent(
        event: Lifecycle.Event,
    ) {
        when (event) {
            Lifecycle.Event.ON_START ->
                start()

            Lifecycle.Event.ON_RESUME ->
                resume()

            Lifecycle.Event.ON_PAUSE ->
                pause()

            Lifecycle.Event.ON_STOP ->
                stop()

            Lifecycle.Event.ON_DESTROY -> {
                pause()
                stop()
            }

            Lifecycle.Event.ON_CREATE,
            Lifecycle.Event.ON_ANY ->
                Unit
        }
    }

    fun dispose() {
        if (disposed) {
            return
        }

        pause()
        stop()
        mapView.onDestroy()
        disposed = true
    }

    private fun start() {
        if (
            disposed ||
            started
        ) {
            return
        }

        mapView.onStart()
        started = true
    }

    private fun resume() {
        if (
            disposed ||
            resumed
        ) {
            return
        }

        if (!started) {
            start()
        }

        mapView.onResume()
        resumed = true
    }

    private fun pause() {
        if (
            disposed ||
            !resumed
        ) {
            return
        }

        mapView.onPause()
        resumed = false
    }

    private fun stop() {
        if (
            disposed ||
            !started
        ) {
            return
        }

        pause()
        mapView.onStop()
        started = false
    }
}

private const val DEVICE_LOCATION_SOURCE_ID =
    "vexmaps-device-location"

private const val DEVICE_LOCATION_LAYER_ID =
    "vexmaps-device-location-layer"

private const val OPEN_FREE_MAP_LIBERTY_STYLE =
    "https://tiles.openfreemap.org/styles/liberty"

private const val OPEN_FREE_MAP_DARK_STYLE =
    "https://tiles.openfreemap.org/styles/dark"

// [VXG RealForever]
