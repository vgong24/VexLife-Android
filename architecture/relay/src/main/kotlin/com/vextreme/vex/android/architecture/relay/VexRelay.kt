package com.vextreme.vex.android.architecture.relay

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Small Source-of-Truth Relay for one state type.
 *
 * A component usually subclasses this, exposes intention-revealing methods such as
 * screenChanged(...) or itemsArrived(...), and keeps [send]/[update] protected.
 *
 * Processing is synchronous and serialized. It does not depend on StateFlow collectors, so a slow
 * UI observer cannot cause the Relay itself to skip input processing.
 */
abstract class VexRelay<State : VexState<State>>(
    initialState: State,
) : VexObservableRelay<State> {
    private val lock = Any()
    private var processing = false

    private val mutableCurrentState =
        MutableStateFlow(initialState.deepCopy())

    override val currentState: StateFlow<State> =
        DefensiveVexStateFlow(mutableCurrentState.asStateFlow())

    /**
     * Default simple-Relay behavior: the new state replaces the prior state.
     *
     * Override only when this state type has its own admission/freshness rule, for example ignoring
     * an older source revision.
     */
    protected open fun processState(
        inputState: State,
        currentState: State,
    ): State = inputState.deepCopy()

    /** Send a whole state value through this Relay's serialized processing boundary. */
    protected fun send(
        inputState: State,
    ): State =
        synchronized(lock) {
            processLocked(inputState.deepCopy())
        }

    /**
     * Safely change only part of the current state.
     *
     * The transform receives a deep copy while the Relay lock is held, so two concurrent partial
     * changes cannot both start from the same stale state and overwrite one another.
     */
    protected fun update(
        transform: (currentState: State) -> State,
    ): State =
        synchronized(lock) {
            val inputState =
                transform(mutableCurrentState.value.deepCopy()).deepCopy()
            processLocked(inputState)
        }

    private fun processLocked(
        inputState: State,
    ): State {
        check(!processing) {
            "VexRelay cannot recursively send state from processState"
        }

        val currentState = mutableCurrentState.value.deepCopy()

        processing = true
        return try {
            val nextState =
                processState(
                    inputState.deepCopy(),
                    currentState.deepCopy(),
                ).deepCopy()

            if (nextState != currentState) {
                mutableCurrentState.value = nextState.deepCopy()
            }

            nextState.deepCopy()
        } finally {
            processing = false
        }
    }
}

// [VXG RealForever]
