package com.vextreme.vexlife.android.relay

import com.vextreme.vexlife.android.execution.VexExecutionRoot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

abstract class VexCompoundRelay<
    InputState : VexInputState<InputState>,
    OutputState : VexOutputState<OutputState>,
>(
    val relayRef: String,
    initialInputState: InputState,
    initialOutputState: OutputState,
    private val executionRoot: VexExecutionRoot? = null,
    private val observedDependencies: List<VexRelayDependency<InputState>> = emptyList(),
) :
    VexConvergence<
        InputState,
        VexCompoundState<
            InputState,
            OutputState
        >
    >(
        initialCurrent =
            VexCompoundState(
                inputState =
                    initialInputState
                        .deepCopy(),
                outputState =
                    initialOutputState
                        .deepCopy(),
            ),
    ),
    VexObservableRelay<
        VexCompoundState<
            InputState,
            OutputState
        >
    > {
    private val observationLock =
        Any()

    private var observationsStarted =
        false

    init {
        require(relayRef.isNotBlank()) { "relayRef must not be blank" }

        require(
            observedDependencies
                .map { dependency ->
                    dependency.dependencyRef
                }
                .distinct()
                .size ==
                observedDependencies.size,
        ) {
            "VexCompoundRelay observed dependencies must use unique dependencyRef values"
        }

        require(
            observedDependencies.isEmpty() ||
                executionRoot != null,
        ) {
            "VexCompoundRelay with observed dependencies requires VexExecutionRoot"
        }
    }

    override val states:
        Flow<
            VexCompoundState<
                InputState,
                OutputState
            >
        > =
        convergedStates
            .map { state ->
                state.deepCopy()
            }

    val inputStates:
        Flow<
            InputState
        > =
        convergedStates
            .map { state ->
                state.inputState
            }
            .distinctUntilChanged()
            .map { input ->
                input.deepCopy()
            }

    val currentInput:
        InputState
        get() =
            convergedCurrent
                .inputState
                .deepCopy()

    val outputStates:
        Flow<
            OutputState
        > =
        convergedStates
            .map { state ->
                state.outputState
            }
            .distinctUntilChanged()
            .map { output ->
                output.deepCopy()
            }

    val currentOutput:
        OutputState
        get() =
            convergedCurrent
                .outputState
                .deepCopy()

    override val current:
        VexCompoundState<
            InputState,
            OutputState
        >
        get() =
            convergedCurrent
                .deepCopy()

    /**
     * Admit this Relay's constructor-declared observation dependencies into the canonical
     * VexExecution lifetime.
     *
     * Concrete observed Relays call this once from their own init block, after concrete fields are
     * initialized. Callers constructing the Relay do not activate or supply a raw CoroutineScope.
     */
    protected fun startObservedDependencies() {
        if (
            observedDependencies.isEmpty()
        ) {
            return
        }

        synchronized(observationLock) {
            check(
                !observationsStarted,
            ) {
                "VexCompoundRelay observed dependencies are already started: $relayRef"
            }

            val root =
                requireNotNull(
                    executionRoot,
                ) {
                    "VexCompoundRelay observed dependencies require VexExecutionRoot"
                }

            observedDependencies.forEach { dependency ->
                dependency.observeWith(
                    executionRoot =
                        root,
                    owner =
                        this,
                    send = { inputState ->
                        send(
                            inputState,
                        )
                        Unit
                    },
                )
            }

            observationsStarted =
                true
        }
    }

    /**
     * Relay-specific relationship calculation over facts that have already been observed.
     *
     * Compound Relay keeps its existing full bounded relationship reevaluation law here. The lower
     * VexConvergence class supplies only serialized retained-CURRENT mechanics; it does not authorize
     * hand-maintained partial invalidation inside a Relay.
     */
    protected abstract fun processStates(
        inputState: InputState,
        currentState: VexCompoundState<InputState, OutputState>,
    ): VexCompoundState<InputState, OutputState>

    final override fun converge(
        update:
            InputState,
        current:
            VexCompoundState<
                InputState,
                OutputState
            >,
    ):
        VexCompoundState<
            InputState,
            OutputState
        > {
        val processed =
            processStates(
                inputState =
                    update
                        .deepCopy(),
                currentState =
                    current
                        .deepCopy(),
            )
                .deepCopy()

        return VexCompoundState(
            inputState =
                processed
                    .inputState
                    .deepCopy(),
            outputState =
                processed
                    .outputState
                    .deepCopy(),
        )
    }

    protected fun send(
        inputState: InputState,
    ): VexCompoundState<InputState, OutputState> =
        admit(
            inputState
                .deepCopy(),
        )
            .deepCopy()
}

// [VXG RealForever]
