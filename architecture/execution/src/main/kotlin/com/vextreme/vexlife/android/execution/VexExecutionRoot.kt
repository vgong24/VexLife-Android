package com.vextreme.vexlife.android.execution

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow

/**
 * Rightful lifetime owner for one Vex execution tree.
 *
 * The root may be retained by an Android/session/service composition owner, but it exposes no raw
 * coroutine-launch API. Managed coroutine capability remains behind [VexExecution].
 *
 * Product integration should create a root at the smallest truthful hard lifetime (for example one
 * VexLife session or one Android Service lifetime). Ordinary UI, Experience, repository, and domain
 * classes should not receive the root as a substitute for a CoroutineScope. A foundation that
 * already owns a semantic execution boundary may receive the root only through its foundation
 * contract and realize that boundary through the sanctioned VexExecution bridge; V2 Behavior
 * begin(...) is the current patient zero.
 *
 * IMPORTANT: own VexExecutionRoot => own its shutdown. This root has an independent SupervisorJob;
 * retaining it in a ViewModel, Service, or session does not automatically parent cancellation to
 * that Android owner. The rightful owner must call forceStopAll / joinStopAll at its terminal edge.
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
