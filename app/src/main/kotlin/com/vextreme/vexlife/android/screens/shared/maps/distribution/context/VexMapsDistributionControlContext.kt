package com.vextreme.vexlife.android.screens.shared.maps.distribution.context

import com.vextreme.vexlife.android.screens.shared.maps.distribution.mode.VexMapsDistributionModeControl
import com.vextreme.vexlife.android.screens.shared.maps.distribution.mode.VexMapsDistributionModeProjection
import com.vextreme.vexlife.android.screens.shared.maps.distribution.mode.VexMapsDistributionModeState
import com.vextreme.vexlife.android.screens.shared.maps.distribution.run.VexMapsDistributionRunProjection
import com.vextreme.vexlife.android.screens.shared.maps.distribution.run.VexMapsDistributionRunState
import com.vextreme.vexlife.android.relay.VexInputState
import com.vextreme.vexlife.android.relay.VexOutputState

/**
 * Higher-order context formed from complete lower Experience projections.
 *
 * This is the feature-local "muscle" over two independent "tissues". It derives relational meaning
 * but does not acquire either family's State or Effect ownership.
 */
internal data class VexMapsDistributionControlContextInputState(
    val mode:
        VexMapsDistributionModeProjection?,
    val run:
        VexMapsDistributionRunProjection?,
) :
    VexInputState<VexMapsDistributionControlContextInputState> {
    override fun deepCopy():
        VexMapsDistributionControlContextInputState =
        copy(
            mode =
                mode?.copy(
                    controls =
                        mode.controls.toMap(),
                ),
            run =
                run?.copy(
                    controls =
                        run.controls.toMap(),
                ),
        )
}

internal enum class VexMapsDistributionControlLayer {
    Unavailable,
    Regular,
    ReconcilingDistributionMode,
    DistributionEmpty,
    DistributionDraft,
    DistributionReady,
    DistributionRunning,
    DistributionAtStop,
    DistributionCompleted,
}

/**
 * Relational answer exposed by the Distribution Control Context.
 *
 * [requestedModeControl] is a bounded request back toward the Mode family. A separate feedback
 * boundary must activate that Control through the Mode Experience; this output never carries or
 * executes a foreign Effect.
 */
internal data class VexMapsDistributionControlContext(
    val layer:
        VexMapsDistributionControlLayer,
    val requestedModeControl:
        VexMapsDistributionModeControl? =
        null,
) :
    VexOutputState<VexMapsDistributionControlContext> {
    override fun deepCopy():
        VexMapsDistributionControlContext =
        copy()
}

internal fun projectVexMapsDistributionControlContext(
    input:
        VexMapsDistributionControlContextInputState,
): VexMapsDistributionControlContext {
    val mode =
        input.mode
            ?: return VexMapsDistributionControlContext(
                layer =
                    VexMapsDistributionControlLayer.Unavailable,
            )
    val run =
        input.run
            ?: return VexMapsDistributionControlContext(
                layer =
                    VexMapsDistributionControlLayer.Unavailable,
            )

    val runState =
        run.state
    val modeState =
        mode.state

    if (
        runState !is
            VexMapsDistributionRunState.Empty &&
        modeState is
            VexMapsDistributionModeState.Regular
    ) {
        return VexMapsDistributionControlContext(
            layer =
                VexMapsDistributionControlLayer
                    .ReconcilingDistributionMode,
            requestedModeControl =
                VexMapsDistributionModeControl
                    .ToggleDistribution,
        )
    }

    if (
        modeState is
            VexMapsDistributionModeState.Regular
    ) {
        return VexMapsDistributionControlContext(
            layer =
                VexMapsDistributionControlLayer.Regular,
        )
    }

    return VexMapsDistributionControlContext(
        layer =
            when (
                runState
            ) {
                VexMapsDistributionRunState.Empty ->
                    VexMapsDistributionControlLayer
                        .DistributionEmpty

                is VexMapsDistributionRunState.Draft ->
                    VexMapsDistributionControlLayer
                        .DistributionDraft

                is VexMapsDistributionRunState.Ready ->
                    VexMapsDistributionControlLayer
                        .DistributionReady

                is VexMapsDistributionRunState.Running ->
                    VexMapsDistributionControlLayer
                        .DistributionRunning

                is VexMapsDistributionRunState.AtStop ->
                    VexMapsDistributionControlLayer
                        .DistributionAtStop

                is VexMapsDistributionRunState.Completed ->
                    VexMapsDistributionControlLayer
                        .DistributionCompleted
            },
    )
}

// [VXG RealForever]
