package com.vextreme.vexlife.android.screens.shared.maps

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.vextreme.vexlife.android.R
import com.vextreme.vexlife.android.data.maps.VexMapsRepository
import com.vextreme.vexlife.android.data.maps.VexMapsStoredLocation
import com.vextreme.vexlife.android.elements.ElementActionHandler
import com.vextreme.vexlife.android.elements.VexActionFab
import com.vextreme.vexlife.android.elements.contextdock.VexContextDock
import com.vextreme.vexlife.android.elements.contextdock.VexContextDockPageRef
import com.vextreme.vexlife.android.elements.vexElement
import com.vextreme.vexlife.android.runtime.VexRuntime
import com.vextreme.vexlife.android.screens.shared.vexstream.rememberVexStreamContextDockPage
import java.time.Instant
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.maps.MapView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VexMapsMapSurface(
    runtime: VexRuntime,
    repository: VexMapsRepository,
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

    val mapAdapter =
        remember {
            VexMapLibreAdapter()
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
            MapLibre.getInstance(
                context.applicationContext,
            )
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

    val defaultOnlineStyle =
        VexMapsOnlineStyle.default(
            darkTheme,
        )
    val styleConvergenceRelay =
        remember {
            VexMapsStyleConvergenceRelay()
        }
    val styleConvergence by
        styleConvergenceRelay
            .outputStates
            .collectAsState(
                initial =
                    styleConvergenceRelay
                        .currentOutput,
            )
    var preferredDockPageRef by
        rememberSaveable {
            mutableStateOf(
                defaultOnlineStyle
                    .dockPageRef
                    .value,
            )
        }
    val routeRequestGate =
        remember {
            VexMapsLatestRequestGate()
        }
    val currentLocationRequestGate =
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
        if (
            !shouldVexMapsCurrentLocationAffectRoute(
                selectedOriginLatitude =
                    selectedOriginLatitude,
                selectedOriginLongitude =
                    selectedOriginLongitude,
            )
        ) {
            return
        }

        routeRequestGate.invalidate()
        planningState =
            planningState
                .afterVexMapsRouteInputChanged()
    }

    fun readCurrentPreciseLocation(
        onLocated: (() -> Unit)? = null,
    ) {
        val requestGeneration =
            currentLocationRequestGate.begin()
        locationState =
            VexMapsLocationState.Finding

        scope.launch {
            val result =
                locationSource
                    .currentPreciseLocation()

            if (
                !currentLocationRequestGate
                    .accepts(
                        requestGeneration,
                    )
            ) {
                return@launch
            }

            val projectedLocation =
                projectVexMapsDeviceLocation(
                    result,
                )

            invalidateDeviceRouteOriginIfUnpinned()
            currentLocation =
                projectedLocation

            if (
                result is
                    VexDeviceLocationResult.Located
            ) {
                scope.launch {
                    runCatching {
                        repository.saveLastLocation(
                            VexMapsStoredLocation(
                                latitude =
                                    result.latitude,
                                longitude =
                                    result.longitude,
                                accuracyMeters =
                                    result.accuracyMeters,
                                capturedAt =
                                    Instant.now(),
                            ),
                        )
                    }
                }
            }

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

    LaunchedEffect(repository) {
        val storedState =
            runCatching {
                repository.load()
            }.getOrNull()

        val restoredStyle =
            storedState
                ?.onlineStyleRef
                ?.let { styleRef ->
                    VexMapsOnlineStyle
                        .fromStyleRef(
                            styleRef,
                        )
                }
                ?: defaultOnlineStyle

        styleConvergenceRelay
            .styleSelected(
                styleRef =
                    restoredStyle.styleRef,
                styleUri =
                    restoredStyle.styleUri,
            )
        preferredDockPageRef =
            restoredStyle
                .dockPageRef
                .value

        val precisePermissionGranted =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) ==
                PackageManager.PERMISSION_GRANTED

        if (precisePermissionGranted) {
            storedState
                ?.lastLocation
                ?.let { storedLocation ->
                    currentLocation =
                        VexDeviceLocationResult.Located(
                            latitude =
                                storedLocation.latitude,
                            longitude =
                                storedLocation.longitude,
                            accuracyMeters =
                                storedLocation.accuracyMeters,
                        )
                    locationState =
                        VexMapsLocationState.Finding
                }

            readCurrentPreciseLocation()
        }
    }

    var offlineMapState by
        remember {
            mutableStateOf<VexMapsOfflineMapState>(
                VexMapsOfflineMapState.Loading(),
            )
        }

    LaunchedEffect(
        offlineMapStore,
    ) {
        offlineMapState =
            runCatching {
                offlineMapStore.load()
            }.getOrElse {
                VexMapsOfflineMapState.Error()
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

            val retainedReady =
                offlineMapState
                    .effectiveReady()
            offlineMapState =
                VexMapsOfflineMapState.Loading(
                    retainedReady =
                        retainedReady,
                )

            scope.launch {
                offlineMapState =
                    runCatching {
                        offlineMapStore.import(
                            uri,
                        )
                    }.getOrElse {
                        VexMapsOfflineMapState.Error(
                            retainedReady =
                                retainedReady,
                        )
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
        mapAdapter
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
                                    planningState
                                        .afterVexMapsRouteInputChanged()
                                mapAdapter
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
                                    planningState
                                        .afterVexMapsRouteInputChanged()
                                mapAdapter
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
        mapAdapter.setOnViewportChanged { snapshot ->
            setViewportSnapshot(
                snapshot = snapshot,
                routeKey = currentRouteKey,
            )
        }
    }

    /*
     * The Style Convergence Relay owns the relationship between the human's selected style and the
     * style MapLibre has actually confirmed loaded. Imported offline regions remain cached capability,
     * not a hidden visual-preference override.
     */
    val styleSelection =
        styleConvergence.selection

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
            mapAdapter,
        ) {
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
                }
            }
        }

    LaunchedEffect(
        mapView,
        styleConvergence.availability,
        styleSelection?.revision,
    ) {
        val selection =
            styleSelection
                ?: return@LaunchedEffect

        if (
            styleConvergence.availability !=
            VexMapsStyleConvergenceAvailability
                .NEEDS_APPLY
        ) {
            return@LaunchedEffect
        }

        styleConvergenceRelay
            .applyStarted(
                selection,
            )

        mapView.getMapAsync { map ->
            map.setStyle(
                selection.styleUri,
            ) { style ->
                mapAdapter.attach(
                    map = map,
                    style = style,
                    restoredRouteKey =
                        restoredRouteKey,
                )
                styleConvergenceRelay
                    .styleLoaded(
                        selection,
                    )
            }
        }
    }

    LaunchedEffect(
        styleConvergence.availability,
        styleSelection?.revision,
    ) {
        val selection =
            styleSelection
                ?: return@LaunchedEffect

        if (
            styleConvergence.availability ==
            VexMapsStyleConvergenceAvailability
                .APPLIED
        ) {
            runCatching {
                repository.saveOnlineStyle(
                    selection.styleRef,
                )
            }
        }
    }

    LaunchedEffect(
        selectedOrigin,
        mapAdapter,
    ) {
        if (selectedOrigin == null) {
            mapAdapter.clearOrigin()
        } else {
            mapAdapter.showOrigin(
                selectedOrigin,
            )
        }
    }

    LaunchedEffect(
        destination,
        mapAdapter,
    ) {
        if (destination == null) {
            mapAdapter.clearDestination()
        } else {
            mapAdapter.showDestination(
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
        mapAdapter,
    ) {
        if (mapDeviceLocation == null) {
            mapAdapter.clearDeviceLocation()
        } else {
            mapAdapter.showDeviceLocation(
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
        mapAdapter,
    ) {
        val routeResult =
            navigationState.routeResult

        if (routeResult == null) {
            mapAdapter.clearRoute()
        } else {
            mapAdapter.showRoute(
                routeResult,
            )
        }
    }

    DisposableEffect(
        lifecycle,
        mapView,
        mapAdapter,
        styleConvergenceRelay,
    ) {
        val lifecycleBridge =
            VexMapLibreLifecycle(
                mapView = mapView,
            )
        val styleFailureListener =
            MapView.OnDidFailLoadingMapListener {
                    errorMessage ->
                styleConvergenceRelay
                    .currentApplyFailed(
                        errorMessage,
                    )
            }

        lifecycleBridge.synchronize(
            lifecycle.currentState,
        )
        mapView.addOnDidFailLoadingMapListener(
            styleFailureListener,
        )

        val observer =
            LifecycleEventObserver { _, event ->
                lifecycleBridge.onEvent(event)
            }

        lifecycle.addObserver(observer)

        onDispose {
            lifecycle.removeObserver(observer)
            mapView.removeOnDidFailLoadingMapListener(
                styleFailureListener,
            )
            mapAdapter.detach()
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

    val vexStreamDockPage =
        rememberVexStreamContextDockPage(
            runtime = runtime,
        )
    val contextDockPages =
        buildList {
            vexStreamDockPage?.let(::add)
            VexMapsOnlineStyle
                .entries
                .map { style ->
                    style.contextDockPage(
                        availability =
                            styleConvergence
                                .availability
                                .takeIf {
                                    styleConvergence
                                        .selection
                                        ?.styleRef ==
                                        style.styleRef
                                },
                    )
                }
                .forEach(::add)
        }
    val preferredContextDockPage =
        remember(preferredDockPageRef) {
            VexContextDockPageRef(
                preferredDockPageRef,
            )
        }
    val offlineSheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = false,
        )
    val locationActionHandler =
        ElementActionHandler { element ->
            runtime.activate(element)

            if (precisePermissionGranted) {
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
        }

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
        )

        Box(
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(
                        end = 8.dp,
                    ),
        ) {
            DropdownMenu(
                expanded =
                    runtime.presentation.isOpen(
                        VexMapsScreen.MapMenu,
                    ),
                onDismissRequest = {
                    runtime.dismissPresentation(
                        VexMapsScreen.MapMenu,
                    )
                },
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(
                                R.string
                                    .vexmaps_offline_maps,
                            ),
                        )
                    },
                    onClick = {
                        runtime.activate(
                            VexMapsScreen
                                .OpenOfflineMaps,
                        )
                    },
                )
            }
        }

        if (
            runtime.presentation.isOpen(
                VexMapsScreen.OfflinePanel,
            )
        ) {
            ModalBottomSheet(
                onDismissRequest = {
                    runtime.dismissPresentation(
                        VexMapsScreen.OfflinePanel,
                    )
                },
                sheetState = offlineSheetState,
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
                onToggleOfflinePreparation = {
                    runtime.activate(
                        VexMapsScreen.ToggleOfflinePreparation,
                    )
                    runtime.presentation.close(
                        VexMapsScreen.OfflinePanel,
                    )
                },
                onPrepareOfflineAroundMe = {
                    runtime.activate(
                        VexMapsScreen.PrepareOfflineAroundMe,
                    )
                },
                onPrepareOfflineTrip = {
                    runtime.activate(
                        VexMapsScreen.PrepareOfflineTrip,
                    )
                },
                onBrowseOfflineRegions = {
                    runtime.activate(
                        VexMapsScreen.BrowseOfflineRegions,
                    )
                },
                onToggleAdvancedOfflineSetup = {
                    runtime.activate(
                        VexMapsScreen.ToggleAdvancedOfflineSetup,
                    )
                },
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
                        planningState
                            .afterVexMapsRouteInputChanged()
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
                                planningState
                                    .afterVexMapsRouteInputChanged()
                            placeSearchState =
                                VexMapsPlaceSearchState.Idle
                            mapAdapter
                                .showOrigin(
                                    place.coordinate,
                                )
                            mapAdapter
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
                                planningState
                                    .afterVexMapsRouteInputChanged()
                            placeSearchState =
                                VexMapsPlaceSearchState.Idle
                            mapAdapter
                                .showDestination(
                                    place.coordinate,
                                )
                            mapAdapter
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
                        planningState
                            .afterVexMapsRouteInputChanged()
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
                        planningState
                            .afterVexMapsRouteInputChanged()
                    mapAdapter.clearOrigin()
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
                        .fillMaxWidth()
                        .padding(
                            horizontal = 12.dp,
                            vertical = 8.dp,
                        ),
                )
            }
        }

        if (
            navigationState.phase !=
            VexNavigationSessionPhase.IDLE
        ) {
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
                            mapAdapter.focusOn(
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
                    val expectedRouteKey =
                        navigationState
                            .routeResult
                            ?.vexMapsRouteKey()
                            ?: return@VexMapsNavigationCard

                    readCurrentPreciseLocation {
                        val currentState =
                            VexMapsNavigationProcess
                                .session
                                .current
                        val currentRouteKey =
                            currentState
                                .routeResult
                                ?.vexMapsRouteKey()

                        if (
                            canContinueVexMapsNavigationStart(
                                expectedRouteKey =
                                    expectedRouteKey,
                                currentPhase =
                                    currentState.phase,
                                currentRouteKey =
                                    currentRouteKey,
                            )
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
                    VexMapsNavigationService
                        .dismissArrivalNotification(
                            context,
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
            VexActionFab(
                element =
                    VexMapsScreen
                        .UsePreciseLocation,
                handler =
                    locationActionHandler,
                enabled =
                    locationState !=
                        VexMapsLocationState.Finding,
                modifier =
                    Modifier
                        .align(
                            Alignment.BottomEnd,
                        )
                        .padding(
                            end = 14.dp,
                            bottom = 156.dp,
                        ),
            )

            locationState
                .stringResOrNull
                ?.takeIf {
                    locationState !=
                        VexMapsLocationState.Ready
                }
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
                                .align(
                                    Alignment.BottomEnd,
                                )
                                .padding(
                                    end = 12.dp,
                                    bottom = 214.dp,
                                )
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

        VexContextDock(
            pages = contextDockPages,
            preferredPageRef =
                preferredContextDockPage,
            onSelectedPageChanged = {
                    selectedPageRef ->
                preferredDockPageRef =
                    selectedPageRef.value

                VexMapsOnlineStyle
                    .fromDockPageRef(
                        selectedPageRef,
                    )
                    ?.let { selectedStyle ->
                        if (
                            styleConvergence
                                .selection
                                ?.styleRef !=
                            selectedStyle.styleRef
                        ) {
                            runtime.recordDynamicInteraction(
                                subjectRef =
                                    "presentation.context-dock.vexmaps.style",
                                action = "Selected",
                            )
                            styleConvergenceRelay
                                .styleSelected(
                                    styleRef =
                                        selectedStyle.styleRef,
                                    styleUri =
                                        selectedStyle.styleUri,
                                )
                        }
                    }
            },
            modifier =
                Modifier
                    .align(
                        Alignment.BottomCenter,
                    )
                    .padding(
                        horizontal = 10.dp,
                    )
                    .padding(
                        bottom = 34.dp,
                    ),
        )

        Text(
            text = stringResource(
                R.string.vexmaps_attribution,
            ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = 8.dp,
                        bottom = 8.dp,
                    )
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
    onToggleOfflinePreparation: () -> Unit,
    onPrepareOfflineAroundMe: () -> Unit,
    onPrepareOfflineTrip: () -> Unit,
    onBrowseOfflineRegions: () -> Unit,
    onToggleAdvancedOfflineSetup: () -> Unit,
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
    var offlinePreparationVisible by
        rememberSaveable {
            mutableStateOf(true)
        }
    var preparationModeName by
        rememberSaveable {
            mutableStateOf<String?>(null)
        }
    var tripDestinationDraft by
        rememberSaveable {
            mutableStateOf("")
        }
    var regionDraft by
        rememberSaveable {
            mutableStateOf("")
        }
    var advancedOfflineVisible by
        rememberSaveable {
            mutableStateOf(false)
        }

    Card(
        modifier = modifier,
    ) {
        Column(
            modifier =
                Modifier
                    .heightIn(max = 500.dp)
                    .verticalScroll(
                        rememberScrollState(),
                    )
                    .padding(14.dp),
            verticalArrangement =
                Arrangement.spacedBy(10.dp),
        ) {
            if (!offlinePreparationVisible) {
                Text(
                    text =
                        stringResource(
                            R.string.vexmaps_online_title,
                        ),
                    style =
                        MaterialTheme
                            .typography
                            .titleMedium,
                )
                Text(
                    text =
                        stringResource(
                            R.string.vexmaps_online_body,
                        ),
                    style =
                        MaterialTheme
                            .typography
                            .bodySmall,
                )
                Button(
                    modifier =
                        Modifier.vexElement(
                            VexMapsScreen.ToggleOfflinePreparation,
                        ),
                    onClick = {
                        onToggleOfflinePreparation()
                        offlinePreparationVisible = true
                    },
                ) {
                    Text(
                        stringResource(
                            R.string.vexmaps_offline_maps,
                        ),
                    )
                }
            } else {
                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp),
                    verticalAlignment =
                        Alignment.CenterVertically,
                ) {
                    Text(
                        text =
                            stringResource(
                                R.string.vexmaps_offline_prepare_title,
                            ),
                        style =
                            MaterialTheme
                                .typography
                                .titleMedium,
                        modifier =
                            Modifier.weight(1f),
                    )
                    OutlinedButton(
                        modifier =
                            Modifier.vexElement(
                                VexMapsScreen.ToggleOfflinePreparation,
                            ),
                        onClick = {
                            onToggleOfflinePreparation()
                            offlinePreparationVisible = false
                            preparationModeName = null
                            advancedOfflineVisible = false
                        },
                    ) {
                        Text(
                            stringResource(
                                R.string.vexmaps_offline_maps_close,
                            ),
                        )
                    }
                }

                Text(
                    text =
                        stringResource(
                            R.string.vexmaps_offline_prepare_body,
                        ),
                    style =
                        MaterialTheme
                            .typography
                            .bodySmall,
                )

                OutlinedButton(
                    modifier =
                        Modifier.vexElement(
                            VexMapsScreen.PrepareOfflineAroundMe,
                        ),
                    onClick = {
                        onPrepareOfflineAroundMe()
                        preparationModeName = "AROUND_ME"
                    },
                ) {
                    Text(
                        stringResource(
                            R.string.vexmaps_offline_prepare_around,
                        ),
                    )
                }

                OutlinedButton(
                    modifier =
                        Modifier.vexElement(
                            VexMapsScreen.PrepareOfflineTrip,
                        ),
                    onClick = {
                        onPrepareOfflineTrip()
                        preparationModeName = "TRIP"
                    },
                ) {
                    Text(
                        stringResource(
                            R.string.vexmaps_offline_prepare_trip,
                        ),
                    )
                }

                OutlinedButton(
                    modifier =
                        Modifier.vexElement(
                            VexMapsScreen.BrowseOfflineRegions,
                        ),
                    onClick = {
                        onBrowseOfflineRegions()
                        preparationModeName = "REGION"
                    },
                ) {
                    Text(
                        stringResource(
                            R.string.vexmaps_offline_prepare_regions,
                        ),
                    )
                }

                when (preparationModeName) {
                    "AROUND_ME" -> {
                        Text(
                            text =
                                stringResource(
                                    if (routeOrigin != null) {
                                        R.string.vexmaps_offline_prepare_around_ready
                                    } else {
                                        R.string.vexmaps_offline_prepare_around_location_needed
                                    },
                                ),
                            style =
                                MaterialTheme
                                    .typography
                                    .bodyMedium,
                        )
                    }

                    "TRIP" -> {
                        Text(
                            text =
                                stringResource(
                                    if (routeOrigin != null) {
                                        R.string.vexmaps_offline_prepare_trip_start_current
                                    } else {
                                        R.string.vexmaps_offline_prepare_trip_start_needed
                                    },
                                ),
                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall,
                        )
                        OutlinedTextField(
                            value = tripDestinationDraft,
                            onValueChange = {
                                tripDestinationDraft = it
                            },
                            singleLine = true,
                            label = {
                                Text(
                                    stringResource(
                                        R.string.vexmaps_offline_prepare_trip_destination,
                                    ),
                                )
                            },
                        )
                        Text(
                            text =
                                stringResource(
                                    R.string.vexmaps_offline_prepare_trip_body,
                                ),
                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall,
                        )
                    }

                    "REGION" -> {
                        OutlinedTextField(
                            value = regionDraft,
                            onValueChange = {
                                regionDraft = it
                            },
                            singleLine = true,
                            label = {
                                Text(
                                    stringResource(
                                        R.string.vexmaps_offline_prepare_region_name,
                                    ),
                                )
                            },
                        )
                        Text(
                            text =
                                stringResource(
                                    R.string.vexmaps_offline_prepare_region_body,
                                ),
                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall,
                        )
                        Text(
                            text =
                                stringResource(
                                    R.string.vexmaps_offline_prepare_region_catalog_missing,
                                ),
                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall,
                        )
                    }
                }

                if (preparationModeName != null) {
                    Text(
                        text =
                            stringResource(
                                R.string.vexmaps_offline_prepare_review_title,
                            ),
                        style =
                            MaterialTheme
                                .typography
                                .titleSmall,
                    )
                    Text(
                        text =
                            stringResource(
                                R.string.vexmaps_offline_prepare_review_components,
                            ),
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall,
                    )
                    Text(
                        text =
                            stringResource(
                                R.string.vexmaps_offline_prepare_review_size,
                            ),
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall,
                    )
                    Text(
                        text =
                            stringResource(
                                R.string.vexmaps_offline_prepare_source_missing,
                            ),
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall,
                    )
                }

                OutlinedButton(
                    modifier =
                        Modifier.vexElement(
                            VexMapsScreen.ToggleAdvancedOfflineSetup,
                        ),
                    onClick = {
                        onToggleAdvancedOfflineSetup()
                        advancedOfflineVisible =
                            !advancedOfflineVisible
                    },
                ) {
                    Text(
                        stringResource(
                            if (advancedOfflineVisible) {
                                R.string.vexmaps_offline_prepare_hide_advanced
                            } else {
                                R.string.vexmaps_offline_prepare_advanced
                            },
                        ),
                    )
                }

                if (advancedOfflineVisible) {
                    Text(
                        text =
                            stringResource(
                                if (placeSearchPack == null) {
                                    R.string.vexmaps_place_search_none
                                } else {
                                    R.string.vexmaps_place_search_ready
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
                                VexMapsScreen.ImportPlaceSearch,
                            ),
                        onClick = onImportPlaceSearch,
                        enabled =
                            placeSearchState !is VexMapsPlaceSearchState.Importing &&
                                placeSearchState !is VexMapsPlaceSearchState.Searching,
                    ) {
                        Text(
                            stringResource(
                                if (placeSearchPack == null) {
                                    R.string.vexmaps_place_search_import
                                } else {
                                    R.string.vexmaps_place_search_replace
                                },
                            ),
                        )
                    }

                    Text(
                        text =
                            when (offlineMapState) {
                                is VexMapsOfflineMapState.Loading ->
                                    stringResource(
                                        R.string.vexmaps_offline_map_loading,
                                    )

                                VexMapsOfflineMapState.None ->
                                    stringResource(
                                        R.string.vexmaps_offline_map_none,
                                    )

                                is VexMapsOfflineMapState.Ready ->
                                    stringResource(
                                        R.string.vexmaps_offline_map_ready,
                                        offlineMapState.completeRegionCount,
                                    )

                                is VexMapsOfflineMapState.Error ->
                                    stringResource(
                                        R.string.vexmaps_offline_map_error,
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
                            offlineMapState !is VexMapsOfflineMapState.Loading,
                    ) {
                        Text(
                            stringResource(
                                R.string.vexmaps_offline_map_import,
                            ),
                        )
                    }

                    Text(
                        text =
                            stringResource(
                                if (regionPack == null) {
                                    R.string.vexmaps_region_pack_needed
                                } else {
                                    R.string.vexmaps_region_pack_ready
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
                                VexMapsScreen.ImportRoutingRegion,
                            ),
                        onClick = onImportRegion,
                        enabled =
                            planningState !is VexMapsPlanningState.Importing &&
                                planningState !is VexMapsPlanningState.Routing,
                    ) {
                        Text(
                            stringResource(
                                if (regionPack == null) {
                                    R.string.vexmaps_region_pack_import
                                } else {
                                    R.string.vexmaps_region_pack_replace
                                },
                            ),
                        )
                    }
                }
            }

            if (placeSearchPack != null) {
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
                                R.string.vexmaps_place_search_query,
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
                        placeSearchQuery.isNotBlank() &&
                            placeSearchState !is VexMapsPlaceSearchState.Importing &&
                            placeSearchState !is VexMapsPlaceSearchState.Searching,
                ) {
                    Text(
                        stringResource(
                            R.string.vexmaps_place_search_action,
                        ),
                    )
                }

                when (placeSearchState) {
                    VexMapsPlaceSearchState.Searching ->
                        Text(
                            stringResource(
                                R.string.vexmaps_place_searching,
                            ),
                        )

                    is VexMapsPlaceSearchState.Results ->
                        placeSearchState.places.forEach { place ->
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
                                                text = subtitle,
                                                style =
                                                    MaterialTheme
                                                        .typography
                                                        .bodySmall,
                                            )
                                        }
                                }
                            }
                        }

                    is VexMapsPlaceSearchState.Error ->
                        Text(
                            stringResource(
                                R.string.vexmaps_place_search_error,
                            ),
                        )

                    VexMapsPlaceSearchState.Importing,
                    VexMapsPlaceSearchState.Idle ->
                        Unit
                }
            }

            if (regionPack != null) {
                Text(
                    text =
                        stringResource(
                            R.string.vexmaps_route_mode,
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
                    VexMapsTravelMode.entries.forEach { mode ->
                        if (mode == travelMode) {
                            Button(
                                modifier =
                                    Modifier.vexElement(
                                        VexMapsScreen.travelModeElement(
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
                                        VexMapsScreen.travelModeElement(
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
                                    R.string.vexmaps_route_start_selecting

                                usingPinnedOrigin ->
                                    R.string.vexmaps_route_start_pinned

                                routeOrigin != null ->
                                    R.string.vexmaps_route_start_current

                                else ->
                                    R.string.vexmaps_route_start_needed
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
                        planningState !is VexMapsPlanningState.Routing &&
                            planningState !is VexMapsPlanningState.Importing,
                ) {
                    Text(
                        stringResource(
                            if (usingPinnedOrigin) {
                                R.string.vexmaps_route_change_start
                            } else {
                                R.string.vexmaps_route_choose_start
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
                    ) {
                        Text(
                            stringResource(
                                R.string.vexmaps_route_use_current_start,
                            ),
                        )
                    }
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
                        planningState !is VexMapsPlanningState.Routing &&
                            planningState !is VexMapsPlanningState.Importing,
                ) {
                    Text(
                        stringResource(
                            R.string.vexmaps_route_here,
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

internal fun shouldVexMapsCurrentLocationAffectRoute(
    selectedOriginLatitude: Double?,
    selectedOriginLongitude: Double?,
): Boolean =
    restoreVexMapCoordinate(
        latitude =
            selectedOriginLatitude,
        longitude =
            selectedOriginLongitude,
    ) == null

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

internal fun canContinueVexMapsNavigationStart(
    expectedRouteKey: String,
    currentPhase: VexNavigationSessionPhase,
    currentRouteKey: String?,
): Boolean =
    currentPhase ==
        VexNavigationSessionPhase.READY &&
        currentRouteKey ==
        expectedRouteKey

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

internal sealed interface VexMapsPlanningState {
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

internal enum class VexMapsPlanningError {
    REGION_IMPORT,
    ROUTE,
}

internal fun VexMapsPlanningState
    .afterVexMapsRouteInputChanged():
    VexMapsPlanningState =
    when (this) {
        VexMapsPlanningState.Routing ->
            VexMapsPlanningState.Idle

        is VexMapsPlanningState.Error ->
            if (
                error ==
                VexMapsPlanningError.ROUTE
            ) {
                VexMapsPlanningState.Idle
            } else {
                this
            }

        VexMapsPlanningState.Idle,
        VexMapsPlanningState.Importing ->
            this
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

private const val SECONDS_PER_MINUTE =
    60

private const val MINUTES_PER_HOUR =
    60

// [VXG RealForever]
