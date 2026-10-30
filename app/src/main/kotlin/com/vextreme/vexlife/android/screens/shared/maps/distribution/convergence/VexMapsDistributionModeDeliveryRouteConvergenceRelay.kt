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
 * Inputs for the feature-home question "Do Distribution Mode and the Delivery Route agree?"
 *
 * mode
 *     current Distribution Mode projection;
 *
 * deliveryRoute
 *     current Delivery Route projection.
 *
 * These are independently-owned truths. Their combination may require a bounded Mode Control
 * request, but neither input becomes subordinate to this Relay.
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
    override fun latest(
        incomingInputState:
            VexMapsDistributionModeDeliveryRouteInputState,
    ):
        VexMapsDistributionModeDeliveryRouteInputState {
        val latestMode =
            incomingInputState.mode
                ?: mode
        val latestDeliveryRoute =
            incomingInputState.deliveryRoute
                ?: deliveryRoute

        return VexMapsDistributionModeDeliveryRouteInputState(
            mode =
                latestMode?.copy(
                    controls =
                        latestMode.controls.toMap(),
                ),
            deliveryRoute =
                latestDeliveryRoute?.copy(
                    controls =
                        latestDeliveryRoute.controls.toMap(),
                ),
        )
    }

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
 * One feature-owned relationship from the VexMaps Distribution question map:
 *
 * "DO DISTRIBUTION MODE AND THE DELIVERY ROUTE AGREE?"
 *
 * Canonical feature context:
 * app/.../screens/shared/maps/distribution/README.md
 *
 * Ownership remains separate:
 *
 * DISTRIBUTION_MODE != DELIVERY_ROUTE
 *
 * This Relay observes those two independently complete truths and projects only their coherence.
 * When a non-empty Delivery Route exists while Mode is Regular, the relationship may request
 * VexMapsDistributionModeControl.UseDistribution; it does not choose Mode's Act/Effect itself.
 *
 * Focused proof example:
 * VexMapsDistributionModeDeliveryRouteConvergenceRelayTest
 *     #nonEmptyDeliveryRouteRequestsOnlyModeReconciliationThenBecomesCoherent
 *
 * That test is evidence for this relationship only. The feature home explicitly records that the
 * full Delivery Route human walk is still separate proof debt.
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

    override fun process(
        latestStateInput:
            VexMapsDistributionModeDeliveryRouteInputState,
        currentState:
            VexCompoundState<
                VexMapsDistributionModeDeliveryRouteInputState,
                VexMapsDistributionModeDeliveryRouteOutput
            >,
    ):
        VexMapsDistributionModeDeliveryRouteOutput {
        val mode =
            latestStateInput.mode
                ?: return currentState.outputState.deepCopy()
        val deliveryRoute =
            latestStateInput.deliveryRoute
                ?: return currentState.outputState.deepCopy()

        return projectModeDeliveryRoute(
            mode =
                mode,
            deliveryRoute =
                deliveryRoute,
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
