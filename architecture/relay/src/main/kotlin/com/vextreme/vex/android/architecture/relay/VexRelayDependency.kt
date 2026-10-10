package com.vextreme.vex.android.architecture.relay

import com.vextreme.vex.android.architecture.execution.VexCoroutineHandle
import com.vextreme.vex.android.architecture.execution.VexCoroutineRef
import com.vextreme.vex.android.architecture.execution.VexCoroutineRestartPolicy
import com.vextreme.vex.android.architecture.execution.VexExecutionOwner
import kotlinx.coroutines.flow.collect

/**
 * One observable child Relay mapped into one partial Compound InputState patch.
 *
 * The child keeps ownership of its own state. The execution owner owns the named collector
 * lifetime. The Compound Relay only receives defensive snapshots of the child's current truth.
 */
interface VexRelayDependency<InputState : VexInputState<InputState>> {
    val coroutineRef: VexCoroutineRef

    fun observe(
        executionOwner: VexExecutionOwner,
        sendInput: (InputState) -> Unit,
    ): VexCoroutineHandle
}

fun <
    ChildState : VexState<ChildState>,
    InputState : VexInputState<InputState>,
> vexRelayDependency(
    coroutineRef: VexCoroutineRef,
    relay: VexObservableRelay<ChildState>,
    restartPolicy: VexCoroutineRestartPolicy = VexCoroutineRestartPolicy.Never,
    toInputState: (ChildState) -> InputState,
): VexRelayDependency<InputState> =
    object : VexRelayDependency<InputState> {
        override val coroutineRef: VexCoroutineRef =
            coroutineRef

        override fun observe(
            executionOwner: VexExecutionOwner,
            sendInput: (InputState) -> Unit,
        ): VexCoroutineHandle =
            executionOwner.launchCoroutine(
                coroutineRef = coroutineRef,
                restartPolicy = restartPolicy,
            ) {
                relay.currentState.collect { childState ->
                    sendInput(
                        toInputState(
                            childState.deepCopy(),
                        ).deepCopy(),
                    )
                }
            }
    }

// [VXG RealForever]
