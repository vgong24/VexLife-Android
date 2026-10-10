package com.vextreme.vex.android.architecture.relay

import com.vextreme.vex.android.architecture.execution.VexCoroutineHandle
import com.vextreme.vex.android.architecture.execution.VexCoroutineRef
import com.vextreme.vex.android.architecture.execution.VexCoroutineRestartPolicy
import com.vextreme.vex.android.architecture.execution.VexExecutionOwner
import com.vextreme.vex.android.architecture.observability.VexLogRef
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.collect

class VexRelayDependency<InputState : VexInputState<InputState>> internal constructor(
    val coroutineRef: VexCoroutineRef,
    val sourceRelay: VexObservableRelay<*>,
    private val observeBlock: (
        executionOwner: VexExecutionOwner,
        activationGate: Deferred<Unit>,
        sendInput: (
            inputState: InputState,
            originRef: VexLogRef,
        ) -> Unit,
    ) -> VexCoroutineHandle,
) {
    internal fun observe(
        executionOwner: VexExecutionOwner,
        activationGate: Deferred<Unit>,
        sendInput: (
            inputState: InputState,
            originRef: VexLogRef,
        ) -> Unit,
    ): VexCoroutineHandle =
        observeBlock(
            executionOwner,
            activationGate,
            sendInput,
        )
}

/**
 * Declares one visible child-Relay relationship.
 *
 * [toInputState] is a bounded projection from a defensive child snapshot to one parent input patch.
 * It must not mutate Relay owners or dispatch external effects; doing so would create a causal edge
 * that is absent from the declared dependency topology.
 */
fun <
    ChildState : VexState<ChildState>,
    InputState : VexInputState<InputState>,
> vexRelayDependency(
    coroutineRef: VexCoroutineRef,
    relay: VexObservableRelay<ChildState>,
    restartPolicy: VexCoroutineRestartPolicy = VexCoroutineRestartPolicy.Never,
    toInputState: (ChildState) -> InputState,
): VexRelayDependency<InputState> =
    VexRelayDependency(
        coroutineRef = coroutineRef,
        sourceRelay = relay,
    ) { executionOwner, activationGate, sendInput ->
        executionOwner.launchCoroutine(
            coroutineRef = coroutineRef,
            restartPolicy = restartPolicy,
        ) {
            activationGate.await()

            relay.currentState.collect { childState ->
                sendInput(
                    toInputState(
                        childState.deepCopy(),
                    ).deepCopy(),
                    VexLogRef(relay.relayRef.value),
                )
            }
        }
    }

// [VXG RealForever]
