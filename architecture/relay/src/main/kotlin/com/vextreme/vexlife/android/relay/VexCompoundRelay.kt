package com.vextreme.vexlife.android.relay

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

abstract class VexCompoundRelay<
    InputState : VexInputState<InputState>,
    OutputState : VexOutputState<OutputState>,
>(
    val relayRef: String,
    initialInputState: InputState,
    initialOutputState: OutputState,
) : VexObservableRelay<VexCompoundState<InputState, OutputState>> {
    private val lock = Any()
    private var processing = false

    init {
        require(relayRef.isNotBlank()) { "relayRef must not be blank" }
    }

    private val mutableCurrentState =
        MutableStateFlow(
            VexCompoundState(
                inputState = initialInputState.deepCopy(),
                outputState = initialOutputState.deepCopy(),
            ),
        )

    /**
     * Full retained coordination truth.
     *
     * This stream may change when only Compound inputs/currentness change. Consumers that care only
     * about the derived semantic result should use [outputStates] rather than collecting this stream
     * and remembering to add map/distinctUntilChanged themselves.
     */
    override val states: Flow<VexCompoundState<InputState, OutputState>> =
        mutableCurrentState.map { state ->
            state.deepCopy()
        }

    /**
     * Canonical downstream projection of this Compound Relay.
     *
     * Distinct-by-output is an architectural guarantee, not a consumer habit:
     *
     * retained input/revision change + equal output
     * -> [states] may emit
     * -> [outputStates] does not emit
     *
     * Equality is evaluated against Relay-owned output state before a defensive copy is emitted, so
     * a consumer cannot mutate the value retained by distinctUntilChanged.
     */
    val outputStates: Flow<OutputState> =
        mutableCurrentState
            .map { state ->
                state.outputState
            }
            .distinctUntilChanged()
            .map { output ->
                output.deepCopy()
            }

    val currentOutput: OutputState
        get() =
            synchronized(lock) {
                mutableCurrentState.value
                    .outputState
                    .deepCopy()
            }

    override val current: VexCompoundState<InputState, OutputState>
        get() =
            synchronized(lock) {
                mutableCurrentState.value.deepCopy()
            }

    protected abstract fun processStates(
        inputState: InputState,
        currentState: VexCompoundState<InputState, OutputState>,
    ): VexCompoundState<InputState, OutputState>

    protected fun send(
        inputState: InputState,
    ): VexCompoundState<InputState, OutputState> =
        synchronized(lock) {
            check(!processing) {
                "VexCompoundRelay cannot recursively send state from processStates"
            }

            val current =
                mutableCurrentState.value.deepCopy()

            processing = true
            try {
                val processed =
                    processStates(
                        inputState = inputState.deepCopy(),
                        currentState = current.deepCopy(),
                    ).deepCopy()

                val meaningChanged =
                    processed.inputState != current.inputState ||
                        processed.outputState != current.outputState

                if (!meaningChanged) {
                    return@synchronized current.deepCopy()
                }

                val next =
                    VexCompoundState(
                        inputState = processed.inputState.deepCopy(),
                        outputState = processed.outputState.deepCopy(),
                        revision = current.revision + 1L,
                    )

                mutableCurrentState.value =
                    next.deepCopy()

                next.deepCopy()
            } finally {
                processing = false
            }
        }
}

// [VXG RealForever]
