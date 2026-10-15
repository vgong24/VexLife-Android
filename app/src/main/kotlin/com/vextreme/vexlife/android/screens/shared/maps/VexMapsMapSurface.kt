package com.vextreme.vexlife.android.screens.shared.maps

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import com.vextreme.vexlife.android.data.maps.distribution.VexMapsDistributionRepository
import com.vextreme.vexlife.android.elements.ElementActionHandler
import com.vextreme.vexlife.android.elements.VexActionFab
import com.vextreme.vexlife.android.elements.contextdock.VexContextDock
import com.vextreme.vexlife.android.elements.contextdock.VexContextDockPageRef
import com.vextreme.vexlife.android.runtime.VexRuntime
import com.vextreme.vexlife.android.screens.shared.maps.distribution.VEXMAPS_DISTRIBUTION_CONTEXT_DOCK_PAGE_REF
import com.vextreme.vexlife.android.screens.shared.maps.distribution.VexMapsDistributionExperience
import com.vextreme.vexlife.android.screens.shared.maps.distribution.vexMapsDistributionContextDockPage
import com.vextreme.vexlife.android.screens.shared.maps.distribution.convergence.VexMapsDistributionNavigationLayer
import com.vextreme.vexlife.android.screens.shared.maps.distribution.mode.VexMapsDistributionModeState
import com.vextreme.vexlife.android.screens.shared.maps.distribution.run.VexMapsDistributionRunState
import com.vextreme.vexlife.android.screens.shared.maps.distribution.session.VexMapsDistributionProcess
import com.vextreme.vexlife.android.screens.shared.vexstream.rememberVexStreamContextDockPage
import java.time.Instant
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.maps.MapView

/**
 * Native VexMaps composition root.
 *
 * Read this file as wiring between already-owned questions, not as one giant Maps controller:
 *
 * - VexMapsNavigationProcess / VexMapsStyleConvergenceRelay own changing semantic relationships;
 * - VexMapsRepository owns durable Maps-local checkpoints/preferences;
 * - VexDeviceLocationSource / stores own Android or provider observations/effects;
 * - VexMapLibreAdapter owns translation from Maps rendering intent into MapLibre mechanics;
 * - planner/navigation/location files own their smaller state or presentation questions;
 * - this function decides when those owners are created, which current facts are handed between
 *   them, and how their projections are composed on screen.
 *
 * If a future change starts answering "what does this fact mean?" rather than "when/how do these
 * existing owners meet?", it probably belongs in one of those narrower owners instead of growing
 * this composition surface again.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VexMapsMapSurface(
    runtime: VexRuntime,
    repository: VexMapsRepository,
    distributionRepository:
        VexMapsDistributionRepository,
    darkTheme: Boolean,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val scope = rememberCoroutineScope()
    val distributionSession =
        remember(
            context.applicationContext,
            distributionRepository,
        ) {
            VexMapsDistributionProcess
                .session(
                    context =
                        context,
                    repository =
                        distributionRepository,
                )
        }
    val distributionExperience =
        remember(
            distributionSession,
        ) {
            VexMapsDistributionExperience(
                distributionSession,
            )
        }
    val distributionSessionState by
        distributionExperience
            .states
            .collectAsState()
    val navigationState by
        VexMapsNavigationProcess
            .session
            .states
            .collectAsState()
    val navigationVisualLocation by
        VexMapsNavigationProcess
            .visualLocations
            .collectAsState()
    val distributionModeProjection by
        distributionExperience
            .modeProjections
            .collectAsState()
    val distributionRunProjection by
        distributionExperience
            .runProjections
            .collectAsState()
    val distributionNavigation by
        distributionExperience
            .navigationOutputs
            .collectAsState(
                initial =
                    distributionExperience
                        .currentNavigation,
            )

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

    /*
     * Keep three different notions separate:
     * - locationState: human-facing readiness/failure projection;
     * - currentLocation: fresh Android observation allowed to affect routing/navigation;
     * - restoredMapContext: historical repository checkpoint allowed only to seed browsing context.
     *
     * Their coordinate shapes may look similar, but their authority/currentness is intentionally
     * different. Do not collapse restoredMapContext back into currentLocation.
     */
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

    var restoredMapContext by
        remember {
            mutableStateOf<
                VexMapsRestoredMapContext?
            >(null)
        }

    /*
     * Style selection is semantic relationship state; MapLibre loading is a provider effect.
     * The Relay reconciles selected/requested/loaded/failed meaning while VexMapLibreAdapter performs
     * the actual SDK work below. Keeping those two halves separate prevents provider callbacks from
     * becoming the source of human intent.
     */
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
    /*
     * These gates are not domain state. They are composition-level admission tokens that stop a
     * slower async request from overwriting a newer human/location decision after the coroutine
     * eventually returns.
     */
    val routeRequestGate =
        remember {
            VexMapsLatestRequestGate()
        }
    val currentLocationRequestGate =
        remember {
            VexMapsLatestRequestGate()
        }

    /*
     * Viewport retention belongs to this rendered Screen lifetime, not to durable location truth.
     * Saving the camera components across recomposition/configuration lets MapLibre be recreated
     * without pretending a camera center is a fresh device fix or a persisted route origin.
     */
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
        distributionExperience
            .humanPlanningInputChanged()
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
        distributionExperience
            .humanPlanningInputChanged()
        travelModeName =
            mode.name
    }

    /*
     * Live device fixes are current Navigation/provider observations. They invalidate ordinary
     * one-off route-request currentness when the device is the route origin, but they do not replace
     * a Ready Distribution Plan's sealed preparation origin. Only explicit human planning changes
     * call humanPlanningInputChanged().
     */
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

    /*
     * Repository restoration is intentionally one-way into bounded startup context:
     * - persisted style becomes current human preference;
     * - persisted location becomes restored browse context only;
     * - a fresh precise Android observation is requested separately when permission allows.
     *
     * This is the currentness boundary that prevents process history from silently satisfying
     * navigation readiness.
     */
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

        restoredMapContext =
            projectVexMapsStoredMapContext(
                storedLocation =
                    storedState?.lastLocation,
                precisePermissionGranted =
                    precisePermissionGranted,
            )

        if (precisePermissionGranted) {
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

            if (
                !distributionExperience
                    .routingRegionReplacementRequested()
            ) {
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
        styleConvergence.status,
        styleSelection?.revision,
    ) {
        val selection =
            styleSelection
                ?: return@LaunchedEffect

        if (
            styleConvergence.status !=
            VexMapsStyleConvergenceStatus
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
        styleConvergence.status,
        styleSelection?.revision,
    ) {
        val selection =
            styleSelection
                ?: return@LaunchedEffect

        if (
            styleConvergence.status ==
            VexMapsStyleConvergenceStatus
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
        restoredMapContext,
        currentLocation,
        styleConvergence.status,
        mapAdapter,
    ) {
        if (
            styleConvergence.status ==
                VexMapsStyleConvergenceStatus.APPLIED &&
            currentLocation == null
        ) {
            restoredMapContext
                ?.center
                ?.let(mapAdapter::focusOn)
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
    val distributionDockPage =
        vexMapsDistributionContextDockPage(
            experience =
                distributionExperience,
            canPrepare =
                !distributionSessionState
                    .draftOperationInFlight &&
                    regionPack != null &&
                    routeOrigin != null &&
                    distributionSessionState
                        .draft
                        .stops
                        .isNotEmpty(),
            onToggleMode = {
                runtime.activate(
                    VexMapsScreen
                        .ToggleDistributionMode,
                )
                distributionExperience
                    .toggleMode()
            },
            onAddStop = {
                runtime.activate(
                    VexMapsScreen
                        .AddDistributionStop,
                )
                runtime.presentation.open(
                    VexMapsScreen
                        .OfflinePanel,
                )
            },
            onRemoveStop = { stopRef ->
                runtime.recordDynamicInteraction(
                    subjectRef =
                        "presentation.vexmaps.distribution.stop." +
                            stopRef,
                    action =
                        "Removed",
                )
                distributionExperience
                    .removeStop(
                        stopRef,
                    )
            },
            onToggleReturnToStart = {
                runtime.activate(
                    VexMapsScreen
                        .ToggleDistributionReturnToStart,
                )
                distributionExperience
                    .setReturnToStart(
                        !distributionSessionState
                            .draft
                            .returnToStart,
                    )
            },
            onPrepare = {
                val pack =
                    regionPack
                        ?: return@vexMapsDistributionContextDockPage
                val origin =
                    routeOrigin
                        ?: return@vexMapsDistributionContextDockPage

                runtime.activate(
                    VexMapsScreen
                        .PrepareDistributionRun,
                )
                distributionExperience
                    .prepare(
                        origin =
                            origin,
                        travelMode =
                            travelMode,
                        pack =
                            pack,
                    )
            },
            onStart = {
                runtime.activate(
                    VexMapsScreen
                        .StartDistributionRun,
                )
                readCurrentPreciseLocation {
                    distributionExperience
                        .startPreparedRun()
                }
            },
            onCompleteStop = {
                runtime.activate(
                    VexMapsScreen
                        .CompleteDistributionStop,
                )
                distributionExperience
                    .completeCurrentStop()
            },
            onRetryLeg = {
                runtime.activate(
                    VexMapsScreen
                        .RetryDistributionLeg,
                )
                distributionExperience
                    .retryFailedLeg()
            },
            onReset = {
                runtime.activate(
                    VexMapsScreen
                        .ResetDistributionRun,
                )
                distributionExperience
                    .clearAll()
            },
        )
    val contextDockPages =
        buildList {
            vexStreamDockPage?.let(::add)
            add(
                distributionDockPage,
            )
            VexMapsOnlineStyle
                .entries
                .map { style ->
                    style.contextDockPage(
                        status =
                            styleConvergence
                                .status
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

    /*
     * Visual layering starts with the provider view and then composes Vex-owned controls above it.
     * AndroidView is therefore an effect/rendering boundary, not another Screen registry or source
     * of Maps semantic truth.
     */
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
                    if (
                        distributionModeProjection
                            .state is
                            VexMapsDistributionModeState.Distribution
                    ) {
                        val currentRunState =
                            distributionExperience
                                .runProjection
                                .state
                        if (
                            currentRunState !is
                                VexMapsDistributionRunState.Empty &&
                            currentRunState !is
                                VexMapsDistributionRunState.Draft
                        ) {
                            return@VexMapsRoutePlannerCard
                        }

                        runtime.recordDynamicInteraction(
                            subjectRef =
                                "presentation.vexmaps.place-search.result",
                            action =
                                "AddedToDistribution",
                        )
                        distributionExperience
                            .addStop(
                                placeRef =
                                    place.placeRef,
                                name =
                                    place.name,
                                subtitle =
                                    place.subtitle,
                                latitude =
                                    place.coordinate.latitude,
                                longitude =
                                    place.coordinate.longitude,
                            )
                        preferredDockPageRef =
                            VEXMAPS_DISTRIBUTION_CONTEXT_DOCK_PAGE_REF
                                .value
                        placeSearchState =
                            VexMapsPlaceSearchState.Idle
                        mapAdapter.focusOn(
                            place.coordinate,
                        )
                    } else when (mapSelectionTarget) {
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
                    if (
                        !distributionExperience
                            .canReplaceRoutingRegion()
                    ) {
                        return@VexMapsRoutePlannerCard
                    }

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
                    if (
                        distributionModeProjection
                            .state is
                            VexMapsDistributionModeState
                                .Distribution
                    ) {
                        distributionExperience
                            .cancelExecutionPreservingDraft()
                    } else {
                        VexMapsNavigationProcess
                            .stop()
                    }
                },
                onStart = {
                    if (
                        distributionModeProjection
                            .state is
                            VexMapsDistributionModeState
                                .Distribution
                    ) {
                        if (
                            distributionRunProjection
                                .state is
                                VexMapsDistributionRunState.Ready &&
                            distributionNavigation
                                .layer ==
                                VexMapsDistributionNavigationLayer
                                    .ReadyToStart
                        ) {
                            runtime.activate(
                                VexMapsScreen
                                    .StartDistributionRun,
                            )
                            readCurrentPreciseLocation {
                                distributionExperience
                                    .startPreparedRun()
                            }
                        }
                    } else {
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
                    }
                },
                onStop = {
                    runtime.activate(
                        VexMapsScreen.StopNavigation,
                    )
                    currentLocation = null
                    locationState =
                        VexMapsLocationState.Idle

                    if (
                        distributionModeProjection
                            .state is
                            VexMapsDistributionModeState
                                .Distribution
                    ) {
                        distributionExperience
                            .cancelExecutionPreservingDraft()
                    } else {
                        VexMapsNavigationService
                            .stop(context)
                    }
                },
                onDone = {
                    if (
                        distributionModeProjection
                            .state is
                            VexMapsDistributionModeState
                                .Distribution
                    ) {
                        if (
                            distributionRunProjection
                                .state is
                                VexMapsDistributionRunState.AtStop
                        ) {
                            runtime.activate(
                                VexMapsScreen
                                    .CompleteDistributionStop,
                            )
                            distributionExperience
                                .completeCurrentStop()
                        }
                    } else {
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
                    }
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

        /*
         * Context Dock may project a style choice into style convergence, but automatic convergence
         * is not a human interaction. We record Selected only when the human actually changed the
         * preferred page; restored/already-preferred synchronization remains non-human evidence.
         */
        VexContextDock(
            pages = contextDockPages,
            preferredPageRef =
                preferredContextDockPage,
            onSelectedPageChanged = {
                    selectedPageRef ->
                val humanSelectionChanged =
                    selectedPageRef.value !=
                        preferredDockPageRef

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
                            if (humanSelectionChanged) {
                                runtime.recordDynamicInteraction(
                                    subjectRef =
                                        "presentation.context-dock.vexmaps.style",
                                    action = "Selected",
                                )
                            }
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

// [VXG RealForever]
