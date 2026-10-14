package com.vextreme.vexlife.android.relay

import kotlinx.coroutines.flow.Flow

/**
 * Relay for input-state-driven reconciliation.
 *
 * Use when a caller/source supplies a State-shaped observation and the Relay must reconcile that
 * input against exact retained current State.
 *
 * Path:
 *
 *     input State
 *     -> send(inputState)
 *     -> processState(inputState, currentState)
 *     -> one retained next State
 *
 * For named human/domain intentions that should not manufacture State-shaped input, use
 * [VexActionRelay] instead.
 */
abstract class VexRelay<State : VexState<State>>(
    val relayRef: String,
    initialState: State,
) : VexObservableRelay<State> {
    init {
        require(relayRef.isNotBlank()) {
            "relayRef must not be blank"
        }
    }

    private val store =
        VexRelayStateStore(
            initialState = initialState,
        )

    override val states: Flow<State>
        get() =
            store.states

    override val current: State
        get() =
            store.current

    /**
     * Reconcile one admitted State-shaped observation against exact retained current State.
     *
     * Reducer authors own domain meaning only. Do not deepCopy() merely for Relay ownership; the
     * retained-state kernel owns admission, reducer isolation, retention and outward exposure.
     */
    protected open fun processState(
        inputState: State,
        currentState: State,
    ): State =
        inputState

    /**
     * Admit one caller/source State value.
     *
     * Caller ownership is detached before [processState] receives the input. Concurrent callers
     * serialize through the shared retained-state kernel; semantic freshness still belongs to the
     * source/reducer.
     */
    protected fun send(
        inputState: State,
    ): State =
        store.transition(
            input = inputState,
            snapshotInput = { admittedInput ->
                admittedInput.deepCopy()
            },
            nextStateFor = ::processState,
        )
}

// [VXG RealForever]
