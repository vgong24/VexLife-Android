package com.vextreme.vexlife.android.relay

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Serial reconciler for several related truths that must produce one bounded current projection.
 *
 * EAP-00 deliberately owns processing only. Async dependency subscription/lifecycle is not hidden in
 * this base yet; the first real Events external-feed wiring will earn that seam from an actual
 * producer lifetime instead of importing the old experimental AppScope machinery wholesale.
 */
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

    override val currentState: StateFlow<VexCompoundState<InputState, OutputState>> =
        DefensiveVexStateFlow(mutableCurrentState.asStateFlow())

    /**
     * The semantic heart of the relationship.
     *
     * A concrete Compound Relay should make its dependencies perceptible in [InputState], merge
     * partial patches against [currentState], then derive exactly one next [OutputState].
     *
     * Long-running I/O, timers, SDK calls, and UI rendering do not belong in this function.
     */
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
