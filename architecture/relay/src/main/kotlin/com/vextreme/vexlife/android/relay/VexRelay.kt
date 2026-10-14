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

    protected fun send(inputState: State): State =
        synchronized(lock) {
            check(!processing) {
                "VexRelay cannot recursively send state from processState"
            }

            /*
             * The retained current value is already Relay-owned. Do not copy it merely to read it.
             * Only the value handed into overridable processing is detached when the state contract
             * requires detachment; VexImmutableState turns that snapshot into a zero-allocation
             * identity operation.
             */
            val current =
                mutableCurrentState.value
            val admittedInput =
                inputState.deepCopy()

            processing = true
            try {
                val processed =
                    processState(
                        inputState = admittedInput,
                        currentState = current.deepCopy(),
                    )

                /*
                 * Claim exactly one ownership snapshot before retaining arbitrary reducer output.
                 * A legacy mutable-backed state is detached here; a transitively immutable state is
                 * safely shared.
                 */
                val ownedNext =
                    processed.deepCopy()

                if (ownedNext == current) {
                    return@synchronized current.deepCopy()
                }

                mutableCurrentState.value =
                    ownedNext

                ownedNext.deepCopy()
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
                mutableCurrentState.value

            processing = true
            try {
                /*
                 * transform() receives an ownership-safe snapshot. Its result is temporary candidate
                 * state, not a second retained truth. processState() reconciles that candidate
                 * against another ownership-safe view of the exact retained current state.
                 */
                val candidate =
                    transform(
                        current.deepCopy(),
                    )

                val processed =
                    processState(
                        inputState = candidate,
                        currentState = current.deepCopy(),
                    )

                val ownedNext =
                    processed.deepCopy()

                if (ownedNext == current) {
                    return@synchronized current.deepCopy()
                }

                mutableCurrentState.value =
                    ownedNext

                ownedNext.deepCopy()
            } finally {
                processing = false
            }
        }
}

// [VXG RealForever]
