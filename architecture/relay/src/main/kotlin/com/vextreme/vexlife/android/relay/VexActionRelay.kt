package com.vextreme.vexlife.android.relay

import kotlinx.coroutines.flow.Flow

/**
 * One named semantic action admitted into a Relay-owned state transition.
 *
 * Actions are immutable intentions by contract. They describe what was requested, not what
 * ultimately became true. The resulting State remains authoritative truth.
 */
interface VexAction

/**
 * Relay for action-driven state.
 *
 * Use when callers should submit a named semantic action rather than manufacture the next State.
 *
 * Path:
 *
 *     action
 *     -> sendAction(action)
 *     -> nextStateFor(action, currentState)
 *     -> one retained next State
 *
 * Feature code cannot bypass the reducer with an arbitrary current -> next lambda. Every action
 * family therefore has one inspectable processing surface.
 */
abstract class VexActionRelay<
    Action : VexAction,
    State : VexState<State>,
>(
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
     * The one required action reducer.
     *
     * Read it as:
     *
     *     this named action
     *     + this exact current State
     *     -> this next State
     *
     * Keep exhaustive action handling here (normally a sealed interface + when). Extract large pure
     * calculations into named functions, but keep the action-to-next-state relationship visible here.
     *
     * Do not deepCopy() for Relay ownership in this method. The store owns retention/exposure
     * snapshots around this reducer.
     */
    protected abstract fun nextStateFor(
        action: Action,
        currentState: State,
    ): State

    /**
     * Submit one immutable semantic action.
     *
     * The returned State is the exact transition result after atomic commit/no-op resolution.
     */
    protected fun sendAction(
        action: Action,
    ): State =
        store.transition(
            input = action,
            // VexAction is immutable by contract; action identity may cross into reducer code.
            snapshotInput = { admittedAction ->
                admittedAction
            },
            nextStateFor = ::nextStateFor,
        )
}

// [VXG RealForever]
