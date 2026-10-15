package com.vextreme.vexlife.android.screens.shared.maps.distribution

import com.vextreme.vexlife.android.screens.shared.maps.distribution.context.VexMapsDistributionControlContext
import com.vextreme.vexlife.android.screens.shared.maps.distribution.context.VexMapsDistributionControlContextRelay
import com.vextreme.vexlife.android.screens.shared.maps.distribution.mode.CurrentVexMapsDistributionModeExperience
import com.vextreme.vexlife.android.screens.shared.maps.distribution.mode.VexMapsDistributionModeControl
import com.vextreme.vexlife.android.screens.shared.maps.distribution.mode.VexMapsDistributionModeExperience
import com.vextreme.vexlife.android.screens.shared.maps.distribution.run.CurrentVexMapsDistributionRunExperience
import com.vextreme.vexlife.android.screens.shared.maps.distribution.run.VexMapsDistributionRunControl
import com.vextreme.vexlife.android.screens.shared.maps.distribution.run.VexMapsDistributionRunExperience

/**
 * Feature-local membrane composing two complete lower Experiences plus their relational Control
 * Context.
 *
 * The feedback loop is circular at the system level but not at the ownership level:
 *
 * lower truth -> convergence -> target Control -> target Behavior -> new lower truth -> convergence
 *
 * Feedback occurs only after Relay reduction has completed, so the Relay never recursively sends
 * from its reducer.
 */
internal class VexMapsDistributionExperience(
    internal val mode:
        VexMapsDistributionModeExperience =
        CurrentVexMapsDistributionModeExperience(),
    internal val run:
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

    fun observeDraftStopCount(
        stopCount: Int,
    ): Boolean {
        val admitted =
            run.observeDraftStopCount(
                stopCount,
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
     * Resolve higher-order requests one family at a time.
     *
     * Four passes is deliberately more than the current one-hop Mode reconciliation requires while
     * still failing closed if future relationships accidentally form a non-settling cycle.
     */
    private fun settleFeedback() {
        repeat(MAX_FEEDBACK_PASSES) {
            val requested =
                controlContext
                    .requestedModeControl
                    ?: return

            val before =
                mode.currentProjection

            mode.activate(
                requested,
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

        error(
            "Distribution Control Context did not settle within " +
                "$MAX_FEEDBACK_PASSES feedback passes",
        )
    }
}

private const val MAX_FEEDBACK_PASSES =
    4

// [VXG RealForever]
