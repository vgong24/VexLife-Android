package com.vextreme.vexlife.android.execution

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow

/**
 * Rightful lifetime owner for one Vex execution tree.
 *
 * The root may be retained by an Android/session/service composition owner, but it exposes no
 * coroutine-launch API. Managed coroutine capability is available only through [VexExecution].
 *
 * Product integration should create a root at the smallest truthful hard lifetime (for example one
 * VexLife session or one Android Service lifetime) and pass that root only to VexExecution
 * subclasses. UI, Experience, Behavior, repository, and ordinary domain classes should not receive
 * the root as a substitute for a CoroutineScope.
 */
class VexExecutionRoot(
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    maxCollectRestarts: Int = 3,
    collectRestartDelayMillis: Long = 250L,
) {
    internal val coroutineLauncher =
        VexCoroutineLauncher(
            dispatcher = dispatcher,
            maxCollectRestarts = maxCollectRestarts,
            collectRestartDelayMillis = collectRestartDelayMillis,
        )

    val executions: StateFlow<Map<VexCoroutineRef, VexCoroutineState>>
        get() = coroutineLauncher.executions

    fun forceStopAll(
        reason: String = "execution root force stopped",
    ) {
        coroutineLauncher.forceStopAll(reason)
    }

    suspend fun joinStopAll(
        reason: String = "execution root join stopped",
    ) {
        coroutineLauncher.joinStopAll(reason)
    }
}

// [VXG RealForever]
