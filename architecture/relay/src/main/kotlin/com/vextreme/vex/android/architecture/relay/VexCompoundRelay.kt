package com.vextreme.vex.android.architecture.relay

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
    private var dependencyJobs: List<Job>? = null

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
     * Start this Compound Relay's child observations exactly once.
     *
     * The lifetime is supplied from outside (for example an application/session Koin scope).
     * VexCompoundRelay never invents a GlobalScope and never reaches into Koin itself.
     */
    protected fun observeDependencies(
        scope: CoroutineScope,
        vararg dependencies: VexRelayDependency<InputState>,
    ): List<Job> =
        synchronized(lock) {
            check(dependencyJobs == null) {
                "VexCompoundRelay dependencies are already being observed"
            }

            dependencies
                .map { dependency ->
                    dependency.observe(
                        scope = scope,
                        sendInput = { inputState ->
                            send(inputState)
                        },
                    )
                }
                .also { jobs ->
                    dependencyJobs = jobs
                }
        }

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
}

// [VXG RealForever]
