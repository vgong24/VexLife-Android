package com.vextreme.vex.android.architecture.relay

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Relay for one organized component relationship with a separate input family and output family.
 *
 * Incoming InputState is allowed to be a partial patch (for example only screen != null).
 * [processStates] receives both that incoming patch and one deep-copied current compound state.
 * The subclass merges the patch with current input, derives output, and returns the next totality.
 */
abstract class VexCompoundRelay<
    InputState : VexInputState<InputState>,
    OutputState : VexOutputState<OutputState>,
>(
    initialInputState: InputState,
    initialOutputState: OutputState,
) : VexObservableRelay<VexCompoundState<InputState, OutputState>> {
    private val lock = Any()
    private var processing = false

    private val mutableCurrentState =
        MutableStateFlow(
            VexCompoundState(
                inputState = initialInputState.deepCopy(),
                outputState = initialOutputState.deepCopy(),
            ),
        )

    override val currentState: StateFlow<VexCompoundState<InputState, OutputState>> =
        DefensiveVexStateFlow(mutableCurrentState.asStateFlow())

    /**
     * Component-specific reconciliation.
     *
     * Typical pattern:
     * 1. name currentInputState once;
     * 2. resolve each incoming nullable field against currentInputState;
     * 3. form one next InputState;
     * 4. process component logic;
     * 5. form one OutputState;
     * 6. return currentState.copy(inputState = ..., outputState = ...).
     */
    protected abstract fun processStates(
        inputState: InputState,
        currentState: VexCompoundState<InputState, OutputState>,
    ): VexCompoundState<InputState, OutputState>

    /**
     * Send a partial input family update.
     *
     * All senders share one serialization boundary, so screen/event/item updates cannot overwrite
     * sibling fields when processStates follows the merge-with-current contract.
     */
    protected fun send(
        inputState: InputState,
    ): VexCompoundState<InputState, OutputState> =
        synchronized(lock) {
            check(!processing) {
                "VexCompoundRelay cannot recursively send state from processStates"
            }

            val currentState = mutableCurrentState.value.deepCopy()

            processing = true
            try {
                val processed =
                    processStates(
                        inputState.deepCopy(),
                        currentState.deepCopy(),
                    ).deepCopy()

                val meaningChanged =
                    processed.inputState != currentState.inputState ||
                        processed.outputState != currentState.outputState

                if (!meaningChanged) {
                    return@synchronized currentState.deepCopy()
                }

                val nextState =
                    VexCompoundState(
                        inputState = processed.inputState.deepCopy(),
                        outputState = processed.outputState.deepCopy(),
                        revision = currentState.revision + 1L,
                    )

                mutableCurrentState.value = nextState.deepCopy()
                nextState.deepCopy()
            } finally {
                processing = false
            }
        }

    /**
     * Make another Relay one input source of this compound Relay.
     *
     * The caller supplies the CoroutineScope so Relay composition never invents a hidden lifetime.
     * StateFlow here means "latest child truth". It is not an event ledger.
     */
    protected fun <ChildState : VexState<ChildState>> bindRelay(
        scope: CoroutineScope,
        relay: VexObservableRelay<ChildState>,
        toInputState: (ChildState) -> InputState,
    ): Job =
        scope.launch {
            relay.currentState.collect { childState ->
                send(
                    toInputState(
                        childState.deepCopy(),
                    ).deepCopy(),
                )
            }
        }
}

// [VXG RealForever]
