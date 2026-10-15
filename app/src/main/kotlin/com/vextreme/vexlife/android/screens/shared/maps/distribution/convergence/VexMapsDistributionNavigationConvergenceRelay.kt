package com.vextreme.vexlife.android.screens.shared.maps.distribution.convergence

import com.vextreme.vexlife.android.relay.VexCompoundRelay
import com.vextreme.vexlife.android.relay.VexCompoundState
import com.vextreme.vexlife.android.relay.VexInputState
import com.vextreme.vexlife.android.relay.VexOutputState
import com.vextreme.vexlife.android.relay.vexDependencies
import com.vextreme.vexlife.android.screens.shared.maps.VexNavigationSessionPhase
import com.vextreme.vexlife.android.screens.shared.maps.VexNavigationSessionState
import com.vextreme.vexlife.android.screens.shared.maps.distribution.run.VexMapsDistributionRunControl
import com.vextreme.vexlife.android.screens.shared.maps.distribution.run.VexMapsDistributionRunProjection
import com.vextreme.vexlife.android.screens.shared.maps.distribution.run.VexMapsDistributionRunState
import kotlinx.coroutines.flow.StateFlow

internal sealed interface VexMapsDistributionLegTarget {
    data class Stop(
        val stopRef: String,
    ) :
        VexMapsDistributionLegTarget {
        init {
            require(stopRef.isNotBlank())
        }
    }

    data object ReturnToStart :
        VexMapsDistributionLegTarget
}

internal data class VexMapsDistributionRouteIdentity(
    val routeRef: String,
    val routeRevision: Long,
) {
    init {
        require(routeRef.isNotBlank())
        require(routeRevision >= 0L)
    }
}

internal data class VexMapsDistributionExpectedLeg(
    val planRef: String,
    val target:
        VexMapsDistributionLegTarget,
    val route:
        VexMapsDistributionRouteIdentity,
) {
    init {
        require(planRef.isNotBlank())
    }
}

internal sealed interface VexMapsDistributionExpectedLegState {
    data object None :
        VexMapsDistributionExpectedLegState

    data class Expected(
        val leg:
            VexMapsDistributionExpectedLeg,
    ) :
        VexMapsDistributionExpectedLegState
}

internal enum class VexMapsDistributionLegFailureStage {
    RoutePreparation,
    NavigationStart,
    PreviousLegCleanup,
}

internal sealed interface VexMapsDistributionLegAttempt {
    data object Idle :
        VexMapsDistributionLegAttempt

    data class CleaningUpPreviousLeg(
        val nextTarget:
            VexMapsDistributionLegTarget?,
    ) :
        VexMapsDistributionLegAttempt

    data class Preparing(
        val target:
            VexMapsDistributionLegTarget,
    ) :
        VexMapsDistributionLegAttempt

    data class Failed(
        val stage:
            VexMapsDistributionLegFailureStage,
        val target:
            VexMapsDistributionLegTarget?,
        val reason: String,
    ) :
        VexMapsDistributionLegAttempt {
        init {
            require(reason.isNotBlank())
        }
    }
}

internal data class VexMapsDistributionNavigationInputState(
    val run:
        VexMapsDistributionRunProjection? =
        null,
    val navigation:
        VexNavigationSessionState? =
        null,
    val expectedLeg:
        VexMapsDistributionExpectedLegState? =
        null,
    val legAttempt:
        VexMapsDistributionLegAttempt? =
        null,
) :
    VexInputState<VexMapsDistributionNavigationInputState> {
    override fun deepCopy():
        VexMapsDistributionNavigationInputState =
        copy(
            run =
                run?.copy(
                    controls =
                        run.controls.toMap(),
                ),
            navigation =
                navigation?.copy(),
        )
}

internal enum class VexMapsDistributionNavigationLayer {
    Drafting,
    PreparingRun,
    NeedsLeg,
    CleaningUpPreviousLeg,
    PreparingLeg,
    LegFailed,
    ReadyToStart,
    Navigating,
    AtStop,
    ForeignNavigation,
    Complete,
}

internal sealed interface VexMapsDistributionNavigationRequest {
    data class PrepareLeg(
        val stopRef: String,
    ) :
        VexMapsDistributionNavigationRequest

    data object PrepareReturn :
        VexMapsDistributionNavigationRequest

    data object StartPreparedNavigation :
        VexMapsDistributionNavigationRequest

    data class RunControl(
        val control:
            VexMapsDistributionRunControl,
    ) :
        VexMapsDistributionNavigationRequest
}

internal data class VexMapsDistributionNavigationOutput(
    val layer:
        VexMapsDistributionNavigationLayer,
    val request:
        VexMapsDistributionNavigationRequest? =
        null,
) :
    VexOutputState<VexMapsDistributionNavigationOutput> {
    override fun deepCopy():
        VexMapsDistributionNavigationOutput =
        copy()
}

/**
 * Pure relationship among complete Run truth, Navigation truth and the Distribution session's
 * expected-leg/attempt facts.
 *
 * A Navigation phase is never treated as sufficient identity. READY/ACTIVE/ARRIVED only mean
 * Distribution when the concrete routeRef + routeRevision matches the expected Distribution leg.
 */
internal class VexMapsDistributionNavigationConvergenceRelay(
    private val runProjections:
        StateFlow<VexMapsDistributionRunProjection>,
    private val navigationStates:
        StateFlow<VexNavigationSessionState>,
    private val expectedLegStates:
        StateFlow<VexMapsDistributionExpectedLegState>,
    private val legAttempts:
        StateFlow<VexMapsDistributionLegAttempt>,
) :
    VexCompoundRelay<
        VexMapsDistributionNavigationInputState,
        VexMapsDistributionNavigationOutput
    >(
        relayRef =
            "vexmaps.distribution.navigation",
        initialInputState =
            VexMapsDistributionNavigationInputState(
                run =
                    runProjections.value,
                navigation =
                    navigationStates.value,
                expectedLeg =
                    expectedLegStates.value,
                legAttempt =
                    legAttempts.value,
            ),
        initialOutputState =
            projectDistributionNavigation(
                run =
                    runProjections.value,
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
            VexMapsDistributionNavigationInputState
        > {
            observe(
                dependencyRef =
                    "run",
                flow =
                    runProjections,
            ) { projection ->
                VexMapsDistributionNavigationInputState(
                    run =
                        projection,
                )
            }

            observe(
                dependencyRef =
                    "navigation",
                flow =
                    navigationStates,
            ) { state ->
                VexMapsDistributionNavigationInputState(
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
                VexMapsDistributionNavigationInputState(
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
                VexMapsDistributionNavigationInputState(
                    legAttempt =
                        attempt,
                )
            }
        }

    override fun processStates(
        inputState:
            VexMapsDistributionNavigationInputState,
        currentState:
            VexCompoundState<
                VexMapsDistributionNavigationInputState,
                VexMapsDistributionNavigationOutput
            >,
    ):
        VexCompoundState<
            VexMapsDistributionNavigationInputState,
            VexMapsDistributionNavigationOutput
        > {
        val previous =
            currentState.inputState
        val coherent =
            VexMapsDistributionNavigationInputState(
                run =
                    inputState.run
                        ?: previous.run,
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

        val run =
            coherent.run
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
                ?: VexMapsDistributionExpectedLegState.None
        val legAttempt =
            coherent.legAttempt
                ?: VexMapsDistributionLegAttempt.Idle

        return currentState.copy(
            inputState =
                coherent,
            outputState =
                projectDistributionNavigation(
                    run =
                        run,
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

private fun projectDistributionNavigation(
    run:
        VexMapsDistributionRunProjection,
    navigation:
        VexNavigationSessionState,
    expectedLeg:
        VexMapsDistributionExpectedLegState,
    legAttempt:
        VexMapsDistributionLegAttempt,
): VexMapsDistributionNavigationOutput =
    when (
        val runState =
            run.state
    ) {
        VexMapsDistributionRunState.Empty,
        is VexMapsDistributionRunState.Draft ->
            VexMapsDistributionNavigationOutput(
                layer =
                    VexMapsDistributionNavigationLayer
                        .Drafting,
            )

        is VexMapsDistributionRunState.Preparing ->
            VexMapsDistributionNavigationOutput(
                layer =
                    VexMapsDistributionNavigationLayer
                        .PreparingRun,
            )

        is VexMapsDistributionRunState.Ready ->
            projectExpectedLeg(
                planRef =
                    runState.planRef,
                target =
                    VexMapsDistributionLegTarget.Stop(
                        runState
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

        is VexMapsDistributionRunState.Running ->
            projectExpectedLeg(
                planRef =
                    runState.planRef,
                target =
                    VexMapsDistributionLegTarget.Stop(
                        runState
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

        is VexMapsDistributionRunState.AtStop ->
            VexMapsDistributionNavigationOutput(
                layer =
                    VexMapsDistributionNavigationLayer
                        .AtStop,
            )

        is VexMapsDistributionRunState.Returning ->
            projectExpectedLeg(
                planRef =
                    runState.planRef,
                target =
                    VexMapsDistributionLegTarget
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

        is VexMapsDistributionRunState.Completed ->
            VexMapsDistributionNavigationOutput(
                layer =
                    VexMapsDistributionNavigationLayer
                        .Complete,
            )
    }

private fun projectExpectedLeg(
    planRef: String,
    target:
        VexMapsDistributionLegTarget,
    navigation:
        VexNavigationSessionState,
    expectedLeg:
        VexMapsDistributionExpectedLegState,
    legAttempt:
        VexMapsDistributionLegAttempt,
    humanStartRequired:
        Boolean,
): VexMapsDistributionNavigationOutput {
    val expected =
        (
            expectedLeg as?
                VexMapsDistributionExpectedLegState.Expected
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
        !navigationMatchesExpectedLeg(
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
            return VexMapsDistributionNavigationOutput(
                layer =
                    VexMapsDistributionNavigationLayer
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
                VexMapsDistributionNavigationOutput(
                    layer =
                        VexMapsDistributionNavigationLayer
                            .ReadyToStart,
                )
            } else {
                VexMapsDistributionNavigationOutput(
                    layer =
                        VexMapsDistributionNavigationLayer
                            .ReadyToStart,
                    request =
                        VexMapsDistributionNavigationRequest
                            .StartPreparedNavigation,
                )
            }
        }

        VexNavigationSessionPhase.ACTIVE,
        VexNavigationSessionPhase.OFF_ROUTE,
        VexNavigationSessionPhase.LOCATION_HELD ->
            VexMapsDistributionNavigationOutput(
                layer =
                    VexMapsDistributionNavigationLayer
                        .Navigating,
            )

        VexNavigationSessionPhase.ARRIVED ->
            VexMapsDistributionNavigationOutput(
                layer =
                    VexMapsDistributionNavigationLayer
                        .AtStop,
                request =
                    VexMapsDistributionNavigationRequest
                        .RunControl(
                            VexMapsDistributionRunControl
                                .Arrive,
                        ),
            )
    }
}

private fun projectRetainedLegAttempt(
    target:
        VexMapsDistributionLegTarget,
    legAttempt:
        VexMapsDistributionLegAttempt,
): VexMapsDistributionNavigationOutput? =
    when (
        legAttempt
    ) {
        is VexMapsDistributionLegAttempt.CleaningUpPreviousLeg ->
            if (
                legAttempt.nextTarget ==
                target
            ) {
                VexMapsDistributionNavigationOutput(
                    layer =
                        VexMapsDistributionNavigationLayer
                            .CleaningUpPreviousLeg,
                )
            } else {
                null
            }

        is VexMapsDistributionLegAttempt.Preparing ->
            if (
                legAttempt.target ==
                target
            ) {
                VexMapsDistributionNavigationOutput(
                    layer =
                        VexMapsDistributionNavigationLayer
                            .PreparingLeg,
                )
            } else {
                null
            }

        is VexMapsDistributionLegAttempt.Failed ->
            if (
                legAttempt.target ==
                target
            ) {
                VexMapsDistributionNavigationOutput(
                    layer =
                        VexMapsDistributionNavigationLayer
                            .LegFailed,
                )
            } else {
                null
            }

        VexMapsDistributionLegAttempt.Idle ->
            null
    }

private fun projectLegNeed(
    target:
        VexMapsDistributionLegTarget,
    legAttempt:
        VexMapsDistributionLegAttempt,
): VexMapsDistributionNavigationOutput =
    when (
        legAttempt
    ) {
        is VexMapsDistributionLegAttempt.CleaningUpPreviousLeg ->
            if (
                legAttempt.nextTarget ==
                target
            ) {
                VexMapsDistributionNavigationOutput(
                    layer =
                        VexMapsDistributionNavigationLayer
                            .CleaningUpPreviousLeg,
                )
            } else {
                needsLeg(
                    target,
                )
            }

        is VexMapsDistributionLegAttempt.Preparing ->
            if (
                legAttempt.target ==
                target
            ) {
                VexMapsDistributionNavigationOutput(
                    layer =
                        VexMapsDistributionNavigationLayer
                            .PreparingLeg,
                )
            } else {
                needsLeg(
                    target,
                )
            }

        is VexMapsDistributionLegAttempt.Failed ->
            if (
                legAttempt.target ==
                target
            ) {
                VexMapsDistributionNavigationOutput(
                    layer =
                        VexMapsDistributionNavigationLayer
                            .LegFailed,
                )
            } else {
                needsLeg(
                    target,
                )
            }

        VexMapsDistributionLegAttempt.Idle ->
            needsLeg(
                target,
            )
    }

private fun needsLeg(
    target:
        VexMapsDistributionLegTarget,
): VexMapsDistributionNavigationOutput =
    VexMapsDistributionNavigationOutput(
        layer =
            VexMapsDistributionNavigationLayer
                .NeedsLeg,
        request =
            when (
                target
            ) {
                is VexMapsDistributionLegTarget.Stop ->
                    VexMapsDistributionNavigationRequest
                        .PrepareLeg(
                            stopRef =
                                target.stopRef,
                        )

                VexMapsDistributionLegTarget.ReturnToStart ->
                    VexMapsDistributionNavigationRequest
                        .PrepareReturn
            },
    )

private fun navigationMatchesExpectedLeg(
    navigation:
        VexNavigationSessionState,
    expected:
        VexMapsDistributionExpectedLeg,
): Boolean {
    val route =
        navigation
            .routeResult
            ?.route
            ?: return false

    return route.routeRef ==
        expected.route.routeRef &&
        route.routeRevision ==
        expected.route.routeRevision
}

// [VXG RealForever]
