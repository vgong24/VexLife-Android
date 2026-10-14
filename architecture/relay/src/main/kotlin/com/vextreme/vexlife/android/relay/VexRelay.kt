package com.vextreme.vexlife.android.relay

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

abstract class VexRelay<State : VexState<State>>(
    val relayRef: String,
    initialState: State,
) : VexObservableRelay<State> {
    private val lock = Any()
    private var processing = false

    init {
        require(relayRef.isNotBlank()) { "relayRef must not be blank" }
    }

    // CONSTRUCTION BOUNDARY — the caller that supplied initialState must not share mutable ownership
    // with Relay's first retained truth. Immutable state returns itself here.
    private val mutableCurrentState =
        MutableStateFlow(initialState.deepCopy())

    // STREAM EXPOSURE BOUNDARY — every collector receives an ownership-safe state view. Immutable
    // state may be shared; mutable-backed state detaches per emission.
    override val states: Flow<State> =
        mutableCurrentState.map { state ->
            state.deepCopy()
        }

    // POINT-IN-TIME EXPOSURE BOUNDARY — current never hands the retained mutable object to a caller.
    override val current: State
        get() =
            synchronized(lock) {
                mutableCurrentState.value.deepCopy()
            }

    /**
     * Reconcile one State family.
     *
     * VexRelay uses singular processState because input/current/next are the same semantic State
     * type. Reducer authors should return domain meaning directly and should not deepCopy() merely
     * for Relay ownership; send()/update() own admission, reducer isolation, retention, and exposure
     * snapshots around this method.
     */
    protected open fun processState(
        inputState: State,
        currentState: State,
    ): State =
        inputState

    /**
     * Admit one complete State value through the same ownership protocol as Compound Relay.
     *
     * caller input
     * -> admittedInput
     *
     * retainedCurrent
     * -> processingCurrent
     *
     * processState(...)
     * -> reducerCandidate
     * -> ownedCandidate
     *
     * compare
     * -> equal: keep retainedCurrent
     * -> changed: commit ownedCandidate
     *
     * retained result
     * -> exposed result
     *
     * Ordinary immutable VexState makes each snapshot an identity operation. Mutable-backed states still copy
     * at the individual ownership boundaries.
     */
    protected fun send(inputState: State): State =
        synchronized(lock) {
            // STEP 0 — synchronized(lock) serializes concurrent callers. A second caller
            // waits for this transition to release the lock, then processes against the newly
            // retained current state; it is not rejected by processing.
            //
            // processing rejects only same-thread recursive reducer re-entry. JVM synchronized
            // monitors are re-entrant, so the flag prevents processState() from calling send() and
            // starting a nested semantic transition before the current one has finished.
            check(!processing) {
                "VexRelay cannot recursively send state from processState"
            }

            // STEP 1 — canonical current truth is already Relay-owned; read it without copying.
            val retainedCurrent =
                mutableCurrentState.value

            // STEP 2 — detach caller ownership before reducer code can retain/reuse the input.
            val admittedInput =
                inputState.deepCopy()

            // STEP 3 — isolate canonical retained truth from overridable processState().
            val processingCurrent =
                retainedCurrent.deepCopy()

            processing = true
            try {
                // STEP 4 — reducerCandidate is temporary only. It has no retention authority yet.
                val reducerCandidate =
                    processState(
                        inputState = admittedInput,
                        currentState = processingCurrent,
                    )

                // STEP 5 — compare before claiming ownership. A no-op candidate is discarded, so
                // copying it first would be wasted work.
                if (reducerCandidate == retainedCurrent) {
                    // STEP 6A — semantic no-op. Canonical truth stays exactly where it is.
                    // Snapshot once because the return value crosses back out of Relay ownership.
                    val exposedCurrent =
                        retainedCurrent.deepCopy()

                    return@synchronized exposedCurrent
                }

                // STEP 6B — this candidate will be retained, so now claim Relay ownership.
                val ownedNext =
                    reducerCandidate.deepCopy()

                // STEP 7 — atomic commit of the owned candidate.
                mutableCurrentState.value =
                    ownedNext

                // STEP 8 — return an ownership-safe value to the caller.
                val exposedNext =
                    ownedNext.deepCopy()

                exposedNext
            } finally {
                // STEP 9 — processing is temporary execution state, never durable product truth.
                processing = false
            }
        }

    /**
     * Atomically derive next State from exact retained current State.
     *
     * Use this path when the operation itself is naturally expressed as:
     *
     *     current -> next
     *
     * Examples: acknowledge one item, advance one ordinal, reset one local preference.
     *
     * This is intentionally different from [send]:
     *
     *     send(input)
     *     -> processState(input, current)
     *
     *     updateCurrent { current -> next }
     *     -> commit next directly
     *
     * updateCurrent does NOT call [processState] afterward. The lambda is already the complete
     * current-state transition for this operation. This keeps one semantic reducer per path instead
     * of hiding a second processing pass behind a generic "transform".
     *
     * The lambda executes while the Relay serialization lock is held, so every read used to derive
     * next State belongs to the same atomic transition. Do not read [current] before updateCurrent
     * when that read participates in deciding the next State; read it through [currentState] here.
     */
    protected fun updateCurrent(
        nextStateFromCurrent: (currentState: State) -> State,
    ): State =
        synchronized(lock) {
            // STEP 0 — concurrent callers wait for this lock. processing only rejects recursive
            // same-thread Relay re-entry from inside transition logic.
            check(!processing) {
                "VexRelay cannot recursively update current state"
            }

            // STEP 1 — canonical retained truth belongs to Relay.
            val retainedCurrent =
                mutableCurrentState.value

            // STEP 2 — transition logic receives an ownership-safe snapshot of that exact truth.
            val processingCurrent =
                retainedCurrent.deepCopy()

            processing = true
            try {
                // STEP 3 — derive one temporary next-state candidate from exact current truth.
                val candidate =
                    nextStateFromCurrent(
                        processingCurrent,
                    )

                // STEP 4 — no-op candidates never acquire retention ownership.
                if (candidate == retainedCurrent) {
                    return@synchronized retainedCurrent.deepCopy()
                }

                // STEP 5 — changed candidate crosses from transition-local ownership into Relay.
                val ownedNext =
                    candidate.deepCopy()

                // STEP 6 — one atomic canonical truth move.
                mutableCurrentState.value =
                    ownedNext

                // STEP 7 — caller receives an ownership-safe projection.
                ownedNext.deepCopy()
            } finally {
                processing = false
            }
        }
}

// [VXG RealForever]
