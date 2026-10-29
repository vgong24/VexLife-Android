package com.vextreme.vexlife.android.execution

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow

/**
 * Rightful lifetime owner for one Vex execution tree.
 *
 * The root owns shutdown and exposes current active execution custody by exact per-launch InstanceId.
 *
 * Exact terminal state remains on each returned VexExecution.Handle; this root projection is not an
 * execution-history ledger.
 */
class VexExecutionRoot(
    dispatcher:
        CoroutineDispatcher =
        Dispatchers.Default,
    maxCollectRestarts:
        Int =
        3,
    collectRestartDelayMillis:
        Long =
        250L,
) {
    internal val coroutineLauncher =
        VexCoroutineLauncher(
            dispatcher =
                dispatcher,
            maxCollectRestarts =
                maxCollectRestarts,
            collectRestartDelayMillis =
                collectRestartDelayMillis,
        )

    val executions:
        StateFlow<
            Map<
                VexExecution.InstanceId,
                VexExecution.State
            >
        >
        get() =
            coroutineLauncher.executions

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
