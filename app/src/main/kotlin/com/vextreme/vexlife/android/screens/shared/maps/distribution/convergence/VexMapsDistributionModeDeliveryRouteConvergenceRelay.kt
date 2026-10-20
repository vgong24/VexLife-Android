package com.vextreme.vexlife.android.screens.shared.maps.distribution.convergence

import com.vextreme.vexlife.android.relay.VexCompoundRelay
import com.vextreme.vexlife.android.relay.VexCompoundState
import com.vextreme.vexlife.android.relay.VexInputState
import com.vextreme.vexlife.android.relay.VexOutputState
import com.vextreme.vexlife.android.relay.vexDependencies
import com.vextreme.vexlife.android.screens.shared.maps.distribution.mode.VexMapsDistributionModeControl
import com.vextreme.vexlife.android.screens.shared.maps.distribution.mode.VexMapsDistributionModeProjection
import com.vextreme.vexlife.android.screens.shared.maps.distribution.mode.VexMapsDistributionModeState
import com.vextreme.vexlife.android.screens.shared.maps.distribution.deliveryroute.VexMapsDeliveryRouteProjection
import com.vextreme.vexlife.android.screens.shared.maps.distribution.deliveryroute.VexMapsDeliveryRouteState
import kotlinx.coroutines.flow.StateFlow

/**
 * Current truths used to answer one relationship question:
 *
 * "Does the independently-owned Distribution Mode agree with the independently-owned Delivery
 * Route?"
 *
 * mode
 *     tells whether Maps is currently operating in Regular or Distribution mode;
 *
 * deliveryRoute
 *     tells whether a delivery-route truth currently exists.
 *
 * A non-empty Delivery Route combined with Regular mode is the only relationship that earns a
 * bounded request back through [VexMapsDistributionModeControl.UseDistribution].
 */
internal data class VexMapsDistributionModeDeliveryRouteInputState(
    val mode:
        VexMapsDistributionModeProjection? =
        null,
    val deliveryRoute:
        VexMapsDeliveryRouteProjection? =
        null,
) :
    VexInputState<VexMapsDistributionModeDeliveryRouteInputState> {
    override fun deepCopy():
        VexMapsDistributionModeDeliveryRouteInputState =
        copy(
            mode =
                mode?.copy(
                    controls =
                        mode.controls.toMap(),
                ),
            deliveryRoute =
                deliveryRoute?.copy(
                    controls =
                        deliveryRoute.controls.toMap(),
                ),
        )
}

internal enum class VexMapsDistributionModeDeliveryRouteCoherence {
    Coherent,
    NeedsDistributionMode,
}

internal data class VexMapsDistributionModeDeliveryRouteOutput(
    /** Current truth of the Mode / Delivery Route relationship. */
    val coherence:
        VexMapsDistributionModeDeliveryRouteCoherence,

    /**
     * Optional bounded feedback into the Mode family's existing SCAE boundary.
     *
     * Convergence never selects a Mode Act/Effect directly; Mode re-resolves this Control against
     * its own current State.
     */
    val requestedModeControl:
        VexMapsDistributionModeControl? =
        null,
) :
    VexOutputState<VexMapsDistributionModeDeliveryRouteOutput> {
    override fun deepCopy():
        VexMapsDistributionModeDeliveryRouteOutput =
        copy()
}

/**
 * One higher-order question:
 *
 * Is the independently-owned Maps Mode coherent with the independently-owned Delivery Route?
 *
 * This Relay deliberately does not restate Delivery Route phase. A Delivery-Route-state addition should require a change
 * here only when it changes the Mode/Delivery Route relationship itself.
 */
internal class VexMapsDistributionModeDeliveryRouteConvergenceRelay(
    private val modeProjections:
        StateFlow<VexMapsDistributionModeProjection>,
    private val deliveryRouteProjections:
        StateFlow<VexMapsDeliveryRouteProjection>,
) :
    VexCompoundRelay<
        VexMapsDistributionModeDeliveryRouteInputState,
        VexMapsDistributionModeDeliveryRouteOutput
    >(
        relayRef =
            "vexmaps.distribution.mode-delivery-route",
        initialInputState =
            VexMapsDistributionModeDeliveryRouteInputState(
                mode =
                    modeProjections.value,
                deliveryRoute =
                    deliveryRouteProjections.value,
            ),
        initialOutputState =
            projectModeDeliveryRoute(
                mode =
                    modeProjections.value,
                deliveryRoute =
                    deliveryRouteProjections.value,
            ),
    ) {
    override fun dependencies() =
        vexDependencies<
            VexMapsDistributionModeDeliveryRouteInputState
        > {
            observe(
                dependencyRef =
                    "mode",
                flow =
                    modeProjections,
            ) { projection ->
                VexMapsDistributionModeDeliveryRouteInputState(
                    mode =
                        projection,
                )
            }

            observe(
                dependencyRef =
                    "delivery-route",
                flow =
                    deliveryRouteProjections,
            ) { projection ->
                VexMapsDistributionModeDeliveryRouteInputState(
                    deliveryRoute =
                        projection,
                )
            }
        }

    override fun processStates(
        inputState:
            VexMapsDistributionModeDeliveryRouteInputState,
        currentState:
            VexCompoundState<
                VexMapsDistributionModeDeliveryRouteInputState,
                VexMapsDistributionModeDeliveryRouteOutput
            >,
    ):
        VexCompoundState<
            VexMapsDistributionModeDeliveryRouteInputState,
            VexMapsDistributionModeDeliveryRouteOutput
        > {
        /*
         * Restore the complete current relationship first. Either observed dependency may arrive
         * alone, but the output must always be projected from the latest Mode + Delivery Route pair.
         */
        val previousInput =
            currentState.inputState

        val currentMode =
            inputState.mode
                ?: previousInput.mode
        val currentDeliveryRoute =
            inputState.deliveryRoute
                ?: previousInput.deliveryRoute

        val currentInput =
            VexMapsDistributionModeDeliveryRouteInputState(
                mode =
                    currentMode,
                deliveryRoute =
                    currentDeliveryRoute,
            )

        val mode =
            currentMode
                ?: return currentState.copy(
                    inputState =
                        currentInput,
                )
        val deliveryRoute =
            currentDeliveryRoute
                ?: return currentState.copy(
                    inputState =
                        currentInput,
                )

        /*
         * Project relationship meaning before assembling the next Compound State.
         *
         * projectModeDeliveryRoute is the only feature decision here: coherent, or distribution
         * mode is required and should be requested through the Mode Control boundary.
         */
        val currentOutput =
            projectModeDeliveryRoute(
                mode =
                    mode,
                deliveryRoute =
                    deliveryRoute,
            )

        return currentState.copy(
            inputState =
                currentInput,
            outputState =
                currentOutput,
        )
    }
}

private fun projectModeDeliveryRoute(
    mode:
        VexMapsDistributionModeProjection,
    deliveryRoute:
        VexMapsDeliveryRouteProjection,
): VexMapsDistributionModeDeliveryRouteOutput {
    val hasDeliveryRouteTruth =
        deliveryRoute.state !is
            VexMapsDeliveryRouteState.Empty

    return if (
        hasDeliveryRouteTruth &&
        mode.state is
            VexMapsDistributionModeState.Regular
    ) {
        VexMapsDistributionModeDeliveryRouteOutput(
            coherence =
                VexMapsDistributionModeDeliveryRouteCoherence
                    .NeedsDistributionMode,
            requestedModeControl =
                VexMapsDistributionModeControl
                    .UseDistribution,
        )
    } else {
        VexMapsDistributionModeDeliveryRouteOutput(
            coherence =
                VexMapsDistributionModeDeliveryRouteCoherence
                    .Coherent,
        )
    }
}

// [VXG RealForever]
