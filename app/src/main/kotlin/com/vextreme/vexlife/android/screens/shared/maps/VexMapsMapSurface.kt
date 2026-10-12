package com.vextreme.vexlife.android.screens.shared.maps

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.vextreme.vexlife.android.elements.vexElement
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
    val navigationVisualLocation by
        VexMapsNavigationProcess
            .visualLocations
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
    val placeSearchStore =
        remember(context) {
            VexMapsPlaceSearchStore(
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
    val routeRequestGate =
        remember {
            VexMapsLatestRequestGate()
        }

    var viewportLatitude by
        rememberSaveable {
            mutableStateOf<Double?>(null)
        }
    var viewportLongitude by
        rememberSaveable {
            mutableStateOf<Double?>(null)
        }
    var viewportZoom by
        rememberSaveable {
            mutableStateOf<Double?>(null)
        }
    var viewportBearing by
        rememberSaveable {
            mutableStateOf<Double?>(null)
        }
    var viewportTilt by
        rememberSaveable {
            mutableStateOf<Double?>(null)
        }
    var viewportRouteKey by
        rememberSaveable {
            mutableStateOf<String?>(null)
        }
    val viewportSnapshot =
        restoreVexMapsViewportSnapshot(
            latitude = viewportLatitude,
            longitude = viewportLongitude,
            zoom = viewportZoom,
            bearing = viewportBearing,
            tilt = viewportTilt,
        )
    fun setViewportSnapshot(
        snapshot: VexMapsViewportSnapshot,
        routeKey: String?,
    ) {
        viewportLatitude =
            snapshot.center.latitude
        viewportLongitude =
            snapshot.center.longitude
        viewportZoom =
            snapshot.zoom
        viewportBearing =
            snapshot.bearing
        viewportTilt =
            snapshot.tilt
        viewportRouteKey =
            routeKey
    }

    var selectedOriginLatitude by
        rememberSaveable {
            mutableStateOf<Double?>(
                null,
            )
        }
    var selectedOriginLongitude by
        rememberSaveable {
            mutableStateOf<Double?>(
                null,
            )
        }
    val selectedOrigin =
        restoreVexMapCoordinate(
            latitude =
                selectedOriginLatitude,
            longitude =
                selectedOriginLongitude,
        )
    fun setSelectedOrigin(
        coordinate: VexMapCoordinate?,
    ) {
        routeRequestGate.invalidate()
        selectedOriginLatitude =
            coordinate?.latitude
        selectedOriginLongitude =
            coordinate?.longitude
    }

    var destinationLatitude by
        rememberSaveable {
            mutableStateOf<Double?>(
                null,
            )
        }
    var destinationLongitude by
        rememberSaveable {
            mutableStateOf<Double?>(
                null,
            )
        }
    val destination =
        restoreVexMapCoordinate(
            latitude =
                destinationLatitude,
            longitude =
                destinationLongitude,
        )
    fun setDestination(
        coordinate: VexMapCoordinate?,
    ) {
        routeRequestGate.invalidate()
        destinationLatitude =
            coordinate?.latitude
        destinationLongitude =
            coordinate?.longitude
    }

    var destinationLabel by
        rememberSaveable {
            mutableStateOf<String?>(
                null,
            )
        }
    var placeSearchPack by
        remember {
            mutableStateOf(
                placeSearchStore.currentPack(),
            )
        }
    var placeSearchQuery by
        rememberSaveable {
            mutableStateOf("")
        }
    var placeSearchState by
        remember {
            mutableStateOf<VexMapsPlaceSearchState>(
                VexMapsPlaceSearchState.Idle,
            )
        }
    val placeSearchRequestGate =
        remember {
            VexMapsLatestRequestGate()
        }
    var mapSelectionTargetName by
        rememberSaveable {
            mutableStateOf(
                VexMapsMapSelectionTarget
                    .DESTINATION
                    .name,
            )
        }
    val mapSelectionTarget =
        restoreVexMapsMapSelectionTarget(
            mapSelectionTargetName,
        )
    fun setMapSelectionTarget(
        target: VexMapsMapSelectionTarget,
    ) {
        mapSelectionTargetName =
            target.name
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
    var travelModeName by
        rememberSaveable {
            mutableStateOf(
                VexMapsTravelMode
                    .DRIVE
                    .name,
            )
        }
    val travelMode =
        restoreVexMapsTravelMode(
            travelModeName,
        )
    fun setTravelMode(
        mode: VexMapsTravelMode,
    ) {
        routeRequestGate.invalidate()
        travelModeName =
            mode.name
    }

    fun invalidateDeviceRouteOriginIfUnpinned() {
        if (selectedOrigin != null) {
            return
        }

        routeRequestGate.invalidate()
        if (
            planningState is
                VexMapsPlanningState.Routing
        ) {
            planningState =
                VexMapsPlanningState.Idle
        }
    }

    fun readCurrentPreciseLocation(
        onLocated: (() -> Unit)? = null,
    ) {
        locationState =
            VexMapsLocationState.Finding

        scope.launch {
            val result =
                locationSource
                    .currentPreciseLocation()
            val projectedLocation =
                projectVexMapsDeviceLocation(
                    result,
                )

            invalidateDeviceRouteOriginIfUnpinned()
            currentLocation =
                projectedLocation

            locationState =
                when (result) {
                    is VexDeviceLocationResult.Located ->
                        if (
                            isVexMapsNavigationStartPreflightSatisfied(
                                result,
                            )
                        ) {
                            VexMapsLocationState.Ready
                        } else {
                            VexMapsLocationState.Inaccurate
                        }

                    VexDeviceLocationResult.PermissionMissing ->
                        VexMapsLocationState.Denied

                    VexDeviceLocationResult.ApproximateOnly ->
                        VexMapsLocationState.Approximate

                    VexDeviceLocationResult.LocationDisabled ->
                        VexMapsLocationState.Disabled

                    VexDeviceLocationResult.Unavailable ->
                        VexMapsLocationState.Unavailable
                }

            if (
                isVexMapsNavigationStartPreflightSatisfied(
                    result,
                )
            ) {
                onLocated?.invoke()
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
                invalidateDeviceRouteOriginIfUnpinned()
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

    val placeSearchImportLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts
                .OpenDocument(),
        ) { uri ->
            if (uri == null) {
                return@rememberLauncherForActivityResult
            }

            runtime.recordDynamicInteraction(
                subjectRef =
                    "presentation.vexmaps.place-search.import",
                action = "Selected",
            )
            placeSearchState =
                VexMapsPlaceSearchState.Importing

            scope.launch {
                runCatching {
                    placeSearchStore.import(
                        uri,
                    )
                }.onSuccess { imported ->
                    placeSearchPack = imported
                    placeSearchState =
                        VexMapsPlaceSearchState.Idle
                }.onFailure {
                    placeSearchState =
                        VexMapsPlaceSearchState.Error(
                            VexMapsPlaceSearchError.IMPORT,
                        )
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
                                setSelectedOrigin(
                                    selected,
                                )
                                setMapSelectionTarget(
                                    VexMapsMapSelectionTarget.DESTINATION,
                                )
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
                                setDestination(
                                    selected,
                                )
                                destinationLabel =
                                    context.getString(
                                        R.string
                                            .vexmaps_route_destination_label,
                                    )
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
        val currentRouteKey =
            navigationState
                .routeResult
                ?.vexMapsRouteKey()
        mapController.setOnViewportChanged { snapshot ->
            setViewportSnapshot(
                snapshot = snapshot,
                routeKey = currentRouteKey,
            )
        }
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

    val currentRouteKey =
        navigationState
            .routeResult
            ?.vexMapsRouteKey()
    val restoredRouteKey =
        resolveVexMapsRestoredRouteFitKey(
            hasRestoredViewport =
                viewportSnapshot != null,
            savedViewportRouteKey =
                viewportRouteKey,
            currentRouteKey =
                currentRouteKey,
        )

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
                        viewportSnapshot
                            ?.toCameraPosition()
                            ?: CameraPosition.Builder()
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
                            restoredRouteKey =
                                restoredRouteKey,
                        )
                    }
                }
            }
        }

    LaunchedEffect(
        selectedOrigin,
        mapController,
    ) {
        if (selectedOrigin == null) {
            mapController.clearOrigin()
        } else {
            mapController.showOrigin(
                selectedOrigin,
            )
        }
    }

    LaunchedEffect(
        destination,
        mapController,
    ) {
        if (destination == null) {
            mapController.clearDestination()
        } else {
            mapController.showDestination(
                destination,
            )
        }
    }

    val mapDeviceLocation =
        projectVexMapsMapDeviceLocation(
            plannerLocation = currentLocation,
            navigationVisualLocation =
                navigationVisualLocation,
        )

    LaunchedEffect(
        mapDeviceLocation,
        mapController,
    ) {
        if (mapDeviceLocation == null) {
            mapController.clearDeviceLocation()
        } else {
            mapController.showDeviceLocation(
                mapDeviceLocation,
            )
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

    val precisePermissionGranted =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) ==
            PackageManager.PERMISSION_GRANTED

    val navigationLocationReady =
        canStartVexMapsNavigation(
            precisePermissionGranted =
                precisePermissionGranted,
            hasCurrentLocation =
                currentLocation != null,
            locationFixReady =
                locationState ==
                    VexMapsLocationState.Ready,
        )

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
                placeSearchPack =
                    placeSearchPack,
                placeSearchQuery =
                    placeSearchQuery,
                placeSearchState =
                    placeSearchState,
                travelMode =
                    travelMode,
                planningState =
                    planningState,
                onTravelModeSelected = { selected ->
                    runtime.activate(
                        VexMapsScreen.travelModeElement(
                            selected,
                        ),
                    )
                    setTravelMode(
                        selected,
                    )
                    planningState =
                        VexMapsPlanningState.Idle
                },
                onPlaceSearchQueryChange = { query ->
                    placeSearchRequestGate.invalidate()
                    placeSearchQuery = query
                    if (
                        placeSearchState !is
                            VexMapsPlaceSearchState.Importing
                    ) {
                        placeSearchState =
                            VexMapsPlaceSearchState.Idle
                    }
                },
                onImportPlaceSearch = {
                    runtime.activate(
                        VexMapsScreen.ImportPlaceSearch,
                    )
                    placeSearchRequestGate.invalidate()
                    placeSearchImportLauncher.launch(
                        arrayOf("*/*"),
                    )
                },
                onSearchPlaces = {
                    val pack =
                        placeSearchPack
                            ?: return@VexMapsRoutePlannerCard
                    val query =
                        placeSearchQuery
                            .trim()
                    if (query.isEmpty()) {
                        return@VexMapsRoutePlannerCard
                    }

                    runtime.activate(
                        VexMapsScreen.SearchPlaces,
                    )
                    val requestGeneration =
                        placeSearchRequestGate.begin()
                    placeSearchState =
                        VexMapsPlaceSearchState.Searching

                    scope.launch {
                        val result =
                            runCatching {
                                placeSearchStore.search(
                                    pack = pack,
                                    query = query,
                                )
                            }

                        if (
                            !placeSearchRequestGate
                                .accepts(
                                    requestGeneration,
                                )
                        ) {
                            return@launch
                        }

                        result.onSuccess { places ->
                            placeSearchState =
                                VexMapsPlaceSearchState.Results(
                                    places,
                                )
                        }.onFailure {
                            placeSearchState =
                                VexMapsPlaceSearchState.Error(
                                    VexMapsPlaceSearchError.SEARCH,
                                )
                        }
                    }
                },
                onPlaceSelected = { place ->
                    when (mapSelectionTarget) {
                        VexMapsMapSelectionTarget.ORIGIN -> {
                            runtime.recordDynamicInteraction(
                                subjectRef =
                                    "presentation.vexmaps.place-search.result",
                                action =
                                    "SelectedAsRouteStart",
                            )
                            setSelectedOrigin(
                                place.coordinate,
                            )
                            setMapSelectionTarget(
                                nextVexMapsSelectionTarget(
                                    VexMapsMapSelectionTarget.ORIGIN,
                                ),
                            )
                            planningState =
                                VexMapsPlanningState.Idle
                            placeSearchState =
                                VexMapsPlaceSearchState.Idle
                            mapController
                                .showOrigin(
                                    place.coordinate,
                                )
                            mapController
                                .focusOn(
                                    place.coordinate,
                                )
                        }

                        VexMapsMapSelectionTarget.DESTINATION -> {
                            runtime.recordDynamicInteraction(
                                subjectRef =
                                    "presentation.vexmaps.place-search.result",
                                action =
                                    "SelectedAsDestination",
                            )
                            setDestination(
                                place.coordinate,
                            )
                            destinationLabel =
                                place.name
                            planningState =
                                VexMapsPlanningState.Idle
                            placeSearchState =
                                VexMapsPlaceSearchState.Idle
                            mapController
                                .showDestination(
                                    place.coordinate,
                                )
                            mapController
                                .focusOn(
                                    place.coordinate,
                                )
                        }
                    }
                },
                onChooseOriginOnMap = {
                    runtime.activate(
                        VexMapsScreen.ChooseOriginOnMap,
                    )
                    routeRequestGate.invalidate()
                    setMapSelectionTarget(
                        VexMapsMapSelectionTarget.ORIGIN,
                    )
                    planningState =
                        VexMapsPlanningState.Idle
                },
                onUseDeviceOrigin = {
                    runtime.activate(
                        VexMapsScreen.UseDeviceOrigin,
                    )
                    setSelectedOrigin(
                        null,
                    )
                    setMapSelectionTarget(
                        VexMapsMapSelectionTarget.DESTINATION,
                    )
                    planningState =
                        VexMapsPlanningState.Idle
                    mapController.clearOrigin()
                },
                onImportOfflineMap = {
                    runtime.activate(
                        VexMapsScreen.ImportOfflineMap,
                    )
                    offlineMapImportLauncher.launch(
                        arrayOf("*/*"),
                    )
                },
                onImportRegion = {
                    runtime.activate(
                        VexMapsScreen.ImportRoutingRegion,
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

                    runtime.activate(
                        VexMapsScreen.PlanRoute,
                    )
                    val requestGeneration =
                        routeRequestGate.begin()
                    planningState =
                        VexMapsPlanningState.Routing

                    scope.launch {
                        val result =
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
                                                    destinationLabel
                                                        ?: context.getString(
                                                            R.string
                                                                .vexmaps_route_destination_label,
                                                        ),
                                                travelMode =
                                                    travelMode,
                                            ),
                                    )
                            }

                        if (
                            !routeRequestGate
                                .accepts(
                                    requestGeneration,
                                )
                        ) {
                            return@launch
                        }

                        result.onSuccess { route ->
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
                    navigationLocationReady,
                canRecenter =
                    navigationVisualLocation !=
                        null,
                onRecenter = {
                    runtime.activate(
                        VexMapsScreen.RecenterNavigation,
                    )
                    navigationVisualLocation
                        ?.let { visual ->
                            mapController.focusOn(
                                visual.coordinate,
                            )
                        }
                },
                onEditRoute = {
                    runtime.activate(
                        VexMapsScreen.EditRoute,
                    )
                    VexMapsNavigationProcess
                        .stop()
                },
                onStart = {
                    runtime.activate(
                        VexMapsScreen.StartNavigation,
                    )
                    readCurrentPreciseLocation {
                        if (
                            VexMapsNavigationProcess
                                .session
                                .current
                                .phase ==
                            VexNavigationSessionPhase.READY
                        ) {
                            VexMapsNavigationService
                                .startPrepared(
                                    context,
                                )
                        }
                    }
                },
                onStop = {
                    runtime.activate(
                        VexMapsScreen.StopNavigation,
                    )
                    currentLocation = null
                    locationState =
                        VexMapsLocationState.Idle
                    VexMapsNavigationService
                        .stop(context)
                },
                onDone = {
                    runtime.activate(
                        VexMapsScreen.DoneNavigation,
                    )
                    currentLocation = null
                    locationState =
                        VexMapsLocationState.Idle
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
                    modifier =
                        Modifier.vexElement(
                            VexMapsScreen.UsePreciseLocation,
                        ),
                    onClick = {
                        runtime.activate(
                            VexMapsScreen.UsePreciseLocation,
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
    placeSearchPack: VexMapsPlaceSearchPack?,
    placeSearchQuery: String,
    placeSearchState: VexMapsPlaceSearchState,
    travelMode: VexMapsTravelMode,
    planningState: VexMapsPlanningState,
    onTravelModeSelected: (VexMapsTravelMode) -> Unit,
    onPlaceSearchQueryChange: (String) -> Unit,
    onImportPlaceSearch: () -> Unit,
    onSearchPlaces: () -> Unit,
    onPlaceSelected: (VexMapsPlaceSearchResult) -> Unit,
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
                Modifier
                    .heightIn(max = 560.dp)
                    .verticalScroll(
                        rememberScrollState(),
                    )
                    .padding(14.dp),
            verticalArrangement =
                Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text =
                    stringResource(
                        if (placeSearchPack == null) {
                            R.string
                                .vexmaps_place_search_none
                        } else {
                            R.string
                                .vexmaps_place_search_ready
                        },
                    ),
                style =
                    MaterialTheme
                        .typography
                        .titleMedium,
            )

            OutlinedButton(
                modifier =
                    Modifier.vexElement(
                        VexMapsScreen.ImportPlaceSearch,
                    ),
                onClick = onImportPlaceSearch,
                enabled =
                    placeSearchState !is
                        VexMapsPlaceSearchState.Importing &&
                        placeSearchState !is
                        VexMapsPlaceSearchState.Searching,
            ) {
                Text(
                    stringResource(
                        if (placeSearchPack == null) {
                            R.string
                                .vexmaps_place_search_import
                        } else {
                            R.string
                                .vexmaps_place_search_replace
                        },
                    ),
                )
            }

            if (placeSearchPack != null) {
                Text(
                    text =
                        stringResource(
                            if (selectingOrigin) {
                                R.string
                                    .vexmaps_place_search_selects_start
                            } else {
                                R.string
                                    .vexmaps_place_search_selects_destination
                            },
                        ),
                    style =
                        MaterialTheme
                            .typography
                            .bodySmall,
                )

                OutlinedTextField(
                    value = placeSearchQuery,
                    modifier =
                        Modifier.vexElement(
                            VexMapsScreen.PlaceSearchQuery,
                        ),
                    onValueChange =
                        onPlaceSearchQueryChange,
                    singleLine = true,
                    label = {
                        Text(
                            stringResource(
                                R.string
                                    .vexmaps_place_search_query,
                            ),
                        )
                    },
                )

                OutlinedButton(
                    modifier =
                        Modifier.vexElement(
                            VexMapsScreen.SearchPlaces,
                        ),
                    onClick = onSearchPlaces,
                    enabled =
                        placeSearchQuery
                            .isNotBlank() &&
                            placeSearchState !is
                            VexMapsPlaceSearchState.Importing &&
                            placeSearchState !is
                            VexMapsPlaceSearchState.Searching,
                ) {
                    Text(
                        stringResource(
                            R.string
                                .vexmaps_place_search_action,
                        ),
                    )
                }

                when (placeSearchState) {
                    VexMapsPlaceSearchState.Searching ->
                        Text(
                            stringResource(
                                R.string
                                    .vexmaps_place_searching,
                            ),
                        )

                    is VexMapsPlaceSearchState.Results ->
                        if (
                            placeSearchState
                                .places
                                .isEmpty()
                        ) {
                            Text(
                                stringResource(
                                    R.string
                                        .vexmaps_place_search_no_results,
                                ),
                            )
                        } else {
                            placeSearchState
                                .places
                                .forEach { place ->
                                    OutlinedButton(
                                        onClick = {
                                            onPlaceSelected(
                                                place,
                                            )
                                        },
                                    ) {
                                        Column {
                                            Text(
                                                place.name,
                                            )
                                            place.subtitle
                                                ?.takeIf {
                                                    it.isNotBlank()
                                                }
                                                ?.let { subtitle ->
                                                    Text(
                                                        text =
                                                            subtitle,
                                                        style =
                                                            MaterialTheme
                                                                .typography
                                                                .bodySmall,
                                                    )
                                                }
                                        }
                                    }
                                }
                        }

                    is VexMapsPlaceSearchState.Error ->
                        Text(
                            stringResource(
                                when (
                                    placeSearchState.error
                                ) {
                                    VexMapsPlaceSearchError.IMPORT ->
                                        R.string
                                            .vexmaps_place_search_import_error

                                    VexMapsPlaceSearchError.SEARCH ->
                                        R.string
                                            .vexmaps_place_search_error
                                },
                            ),
                        )

                    VexMapsPlaceSearchState.Importing ->
                        Text(
                            stringResource(
                                R.string
                                    .vexmaps_place_search_importing,
                            ),
                        )

                    VexMapsPlaceSearchState.Idle ->
                        Unit
                }
            }

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
                modifier =
                    Modifier.vexElement(
                        VexMapsScreen.ImportOfflineMap,
                    ),
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
                modifier =
                    Modifier.vexElement(
                        VexMapsScreen.ImportRoutingRegion,
                    ),
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
                            R.string
                                .vexmaps_route_mode,
                        ),
                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium,
                )

                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(6.dp),
                ) {
                    VexMapsTravelMode
                        .entries
                        .forEach { mode ->
                            if (mode == travelMode) {
                                Button(
                                    modifier =
                                        Modifier.vexElement(
                                            VexMapsScreen
                                                .travelModeElement(
                                                    mode,
                                                ),
                                        ),
                                    onClick = {
                                        onTravelModeSelected(
                                            mode,
                                        )
                                    },
                                ) {
                                    Text(
                                        stringResource(
                                            mode.labelRes,
                                        ),
                                    )
                                }
                            } else {
                                OutlinedButton(
                                    modifier =
                                        Modifier.vexElement(
                                            VexMapsScreen
                                                .travelModeElement(
                                                    mode,
                                                ),
                                        ),
                                    onClick = {
                                        onTravelModeSelected(
                                            mode,
                                        )
                                    },
                                ) {
                                    Text(
                                        stringResource(
                                            mode.labelRes,
                                        ),
                                    )
                                }
                            }
                        }
                }

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
                    modifier =
                        Modifier.vexElement(
                            VexMapsScreen.ChooseOriginOnMap,
                        ),
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
                        modifier =
                            Modifier.vexElement(
                                VexMapsScreen.UseDeviceOrigin,
                            ),
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
                    modifier =
                        Modifier.vexElement(
                            VexMapsScreen.PlanRoute,
                        ),
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
    canRecenter: Boolean,
    onRecenter: () -> Unit,
    onEditRoute: () -> Unit,
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

                    state
                        .routeResult
                        ?.estimatedDurationSeconds
                        ?.let { durationSeconds ->
                            val estimate =
                                projectVexMapsDurationEstimate(
                                    durationSeconds,
                                )

                            Text(
                                text =
                                    if (estimate.hours > 0) {
                                        stringResource(
                                            R.string
                                                .vexmaps_navigation_eta_hours,
                                            estimate.hours,
                                            estimate.minutes,
                                        )
                                    } else {
                                        stringResource(
                                            R.string
                                                .vexmaps_navigation_eta_minutes,
                                            estimate.minutes,
                                        )
                                    },
                                style =
                                    MaterialTheme
                                        .typography
                                        .bodyMedium,
                            )
                        }

                    state
                        .routeResult
                        ?.travelMode
                        ?.let { mode ->
                            Text(
                                text =
                                    stringResource(
                                        R.string
                                            .vexmaps_navigation_route_mode,
                                        stringResource(
                                            mode.labelRes,
                                        ),
                                    ),
                                style =
                                    MaterialTheme
                                        .typography
                                        .bodyMedium,
                            )
                        }

                    OutlinedButton(
                        modifier =
                            Modifier.vexElement(
                                VexMapsScreen.EditRoute,
                            ),
                        onClick = onEditRoute,
                    ) {
                        Text(
                            stringResource(
                                R.string
                                    .vexmaps_navigation_edit_route,
                            ),
                        )
                    }

                    if (locationReady) {
                        Button(
                            modifier =
                                Modifier.vexElement(
                                    VexMapsScreen.StartNavigation,
                                ),
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
                        modifier =
                            Modifier.vexElement(
                                VexMapsScreen.DoneNavigation,
                            ),
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

            if (
                canRecenter &&
                (
                    state.phase ==
                        VexNavigationSessionPhase.ACTIVE ||
                    state.phase ==
                        VexNavigationSessionPhase.OFF_ROUTE ||
                    state.phase ==
                        VexNavigationSessionPhase.LOCATION_HELD
                )
            ) {
                OutlinedButton(
                    modifier =
                        Modifier.vexElement(
                            VexMapsScreen.RecenterNavigation,
                        ),
                    onClick = onRecenter,
                ) {
                    Text(
                        stringResource(
                            R.string
                                .vexmaps_navigation_recenter,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun NavigationStopButton(
    onStop: () -> Unit,
) {
    OutlinedButton(
        modifier =
            Modifier.vexElement(
                VexMapsScreen.StopNavigation,
            ),
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

internal class VexMapsLatestRequestGate {
    private var generation = 0L

    fun begin(): Long {
        generation += 1L
        return generation
    }

    fun invalidate() {
        generation += 1L
    }

    fun accepts(
        requestGeneration: Long,
    ): Boolean =
        requestGeneration ==
            generation
}

internal sealed interface VexMapsPlaceSearchState {
    data object Idle :
        VexMapsPlaceSearchState

    data object Importing :
        VexMapsPlaceSearchState

    data object Searching :
        VexMapsPlaceSearchState

    data class Results(
        val places: List<VexMapsPlaceSearchResult>,
    ) : VexMapsPlaceSearchState

    data class Error(
        val error: VexMapsPlaceSearchError,
    ) : VexMapsPlaceSearchState
}

internal enum class VexMapsPlaceSearchError {
    IMPORT,
    SEARCH,
}

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

private fun VexMapsViewportSnapshot
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

private fun VexOfflineRouteResult
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

internal fun restoreVexMapCoordinate(
    latitude: Double?,
    longitude: Double?,
): VexMapCoordinate? {
    if (
        latitude == null ||
        longitude == null
    ) {
        return null
    }

    return runCatching {
        VexMapCoordinate(
            latitude = latitude,
            longitude = longitude,
        )
    }.getOrNull()
}

internal fun restoreVexMapsMapSelectionTarget(
    savedName: String,
): VexMapsMapSelectionTarget =
    VexMapsMapSelectionTarget
        .entries
        .firstOrNull { target ->
            target.name == savedName
        }
        ?: VexMapsMapSelectionTarget.DESTINATION

internal fun restoreVexMapsTravelMode(
    savedName: String,
): VexMapsTravelMode =
    VexMapsTravelMode
        .entries
        .firstOrNull { mode ->
            mode.name == savedName
        }
        ?: VexMapsTravelMode.DRIVE

internal fun projectVexMapsDeviceLocation(
    result: VexDeviceLocationResult,
): VexDeviceLocationResult.Located? =
    result as? VexDeviceLocationResult.Located

internal fun projectVexMapsMapDeviceLocation(
    plannerLocation:
        VexDeviceLocationResult.Located?,
    navigationVisualLocation:
        VexMapsNavigationVisualLocation?,
): VexDeviceLocationResult.Located? =
    navigationVisualLocation
        ?.let { visual ->
            VexDeviceLocationResult.Located(
                latitude =
                    visual.coordinate.latitude,
                longitude =
                    visual.coordinate.longitude,
                accuracyMeters =
                    visual.accuracyMeters,
            )
        }
        ?: plannerLocation

internal fun isVexMapsNavigationStartPreflightSatisfied(
    result: VexDeviceLocationResult,
    matchingPolicy:
        VexRouteMatchingPolicy =
        VexRouteMatchingPolicy(),
): Boolean =
    result is VexDeviceLocationResult.Located &&
        matchingPolicy.acceptsAccuracy(
            result.accuracyMeters,
        )

internal fun canStartVexMapsNavigation(
    precisePermissionGranted: Boolean,
    hasCurrentLocation: Boolean,
    locationFixReady: Boolean,
): Boolean =
    precisePermissionGranted &&
        hasCurrentLocation &&
        locationFixReady

internal data class VexMapsDurationEstimate(
    val hours: Int,
    val minutes: Int,
)

internal fun projectVexMapsDurationEstimate(
    durationSeconds: Int,
): VexMapsDurationEstimate {
    require(durationSeconds > 0)

    val totalMinutes =
        (
            durationSeconds +
                SECONDS_PER_MINUTE -
                1
        ) /
            SECONDS_PER_MINUTE

    return VexMapsDurationEstimate(
        hours =
            totalMinutes /
                MINUTES_PER_HOUR,
        minutes =
            totalMinutes %
                MINUTES_PER_HOUR,
    )
}

internal fun resolveVexMapsRouteOrigin(
    selectedOrigin: VexMapCoordinate?,
    deviceOrigin: VexMapCoordinate?,
): VexMapCoordinate? =
    selectedOrigin ?: deviceOrigin

private val VexMapsTravelMode.labelRes: Int
    get() =
        when (this) {
            VexMapsTravelMode.DRIVE ->
                R.string.vexmaps_route_mode_drive

            VexMapsTravelMode.WALK ->
                R.string.vexmaps_route_mode_walk

            VexMapsTravelMode.BICYCLE ->
                R.string.vexmaps_route_mode_bicycle
        }

internal enum class VexMapsMapSelectionTarget {
    ORIGIN,
    DESTINATION,
}

internal fun nextVexMapsSelectionTarget(
    selectedRole: VexMapsMapSelectionTarget,
): VexMapsMapSelectionTarget =
    when (selectedRole) {
        VexMapsMapSelectionTarget.ORIGIN ->
            VexMapsMapSelectionTarget.DESTINATION

        VexMapsMapSelectionTarget.DESTINATION ->
            VexMapsMapSelectionTarget.DESTINATION
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

    data object Inaccurate :
        VexMapsLocationState {
        override val stringResOrNull =
            R.string.vexmaps_location_inaccurate
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

private const val SECONDS_PER_MINUTE =
    60

private const val MINUTES_PER_HOUR =
    60

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
