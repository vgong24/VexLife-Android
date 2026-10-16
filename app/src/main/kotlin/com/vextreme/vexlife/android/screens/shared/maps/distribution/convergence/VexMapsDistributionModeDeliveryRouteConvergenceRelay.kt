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
    val coherence:
        VexMapsDistributionModeDeliveryRouteCoherence,
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
        val previous =
            currentState.inputState
        val coherent =
            VexMapsDistributionModeDeliveryRouteInputState(
                mode =
                    inputState.mode
                        ?: previous.mode,
                deliveryRoute =
                    inputState.deliveryRoute
                        ?: previous.deliveryRoute,
            )
        val mode =
            coherent.mode
                ?: return currentState.copy(
                    inputState =
                        coherent,
                )
        val deliveryRoute =
            coherent.deliveryRoute
                ?: return currentState.copy(
                    inputState =
                        coherent,
                )

        return currentState.copy(
            inputState =
                coherent,
            outputState =
                projectModeDeliveryRoute(
                    mode =
                        mode,
                    deliveryRoute =
                        deliveryRoute,
                ),
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
