package com.vextreme.vexlife.android.relay

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

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

    private val dependencyLock = Any()
    private var dependencyOwnerJob: Job? = null

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
     * Continuously observed truths used by this Compound relationship.
     *
     * The concrete Relay declares WHAT it observes. [activate] owns HOW those observations are
     * launched: exactly one named child coroutine per dependency.
     */
    protected open fun dependencies(): List<VexRelayDependency<InputState>> =
        emptyList()

    /**
     * Begins dependency observation under the supplied lifetime owner.
     *
     * Dependency observation is asynchronous; [processStates] deliberately is not. Every observed
     * value becomes one small partial InputState and enters the same serialized [send] boundary.
     */
    fun activate(scope: CoroutineScope) {
        val bindings =
            dependencies()

        require(
            bindings.map { dependency ->
                dependency.dependencyRef
            }.distinct().size == bindings.size,
        ) {
            "VexCompoundRelay dependencies must use unique dependencyRef values"
        }

        synchronized(dependencyLock) {
            check(dependencyOwnerJob == null) {
                "VexCompoundRelay dependencies are already active: $relayRef"
            }

            val parentJob =
                checkNotNull(scope.coroutineContext[Job]) {
                    "VexCompoundRelay activation requires a CoroutineScope with a Job"
                }
            check(parentJob.isActive) {
                "VexCompoundRelay cannot activate in an inactive CoroutineScope"
            }

            val ownerJob =
                SupervisorJob(parentJob)
            val dependencyScope =
                CoroutineScope(
                    scope.coroutineContext +
                        ownerJob,
                )

            dependencyOwnerJob =
                ownerJob

            bindings.forEach { dependency ->
                dependencyScope.launch(
                    CoroutineName(
                        "$relayRef/${dependency.dependencyRef}",
                    ),
                ) {
                    dependency.collectInto { inputState ->
                        send(inputState)
                    }
                }
            }
        }
    }

    suspend fun deactivate() {
        val ownerJob =
            synchronized(dependencyLock) {
                dependencyOwnerJob
            }
                ?: return

        ownerJob.cancelAndJoin()

        synchronized(dependencyLock) {
            if (dependencyOwnerJob === ownerJob) {
                dependencyOwnerJob = null
            }
        }
    }

    /**
     * Atomic in-memory reducer over facts that have already been observed.
     *
     * Keep blocking IO, SDK waits and other suspending work in dependency owners. This method cannot
     * suspend halfway through a transition, so each admitted patch reconciles against one exact
     * retained current state before the next patch may enter.
     *
     * [currentState] is an ownership-safe processing snapshot, not a second retained truth. A
     * reducer may construct as many local values as it needs, but only the returned candidate can be
     * committed after semantic comparison.
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

            /*
             * Retained current is already Relay-owned, so reading it requires no copy. The incoming
             * patch is snapshotted once at admission, and the current state is snapshotted only when
             * crossing into overridable reducer code.
             */
            val current =
                mutableCurrentState.value
            val admittedInput =
                inputState.deepCopy()

            processing = true
            try {
                val processed =
                    processStates(
                        inputState = admittedInput,
                        currentState = current.deepCopy(),
                    )

                /*
                 * Claim exactly one Relay-owned snapshot of arbitrary reducer output. Mutable-backed
                 * states still detach here. Transitively immutable states may return themselves,
                 * allowing unchanged child objects and nested Compound outputs to be structurally
                 * shared rather than recursively recopied.
                 */
                val ownedProcessed =
                    processed.deepCopy()

                val inputChanged =
                    ownedProcessed.inputState != current.inputState
                val outputChanged =
                    ownedProcessed.outputState != current.outputState

                if (!inputChanged && !outputChanged) {
                    return@synchronized current.deepCopy()
                }

                /*
                 * Preserve the exact already-owned branch when its semantic meaning did not change.
                 * This gives nested Compound graphs stable structural sharing: an input-only child
                 * revision does not manufacture a replacement output object for downstream parents.
                 */
                val next =
                    VexCompoundState(
                        inputState =
                            if (inputChanged) {
                                ownedProcessed.inputState
                            } else {
                                current.inputState
                            },
                        outputState =
                            if (outputChanged) {
                                ownedProcessed.outputState
                            } else {
                                current.outputState
                            },
                        revision = current.revision + 1L,
                    )

                mutableCurrentState.value =
                    next

                next.deepCopy()
            } finally {
                processing = false
            }
        }
}

// [VXG RealForever]
