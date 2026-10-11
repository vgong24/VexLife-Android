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
) {
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
                        )
                }

                is VexRouteMatchResult.OffRoute ->
                    mutableState.value =
                        state.copy(
                            phase =
                                VexNavigationSessionPhase.OFF_ROUTE,
                            locationDisposition =
                                VexNavigationLocationDisposition.OFF_ROUTE,
                        )

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
}

// [VXG RealForever]
