package com.vextreme.vexlife.android.screens.shared.maps.distribution.session

import com.vextreme.vexlife.android.execution.VexExecution
import com.vextreme.vexlife.android.execution.VexExecutionRoot

/**
 * Managed execution for sustained Distribution relationship observation.
 *
 * Session still owns semantic meaning and provider failure/retry truth. This boundary owns only the
 * maintained coroutine custody needed to observe the two pure convergence outputs for the process-
 * lifetime Distribution session.
 */
internal class VexMapsDistributionExecution(
    root: VexExecutionRoot,
    private val session:
        VexMapsDistributionSession,
) :
    VexExecution(
        root,
    ) {
    fun start() {
        launchForCollect(
            source =
                session::modeRunOutputsForExecution,
        ) { output ->
            session.acceptModeRunOutput(
                output,
            )
        }

        launchForCollect(
            source =
                session::navigationOutputsForExecution,
        ) { output ->
            session.acceptNavigationOutput(
                output,
            )
        }
    }
}

// [VXG RealForever]
