package com.vextreme.vexlife.android.execution

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow

/**
 * Rightful lifetime owner for one Vex execution tree.
 *
 * The root owns shutdown and exposes observable execution lifecycle State by exact per-launch
 * [VexExecution.Instance].
 *
 * This `executions` projection is not active coroutine custody and is not the maintained
 * RelationshipId map used for Earliest / Latest / Queued overlap handling. Terminal State may remain
 * observable here after active custody and an empty RelationshipId entry have been cleaned up.
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
                VexExecution.Instance,
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
