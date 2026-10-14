package com.vextreme.vexlife.android.relay

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Internal retained-state kernel shared by singular Relay authoring surfaces.
 *
 * Feature authors do not call this class directly. Its purpose is to keep ownership snapshots,
 * serialization, semantic no-op suppression and outward exposure in one place so VexRelay and
 * VexActionRelay can present small, intention-specific APIs.
 */
internal class VexRelayStateStore<State : VexState<State>>(
    initialState: State,
) {
    private val lock =
        Any()

    private var processing =
        false

    // Construction boundary: caller-owned initial state becomes retained Relay truth.
    private val mutableCurrentState =
        MutableStateFlow(
            initialState.deepCopy(),
        )

    // Stream exposure boundary.
    val states: Flow<State> =
        mutableCurrentState.map { state ->
            state.deepCopy()
        }

    // Point-in-time exposure boundary.
    val current: State
        get() =
            synchronized(lock) {
                mutableCurrentState.value.deepCopy()
            }

    /**
     * One atomic input -> current -> next transition.
     *
     * [snapshotInput] exists because input ownership differs by Relay family:
     * - VexRelay receives State values and snapshots them before reducer access;
     * - VexActionRelay receives immutable semantic Actions and may share the action instance.
     *
     * The feature-supplied [nextStateFor] function receives only an admitted input plus an
     * ownership-safe snapshot of exact retained current state. It never receives the retained object.
     */
    fun <Input> transition(
        input: Input,
        snapshotInput: (Input) -> Input,
        nextStateFor: (
            input: Input,
            currentState: State,
        ) -> State,
    ): State =
        synchronized(lock) {
            /*
             * synchronized(lock) serializes concurrent callers.
             *
             * processing solves a different problem: JVM monitors are re-entrant, so a reducer could
             * otherwise recursively start another transition on this same store before finishing the
             * first one.
             */
            check(!processing) {
                "VexRelay cannot recursively transition retained state"
            }

            val retainedCurrent =
                mutableCurrentState.value

            val admittedInput =
                snapshotInput(input)

            val processingCurrent =
                retainedCurrent.deepCopy()

            processing = true
            try {
                val candidate =
                    nextStateFor(
                        admittedInput,
                        processingCurrent,
                    )

                // A semantic no-op never acquires retention ownership.
                if (candidate == retainedCurrent) {
                    return@synchronized retainedCurrent.deepCopy()
                }

                // Changed candidate crosses from reducer ownership into Relay retention ownership.
                val ownedNext =
                    candidate.deepCopy()

                // The only canonical retained truth move.
                mutableCurrentState.value =
                    ownedNext

                // Outward exposure boundary.
                ownedNext.deepCopy()
            } finally {
                processing = false
            }
        }
}

// [VXG RealForever]
