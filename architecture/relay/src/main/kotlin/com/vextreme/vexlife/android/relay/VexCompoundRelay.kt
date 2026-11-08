package com.vextreme.vexlife.android.relay

import com.vextreme.vexlife.android.execution.VexExecutionRoot
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
    private val executionRoot: VexExecutionRoot? = null,
    private val observedDependencies: List<VexRelayDependency<InputState>> = emptyList(),
) : VexObservableRelay<VexCompoundState<InputState, OutputState>> {
    private val lock = Any()
    private var processing = false

    private val observationLock = Any()
    private var observationsStarted = false

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

    val inputStates: Flow<InputState> =
        mutableCurrentState
            .map { state ->
                state.inputState
            }
            .distinctUntilChanged()
            .map { input ->
                input.deepCopy()
            }

    val currentInput: InputState
        get() =
            synchronized(lock) {
                mutableCurrentState.value
                    .inputState
                    .deepCopy()
            }

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
     * Atomic in-memory reducer over facts that have already been observed.
     *
     * Keep blocking IO, SDK waits and other suspending work in dependency owners. This method cannot
     * suspend halfway through a transition, so each admitted patch reconciles against one exact
     * retained current state before the next patch may enter.
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
