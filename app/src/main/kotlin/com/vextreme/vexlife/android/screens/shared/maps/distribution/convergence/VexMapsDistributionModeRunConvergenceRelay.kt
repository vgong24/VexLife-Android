package com.vextreme.vexlife.android.screens.shared.maps.distribution.convergence

import com.vextreme.vexlife.android.relay.VexCompoundRelay
import com.vextreme.vexlife.android.relay.VexCompoundState
import com.vextreme.vexlife.android.relay.VexInputState
import com.vextreme.vexlife.android.relay.VexOutputState
import com.vextreme.vexlife.android.relay.vexDependencies
import com.vextreme.vexlife.android.screens.shared.maps.distribution.mode.VexMapsDistributionModeControl
import com.vextreme.vexlife.android.screens.shared.maps.distribution.mode.VexMapsDistributionModeProjection
import com.vextreme.vexlife.android.screens.shared.maps.distribution.mode.VexMapsDistributionModeState
import com.vextreme.vexlife.android.screens.shared.maps.distribution.run.VexMapsDistributionRunProjection
import com.vextreme.vexlife.android.screens.shared.maps.distribution.run.VexMapsDistributionRunState
import kotlinx.coroutines.flow.StateFlow

internal data class VexMapsDistributionModeRunInputState(
    val mode:
        VexMapsDistributionModeProjection? =
        null,
    val run:
        VexMapsDistributionRunProjection? =
        null,
) :
    VexInputState<VexMapsDistributionModeRunInputState> {
    override fun deepCopy():
        VexMapsDistributionModeRunInputState =
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

internal enum class VexMapsDistributionModeRunCoherence {
    Coherent,
    NeedsDistributionMode,
}

internal data class VexMapsDistributionModeRunOutput(
    val coherence:
        VexMapsDistributionModeRunCoherence,
    val requestedModeControl:
        VexMapsDistributionModeControl? =
        null,
) :
    VexOutputState<VexMapsDistributionModeRunOutput> {
    override fun deepCopy():
        VexMapsDistributionModeRunOutput =
        copy()
}

/**
 * One higher-order question:
 *
 * Is the independently-owned Maps Mode coherent with the independently-owned Distribution Run?
 *
 * This Relay deliberately does not restate Run phase. A Run-state addition should require a change
 * here only when it changes the Mode/Run relationship itself.
 */
internal class VexMapsDistributionModeRunConvergenceRelay(
    private val modeProjections:
        StateFlow<VexMapsDistributionModeProjection>,
    private val runProjections:
        StateFlow<VexMapsDistributionRunProjection>,
) :
    VexCompoundRelay<
        VexMapsDistributionModeRunInputState,
        VexMapsDistributionModeRunOutput
    >(
        relayRef =
            "vexmaps.distribution.mode-run",
        initialInputState =
            VexMapsDistributionModeRunInputState(
                mode =
                    modeProjections.value,
                run =
                    runProjections.value,
            ),
        initialOutputState =
            projectModeRun(
                mode =
                    modeProjections.value,
                run =
                    runProjections.value,
            ),
    ) {
    override fun dependencies() =
        vexDependencies<
            VexMapsDistributionModeRunInputState
        > {
            observe(
                dependencyRef =
                    "mode",
                flow =
                    modeProjections,
            ) { projection ->
                VexMapsDistributionModeRunInputState(
                    mode =
                        projection,
                )
            }

            observe(
                dependencyRef =
                    "run",
                flow =
                    runProjections,
            ) { projection ->
                VexMapsDistributionModeRunInputState(
                    run =
                        projection,
                )
            }
        }

    override fun processStates(
        inputState:
            VexMapsDistributionModeRunInputState,
        currentState:
            VexCompoundState<
                VexMapsDistributionModeRunInputState,
                VexMapsDistributionModeRunOutput
            >,
    ):
        VexCompoundState<
            VexMapsDistributionModeRunInputState,
            VexMapsDistributionModeRunOutput
        > {
        val previous =
            currentState.inputState
        val coherent =
            VexMapsDistributionModeRunInputState(
                mode =
                    inputState.mode
                        ?: previous.mode,
                run =
                    inputState.run
                        ?: previous.run,
            )
        val mode =
            coherent.mode
                ?: return currentState.copy(
                    inputState =
                        coherent,
                )
        val run =
            coherent.run
                ?: return currentState.copy(
                    inputState =
                        coherent,
                )

        return currentState.copy(
            inputState =
                coherent,
            outputState =
                projectModeRun(
                    mode =
                        mode,
                    run =
                        run,
                ),
        )
    }
}

private fun projectModeRun(
    mode:
        VexMapsDistributionModeProjection,
    run:
        VexMapsDistributionRunProjection,
): VexMapsDistributionModeRunOutput {
    val hasDistributionTruth =
        run.state !is
            VexMapsDistributionRunState.Empty

    return if (
        hasDistributionTruth &&
        mode.state is
            VexMapsDistributionModeState.Regular
    ) {
        VexMapsDistributionModeRunOutput(
            coherence =
                VexMapsDistributionModeRunCoherence
                    .NeedsDistributionMode,
            requestedModeControl =
                VexMapsDistributionModeControl
                    .ToggleDistribution,
        )
    } else {
        VexMapsDistributionModeRunOutput(
            coherence =
                VexMapsDistributionModeRunCoherence
                    .Coherent,
        )
    }
}

// [VXG RealForever]
