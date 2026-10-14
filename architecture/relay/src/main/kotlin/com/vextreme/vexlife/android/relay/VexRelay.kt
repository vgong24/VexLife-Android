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

    private val mutableCurrentState =
        MutableStateFlow(initialState.deepCopy())

    override val states: Flow<State> =
        mutableCurrentState.map { state ->
            state.deepCopy()
        }

    override val current: State
        get() =
            synchronized(lock) {
                mutableCurrentState.value.deepCopy()
            }

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
     * VexImmutableState makes each snapshot an identity operation. Mutable-backed states still copy
     * at the individual ownership boundaries.
     */
    protected fun send(inputState: State): State =
        synchronized(lock) {
            // STEP 0 — exactly one state transition may execute at a time.
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

                // STEP 5 — claim candidate ownership for Relay before semantic comparison/commit.
                val ownedCandidate =
                    reducerCandidate.deepCopy()

                if (ownedCandidate == retainedCurrent) {
                    // STEP 6A — semantic no-op. Canonical truth stays exactly where it is.
                    // Snapshot once because the return value crosses back out of Relay ownership.
                    val exposedCurrent =
                        retainedCurrent.deepCopy()

                    return@synchronized exposedCurrent
                }

                // STEP 6B — atomic commit of the already-owned candidate.
                mutableCurrentState.value =
                    ownedCandidate

                // STEP 7 — return an ownership-safe value to the caller.
                val exposedNext =
                    ownedCandidate.deepCopy()

                exposedNext
            } finally {
                // STEP 8 — processing is temporary execution state, never durable product truth.
                processing = false
            }
        }

    /**
     * Derive a new State from current truth without exposing Relay's retained object to transform().
     *
     * This is the same protocol as [send], except the incoming candidate is first derived from a
     * processing snapshot of retained current.
     */
    protected fun update(
        transform: (currentState: State) -> State,
    ): State =
        synchronized(lock) {
            // STEP 0 — serialize update() with send() and every other reducer transition.
            check(!processing) {
                "VexRelay cannot recursively update state from processState"
            }

            // STEP 1 — canonical Relay-owned current truth.
            val retainedCurrent =
                mutableCurrentState.value

            // STEP 2 — transform() gets its own processing snapshot; it cannot mutate canonical truth.
            val transformCurrent =
                retainedCurrent.deepCopy()

            processing = true
            try {
                // STEP 3 — transformCandidate is temporary and has no retention authority.
                val transformCandidate =
                    transform(
                        transformCurrent,
                    )

                // STEP 4 — processState() gets the transformed candidate plus a fresh current
                // processing snapshot. A fresh snapshot matters for mutable-backed states because
                // transformCurrent belonged to transform() and must not be assumed untouched.
                val processingCurrent =
                    retainedCurrent.deepCopy()
                val reducerCandidate =
                    processState(
                        inputState = transformCandidate,
                        currentState = processingCurrent,
                    )

                // STEP 5 — claim reducer output for Relay ownership.
                val ownedCandidate =
                    reducerCandidate.deepCopy()

                if (ownedCandidate == retainedCurrent) {
                    // STEP 6A — semantic no-op; retain exact canonical current and expose safely.
                    val exposedCurrent =
                        retainedCurrent.deepCopy()

                    return@synchronized exposedCurrent
                }

                // STEP 6B — one atomic retained-state move.
                mutableCurrentState.value =
                    ownedCandidate

                // STEP 7 — detach only if the returned value can actually be mutated.
                val exposedNext =
                    ownedCandidate.deepCopy()

                exposedNext
            } finally {
                // STEP 8 — update execution ends even if transform/processState throws.
                processing = false
            }
        }
}

// [VXG RealForever]
