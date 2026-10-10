package com.vextreme.vex.android.architecture.relay

import com.vextreme.vex.android.architecture.execution.VexCoroutineHandle
import com.vextreme.vex.android.architecture.execution.VexCoroutineRef
import com.vextreme.vex.android.architecture.execution.VexCoroutineRestartPolicy
import com.vextreme.vex.android.architecture.execution.VexExecutionOwner
import com.vextreme.vex.android.architecture.observability.VexLogRef
import kotlinx.coroutines.flow.collect

interface VexRelayDependency<InputState : VexInputState<InputState>> {
    val coroutineRef: VexCoroutineRef

    fun observe(
        executionOwner: VexExecutionOwner,
        sendInput: (
            inputState: InputState,
            originRef: VexLogRef,
        ) -> Unit,
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
            sendInput: (
                inputState: InputState,
                originRef: VexLogRef,
            ) -> Unit,
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
                        VexLogRef(coroutineRef.value),
                    )
                }
            }
    }

// [VXG RealForever]
