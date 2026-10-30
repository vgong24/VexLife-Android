package com.vextreme.vexlife.android.execution

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow

/**
 * Rightful lifetime owner for one Vex execution tree.
 *
 * The root owns terminal shutdown and exposes only current active execution custody. Terminal
 * history belongs to each Handle or a separate diagnostic/event surface; this StateFlow is not an
 * unbounded execution ledger.
 */
class VexExecutionRoot(
    dispatcher:
        CoroutineDispatcher =
        Dispatchers.Default,
) {
    internal val coroutineLauncher =
        VexCoroutineLauncher(
            dispatcher =
                dispatcher,
        )

    val activeExecutions:
        StateFlow<
            Map<
                VexExecution.InstanceId,
                VexExecution.State
            >
        >
        get() =
            coroutineLauncher.activeExecutions

    fun forceStopAll(
        reason:
            String =
            "execution root force stopped",
    ) {
        coroutineLauncher.forceStopAll(
            reason,
        )
    }

    suspend fun joinStopAll(
        reason:
            String =
            "execution root join stopped",
    ) {
        coroutineLauncher.joinStopAll(
            reason,
        )
    }
}

// [VXG RealForever]
