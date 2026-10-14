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

    // CONSTRUCTION BOUNDARY — initial input/output arrive from the constructing caller and become
    // Relay-owned retained truth. Mutable-backed branches detach here; immutable branches may share.
    private val mutableCurrentState =
        MutableStateFlow(
            VexCompoundState(
                inputState = initialInputState.deepCopy(),
                outputState = initialOutputState.deepCopy(),
            ),
        )

    // FULL-STATE STREAM EXPOSURE — collectors that need input + output + revision receive an
    // ownership-safe snapshot of the whole retained relationship.
    override val states: Flow<VexCompoundState<InputState, OutputState>> =
        mutableCurrentState.map { state ->
            state.deepCopy()
        }

    // INPUT PROJECTION EXPOSURE — distinctUntilChanged() evaluates Relay-owned input meaning first;
    // only values that actually emit cross the observation boundary through deepCopy().
    val inputStates: Flow<InputState> =
        mutableCurrentState
            .map { state ->
                state.inputState
            }
            .distinctUntilChanged()
            .map { input ->
                input.deepCopy()
            }

    // POINT-IN-TIME INPUT EXPOSURE — never return a mutable retained input branch directly.
    val currentInput: InputState
        get() =
            synchronized(lock) {
                mutableCurrentState.value
                    .inputState
                    .deepCopy()
            }

    // OUTPUT PROJECTION EXPOSURE — output equality is decided before exposure so an input-only
    // Compound revision cannot wake downstream consumers. Emitted output is then ownership-safe.
    val outputStates: Flow<OutputState> =
        mutableCurrentState
            .map { state ->
                state.outputState
            }
            .distinctUntilChanged()
            .map { output ->
                output.deepCopy()
            }

    // POINT-IN-TIME OUTPUT EXPOSURE — same boundary as outputStates for direct current reads.
    val currentOutput: OutputState
        get() =
            synchronized(lock) {
                mutableCurrentState.value
                    .outputState
                    .deepCopy()
            }

    // POINT-IN-TIME FULL EXPOSURE — current returns the complete relationship without leaking a
    // mutable retained branch to its caller.
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
     * [send] compares its semantic branches with retained current, claims only changed branches for
     * Relay ownership, and commits at most one next state.
     *
     * Reducer-authoring rule:
     *
     * - do NOT call deepCopy() merely because a value will be returned to Relay;
     * - do NOT deep-copy previous/input branches merely to build coherent InputState;
     * - ordinary copy()/constructors/immutable collection operations should express domain meaning;
     * - use an explicit local copy only when the domain algorithm itself needs independent mutable
     *   working memory (for example, an in-place sort), not to satisfy Relay ownership.
     *
     * [send] already owns admission, reducer isolation, changed-branch retention snapshots, and
     * outward exposure. Keeping those mechanics here prevents every feature Relay from recreating
     * ownership folklore inside business logic.
     *
     * Recommended reducer shape:
     *
     * 1. REFERENCES   -> name previous input/output needed for reasoning;
     * 2. RECONCILE    -> merge the partial patch into one coherent input snapshot;
     * 3. DERIVE       -> calculate domain meaning from that coherent input;
     * 4. PROJECT      -> construct one output value;
     * 5. RETURN       -> currentState.copy(inputState = coherent, outputState = output).
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
     * Every deepCopy() below marks a different ownership boundary. For an ordinary immutable VexState each
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
     * -> reducerCandidate              // temporary; not retained
     *
     * compare reducerCandidate with retainedCurrent
     * -> no semantic change: discard candidate, retain retainedCurrent
     * -> semantic change: snapshot only changed branch(es) for Relay ownership
     * -> commit one next state
     *
     * retained result
     * -> exposedResult                 // Relay retention -> send() caller
     */
    protected fun send(
        inputState: InputState,
    ): VexCompoundState<InputState, OutputState> =
        synchronized(lock) {
            // STEP 0 — serialize concurrent callers before any transition work begins.
            //
            // synchronized(lock) is the concurrency gate:
            // - if another thread/coroutine calls send() while this transition owns the lock, that
            //   caller waits outside this block and continues after the lock is released;
            // - its patch is not intentionally dropped or overwritten by the processing flag;
            // - this is monitor serialization, not an explicit FIFO/fair event queue, so source
            //   freshness/order still belongs to the source (revision/attempt/epoch when needed).
            //
            // processing solves a different problem. JVM monitors are re-entrant, so processStates()
            // could otherwise call send() recursively on the same thread and start a nested semantic
            // transition against half-finished reducer work. The flag rejects only that re-entry.
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

                // STEP 5 — compare the temporary candidate before copying it.
                //
                // Comparison does not transfer ownership, and a no-op candidate will be discarded.
                // Delaying the ownership snapshot avoids copying a state we will never retain.
                val inputChanged =
                    reducerCandidate.inputState != retainedCurrent.inputState
                val outputChanged =
                    reducerCandidate.outputState != retainedCurrent.outputState

                if (!inputChanged && !outputChanged) {
                    // STEP 6A — no semantic transition. Discard reducerCandidate and keep the exact
                    // retained state/revision. Only the outward return boundary still needs a
                    // snapshot for mutable-backed state.
                    val exposedCurrent =
                        retainedCurrent.deepCopy()

                    return@synchronized exposedCurrent
                }

                // STEP 6B — claim ownership only for branches that will actually enter retained truth.
                //
                // Unchanged branches already belong to Relay, so copying them would add no isolation.
                // Changed mutable-backed branches detach from reducer ownership here. Changed
                // immutable branches return themselves and remain structurally shared.
                val nextInput =
                    if (inputChanged) {
                        reducerCandidate.inputState.deepCopy()
                    } else {
                        retainedCurrent.inputState
                    }
                val nextOutput =
                    if (outputChanged) {
                        reducerCandidate.outputState.deepCopy()
                    } else {
                        retainedCurrent.outputState
                    }

                // STEP 7 — build exactly one new retained wrapper from the changed owned branch(es)
                // plus unchanged already-owned branch(es).
                val nextRetained =
                    VexCompoundState(
                        inputState = nextInput,
                        outputState = nextOutput,
                        revision = retainedCurrent.revision + 1L,
                    )

                // STEP 8 — atomic commit. This is the only point where canonical Relay truth moves.
                mutableCurrentState.value =
                    nextRetained

                // STEP 9 — expose an ownership-safe result to the caller of send().
                // Immutable state returns the same object; mutable-backed state detaches.
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
