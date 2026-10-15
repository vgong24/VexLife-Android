package com.vextreme.vexlife.android.screens.shared.maps.distribution

import com.vextreme.vexlife.android.screens.shared.maps.distribution.context.VexMapsDistributionControlContext
import com.vextreme.vexlife.android.screens.shared.maps.distribution.context.VexMapsDistributionControlContextRelay
import com.vextreme.vexlife.android.screens.shared.maps.distribution.context.VexMapsDistributionControlRequest
import com.vextreme.vexlife.android.screens.shared.maps.distribution.mode.CurrentVexMapsDistributionModeExperience
import com.vextreme.vexlife.android.screens.shared.maps.distribution.mode.VexMapsDistributionModeControl
import com.vextreme.vexlife.android.screens.shared.maps.distribution.mode.VexMapsDistributionModeExperience
import com.vextreme.vexlife.android.screens.shared.maps.distribution.mode.VexMapsDistributionModeProjection
import com.vextreme.vexlife.android.screens.shared.maps.distribution.run.CurrentVexMapsDistributionRunExperience
import com.vextreme.vexlife.android.screens.shared.maps.distribution.run.VexMapsDistributionRunControl
import com.vextreme.vexlife.android.screens.shared.maps.distribution.run.VexMapsDistributionRunExperience
import com.vextreme.vexlife.android.screens.shared.maps.distribution.run.VexMapsDistributionRunProjection
import kotlinx.coroutines.flow.StateFlow

/**
 * Feature-local membrane composing complete lower Experiences plus their relational Control Context.
 *
 * The feedback loop is circular at the system level but not at the ownership level:
 *
 * lower truth -> convergence -> target Control -> target Behavior -> new lower truth -> convergence
 *
 * Feedback occurs only after Relay reduction has completed, so the Relay never recursively sends
 * from its reducer. Lower Experiences remain private so callers cannot bypass this membrane and make
 * the higher-order Context stale accidentally.
 */
internal class VexMapsDistributionExperience(
    private val mode:
        VexMapsDistributionModeExperience =
        CurrentVexMapsDistributionModeExperience(),
    private val run:
        VexMapsDistributionRunExperience =
        CurrentVexMapsDistributionRunExperience(),
) {
    private val controlContextRelay =
        VexMapsDistributionControlContextRelay(
            initialMode =
                mode.currentProjection,
            initialRun =
                run.currentProjection,
        )

    val modeProjection:
        VexMapsDistributionModeProjection
        get() =
            mode.currentProjection

    val modeProjections:
        StateFlow<VexMapsDistributionModeProjection>
        get() =
            mode.projections

    val runProjection:
        VexMapsDistributionRunProjection
        get() =
            run.currentProjection

    val runProjections:
        StateFlow<VexMapsDistributionRunProjection>
        get() =
            run.projections

    val controlContext:
        VexMapsDistributionControlContext
        get() =
            controlContextRelay.currentOutput

    fun activateMode(
        control:
            VexMapsDistributionModeControl,
    ) {
        mode.activate(
            control,
        )
        controlContextRelay.modeChanged(
            mode.currentProjection,
        )
        settleFeedback()
    }

    fun activateRun(
        control:
            VexMapsDistributionRunControl,
    ) {
        run.activate(
            control,
        )
        controlContextRelay.runChanged(
            run.currentProjection,
        )
        settleFeedback()
    }

    fun observeDraft(
        stopCount: Int,
        returnToStart: Boolean,
    ): Boolean {
        val admitted =
            run.observeDraft(
                stopCount = stopCount,
                returnToStart = returnToStart,
            )

        if (!admitted) {
            return false
        }

        controlContextRelay.runChanged(
            run.currentProjection,
        )
        settleFeedback()
        return true
    }

    /**
     * Settle relational feedback through target Controls only.
     *
     * The current feature graph is finite. Repeating the same complete feedback key therefore means
     * the graph has formed a semantic cycle rather than made progress, and fails closed without an
     * arbitrary pass-count policy.
     */
    private fun settleFeedback() {
        val visited =
            mutableSetOf<
                DistributionFeedbackKey
            >()

        while (true) {
            val requested =
                controlContext
                    .requestedControl
                    ?: return

            val key =
                DistributionFeedbackKey(
                    modeState =
                        mode.currentProjection.state,
                    runState =
                        run.currentProjection.state,
                    request =
                        requested,
                )

            check(
                visited.add(
                    key,
                ),
            ) {
                "Distribution Control Context entered a non-settling semantic feedback cycle: $key"
            }

            when (
                requested
            ) {
                is VexMapsDistributionControlRequest.Mode -> {
                    val before =
                        mode.currentProjection

                    mode.activate(
                        requested.control,
                    )

                    val after =
                        mode.currentProjection

                    check(after != before) {
                        "Distribution Control Context requested a Mode Control that made no semantic progress"
                    }

                    controlContextRelay.modeChanged(
                        after,
                    )
                }

                is VexMapsDistributionControlRequest.Run -> {
                    val before =
                        run.currentProjection

                    run.activate(
                        requested.control,
                    )

                    val after =
                        run.currentProjection

                    check(after != before) {
                        "Distribution Control Context requested a Run Control that made no semantic progress"
                    }

                    controlContextRelay.runChanged(
                        after,
                    )
                }
            }
        }
    }
}

private data class DistributionFeedbackKey(
    val modeState: Any,
    val runState: Any,
    val request:
        VexMapsDistributionControlRequest,
)

// [VXG RealForever]
