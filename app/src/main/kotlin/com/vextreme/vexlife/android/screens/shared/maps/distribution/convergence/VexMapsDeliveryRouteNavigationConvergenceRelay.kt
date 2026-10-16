package com.vextreme.vexlife.android.screens.shared.maps.distribution.convergence

import com.vextreme.vexlife.android.relay.VexCompoundRelay
import com.vextreme.vexlife.android.relay.VexCompoundState
import com.vextreme.vexlife.android.relay.VexInputState
import com.vextreme.vexlife.android.relay.VexOutputState
import com.vextreme.vexlife.android.relay.vexDependencies
import com.vextreme.vexlife.android.screens.shared.maps.VexNavigationSessionPhase
import com.vextreme.vexlife.android.screens.shared.maps.VexNavigationSessionState
import com.vextreme.vexlife.android.screens.shared.maps.distribution.deliveryroute.VexMapsDeliveryRouteControl
import com.vextreme.vexlife.android.screens.shared.maps.distribution.deliveryroute.VexMapsDeliveryRouteProjection
import com.vextreme.vexlife.android.screens.shared.maps.distribution.deliveryroute.VexMapsDeliveryRouteState
import kotlinx.coroutines.flow.StateFlow

internal sealed interface VexMapsDeliveryRouteLegTarget {
    data class Stop(
        val stopRef: String,
    ) :
        VexMapsDeliveryRouteLegTarget {
        init {
            require(stopRef.isNotBlank())
        }
    }

    data object ReturnToStart :
        VexMapsDeliveryRouteLegTarget
}

internal data class VexMapsDeliveryRouteNavigationRouteIdentity(
    val routeRef: String,
    val routeRevision: Long,
) {
    init {
        require(routeRef.isNotBlank())
        require(routeRevision >= 0L)
    }
}

/**
 * Binding from one Distribution Plan target to the exact Navigation route initially admitted for it.
 *
 * The stored revision protects initial READY admission. Once that Navigation journey is active,
 * Navigation may legitimately replace its route during reroute while preserving the stable routeRef;
 * active-leg continuity is decided by [vexMapsDeliveryRouteNavigationRealizesExpectedLeg].
 */
internal data class VexMapsDeliveryRouteExpectedLeg(
    val planRef: String,
    val target:
        VexMapsDeliveryRouteLegTarget,
    val route:
        VexMapsDeliveryRouteNavigationRouteIdentity,
) {
    init {
        require(planRef.isNotBlank())
    }
}

internal sealed interface VexMapsDeliveryRouteExpectedLegState {
    data object None :
        VexMapsDeliveryRouteExpectedLegState

    data class Expected(
        val leg:
            VexMapsDeliveryRouteExpectedLeg,
    ) :
        VexMapsDeliveryRouteExpectedLegState
}

internal enum class VexMapsDeliveryRouteLegFailureStage {
    RoutePreparation,
    NavigationStart,
    PreviousLegCleanup,
}

internal sealed interface VexMapsDeliveryRouteLegAttempt {
    data object Idle :
        VexMapsDeliveryRouteLegAttempt

    data class CleaningUpPreviousLeg(
        val nextTarget:
            VexMapsDeliveryRouteLegTarget?,
    ) :
        VexMapsDeliveryRouteLegAttempt

    data class Preparing(
        val target:
            VexMapsDeliveryRouteLegTarget,
    ) :
        VexMapsDeliveryRouteLegAttempt

    data class Failed(
        val stage:
            VexMapsDeliveryRouteLegFailureStage,
        val target:
            VexMapsDeliveryRouteLegTarget?,
        val reason: String,
    ) :
        VexMapsDeliveryRouteLegAttempt {
        init {
            require(reason.isNotBlank())
        }
    }
}

internal data class VexMapsDeliveryRouteNavigationInputState(
    val deliveryRoute:
        VexMapsDeliveryRouteProjection? =
        null,
    val navigation:
        VexNavigationSessionState? =
        null,
    val expectedLeg:
        VexMapsDeliveryRouteExpectedLegState? =
        null,
    val legAttempt:
        VexMapsDeliveryRouteLegAttempt? =
        null,
) :
    VexInputState<VexMapsDeliveryRouteNavigationInputState> {
    override fun deepCopy():
        VexMapsDeliveryRouteNavigationInputState =
        copy(
            deliveryRoute =
                deliveryRoute?.copy(
                    controls =
                        deliveryRoute.controls.toMap(),
                ),
            navigation =
                navigation?.copy(),
        )
}

internal enum class VexMapsDeliveryRouteNavigationLayer {
    Drafting,
    PreparingRoute,
    NeedsLeg,
    CleaningUpPreviousLeg,
    PreparingLeg,
    LegFailed,
    ReadyToStart,
    Navigating,
    AtDelivery,
    ForeignNavigation,
    Complete,
}

internal sealed interface VexMapsDeliveryRouteNavigationRequest {
    data class PrepareLeg(
        val stopRef: String,
    ) :
        VexMapsDeliveryRouteNavigationRequest

    data object PrepareReturn :
        VexMapsDeliveryRouteNavigationRequest

    data object StartPreparedNavigation :
        VexMapsDeliveryRouteNavigationRequest

    data class DeliveryRouteControl(
        val control:
            VexMapsDeliveryRouteControl,
    ) :
        VexMapsDeliveryRouteNavigationRequest
}

internal data class VexMapsDeliveryRouteNavigationOutput(
    val layer:
        VexMapsDeliveryRouteNavigationLayer,
    val request:
        VexMapsDeliveryRouteNavigationRequest? =
        null,
) :
    VexOutputState<VexMapsDeliveryRouteNavigationOutput> {
    override fun deepCopy():
        VexMapsDeliveryRouteNavigationOutput =
        copy()
}

/**
 * Pure relationship among complete Delivery Route truth, Navigation truth and the Distribution session's
 * expected-leg/attempt facts.
 *
 * A Navigation phase is never treated as sufficient identity. READY/ACTIVE/ARRIVED only mean
 * Distribution when the concrete routeRef + routeRevision matches the expected Distribution leg.
 */
internal class VexMapsDeliveryRouteNavigationConvergenceRelay(
    private val deliveryRouteProjections:
        StateFlow<VexMapsDeliveryRouteProjection>,
    private val navigationStates:
        StateFlow<VexNavigationSessionState>,
    private val expectedLegStates:
        StateFlow<VexMapsDeliveryRouteExpectedLegState>,
    private val legAttempts:
        StateFlow<VexMapsDeliveryRouteLegAttempt>,
) :
    VexCompoundRelay<
        VexMapsDeliveryRouteNavigationInputState,
        VexMapsDeliveryRouteNavigationOutput
    >(
        relayRef =
            "vexmaps.distribution.navigation",
        initialInputState =
            VexMapsDeliveryRouteNavigationInputState(
                deliveryRoute =
                    deliveryRouteProjections.value,
                navigation =
                    navigationStates.value,
                expectedLeg =
                    expectedLegStates.value,
                legAttempt =
                    legAttempts.value,
            ),
        initialOutputState =
            projectDeliveryRouteNavigation(
                deliveryRoute =
                    deliveryRouteProjections.value,
                navigation =
                    navigationStates.value,
                expectedLeg =
                    expectedLegStates.value,
                legAttempt =
                    legAttempts.value,
            ),
    ) {
    override fun dependencies() =
        vexDependencies<
            VexMapsDeliveryRouteNavigationInputState
        > {
            observe(
                dependencyRef =
                    "delivery-route",
                flow =
                    deliveryRouteProjections,
            ) { projection ->
                VexMapsDeliveryRouteNavigationInputState(
                    deliveryRoute =
                        projection,
                )
            }

            observe(
                dependencyRef =
                    "navigation",
                flow =
                    navigationStates,
            ) { state ->
                VexMapsDeliveryRouteNavigationInputState(
                    navigation =
                        state,
                )
            }

            observe(
                dependencyRef =
                    "expected-leg",
                flow =
                    expectedLegStates,
            ) { state ->
                VexMapsDeliveryRouteNavigationInputState(
                    expectedLeg =
                        state,
                )
            }

            observe(
                dependencyRef =
                    "leg-attempt",
                flow =
                    legAttempts,
            ) { attempt ->
                VexMapsDeliveryRouteNavigationInputState(
                    legAttempt =
                        attempt,
                )
            }
        }

    override fun processStates(
        inputState:
            VexMapsDeliveryRouteNavigationInputState,
        currentState:
            VexCompoundState<
                VexMapsDeliveryRouteNavigationInputState,
                VexMapsDeliveryRouteNavigationOutput
            >,
    ):
        VexCompoundState<
            VexMapsDeliveryRouteNavigationInputState,
            VexMapsDeliveryRouteNavigationOutput
        > {
        val previous =
            currentState.inputState
        val coherent =
            VexMapsDeliveryRouteNavigationInputState(
                deliveryRoute =
                    inputState.deliveryRoute
                        ?: previous.deliveryRoute,
                navigation =
                    inputState.navigation
                        ?: previous.navigation,
                expectedLeg =
                    inputState.expectedLeg
                        ?: previous.expectedLeg,
                legAttempt =
                    inputState.legAttempt
                        ?: previous.legAttempt,
            )

        val deliveryRoute =
            coherent.deliveryRoute
                ?: return currentState.copy(
                    inputState =
                        coherent,
                )
        val navigation =
            coherent.navigation
                ?: return currentState.copy(
                    inputState =
                        coherent,
                )
        val expectedLeg =
            coherent.expectedLeg
                ?: VexMapsDeliveryRouteExpectedLegState.None
        val legAttempt =
            coherent.legAttempt
                ?: VexMapsDeliveryRouteLegAttempt.Idle

        return currentState.copy(
            inputState =
                coherent,
            outputState =
                projectDeliveryRouteNavigation(
                    deliveryRoute =
                        deliveryRoute,
                    navigation =
                        navigation,
                    expectedLeg =
                        expectedLeg,
                    legAttempt =
                        legAttempt,
                ),
        )
    }
}

private fun projectDeliveryRouteNavigation(
    deliveryRoute:
        VexMapsDeliveryRouteProjection,
    navigation:
        VexNavigationSessionState,
    expectedLeg:
        VexMapsDeliveryRouteExpectedLegState,
    legAttempt:
        VexMapsDeliveryRouteLegAttempt,
): VexMapsDeliveryRouteNavigationOutput =
    when (
        val deliveryRouteState =
            deliveryRoute.state
    ) {
        VexMapsDeliveryRouteState.Empty,
        is VexMapsDeliveryRouteState.Draft ->
            VexMapsDeliveryRouteNavigationOutput(
                layer =
                    VexMapsDeliveryRouteNavigationLayer
                        .Drafting,
            )

        is VexMapsDeliveryRouteState.Preparing ->
            VexMapsDeliveryRouteNavigationOutput(
                layer =
                    VexMapsDeliveryRouteNavigationLayer
                        .PreparingRoute,
            )

        is VexMapsDeliveryRouteState.Ready ->
            projectExpectedLeg(
                planRef =
                    deliveryRouteState.planRef,
                target =
                    VexMapsDeliveryRouteLegTarget.Stop(
                        deliveryRouteState
                            .orderedStopRefs
                            .first(),
                    ),
                navigation =
                    navigation,
                expectedLeg =
                    expectedLeg,
                legAttempt =
                    legAttempt,
                humanStartRequired =
                    true,
            )

        is VexMapsDeliveryRouteState.Delivering ->
            projectExpectedLeg(
                planRef =
                    deliveryRouteState.planRef,
                target =
                    VexMapsDeliveryRouteLegTarget.Stop(
                        deliveryRouteState
                            .currentStopRef,
                    ),
                navigation =
                    navigation,
                expectedLeg =
                    expectedLeg,
                legAttempt =
                    legAttempt,
                humanStartRequired =
                    false,
            )

        is VexMapsDeliveryRouteState.AtDelivery ->
            VexMapsDeliveryRouteNavigationOutput(
                layer =
                    VexMapsDeliveryRouteNavigationLayer
                        .AtDelivery,
            )

        is VexMapsDeliveryRouteState.ReturningToStart ->
            projectExpectedLeg(
                planRef =
                    deliveryRouteState.planRef,
                target =
                    VexMapsDeliveryRouteLegTarget
                        .ReturnToStart,
                navigation =
                    navigation,
                expectedLeg =
                    expectedLeg,
                legAttempt =
                    legAttempt,
                humanStartRequired =
                    false,
            )

        is VexMapsDeliveryRouteState.Completed ->
            VexMapsDeliveryRouteNavigationOutput(
                layer =
                    VexMapsDeliveryRouteNavigationLayer
                        .Complete,
            )
    }

private fun projectExpectedLeg(
    planRef: String,
    target:
        VexMapsDeliveryRouteLegTarget,
    navigation:
        VexNavigationSessionState,
    expectedLeg:
        VexMapsDeliveryRouteExpectedLegState,
    legAttempt:
        VexMapsDeliveryRouteLegAttempt,
    humanStartRequired:
        Boolean,
): VexMapsDeliveryRouteNavigationOutput {
    val expected =
        (
            expectedLeg as?
                VexMapsDeliveryRouteExpectedLegState.Expected
        )
            ?.leg
            ?.takeIf {
                it.planRef ==
                    planRef &&
                    it.target ==
                    target
            }

    if (
        expected == null ||
        !vexMapsDeliveryRouteNavigationRealizesExpectedLeg(
            navigation =
                navigation,
            expected =
                expected,
        )
    ) {
        if (
            navigation.phase !=
                VexNavigationSessionPhase.IDLE &&
            navigation.routeResult !=
                null
        ) {
            return VexMapsDeliveryRouteNavigationOutput(
                layer =
                    VexMapsDeliveryRouteNavigationLayer
                        .ForeignNavigation,
            )
        }

        return projectLegNeed(
            target =
                target,
            legAttempt =
                legAttempt,
        )
    }

    return when (
        navigation.phase
    ) {
        VexNavigationSessionPhase.IDLE ->
            projectLegNeed(
                target =
                    target,
                legAttempt =
                    legAttempt,
            )

        VexNavigationSessionPhase.READY -> {
            val retainedAttempt =
                projectRetainedLegAttempt(
                    target =
                        target,
                    legAttempt =
                        legAttempt,
                )

            if (retainedAttempt != null) {
                retainedAttempt
            } else if (humanStartRequired) {
                VexMapsDeliveryRouteNavigationOutput(
                    layer =
                        VexMapsDeliveryRouteNavigationLayer
                            .ReadyToStart,
                )
            } else {
                VexMapsDeliveryRouteNavigationOutput(
                    layer =
                        VexMapsDeliveryRouteNavigationLayer
                            .ReadyToStart,
                    request =
                        VexMapsDeliveryRouteNavigationRequest
                            .StartPreparedNavigation,
                )
            }
        }

        VexNavigationSessionPhase.ACTIVE,
        VexNavigationSessionPhase.OFF_ROUTE,
        VexNavigationSessionPhase.LOCATION_HELD ->
            VexMapsDeliveryRouteNavigationOutput(
                layer =
                    VexMapsDeliveryRouteNavigationLayer
                        .Navigating,
            )

        VexNavigationSessionPhase.ARRIVED ->
            VexMapsDeliveryRouteNavigationOutput(
                layer =
                    VexMapsDeliveryRouteNavigationLayer
                        .AtDelivery,
                request =
                    VexMapsDeliveryRouteNavigationRequest
                        .DeliveryRouteControl(
                            VexMapsDeliveryRouteControl
                                .Arrive,
                        ),
            )
    }
}

private fun projectRetainedLegAttempt(
    target:
        VexMapsDeliveryRouteLegTarget,
    legAttempt:
        VexMapsDeliveryRouteLegAttempt,
): VexMapsDeliveryRouteNavigationOutput? =
    when (
        legAttempt
    ) {
        is VexMapsDeliveryRouteLegAttempt.CleaningUpPreviousLeg ->
            if (
                legAttempt.nextTarget ==
                target
            ) {
                VexMapsDeliveryRouteNavigationOutput(
                    layer =
                        VexMapsDeliveryRouteNavigationLayer
                            .CleaningUpPreviousLeg,
                )
            } else {
                null
            }

        is VexMapsDeliveryRouteLegAttempt.Preparing ->
            if (
                legAttempt.target ==
                target
            ) {
                VexMapsDeliveryRouteNavigationOutput(
                    layer =
                        VexMapsDeliveryRouteNavigationLayer
                            .PreparingLeg,
                )
            } else {
                null
            }

        is VexMapsDeliveryRouteLegAttempt.Failed ->
            if (
                legAttempt.target ==
                target
            ) {
                VexMapsDeliveryRouteNavigationOutput(
                    layer =
                        VexMapsDeliveryRouteNavigationLayer
                            .LegFailed,
                )
            } else {
                null
            }

        VexMapsDeliveryRouteLegAttempt.Idle ->
            null
    }

private fun projectLegNeed(
    target:
        VexMapsDeliveryRouteLegTarget,
    legAttempt:
        VexMapsDeliveryRouteLegAttempt,
): VexMapsDeliveryRouteNavigationOutput =
    when (
        legAttempt
    ) {
        is VexMapsDeliveryRouteLegAttempt.CleaningUpPreviousLeg ->
            if (
                legAttempt.nextTarget ==
                target
            ) {
                VexMapsDeliveryRouteNavigationOutput(
                    layer =
                        VexMapsDeliveryRouteNavigationLayer
                            .CleaningUpPreviousLeg,
                )
            } else {
                needsLeg(
                    target,
                )
            }

        is VexMapsDeliveryRouteLegAttempt.Preparing ->
            if (
                legAttempt.target ==
                target
            ) {
                VexMapsDeliveryRouteNavigationOutput(
                    layer =
                        VexMapsDeliveryRouteNavigationLayer
                            .PreparingLeg,
                )
            } else {
                needsLeg(
                    target,
                )
            }

        is VexMapsDeliveryRouteLegAttempt.Failed ->
            if (
                legAttempt.target ==
                target
            ) {
                VexMapsDeliveryRouteNavigationOutput(
                    layer =
                        VexMapsDeliveryRouteNavigationLayer
                            .LegFailed,
                )
            } else {
                needsLeg(
                    target,
                )
            }

        VexMapsDeliveryRouteLegAttempt.Idle ->
            needsLeg(
                target,
            )
    }

private fun needsLeg(
    target:
        VexMapsDeliveryRouteLegTarget,
): VexMapsDeliveryRouteNavigationOutput =
    VexMapsDeliveryRouteNavigationOutput(
        layer =
            VexMapsDeliveryRouteNavigationLayer
                .NeedsLeg,
        request =
            when (
                target
            ) {
                is VexMapsDeliveryRouteLegTarget.Stop ->
                    VexMapsDeliveryRouteNavigationRequest
                        .PrepareLeg(
                            stopRef =
                                target.stopRef,
                        )

                VexMapsDeliveryRouteLegTarget.ReturnToStart ->
                    VexMapsDeliveryRouteNavigationRequest
                        .PrepareReturn
            },
    )

/**
 * Does current Navigation still realize this exact Distribution leg?
 *
 * READY is initial admission, so both stable routeRef and exact routeRevision must match.
 *
 * After Navigation is active, Navigation owns reroute evolution inside that same journey. Its
 * accepted reroute path preserves routeRef and advances routeRevision. Distribution therefore keeps
 * the leg bound when the stable routeRef is unchanged and the active revision is not older than the
 * one originally admitted. A different routeRef, an older active revision, or IDLE Navigation is
 * foreign/not-realizing.
 */
internal fun vexMapsDeliveryRouteNavigationRealizesExpectedLeg(
    navigation:
        VexNavigationSessionState,
    expected:
        VexMapsDeliveryRouteExpectedLeg,
): Boolean {
    val route =
        navigation
            .routeResult
            ?.route
            ?: return false

    if (
        route.routeRef !=
        expected.route.routeRef
    ) {
        return false
    }

    return when (
        navigation.phase
    ) {
        VexNavigationSessionPhase.READY ->
            route.routeRevision ==
                expected.route.routeRevision

        VexNavigationSessionPhase.ACTIVE,
        VexNavigationSessionPhase.OFF_ROUTE,
        VexNavigationSessionPhase.LOCATION_HELD,
        VexNavigationSessionPhase.ARRIVED ->
            route.routeRevision >=
                expected.route.routeRevision

        VexNavigationSessionPhase.IDLE ->
            false
    }
}

// [VXG RealForever]
