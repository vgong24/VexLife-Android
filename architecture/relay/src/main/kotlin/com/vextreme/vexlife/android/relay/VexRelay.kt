package com.vextreme.vexlife.android.relay

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * One serialized Source-of-Truth Relay.
 *
 * The concrete subclass owns semantic freshness and reconciliation in [processState]. The base owns
 * the mechanical guarantees: one serialized processing boundary, defensive copying, and suppression
 * of semantically unchanged current state.
 */
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

    override val currentState: StateFlow<State> =
        DefensiveVexStateFlow(mutableCurrentState.asStateFlow())

    /**
     * The small, intention-revealing processing section for this Relay.
     *
     * Examples:
     * - reject a stale external provider revision;
     * - normalize a bounded source snapshot;
     * - preserve current state when the candidate carries no newer meaning.
     */
    protected open fun processState(
        inputState: State,
        currentState: State,
    ): State =
        inputState.deepCopy()

    protected fun send(inputState: State): State =
        synchronized(lock) {
            check(!processing) {
                "VexRelay cannot recursively send state from processState"
            }

            val current =
                mutableCurrentState.value.deepCopy()

            processing = true
            try {
                val next =
                    processState(
                        inputState = inputState.deepCopy(),
                        currentState = current.deepCopy(),
                    ).deepCopy()

                if (next != current) {
                    mutableCurrentState.value =
                        next.deepCopy()
                }

                next.deepCopy()
            } finally {
                processing = false
            }
        }

    protected fun update(
        transform: (currentState: State) -> State,
    ): State =
        synchronized(lock) {
            check(!processing) {
                "VexRelay cannot recursively update state from processState"
            }

            val current =
                mutableCurrentState.value.deepCopy()

            processing = true
            try {
                val candidate =
                    transform(current.deepCopy()).deepCopy()

                val next =
                    processState(
                        inputState = candidate.deepCopy(),
                        currentState = current.deepCopy(),
                    ).deepCopy()

                if (next != current) {
                    mutableCurrentState.value =
                        next.deepCopy()
                }

                next.deepCopy()
            } finally {
                processing = false
            }
        }
}

// [VXG RealForever]
