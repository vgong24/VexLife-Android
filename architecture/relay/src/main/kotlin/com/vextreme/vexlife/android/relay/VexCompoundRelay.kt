package com.vextreme.vexlife.android.relay

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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

    override val states: Flow<VexCompoundState<InputState, OutputState>> =
        mutableCurrentState.map { state ->
            state.deepCopy()
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
