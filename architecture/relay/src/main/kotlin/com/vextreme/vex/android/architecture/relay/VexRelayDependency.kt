package com.vextreme.vex.android.architecture.relay

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * One observable child Relay mapped into one partial Compound InputState patch.
 *
 * The child keeps ownership of its own state. The Compound Relay only receives defensive snapshots
 * of the child's current truth.
 */
interface VexRelayDependency<InputState : VexInputState<InputState>> {
    fun observe(
        scope: CoroutineScope,
        sendInput: (InputState) -> Unit,
    ): Job
}

fun <
    ChildState : VexState<ChildState>,
    InputState : VexInputState<InputState>,
> vexRelayDependency(
    relay: VexObservableRelay<ChildState>,
    toInputState: (ChildState) -> InputState,
): VexRelayDependency<InputState> =
    object : VexRelayDependency<InputState> {
        override fun observe(
            scope: CoroutineScope,
            sendInput: (InputState) -> Unit,
        ): Job =
            scope.launch {
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
