package com.vextreme.vexlife.android.screens.shared.maps

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.SideEffect
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
import androidx.core.content.ContextCompat
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
import org.maplibre.android.camera.CameraPosition
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
    val regionStore =
        remember(context) {
            VexMapsRegionPackStore(
                context,
            )
        }
    val offlineMapStore =
        remember(context) {
            VexMapsOfflineMapStore(
                context,
            )
        }

    var locationState by
        remember {
            mutableStateOf<VexMapsLocationState>(
                VexMapsLocationState.Idle,
            )
        }
    var currentLocation by
        remember {
            mutableStateOf<
                VexDeviceLocationResult.Located?
            >(null)
        }
    var selectedOrigin by
        remember {
            mutableStateOf<VexMapCoordinate?>(
                null,
            )
        }
    var destination by
        remember {
            mutableStateOf<VexMapCoordinate?>(
                null,
            )
        }
    var mapSelectionTarget by
        remember {
            mutableStateOf(
                VexMapsMapSelectionTarget.DESTINATION,
            )
        }
    var regionPack by
        remember {
            mutableStateOf(
                regionStore.currentPack(),
            )
        }
    var planningState by
        remember {
            mutableStateOf<VexMapsPlanningState>(
                VexMapsPlanningState.Idle,
            )
        }

    val readCurrentPreciseLocation: () -> Unit = {
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
                        currentLocation =
                            result
                        mapController
                            .showDeviceLocation(
                                result,
                            )
                        VexMapsLocationState.Ready
                    }

                    VexDeviceLocationResult.PermissionMissing -> {
                        currentLocation = null
                        VexMapsLocationState.Denied
                    }

                    VexDeviceLocationResult.ApproximateOnly -> {
                        currentLocation = null
                        VexMapsLocationState.Approximate
                    }

                    VexDeviceLocationResult.LocationDisabled -> {
                        currentLocation = null
                        VexMapsLocationState.Disabled
                    }

                    VexDeviceLocationResult.Unavailable -> {
                        currentLocation = null
                        VexMapsLocationState.Unavailable
                    }
                }
        }
    }

    val preciseLocationPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts
                .RequestMultiplePermissions(),
        ) { grants ->
            val fineGranted =
                grants[
                    Manifest.permission
                        .ACCESS_FINE_LOCATION
                ] == true ||
                    ContextCompat
                        .checkSelfPermission(
                            context,
                            Manifest.permission
                                .ACCESS_FINE_LOCATION,
                        ) ==
                    PackageManager
                        .PERMISSION_GRANTED

            if (fineGranted) {
                readCurrentPreciseLocation()
            } else {
                currentLocation = null
                val coarseGranted =
                    grants[
                        Manifest.permission
                            .ACCESS_COARSE_LOCATION
                    ] == true ||
                        ContextCompat
                            .checkSelfPermission(
                                context,
                                Manifest.permission
                                    .ACCESS_COARSE_LOCATION,
                            ) ==
                        PackageManager
                            .PERMISSION_GRANTED

                locationState =
                    if (coarseGranted) {
                        VexMapsLocationState.Approximate
                    } else {
                        VexMapsLocationState.Denied
                    }
            }
        }

    var offlineMapState by
        remember {
            mutableStateOf<VexMapsOfflineMapState>(
                VexMapsOfflineMapState.Loading,
            )
        }

    LaunchedEffect(
        offlineMapStore,
    ) {
        offlineMapState =
            runCatching {
                offlineMapStore.load()
            }.getOrElse {
                VexMapsOfflineMapState.Error
            }
    }

    val offlineMapImportLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts
                .OpenDocument(),
        ) { uri ->
            if (uri == null) {
                return@rememberLauncherForActivityResult
            }

            runtime.recordDynamicInteraction(
                subjectRef =
                    "presentation.vexmaps.offline-map.import",
                action = "Selected",
            )

            offlineMapState =
                VexMapsOfflineMapState.Loading

            scope.launch {
                offlineMapState =
                    runCatching {
                        offlineMapStore.import(
                            uri,
                        )
                    }.getOrElse {
                        VexMapsOfflineMapState.Error
                    }
            }
        }

    val importLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts
                .OpenDocument(),
        ) { uri ->
            if (uri == null) {
                return@rememberLauncherForActivityResult
            }

            runtime.recordDynamicInteraction(
                subjectRef =
                    "presentation.vexmaps.region.import",
                action = "Selected",
            )
            planningState =
                VexMapsPlanningState.Importing

            scope.launch {
                runCatching {
                    regionStore.import(uri)
                }.onSuccess { imported ->
                    regionPack = imported
                    planningState =
                        VexMapsPlanningState.Idle
                }.onFailure {
                    planningState =
                        VexMapsPlanningState.Error(
                            VexMapsPlanningError.REGION_IMPORT,
                        )
                }
            }
        }

    SideEffect {
        mapController
            .setOnMapLongPress(
                if (
                    navigationState.phase ==
                    VexNavigationSessionPhase.IDLE
                ) {
                    { selected ->
                        when (mapSelectionTarget) {
                            VexMapsMapSelectionTarget.ORIGIN -> {
                                runtime.recordDynamicInteraction(
                                    subjectRef =
                                        "presentation.vexmaps.origin.pin",
                                    action = "LongPressed",
                                )
                                selectedOrigin =
                                    selected
                                mapSelectionTarget =
                                    VexMapsMapSelectionTarget.DESTINATION
                                planningState =
                                    VexMapsPlanningState.Idle
                                mapController
                                    .showOrigin(
                                        selected,
                                    )
                            }

                            VexMapsMapSelectionTarget.DESTINATION -> {
                                runtime.recordDynamicInteraction(
                                    subjectRef =
                                        "presentation.vexmaps.destination.pin",
                                    action = "LongPressed",
                                )
                                destination =
                                    selected
                                planningState =
                                    VexMapsPlanningState.Idle
                                mapController
                                    .showDestination(
                                        selected,
                                    )
                            }
                        }
                    }
                } else {
                    null
                },
            )
    }

    val fallbackOnlineStyleUri =
        if (darkTheme) {
            OPEN_FREE_MAP_DARK_STYLE
        } else {
            OPEN_FREE_MAP_LIBERTY_STYLE
        }

    val mapStyleUri =
        (
            offlineMapState as?
                VexMapsOfflineMapState.Ready
        )
            ?.preferred
            ?.styleUri
            ?: fallbackOnlineStyleUri

    val mapView =
        remember(
            context,
            mapStyleUri,
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
                        mapStyleUri,
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

    val preciseLocationActive =
        locationState ==
            VexMapsLocationState.Ready ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) ==
            PackageManager.PERMISSION_GRANTED

    val deviceRouteOrigin =
        currentLocation?.let { location ->
            VexMapCoordinate(
                latitude = location.latitude,
                longitude = location.longitude,
            )
        }
    val routeOrigin =
        resolveVexMapsRouteOrigin(
            selectedOrigin = selectedOrigin,
            deviceOrigin = deviceRouteOrigin,
        )

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
        )

        if (
            navigationState.phase ==
            VexNavigationSessionPhase.IDLE
        ) {
            VexMapsRoutePlannerCard(
                regionPack = regionPack,
                offlineMapState =
                    offlineMapState,
                routeOrigin =
                    routeOrigin,
                usingPinnedOrigin =
                    selectedOrigin != null,
                selectingOrigin =
                    mapSelectionTarget ==
                        VexMapsMapSelectionTarget.ORIGIN,
                canUseDeviceOrigin =
                    deviceRouteOrigin != null,
                destination =
                    destination,
                planningState =
                    planningState,
                onChooseOriginOnMap = {
                    runtime.recordDynamicInteraction(
                        subjectRef =
                            "presentation.vexmaps.origin.choose",
                        action = "Clicked",
                    )
                    mapSelectionTarget =
                        VexMapsMapSelectionTarget.ORIGIN
                    planningState =
                        VexMapsPlanningState.Idle
                },
                onUseDeviceOrigin = {
                    runtime.recordDynamicInteraction(
                        subjectRef =
                            "presentation.vexmaps.origin.use-device",
                        action = "Clicked",
                    )
                    selectedOrigin = null
                    mapSelectionTarget =
                        VexMapsMapSelectionTarget.DESTINATION
                    planningState =
                        VexMapsPlanningState.Idle
                    mapController.clearOrigin()
                },
                onImportOfflineMap = {
                    runtime.recordDynamicInteraction(
                        subjectRef =
                            "presentation.vexmaps.offline-map.import",
                        action = "Clicked",
                    )
                    offlineMapImportLauncher.launch(
                        arrayOf("*/*"),
                    )
                },
                onImportRegion = {
                    runtime.recordDynamicInteraction(
                        subjectRef =
                            "presentation.vexmaps.region.import",
                        action = "Clicked",
                    )
                    importLauncher.launch(
                        arrayOf("*/*"),
                    )
                },
                onRouteHere = {
                    val pack =
                        regionPack
                            ?: return@VexMapsRoutePlannerCard
                    val origin =
                        routeOrigin
                            ?: return@VexMapsRoutePlannerCard
                    val target =
                        destination
                            ?: return@VexMapsRoutePlannerCard

                    runtime.recordDynamicInteraction(
                        subjectRef =
                            "presentation.vexmaps.route.calculate",
                        action = "Clicked",
                    )
                    planningState =
                        VexMapsPlanningState.Routing

                    scope.launch {
                        runCatching {
                            VexMapsOfflineRoutingProcess
                                .planner
                                .route(
                                    context = context,
                                    pack = pack,
                                    request =
                                        VexOfflineRouteRequest(
                                            routeRef =
                                                "route.vexmaps.current",
                                            routeRevision =
                                                VexMapsRouteIdentity
                                                    .nextRevision(),
                                            origin =
                                                origin,
                                            destination =
                                                target,
                                            destinationLabel =
                                                context.getString(
                                                    R.string
                                                        .vexmaps_route_destination_label,
                                                ),
                                        ),
                                )
                        }.onSuccess { route ->
                            VexMapsNavigationProcess
                                .prepare(
                                    routeResult = route,
                                    routingPack = pack,
                                )
                            planningState =
                                VexMapsPlanningState.Idle
                        }.onFailure {
                            planningState =
                                VexMapsPlanningState.Error(
                                    VexMapsPlanningError.ROUTE,
                                )
                        }
                    }
                },
                modifier =
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .widthIn(max = 320.dp),
            )
        } else {
            VexMapsNavigationCard(
                state = navigationState,
                locationReady =
                    preciseLocationActive,
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
                        .stop()
                },
                modifier =
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .widthIn(max = 340.dp),
            )
        }

        if (
            navigationState.phase ==
                VexNavigationSessionPhase.IDLE ||
            navigationState.phase ==
                VexNavigationSessionPhase.READY
        ) {
            Column(
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(
                            end = 12.dp,
                            bottom = 52.dp,
                        ),
                horizontalAlignment =
                    Alignment.End,
            ) {
                OutlinedButton(
                    onClick = {
                        runtime.recordDynamicInteraction(
                            subjectRef =
                                "presentation.vexmaps.location.use-precise",
                            action = "Clicked",
                        )

                        if (
                            ContextCompat
                                .checkSelfPermission(
                                    context,
                                    Manifest.permission
                                        .ACCESS_FINE_LOCATION,
                                ) ==
                            PackageManager
                                .PERMISSION_GRANTED
                        ) {
                            readCurrentPreciseLocation()
                        } else {
                            preciseLocationPermissionLauncher
                                .launch(
                                    arrayOf(
                                        Manifest.permission
                                            .ACCESS_COARSE_LOCATION,
                                        Manifest.permission
                                            .ACCESS_FINE_LOCATION,
                                    ),
                                )
                        }
                    },
                ) {
                    Text(
                        stringResource(
                            R.string
                                .vexmaps_use_precise_location,
                        ),
                    )
                }

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
private fun VexMapsRoutePlannerCard(
    regionPack: VexMapsRegionPack?,
    offlineMapState:
        VexMapsOfflineMapState,
    routeOrigin: VexMapCoordinate?,
    usingPinnedOrigin: Boolean,
    selectingOrigin: Boolean,
    canUseDeviceOrigin: Boolean,
    destination: VexMapCoordinate?,
    planningState: VexMapsPlanningState,
    onChooseOriginOnMap: () -> Unit,
    onUseDeviceOrigin: () -> Unit,
    onImportOfflineMap: () -> Unit,
    onImportRegion: () -> Unit,
    onRouteHere: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
    ) {
        Column(
            modifier =
                Modifier.padding(14.dp),
            verticalArrangement =
                Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text =
                    when {
                        regionPack == null ->
                            stringResource(
                                R.string
                                    .vexmaps_region_pack_needed,
                            )

                        else ->
                            stringResource(
                                R.string
                                    .vexmaps_region_pack_ready,
                            )
                    },
                style =
                    MaterialTheme
                        .typography
                        .titleMedium,
            )

            when (planningState) {
                VexMapsPlanningState.Importing ->
                    Text(
                        stringResource(
                            R.string
                                .vexmaps_region_pack_importing,
                        ),
                    )

                VexMapsPlanningState.Routing ->
                    Text(
                        stringResource(
                            R.string
                                .vexmaps_route_calculating,
                        ),
                    )

                is VexMapsPlanningState.Error ->
                    Text(
                        stringResource(
                            when (
                                planningState.error
                            ) {
                                VexMapsPlanningError.REGION_IMPORT ->
                                    R.string
                                        .vexmaps_region_pack_error

                                VexMapsPlanningError.ROUTE ->
                                    R.string
                                        .vexmaps_route_error
                            },
                        ),
                    )

                VexMapsPlanningState.Idle ->
                    Unit
            }

            Text(
                text =
                    when (offlineMapState) {
                        VexMapsOfflineMapState.Loading ->
                            stringResource(
                                R.string
                                    .vexmaps_offline_map_loading,
                            )

                        VexMapsOfflineMapState.None ->
                            stringResource(
                                R.string
                                    .vexmaps_offline_map_none,
                            )

                        is VexMapsOfflineMapState.Ready ->
                            stringResource(
                                R.string
                                    .vexmaps_offline_map_ready,
                                offlineMapState
                                    .completeRegionCount,
                            )

                        VexMapsOfflineMapState.Error ->
                            stringResource(
                                R.string
                                    .vexmaps_offline_map_error,
                            )
                    },
                style =
                    MaterialTheme
                        .typography
                        .bodySmall,
            )

            OutlinedButton(
                onClick = onImportOfflineMap,
                enabled =
                    offlineMapState !is
                    VexMapsOfflineMapState.Loading,
            ) {
                Text(
                    stringResource(
                        R.string
                            .vexmaps_offline_map_import,
                    ),
                )
            }

            OutlinedButton(
                onClick = onImportRegion,
                enabled =
                    planningState !is
                    VexMapsPlanningState.Importing &&
                        planningState !is
                        VexMapsPlanningState.Routing,
            ) {
                Text(
                    stringResource(
                        if (regionPack == null) {
                            R.string
                                .vexmaps_region_pack_import
                        } else {
                            R.string
                                .vexmaps_region_pack_replace
                        },
                    ),
                )
            }

            if (regionPack != null) {
                Text(
                    text =
                        stringResource(
                            when {
                                selectingOrigin ->
                                    R.string
                                        .vexmaps_route_start_selecting

                                usingPinnedOrigin ->
                                    R.string
                                        .vexmaps_route_start_pinned

                                routeOrigin != null ->
                                    R.string
                                        .vexmaps_route_start_current

                                else ->
                                    R.string
                                        .vexmaps_route_start_needed
                            },
                        ),
                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium,
                )

                OutlinedButton(
                    onClick = onChooseOriginOnMap,
                    enabled =
                        planningState !is
                        VexMapsPlanningState.Routing &&
                            planningState !is
                            VexMapsPlanningState.Importing,
                ) {
                    Text(
                        stringResource(
                            if (usingPinnedOrigin) {
                                R.string
                                    .vexmaps_route_change_start
                            } else {
                                R.string
                                    .vexmaps_route_choose_start
                            },
                        ),
                    )
                }

                if (
                    usingPinnedOrigin &&
                    canUseDeviceOrigin
                ) {
                    OutlinedButton(
                        onClick = onUseDeviceOrigin,
                        enabled =
                            planningState !is
                            VexMapsPlanningState.Routing &&
                                planningState !is
                                VexMapsPlanningState.Importing,
                    ) {
                        Text(
                            stringResource(
                                R.string
                                    .vexmaps_route_use_current_start,
                            ),
                        )
                    }
                }

                if (
                    routeOrigin != null &&
                    !selectingOrigin
                ) {
                    Text(
                        text =
                            if (destination == null) {
                                stringResource(
                                    R.string
                                        .vexmaps_route_choose_destination,
                                )
                            } else {
                                stringResource(
                                    R.string
                                        .vexmaps_route_destination_selected,
                                )
                            },
                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium,
                    )
                }
            }

            if (
                regionPack != null &&
                routeOrigin != null &&
                destination != null &&
                !selectingOrigin
            ) {
                Button(
                    onClick = onRouteHere,
                    enabled =
                        planningState !is
                        VexMapsPlanningState.Routing &&
                            planningState !is
                            VexMapsPlanningState.Importing,
                ) {
                    Text(
                        stringResource(
                            R.string
                                .vexmaps_route_here,
                        ),
                    )
                }
            }
        }
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
                                "${it.distanceToManeuverMeters} m",
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

internal fun resolveVexMapsRouteOrigin(
    selectedOrigin: VexMapCoordinate?,
    deviceOrigin: VexMapCoordinate?,
): VexMapCoordinate? =
    selectedOrigin ?: deviceOrigin

private enum class VexMapsMapSelectionTarget {
    ORIGIN,
    DESTINATION,
}

private sealed interface VexMapsPlanningState {
    data object Idle :
        VexMapsPlanningState

    data object Importing :
        VexMapsPlanningState

    data object Routing :
        VexMapsPlanningState

    data class Error(
        val error: VexMapsPlanningError,
    ) : VexMapsPlanningState
}

private enum class VexMapsPlanningError {
    REGION_IMPORT,
    ROUTE,
}

private class VexMapsMapController {
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

    fun setOnMapLongPress(
        callback:
            ((VexMapCoordinate) -> Unit)?,
    ) {
        onMapLongPress =
            callback
    }

    fun attach(
        map: MapLibreMap,
        style: Style,
    ) {
        this.map = map
        this.style = style
        map.addOnMapLongClickListener(
            longClickListener,
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
        map = null
        style = null
        lastFittedRouteKey = null
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

    data object Approximate :
        VexMapsLocationState {
        override val stringResOrNull =
            R.string.vexmaps_location_approximate
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

private const val OPEN_FREE_MAP_LIBERTY_STYLE =
    "https://tiles.openfreemap.org/styles/liberty"

private const val OPEN_FREE_MAP_DARK_STYLE =
    "https://tiles.openfreemap.org/styles/dark"

// [VXG RealForever]
