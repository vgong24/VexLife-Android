package com.vextreme.vexlife.android.screens.shared.maps.distribution.execution

import com.vextreme.vexlife.android.relay.VexCompoundRelay
import com.vextreme.vexlife.android.relay.VexCompoundState
import com.vextreme.vexlife.android.relay.VexInputState
import com.vextreme.vexlife.android.relay.VexOutputState
import com.vextreme.vexlife.android.screens.shared.maps.VexNavigationSessionPhase
import com.vextreme.vexlife.android.screens.shared.maps.VexNavigationSessionState
import com.vextreme.vexlife.android.screens.shared.maps.distribution.run.VexMapsDistributionRunControl
import com.vextreme.vexlife.android.screens.shared.maps.distribution.run.VexMapsDistributionRunProjection
import com.vextreme.vexlife.android.screens.shared.maps.distribution.run.VexMapsDistributionRunState

internal data class VexMapsDistributionNavigationInputState(
    val run:
        VexMapsDistributionRunProjection?,
    val navigation:
        VexNavigationSessionState?,
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
    Unavailable,
    Drafting,
    NeedsLeg,
    ReadyToStart,
    Navigating,
    AtStop,
    Complete,
}

internal sealed interface VexMapsDistributionNavigationRequest {
    data class PrepareLeg(
        val stopIndex: Int,
    ) :
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
 * Higher-order relationship between one Distribution Run and one existing Navigation session.
 *
 * This Relay never owns either truth and never performs routing/service effects. It only derives the
 * current relationship and, when necessary, emits a bounded request for the composition/reality
 * boundary to realize.
 */
internal class VexMapsDistributionNavigationConvergenceRelay(
    initialRun:
        VexMapsDistributionRunProjection,
    initialNavigation:
        VexNavigationSessionState,
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
                    initialRun,
                navigation =
                    initialNavigation,
            ),
        initialOutputState =
            projectDistributionNavigation(
                run =
                    initialRun,
                navigation =
                    initialNavigation,
            ),
    ) {
    fun runChanged(
        projection:
            VexMapsDistributionRunProjection,
    ) {
        send(
            VexMapsDistributionNavigationInputState(
                run =
                    projection,
                navigation =
                    null,
            ),
        )
    }

    fun navigationChanged(
        state:
            VexNavigationSessionState,
    ) {
        send(
            VexMapsDistributionNavigationInputState(
                run =
                    null,
                navigation =
                    state,
            ),
        )
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
            )
        val run =
            coherent.run
                ?: return currentState.copy(
                    inputState =
                        coherent,
                    outputState =
                        VexMapsDistributionNavigationOutput(
                            VexMapsDistributionNavigationLayer
                                .Unavailable,
                        ),
                )
        val navigation =
            coherent.navigation
                ?: return currentState.copy(
                    inputState =
                        coherent,
                    outputState =
                        VexMapsDistributionNavigationOutput(
                            VexMapsDistributionNavigationLayer
                                .Unavailable,
                        ),
                )

        return currentState.copy(
            inputState =
                coherent,
            outputState =
                projectDistributionNavigation(
                    run =
                        run,
                    navigation =
                        navigation,
                ),
        )
    }
}

private fun projectDistributionNavigation(
    run:
        VexMapsDistributionRunProjection,
    navigation:
        VexNavigationSessionState,
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

        is VexMapsDistributionRunState.Ready ->
            when (
                navigation.phase
            ) {
                VexNavigationSessionPhase.IDLE ->
                    VexMapsDistributionNavigationOutput(
                        layer =
                            VexMapsDistributionNavigationLayer
                                .NeedsLeg,
                        request =
                            VexMapsDistributionNavigationRequest
                                .PrepareLeg(
                                    stopIndex = 0,
                                ),
                    )

                VexNavigationSessionPhase.READY ->
                    VexMapsDistributionNavigationOutput(
                        layer =
                            VexMapsDistributionNavigationLayer
                                .ReadyToStart,
                    )

                else ->
                    VexMapsDistributionNavigationOutput(
                        layer =
                            VexMapsDistributionNavigationLayer
                                .Unavailable,
                    )
            }

        is VexMapsDistributionRunState.Running ->
            when (
                navigation.phase
            ) {
                VexNavigationSessionPhase.IDLE ->
                    VexMapsDistributionNavigationOutput(
                        layer =
                            VexMapsDistributionNavigationLayer
                                .NeedsLeg,
                        request =
                            VexMapsDistributionNavigationRequest
                                .PrepareLeg(
                                    stopIndex =
                                        runState
                                            .currentStopIndex,
                                ),
                    )

                VexNavigationSessionPhase.READY ->
                    VexMapsDistributionNavigationOutput(
                        layer =
                            VexMapsDistributionNavigationLayer
                                .ReadyToStart,
                        request =
                            VexMapsDistributionNavigationRequest
                                .StartPreparedNavigation,
                    )

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

        is VexMapsDistributionRunState.AtStop ->
            VexMapsDistributionNavigationOutput(
                layer =
                    VexMapsDistributionNavigationLayer
                        .AtStop,
            )

        is VexMapsDistributionRunState.Completed ->
            VexMapsDistributionNavigationOutput(
                layer =
                    VexMapsDistributionNavigationLayer
                        .Complete,
            )
    }

// [VXG RealForever]
