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
 * Product context for keeping Maps honest while the person is doing distribution work.
 *
 * A person who has an active Delivery Route should not unknowingly be carrying out that route while
 * Maps still behaves like ordinary Regular mode. At the same time, Delivery Route must not take
 * ownership of Maps mode just because it needs Distribution behavior.
 *
 * mode
 *     says how Maps is currently operating for the person;
 *
 * deliveryRoute
 *     says whether the person currently has admitted delivery work that needs Distribution context.
 *
 * When those truths disagree, convergence asks the Mode family to use Distribution through its
 * existing Control boundary. Mode still decides what that request means from its own current State.
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
    /** Whether the person's current Maps mode and current delivery work agree. */
    val coherence:
        VexMapsDistributionModeDeliveryRouteCoherence,

    /**
     * The smallest request needed to repair that product mismatch, when one exists.
     *
     * Asking through Mode's Control preserves the human meaning of the request and lets Mode decide
     * the lawful Act/Effect from its own current truth instead of Delivery Route changing Mode for it.
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
         * Judge the person's delivery context from the current Mode and Delivery Route together.
         *
         * Either can change first; the product should never temporarily treat a stale pairing as
         * coherent merely because the other observation arrived on a different update.
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
         * Answer the only feature question this Relay owns:
         *
         * "Is the person's current Maps mode appropriate for the delivery work they are carrying?"
         * If not, request the rightful Mode Control and let that family own the correction.
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
