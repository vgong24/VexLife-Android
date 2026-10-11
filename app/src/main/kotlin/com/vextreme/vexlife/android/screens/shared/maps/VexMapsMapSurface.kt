package com.vextreme.vexlife.android.screens.shared.maps

import android.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import com.vextreme.vexlife.android.runtime.VexRuntime
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.CameraPosition
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
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
 * Android-native realization of the VexMaps map and current active-navigation presentation.
 *
 * MapLibre owns pixels. Android owns explicit location access. [VexNavigationSession] owns route
 * progress. This Composable only projects those truths into one calm human surface.
 */
@Composable
internal fun VexMapsMapSurface(
    runtime: VexRuntime,
    darkTheme: Boolean,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val scope = rememberCoroutineScope()
    val navigationState by
        VexMapsNavigationProcess
            .session
            .states
            .collectAsState()

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

    LaunchedEffect(
        navigationState
            .routeResult
            ?.route
            ?.routeRef,
        navigationState
            .routeResult
            ?.route
            ?.routeRevision,
        mapController,
    ) {
        val routeResult =
            navigationState.routeResult

        if (routeResult == null) {
            mapController.clearRoute()
        } else {
            mapController.showRoute(
                routeResult,
            )
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

        VexMapsNavigationCard(
            state = navigationState,
            locationReady =
                locationState ==
                    VexMapsLocationState.Ready,
            onStart = {
                runtime.recordDynamicInteraction(
                    subjectRef =
                        "presentation.vexmaps.navigation.start",
                    action = "Clicked",
                )
                VexMapsNavigationService
                    .startPrepared(context)
            },
            onStop = {
                runtime.recordDynamicInteraction(
                    subjectRef =
                        "presentation.vexmaps.navigation.stop",
                    action = "Clicked",
                )
                VexMapsNavigationService
                    .stop(context)
            },
            onDone = {
                runtime.recordDynamicInteraction(
                    subjectRef =
                        "presentation.vexmaps.navigation.done",
                    action = "Clicked",
                )
                VexMapsNavigationProcess
                    .session
                    .stop()
            },
            modifier =
                Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .widthIn(max = 340.dp),
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

@Composable
private fun VexMapsNavigationCard(
    state: VexNavigationSessionState,
    locationReady: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (
        state.phase ==
        VexNavigationSessionPhase.IDLE
    ) {
        return
    }

    val destination =
        state
            .routeResult
            ?.route
            ?.destinationLabel
            .orEmpty()

    Card(
        modifier = modifier,
    ) {
        Column(
            modifier =
                Modifier.padding(14.dp),
            verticalArrangement =
                Arrangement.spacedBy(8.dp),
        ) {
            when (state.phase) {
                VexNavigationSessionPhase.READY -> {
                    Text(
                        text =
                            stringResource(
                                R.string
                                    .vexmaps_navigation_ready,
                                destination,
                                state
                                    .routeResult
                                    ?.route
                                    ?.totalDistanceMeters
                                    ?: 0,
                            ),
                        style =
                            MaterialTheme
                                .typography
                                .titleMedium,
                    )

                    if (locationReady) {
                        Button(
                            onClick = onStart,
                        ) {
                            Text(
                                stringResource(
                                    R.string
                                        .vexmaps_navigation_start,
                                ),
                            )
                        }
                    } else {
                        Text(
                            text =
                                stringResource(
                                    R.string
                                        .vexmaps_navigation_location_needed,
                                ),
                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall,
                        )
                    }
                }

                VexNavigationSessionPhase.ACTIVE -> {
                    val progress =
                        state.convergence?.progress
                    val next =
                        progress?.nextManeuver

                    Text(
                        text =
                            next
                                ?.maneuver
                                ?.instructionProjection
                                ?: stringResource(
                                    R.string
                                        .vexmaps_navigation_continue,
                                ),
                        style =
                            MaterialTheme
                                .typography
                                .titleLarge,
                    )

                    next?.let {
                        Text(
                            text =
                                stringResource(
                                    R.string
                                        .vexmaps_navigation_notification_active,
                                    it.distanceToManeuverMeters,
                                ),
                            style =
                                MaterialTheme
                                    .typography
                                    .headlineSmall,
                        )
                    }

                    Text(
                        text =
                            stringResource(
                                R.string
                                    .vexmaps_navigation_remaining,
                                progress
                                    ?.distanceRemainingMeters
                                    ?: 0,
                            ),
                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium,
                    )

                    NavigationStopButton(
                        onStop = onStop,
                    )
                }

                VexNavigationSessionPhase.OFF_ROUTE -> {
                    Text(
                        text =
                            stringResource(
                                R.string
                                    .vexmaps_navigation_off_route,
                            ),
                        style =
                            MaterialTheme
                                .typography
                                .titleLarge,
                    )
                    Text(
                        text =
                            stringResource(
                                R.string
                                    .vexmaps_navigation_off_route_body,
                            ),
                    )
                    NavigationStopButton(
                        onStop = onStop,
                    )
                }

                VexNavigationSessionPhase.LOCATION_HELD -> {
                    Text(
                        text =
                            stringResource(
                                R.string
                                    .vexmaps_navigation_waiting_location,
                            ),
                        style =
                            MaterialTheme
                                .typography
                                .titleLarge,
                    )
                    Text(
                        text =
                            stringResource(
                                R.string
                                    .vexmaps_navigation_waiting_location_body,
                            ),
                    )
                    NavigationStopButton(
                        onStop = onStop,
                    )
                }

                VexNavigationSessionPhase.ARRIVED -> {
                    Text(
                        text =
                            stringResource(
                                R.string
                                    .vexmaps_navigation_arrived,
                                destination,
                            ),
                        style =
                            MaterialTheme
                                .typography
                                .titleLarge,
                    )
                    Button(
                        onClick = onDone,
                    ) {
                        Text(
                            stringResource(
                                R.string
                                    .vexmaps_navigation_done,
                            ),
                        )
                    }
                }

                VexNavigationSessionPhase.IDLE ->
                    Unit
            }
        }
    }
}

@Composable
private fun NavigationStopButton(
    onStop: () -> Unit,
) {
    OutlinedButton(
        onClick = onStop,
    ) {
        Text(
            stringResource(
                R.string
                    .vexmaps_navigation_stop,
            ),
        )
    }
}

private class VexMapsMapController {
    private var map: MapLibreMap? = null
    private var style: Style? = null
    private var pendingLocation:
        VexDeviceLocationResult.Located? = null
    private var pendingRoute:
        VexOfflineRouteResult? = null
    private var lastFittedRouteKey:
        String? = null

    fun attach(
        map: MapLibreMap,
        style: Style,
    ) {
        this.map = map
        this.style = style

        pendingRoute?.let { route ->
            renderRoute(
                route = route,
                fitCamera = true,
            )
        }

        pendingLocation?.let(
            ::renderDeviceLocation,
        )
    }

    fun detach() {
        map = null
        style = null
        lastFittedRouteKey = null
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
        style.removeLayer(
            ROUTE_LAYER_ID,
        )
        style.removeSource(
            ROUTE_SOURCE_ID,
        )
    }

    fun showDeviceLocation(
        location: VexDeviceLocationResult.Located,
    ) {
        pendingLocation = location
        renderDeviceLocation(location)
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
            route.route.routeRef +
                ":" +
                route.route.routeRevision

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

private const val ROUTE_SOURCE_ID =
    "vexmaps-route"

private const val ROUTE_LAYER_ID =
    "vexmaps-route-layer"

private const val DEVICE_LOCATION_SOURCE_ID =
    "vexmaps-device-location"

private const val DEVICE_LOCATION_LAYER_ID =
    "vexmaps-device-location-layer"

private const val OPEN_FREE_MAP_LIBERTY_STYLE =
    "https://tiles.openfreemap.org/styles/liberty"

private const val OPEN_FREE_MAP_DARK_STYLE =
    "https://tiles.openfreemap.org/styles/dark"

// [VXG RealForever]
