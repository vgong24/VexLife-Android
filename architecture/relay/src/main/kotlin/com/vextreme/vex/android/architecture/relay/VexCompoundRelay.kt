package com.vextreme.vex.android.architecture.relay

import com.vextreme.vex.android.architecture.execution.VexCoroutineHandle
import com.vextreme.vex.android.architecture.execution.VexExecutionOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Relay for one organized component relationship with a separate input family and output family.
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
    private var dependenciesObserved = false

    private val mutableCurrentState =
        MutableStateFlow(
            VexCompoundState(
                inputState = initialInputState.deepCopy(),
                outputState = initialOutputState.deepCopy(),
            ),
        )

    override val currentState: StateFlow<VexCompoundState<InputState, OutputState>> =
        DefensiveVexStateFlow(mutableCurrentState.asStateFlow())

    protected abstract fun processStates(
        inputState: InputState,
        currentState: VexCompoundState<InputState, OutputState>,
    ): VexCompoundState<InputState, OutputState>

    /**
     * Start child Relay observation exactly once.
     *
     * VexCompoundRelay does not retain anonymous Job objects. Each collector is a named coroutine
     * owned and monitored by the provided VexExecutionOwner (normally VexAppScope for app-wide
     * Source-of-Truth composition).
     */
    protected fun observeDependencies(
        executionOwner: VexExecutionOwner,
        vararg dependencies: VexRelayDependency<InputState>,
    ): List<VexCoroutineHandle> =
        synchronized(lock) {
            check(!dependenciesObserved) {
                "VexCompoundRelay dependencies are already being observed"
            }
            dependenciesObserved = true

            dependencies.map { dependency ->
                dependency.observe(
                    executionOwner = executionOwner,
                    sendInput = { inputState ->
                        send(inputState)
                    },
                )
            }
        }

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
}

// [VXG RealForever]
