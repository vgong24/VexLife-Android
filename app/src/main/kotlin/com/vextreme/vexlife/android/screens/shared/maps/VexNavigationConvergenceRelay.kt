package com.vextreme.vexlife.android.screens.shared.maps

import com.vextreme.vexlife.android.relay.VexCompoundRelay
import com.vextreme.vexlife.android.relay.VexCompoundState
import com.vextreme.vexlife.android.relay.VexInputState
import com.vextreme.vexlife.android.relay.VexOutputState

enum class VexNavigationCueBand {
    FAR,
    APPROACHING,
    TURN_NOW,
}

/**
 * Explicit guidance thresholds for one deterministic proof/runtime policy.
 *
 * The navigation core does not silently canonize one provider's distances. A later Android adapter
 * may choose a policy informed by travel mode, road context, speed, or provider guidance while this
 * cue identity/deduplication contract stays unchanged.
 */
data class VexNavigationGuidancePolicy(
    val farWithinMeters: Int,
    val approachingWithinMeters: Int,
    val turnNowWithinMeters: Int,
) {
    init {
        require(turnNowWithinMeters > 0) {
            "turnNowWithinMeters must be positive"
        }
        require(approachingWithinMeters > turnNowWithinMeters) {
            "approachingWithinMeters must exceed turnNowWithinMeters"
        }
        require(farWithinMeters > approachingWithinMeters) {
            "farWithinMeters must exceed approachingWithinMeters"
        }
    }

    fun cueBandFor(
        distanceToManeuverMeters: Int,
    ): VexNavigationCueBand? {
        require(distanceToManeuverMeters >= 0)

        return when {
            distanceToManeuverMeters <= turnNowWithinMeters ->
                VexNavigationCueBand.TURN_NOW

            distanceToManeuverMeters <= approachingWithinMeters ->
                VexNavigationCueBand.APPROACHING

            distanceToManeuverMeters <= farWithinMeters ->
                VexNavigationCueBand.FAR

            else ->
                null
        }
    }
}

data class VexNavigationGuidanceCueKey(
    val routeRef: String,
    val routeRevision: Long,
    val maneuverRef: String,
    val cueBand: VexNavigationCueBand,
)

data class VexNavigationGuidanceCue(
    val key: VexNavigationGuidanceCueKey,
    val instructionProjection: String,
    val distanceToManeuverMeters: Int,
) {
    fun deepCopy(): VexNavigationGuidanceCue =
        copy(
            key = key.copy(),
        )
}

enum class VexNavigationConvergenceAvailability {
    /** No route has been observed yet. */
    UNOBSERVED,

    /** Route meaning exists, but no matching route-relative fix has arrived yet. */
    ROUTE_PREVIEW,

    /** A fix exists, but it belongs to a different route identity/revision. */
    HELD_FIX_ROUTE_MISMATCH,

    /** Matching route/fix truth is producing active progress. */
    NAVIGATING,

    /** Matching progress has reached or passed the route destination. */
    ARRIVED,
}

data class VexNavigationConvergenceInputState(
    val route: VexNavigationRoute? = null,
    val fix: VexNavigationFix? = null,
) : VexInputState<VexNavigationConvergenceInputState> {
    override fun deepCopy(): VexNavigationConvergenceInputState =
        copy(
            route = route?.deepCopy(),
            fix = fix?.deepCopy(),
        )
}

data class VexNavigationConvergenceOutputState(
    val availability: VexNavigationConvergenceAvailability =
        VexNavigationConvergenceAvailability.UNOBSERVED,
    val progress: VexNavigationRouteProgress? = null,

    /**
     * Most recent cue newly issued by this route revision.
     *
     * A speech/presentation adapter can react to a changed [key] without turning every progress
     * update into another spoken instruction.
     */
    val latestIssuedCue: VexNavigationGuidanceCue? = null,

    /**
     * Finite route-local deduplication memory.
     *
     * routeRevision + maneuverRef + cueBand
     * -> at most one issued semantic guidance cue.
     */
    val issuedCueKeys: Set<VexNavigationGuidanceCueKey> =
        emptySet(),
) : VexOutputState<VexNavigationConvergenceOutputState> {
    override fun deepCopy(): VexNavigationConvergenceOutputState =
        copy(
            progress = progress?.deepCopy(),
            latestIssuedCue = latestIssuedCue?.deepCopy(),
            issuedCueKeys =
                issuedCueKeys
                    .map { key -> key.copy() }
                    .toSet(),
        )
}

/**
 * Provider-independent convergence of route truth + route-relative progress.
 *
 * This Relay does not own GPS, permissions, map rendering, provider routing, Android service
 * lifetime, or audio playback. It owns the stable relationship between a route revision, a matching
 * progress fix, and deduplicated semantic guidance eligibility.
 */
class VexNavigationConvergenceRelay(
    private val guidancePolicy: VexNavigationGuidancePolicy,
) :
    VexCompoundRelay<
        VexNavigationConvergenceInputState,
        VexNavigationConvergenceOutputState
    >(
        relayRef = "relay.vexmaps.navigation.convergence",
        initialInputState =
            VexNavigationConvergenceInputState(),
        initialOutputState =
            VexNavigationConvergenceOutputState(),
    ) {
    fun routeChanged(
        route: VexNavigationRoute,
    ) {
        send(
            VexNavigationConvergenceInputState(
                route = route.deepCopy(),
            ),
        )
    }

    fun fixChanged(
        fix: VexNavigationFix,
    ) {
        send(
            VexNavigationConvergenceInputState(
                fix = fix.deepCopy(),
            ),
        )
    }

    override fun processStates(
        inputState: VexNavigationConvergenceInputState,
        currentState: VexCompoundState<
            VexNavigationConvergenceInputState,
            VexNavigationConvergenceOutputState
        >,
    ): VexCompoundState<
        VexNavigationConvergenceInputState,
        VexNavigationConvergenceOutputState
    > {
        val previousInput =
            currentState.inputState

        val route =
            inputState.route
                ?: previousInput.route

        val fix =
            inputState.fix
                ?: previousInput.fix

        val coherentInput =
            VexNavigationConvergenceInputState(
                route = route?.deepCopy(),
                fix = fix?.deepCopy(),
            )

        if (route == null) {
            return currentState.copy(
                inputState = coherentInput,
                outputState =
                    VexNavigationConvergenceOutputState(),
            )
        }

        val sameRouteIdentity =
            previousInput.route?.let { previousRoute ->
                previousRoute.routeRef == route.routeRef &&
                    previousRoute.routeRevision ==
                    route.routeRevision
            } == true

        if (sameRouteIdentity) {
            val previousRoute =
                requireNotNull(
                    previousInput.route,
                )
            require(
                previousRoute == route,
            ) {
                "route meaning changed without a new routeRevision"
            }
        }

        val issuedCueKeys =
            if (sameRouteIdentity) {
                currentState.outputState
                    .issuedCueKeys
                    .map { key -> key.copy() }
                    .toSet()
            } else {
                emptySet()
            }

        val latestIssuedCue =
            if (sameRouteIdentity) {
                currentState.outputState
                    .latestIssuedCue
                    ?.deepCopy()
            } else {
                null
            }

        if (fix == null) {
            return currentState.copy(
                inputState = coherentInput,
                outputState =
                    VexNavigationConvergenceOutputState(
                        availability =
                            VexNavigationConvergenceAvailability.ROUTE_PREVIEW,
                        issuedCueKeys = issuedCueKeys,
                        latestIssuedCue = latestIssuedCue,
                    ),
            )
        }

        /*
         * Arrival is terminal for one route revision. A later noisy/backward fix must not reopen
         * guidance; a newly selected/rerouted trip earns a new routeRevision and resets this state.
         */
        if (
            sameRouteIdentity &&
            currentState.outputState.availability ==
            VexNavigationConvergenceAvailability.ARRIVED
        ) {
            return currentState.copy(
                inputState = coherentInput,
                outputState =
                    currentState.outputState.deepCopy(),
            )
        }

        if (
            fix.routeRef != route.routeRef ||
            fix.routeRevision != route.routeRevision
        ) {
            return currentState.copy(
                inputState = coherentInput,
                outputState =
                    VexNavigationConvergenceOutputState(
                        availability =
                            VexNavigationConvergenceAvailability
                                .HELD_FIX_ROUTE_MISMATCH,
                        issuedCueKeys = issuedCueKeys,
                        latestIssuedCue = latestIssuedCue,
                    ),
            )
        }

        val progress =
            VexNavigationRouteProgressOwner.project(
                route = route,
                fix = fix,
            )

        if (progress.arrived) {
            return currentState.copy(
                inputState = coherentInput,
                outputState =
                    VexNavigationConvergenceOutputState(
                        availability =
                            VexNavigationConvergenceAvailability.ARRIVED,
                        progress = progress,
                        issuedCueKeys = issuedCueKeys,
                        latestIssuedCue = latestIssuedCue,
                    ),
            )
        }

        val nextManeuver =
            progress.nextManeuver

        val cueBand =
            nextManeuver?.let { maneuverProgress ->
                guidancePolicy.cueBandFor(
                    maneuverProgress.distanceToManeuverMeters,
                )
            }

        val candidateCueKey =
            if (
                nextManeuver != null &&
                cueBand != null
            ) {
                VexNavigationGuidanceCueKey(
                    routeRef = route.routeRef,
                    routeRevision = route.routeRevision,
                    maneuverRef =
                        nextManeuver
                            .maneuver
                            .maneuverRef,
                    cueBand = cueBand,
                )
            } else {
                null
            }

        val newlyIssuedCue =
            candidateCueKey
                ?.takeIf { key ->
                    key !in issuedCueKeys
                }
                ?.let { key ->
                    VexNavigationGuidanceCue(
                        key = key,
                        instructionProjection =
                            nextManeuver
                                ?.maneuver
                                ?.instructionProjection
                                .orEmpty(),
                        distanceToManeuverMeters =
                            nextManeuver
                                ?.distanceToManeuverMeters
                                ?: 0,
                    )
                }

        val nextIssuedCueKeys =
            if (newlyIssuedCue == null) {
                issuedCueKeys
            } else {
                issuedCueKeys +
                    newlyIssuedCue.key
            }

        return currentState.copy(
            inputState = coherentInput,
            outputState =
                VexNavigationConvergenceOutputState(
                    availability =
                        VexNavigationConvergenceAvailability.NAVIGATING,
                    progress = progress,
                    latestIssuedCue =
                        newlyIssuedCue
                            ?: latestIssuedCue,
                    issuedCueKeys = nextIssuedCueKeys,
                ),
        )
    }
}

// [VXG RealForever]
