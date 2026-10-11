package com.vextreme.vexlife.android.screens.shared.maps

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class VexNavigationSessionPhase {
    IDLE,
    READY,
    ACTIVE,
    OFF_ROUTE,
    LOCATION_HELD,
    ARRIVED,
}

enum class VexNavigationLocationDisposition {
    NONE,
    MATCHED,
    OFF_ROUTE,
    STALE,
    OUT_OF_ORDER,
    INACCURATE,
    UNAVAILABLE,
}

/**
 * Current in-process navigation session truth.
 *
 * Process-death restoration is intentionally not claimed by this generation.
 */
data class VexNavigationRerouteRequest(
    val requestRef: String,
    val routeRef: String,
    val previousRouteRevision: Long,
    val origin: VexMapCoordinate,
    val destination: VexMapCoordinate,
    val destinationLabel: String,
    val travelMode: VexMapsTravelMode,
)

data class VexNavigationSessionState(
    val phase: VexNavigationSessionPhase =
        VexNavigationSessionPhase.IDLE,
    val routeResult:
        VexOfflineRouteResult? = null,
    val convergence:
        VexNavigationConvergenceOutputState? =
        null,
    val locationDisposition:
        VexNavigationLocationDisposition =
        VexNavigationLocationDisposition.NONE,
    val pendingReroute:
        VexNavigationRerouteRequest? = null,
)

/**
 * Feature-local owner for one active navigation session.
 *
 * The Android service keeps this owner alive and supplies location samples. The service does not
 * calculate route progress itself.
 */
class VexNavigationSession(
    private val guidancePolicy:
        VexNavigationGuidancePolicy =
        VexNavigationGuidancePolicy(
            farWithinMeters = 700,
            approachingWithinMeters = 220,
            turnNowWithinMeters = 55,
        ),
    private val matchingPolicy:
        VexRouteMatchingPolicy =
        VexRouteMatchingPolicy(),
    private val rerouteCooldownMillis:
        Long = 10_000L,
) {
    init {
        require(rerouteCooldownMillis > 0L)
    }
    private val lock = Any()

    private val mutableState =
        MutableStateFlow(
            VexNavigationSessionState(),
        )

    val states:
        StateFlow<VexNavigationSessionState> =
        mutableState.asStateFlow()

    val current:
        VexNavigationSessionState
        get() = mutableState.value

    private var matcher:
        VexRouteGeometryMatcher? = null
    private var convergence:
        VexNavigationConvergenceRelay? = null
    private var lastRerouteRequestElapsedRealtimeMillis:
        Long? = null

    fun prepare(
        routeResult: VexOfflineRouteResult,
    ) {
        synchronized(lock) {
            val relay =
                VexNavigationConvergenceRelay(
                    guidancePolicy,
                )
            relay.routeChanged(
                routeResult.route,
            )

            matcher =
                VexRouteGeometryMatcher(
                    route =
                        routeResult.route,
                    geometry =
                        routeResult.geometry,
                    policy =
                        matchingPolicy,
                )
            convergence =
                relay

            mutableState.value =
                VexNavigationSessionState(
                    phase =
                        VexNavigationSessionPhase.READY,
                    routeResult =
                        routeResult.copy(
                            geometry =
                                routeResult
                                    .geometry
                                    .toList(),
                        ),
                    convergence =
                        relay.currentOutput,
                )
        }
    }

    fun start() {
        synchronized(lock) {
            val current =
                mutableState.value

            require(
                current.phase ==
                    VexNavigationSessionPhase.READY,
            ) {
                "navigation must be prepared before start"
            }

            mutableState.value =
                current.copy(
                    phase =
                        VexNavigationSessionPhase.ACTIVE,
                )
        }
    }

    fun acceptLocation(
        sample: VexNavigationLocationSample,
        nowElapsedRealtimeMillis: Long,
    ) {
        synchronized(lock) {
            val state =
                mutableState.value

            if (
                state.phase ==
                    VexNavigationSessionPhase.IDLE ||
                state.phase ==
                    VexNavigationSessionPhase.READY ||
                state.phase ==
                    VexNavigationSessionPhase.ARRIVED
            ) {
                return
            }

            val matcher =
                requireNotNull(matcher)
            val relay =
                requireNotNull(convergence)

            when (
                val match =
                    matcher.match(
                        sample = sample,
                        nowElapsedRealtimeMillis =
                            nowElapsedRealtimeMillis,
                    )
            ) {
                is VexRouteMatchResult.Matched -> {
                    relay.fixChanged(
                        match.navigationFix,
                    )
                    val output =
                        relay.currentOutput

                    mutableState.value =
                        state.copy(
                            phase =
                                if (
                                    output.availability ==
                                    VexNavigationConvergenceAvailability.ARRIVED
                                ) {
                                    VexNavigationSessionPhase.ARRIVED
                                } else {
                                    VexNavigationSessionPhase.ACTIVE
                                },
                            convergence =
                                output,
                            locationDisposition =
                                VexNavigationLocationDisposition.MATCHED,
                            pendingReroute = null,
                        )
                }

                is VexRouteMatchResult.OffRoute -> {
                    val lastRequest =
                        lastRerouteRequestElapsedRealtimeMillis
                    val canRequest =
                        state.pendingReroute == null &&
                            (
                                lastRequest == null ||
                                sample.elapsedRealtimeMillis -
                                    lastRequest >=
                                    rerouteCooldownMillis
                            )

                    val reroute =
                        if (canRequest) {
                            val currentRoute =
                                requireNotNull(
                                    state.routeResult,
                                )
                            val destination =
                                currentRoute
                                    .geometry
                                    .last()

                            lastRerouteRequestElapsedRealtimeMillis =
                                sample.elapsedRealtimeMillis

                            VexNavigationRerouteRequest(
                                requestRef =
                                    buildString {
                                        append(
                                            currentRoute
                                                .route
                                                .routeRef,
                                        )
                                        append(':')
                                        append(
                                            currentRoute
                                                .route
                                                .routeRevision,
                                        )
                                        append(':')
                                        append(
                                            sample.sampleRef,
                                        )
                                    },
                                routeRef =
                                    currentRoute
                                        .route
                                        .routeRef,
                                previousRouteRevision =
                                    currentRoute
                                        .route
                                        .routeRevision,
                                origin =
                                    VexMapCoordinate(
                                        latitude =
                                            sample.latitude,
                                        longitude =
                                            sample.longitude,
                                    ),
                                destination =
                                    VexMapCoordinate(
                                        latitude =
                                            destination.latitude,
                                        longitude =
                                            destination.longitude,
                                    ),
                                destinationLabel =
                                    currentRoute
                                        .route
                                        .destinationLabel,
                                travelMode =
                                    currentRoute
                                        .travelMode,
                            )
                        } else {
                            state.pendingReroute
                        }

                    mutableState.value =
                        state.copy(
                            phase =
                                VexNavigationSessionPhase.OFF_ROUTE,
                            locationDisposition =
                                VexNavigationLocationDisposition.OFF_ROUTE,
                            pendingReroute =
                                reroute,
                        )
                }

                is VexRouteMatchResult.Inaccurate ->
                    mutableState.value =
                        state.copy(
                            phase =
                                VexNavigationSessionPhase.LOCATION_HELD,
                            locationDisposition =
                                VexNavigationLocationDisposition.INACCURATE,
                        )

                VexRouteMatchResult.Stale ->
                    mutableState.value =
                        state.copy(
                            phase =
                                VexNavigationSessionPhase.LOCATION_HELD,
                            locationDisposition =
                                VexNavigationLocationDisposition.STALE,
                        )

                VexRouteMatchResult.OutOfOrder ->
                    mutableState.value =
                        state.copy(
                            locationDisposition =
                                VexNavigationLocationDisposition.OUT_OF_ORDER,
                        )
            }
        }
    }

    fun replaceActiveRoute(
        routeResult: VexOfflineRouteResult,
    ) {
        synchronized(lock) {
            val state =
                mutableState.value

            require(
                state.phase ==
                    VexNavigationSessionPhase.ACTIVE ||
                    state.phase ==
                    VexNavigationSessionPhase.OFF_ROUTE ||
                    state.phase ==
                    VexNavigationSessionPhase.LOCATION_HELD,
            ) {
                "active navigation is required for reroute replacement"
            }

            val relay =
                VexNavigationConvergenceRelay(
                    guidancePolicy,
                )
            relay.routeChanged(
                routeResult.route,
            )

            matcher =
                VexRouteGeometryMatcher(
                    route =
                        routeResult.route,
                    geometry =
                        routeResult.geometry,
                    policy =
                        matchingPolicy,
                )
            convergence = relay
            lastRerouteRequestElapsedRealtimeMillis = null

            mutableState.value =
                state.copy(
                    phase =
                        VexNavigationSessionPhase.ACTIVE,
                    routeResult =
                        routeResult.copy(
                            geometry =
                                routeResult
                                    .geometry
                                    .toList(),
                        ),
                    convergence =
                        relay.currentOutput,
                    locationDisposition =
                        VexNavigationLocationDisposition.NONE,
                    pendingReroute = null,
                )
        }
    }

    fun rerouteFailed(
        requestRef: String,
    ) {
        synchronized(lock) {
            val state =
                mutableState.value

            if (
                state.pendingReroute
                    ?.requestRef ==
                requestRef
            ) {
                mutableState.value =
                    state.copy(
                        pendingReroute = null,
                    )
            }
        }
    }

    fun locationUnavailable() {
        synchronized(lock) {
            val state =
                mutableState.value

            if (
                state.phase ==
                    VexNavigationSessionPhase.ACTIVE ||
                state.phase ==
                    VexNavigationSessionPhase.OFF_ROUTE ||
                state.phase ==
                    VexNavigationSessionPhase.LOCATION_HELD
            ) {
                mutableState.value =
                    state.copy(
                        phase =
                            VexNavigationSessionPhase.LOCATION_HELD,
                        locationDisposition =
                            VexNavigationLocationDisposition.UNAVAILABLE,
                    )
            }
        }
    }

    fun stop() {
        synchronized(lock) {
            matcher = null
            convergence = null
            lastRerouteRequestElapsedRealtimeMillis = null
            mutableState.value =
                VexNavigationSessionState()
        }
    }
}

/**
 * One process-local navigation owner shared by the VexMaps Screen and its Android foreground service.
 *
 * This is deliberately feature-local rather than a generic service locator.
 */
internal object VexMapsNavigationProcess {
    val session =
        VexNavigationSession()

    private val mutableVisualLocation =
        MutableStateFlow<
            VexMapsNavigationVisualLocation?
        >(null)

    val visualLocations:
        StateFlow<VexMapsNavigationVisualLocation?> =
        mutableVisualLocation.asStateFlow()

    private val lock = Any()
    private var activeRoutingPack:
        VexMapsRegionPack? = null

    fun prepare(
        routeResult: VexOfflineRouteResult,
        routingPack: VexMapsRegionPack,
    ) {
        synchronized(lock) {
            activeRoutingPack =
                routingPack
            mutableVisualLocation.value =
                null
            session.prepare(
                routeResult,
            )
        }
    }

    fun acceptLocation(
        sample: VexNavigationLocationSample,
        nowElapsedRealtimeMillis: Long,
    ) {
        synchronized(lock) {
            mutableVisualLocation.value =
                advanceVexMapsNavigationVisualLocation(
                    current =
                        mutableVisualLocation.value,
                    sample = sample,
                    phase =
                        session.current.phase,
                )

            session.acceptLocation(
                sample = sample,
                nowElapsedRealtimeMillis =
                    nowElapsedRealtimeMillis,
            )
        }
    }

    suspend fun reroutePending(
        context: android.content.Context,
        requestRef: String,
    ): Boolean {
        val request =
            session
                .current
                .pendingReroute
                ?.takeIf {
                    it.requestRef ==
                        requestRef
                }
                ?: return false

        val pack =
            synchronized(lock) {
                activeRoutingPack
            } ?: return false

        val rerouted =
            VexMapsOfflineRoutingProcess
                .planner
                .route(
                    context = context,
                    pack = pack,
                    request =
                        VexOfflineRouteRequest(
                            routeRef =
                                request.routeRef,
                            routeRevision =
                                VexMapsRouteIdentity
                                    .nextRevision(),
                            origin =
                                request.origin,
                            destination =
                                request.destination,
                            destinationLabel =
                                request.destinationLabel,
                            travelMode =
                                request.travelMode,
                        ),
                )

        val stillCurrent =
            session
                .current
                .pendingReroute
                ?.requestRef ==
            requestRef

        if (!stillCurrent) {
            return false
        }

        session.replaceActiveRoute(
            rerouted,
        )
        return true
    }

    fun stop() {
        synchronized(lock) {
            activeRoutingPack = null
            mutableVisualLocation.value =
                null
            session.stop()
        }
    }
}

// [VXG RealForever]
