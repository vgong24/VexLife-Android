package com.vextreme.vex.android.architecture.relay

import com.vextreme.vex.android.architecture.execution.VexExecutionHandle
import com.vextreme.vex.android.architecture.execution.VexExecutionName
import com.vextreme.vex.android.architecture.execution.VexExecutionScope
import com.vextreme.vex.android.architecture.execution.VexRestartPolicy
import kotlinx.coroutines.flow.collect

/**
 * One observable child Relay mapped into one partial Compound InputState patch.
 *
 * The child keeps ownership of its own state. The execution architecture owns the named collector
 * lifetime. The Compound Relay only receives defensive snapshots of the child's current truth.
 */
interface VexRelayDependency<InputState : VexInputState<InputState>> {
    val executionName: VexExecutionName

    fun observe(
        executionScope: VexExecutionScope,
        sendInput: (InputState) -> Unit,
    ): VexExecutionHandle
}

fun <
    ChildState : VexState<ChildState>,
    InputState : VexInputState<InputState>,
> vexRelayDependency(
    executionName: VexExecutionName,
    relay: VexObservableRelay<ChildState>,
    restartPolicy: VexRestartPolicy = VexRestartPolicy.Never,
    toInputState: (ChildState) -> InputState,
): VexRelayDependency<InputState> =
    object : VexRelayDependency<InputState> {
        override val executionName: VexExecutionName =
            executionName

        override fun observe(
            executionScope: VexExecutionScope,
            sendInput: (InputState) -> Unit,
        ): VexExecutionHandle =
            executionScope.launch(
                executionName = executionName,
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
