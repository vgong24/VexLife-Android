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
     * The reducer receives two ownership-safe processing values:
     *
     * 1. [inputState] is the admitted patch snapshot;
     * 2. [currentState] is a processing snapshot of the exact retained current truth.
     *
     * The returned value is only a temporary candidate. It is not retained or observable until
     * [send] claims it for Relay ownership, compares its semantic branches with retained current,
     * and commits at most one next state.
     *
     * Pseudocode:
     *
     * retained current
     * + incoming patch
     * -> snapshot both for reducer ownership
     * -> processStates(...)
     * -> temporary candidate
     * -> snapshot candidate for Relay ownership
     * -> compare input/output meaning
     * -> no change: keep retained current
     * -> change: commit one next state with revision + 1
     */
    protected abstract fun processStates(
        inputState: InputState,
        currentState: VexCompoundState<InputState, OutputState>,
    ): VexCompoundState<InputState, OutputState>

    /**
     * Admit one partial input patch and atomically converge it with retained Compound truth.
     *
     * Every deepCopy() below marks a different ownership boundary. For a VexImmutable* state each
     * call returns the same instance, so these boundaries allocate nothing. For mutable-backed
     * states they remain real copies because the owners are genuinely different.
     *
     * Transition protocol:
     *
     * caller-owned patch
     * -> admittedPatch                 // caller -> Relay processing
     *
     * retainedCurrent                  // canonical Relay-owned truth; never mutate directly
     * -> processingCurrent             // Relay retention -> reducer sandbox
     *
     * processStates(...)
     * -> reducerCandidate              // temporary; not yet trusted for retention
     * -> ownedCandidate                // reducer -> Relay retention ownership
     *
     * compare ownedCandidate with retainedCurrent
     * -> no semantic change: retain retainedCurrent
     * -> semantic change: commit one next state
     *
     * retained result
     * -> exposedResult                 // Relay retention -> send() caller
     */
    protected fun send(
        inputState: InputState,
    ): VexCompoundState<InputState, OutputState> =
        synchronized(lock) {
            // STEP 0 — serialize transitions. A reducer cannot recursively start another transition.
            check(!processing) {
                "VexCompoundRelay cannot recursively send state from processStates"
            }

            // STEP 1 — read canonical retained truth by reference. Relay already owns this object.
            // No copy is needed merely to inspect an object that is already inside the lock/owner.
            val retainedCurrent =
                mutableCurrentState.value

            // STEP 2 — admit caller input into Relay processing ownership.
            // Mutable-backed patches detach here. Immutable patches return themselves.
            val admittedPatch =
                inputState.deepCopy()

            // STEP 3 — isolate retained truth from overridable reducer code.
            // This copy is not the same boundary as STEP 2: it protects Relay's canonical current
            // state from accidental mutation by processStates().
            val processingCurrent =
                retainedCurrent.deepCopy()

            processing = true
            try {
                // STEP 4 — calculate one temporary candidate from the admitted patch + exact
                // processing snapshot. Nothing returned by processStates() is retained yet.
                val reducerCandidate =
                    processStates(
                        inputState = admittedPatch,
                        currentState = processingCurrent,
                    )

                // STEP 5 — claim the reducer result for Relay ownership before comparing/retaining it.
                // The reducer may have built mutable collections or reused processing objects.
                // Mutable-backed state detaches here; transitively immutable state is shared.
                val ownedCandidate =
                    reducerCandidate.deepCopy()

                // STEP 6 — compare semantic branches, not object identity and not candidate revision.
                // Compound revision is owned here by the framework and advances only on real meaning.
                val inputChanged =
                    ownedCandidate.inputState != retainedCurrent.inputState
                val outputChanged =
                    ownedCandidate.outputState != retainedCurrent.outputState

                if (!inputChanged && !outputChanged) {
                    // STEP 7A — no semantic transition. Keep the exact retained state/revision.
                    //
                    // We still snapshot once for exposure because send() returns a value outside the
                    // Relay owner. For immutable state this is the same object with zero allocation.
                    // For mutable-backed state this prevents the caller from mutating retained truth.
                    val exposedCurrent =
                        retainedCurrent.deepCopy()

                    return@synchronized exposedCurrent
                }

                // STEP 7B — build exactly one new retained wrapper.
                //
                // A branch whose meaning did not change keeps the exact already-owned object.
                // This is the structural-sharing rule that makes nested Compound Relay output cheap.
                val nextRetained =
                    VexCompoundState(
                        inputState =
                            if (inputChanged) {
                                ownedCandidate.inputState
                            } else {
                                retainedCurrent.inputState
                            },
                        outputState =
                            if (outputChanged) {
                                ownedCandidate.outputState
                            } else {
                                retainedCurrent.outputState
                            },
                        revision = retainedCurrent.revision + 1L,
                    )

                // STEP 8 — atomic commit. This is the only point where canonical Relay truth moves.
                mutableCurrentState.value =
                    nextRetained

                // STEP 9 — expose an ownership-safe result to the caller of send().
                // Again: immutable state returns the same object; mutable-backed state detaches.
                val exposedNext =
                    nextRetained.deepCopy()

                exposedNext
            } finally {
                // STEP 10 — failure cannot strand the Relay in "processing". The retained state
                // changes only at STEP 8, so an exception before commit leaves prior truth intact.
                processing = false
            }
        }
}

// [VXG RealForever]
