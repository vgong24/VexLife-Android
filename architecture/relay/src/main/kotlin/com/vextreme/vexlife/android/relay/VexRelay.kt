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
