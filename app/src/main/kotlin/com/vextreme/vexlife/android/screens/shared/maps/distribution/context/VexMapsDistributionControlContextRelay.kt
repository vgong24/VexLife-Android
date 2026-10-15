package com.vextreme.vexlife.android.screens.shared.maps.distribution.context

import com.vextreme.vexlife.android.screens.shared.maps.distribution.mode.VexMapsDistributionModeProjection
import com.vextreme.vexlife.android.screens.shared.maps.distribution.run.VexMapsDistributionRunProjection
import com.vextreme.vexlife.android.relay.VexCompoundRelay
import com.vextreme.vexlife.android.relay.VexCompoundState

/**
 * Pure higher-order convergence over complete Mode and Run Experience projections.
 *
 * The Relay may request a bounded Control, but it never activates another family or recursively
 * sends from [processStates].
 */
internal class VexMapsDistributionControlContextRelay(
    initialMode:
        VexMapsDistributionModeProjection,
    initialRun:
        VexMapsDistributionRunProjection,
) :
    VexCompoundRelay<
        VexMapsDistributionControlContextInputState,
        VexMapsDistributionControlContext
    >(
        relayRef =
            "vexmaps.distribution.control-context",
        initialInputState =
            VexMapsDistributionControlContextInputState(
                mode = initialMode,
                run = initialRun,
            ),
        initialOutputState =
            projectVexMapsDistributionControlContext(
                VexMapsDistributionControlContextInputState(
                    mode = initialMode,
                    run = initialRun,
                ),
            ),
    ) {
    fun modeChanged(
        projection:
            VexMapsDistributionModeProjection,
    ) {
        send(
            VexMapsDistributionControlContextInputState(
                mode = projection,
                run = null,
            ),
        )
    }

    fun runChanged(
        projection:
            VexMapsDistributionRunProjection,
    ) {
        send(
            VexMapsDistributionControlContextInputState(
                mode = null,
                run = projection,
            ),
        )
    }

    override fun processStates(
        inputState:
            VexMapsDistributionControlContextInputState,
        currentState:
            VexCompoundState<
                VexMapsDistributionControlContextInputState,
                VexMapsDistributionControlContext
            >,
    ):
        VexCompoundState<
            VexMapsDistributionControlContextInputState,
            VexMapsDistributionControlContext
        > {
        val previous =
            currentState.inputState

        val coherent =
            VexMapsDistributionControlContextInputState(
                mode =
                    inputState.mode
                        ?: previous.mode,
                run =
                    inputState.run
                        ?: previous.run,
            )

        return currentState.copy(
            inputState =
                coherent,
            outputState =
                projectVexMapsDistributionControlContext(
                    coherent,
                ),
        )
    }
}

// [VXG RealForever]
