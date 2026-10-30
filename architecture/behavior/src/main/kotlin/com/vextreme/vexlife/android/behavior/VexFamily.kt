package com.vextreme.vexlife.android.behavior

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Live semantic Family for one State / Control / Effect vocabulary.
 *
 * Family owns semantic meaning and current State. It does not own provider, persistence, coroutine,
 * logging, or lifecycle mechanics.
 *
 * Core loop:
 *
 *     Control + CURRENT State
 *     -> Act?
 *     -> admitted State? + Effect?
 *
 * The admitted State is applied synchronously by [activate]. A concrete Connection/runtime may then
 * realize the returned Effect. Runtime/provider truth may later return as an already-semantic State
 * through [observe].
 */
abstract class VexFamily<
    Family,
    FamilyState,
    FamilyControl,
    FamilyEffect,
>(
    initialState: FamilyState,
) where
    Family : VexFamily<
        Family,
        FamilyState,
        FamilyControl,
        FamilyEffect
    >,
    FamilyState : VexFamily.State<Family>,
    FamilyControl : VexFamily.Control<Family>,
    FamilyEffect : VexFamily.Effect<Family> {
    /** Current semantic truth belonging to one exact Family. */
    interface State<
        Family,
    > where
        Family : VexFamily<
            Family,
            *,
            *,
            *
        >

    /**
     * Semantic request / affordance input belonging to one exact Family.
     *
     * Control is not provider observation and does not carry execution identity.
     */
    interface Control<
        Family,
    > where
        Family : VexFamily<
            Family,
            *,
            *,
            *
        >

    /**
     * Runtime transformation requested by a lawful Act.
     *
     * Effect is not resulting truth. A Connection/runtime may attempt the Effect; observed reality
     * comes back separately as State.
     */
    interface Effect<
        Family,
    > where
        Family : VexFamily<
            Family,
            *,
            *,
            *
        >

    /**
     * Family-owned display classification.
     *
     * DisplayState is derived outward interpretation of [currentState], never another truth owner.
     */
    interface DisplayState<
        out State : VexFamily.State<*>,
    > {
        data class StateOnly<
            State : VexFamily.State<*>,
        >(
            val state: State,
        ) :
            DisplayState<State>
    }

    /**
     * One lawful outcome resolved from exact CURRENT State + Control.
     *
     * [admittedState] is immediate semantic State meaning.
     * [effect] is the optional runtime transformation request to realize after that admission.
     */
    data class Act<
        State : VexFamily.State<*>,
        Effect : VexFamily.Effect<*>,
    > internal constructor(
        val admittedState:
            State?,
        val effect:
            Effect?,
    )

    private val stateLock =
        Any()

    private val stateRelay =
        MutableStateFlow(
            initialState,
        )

    val states:
        StateFlow<FamilyState> =
        stateRelay.asStateFlow()

    val currentState:
        FamilyState
        get() =
            states.value

    /**
     * Resolve one semantic Control against exact CURRENT State.
     *
     * null means no lawful Act.
     * stay(...) means lawful Act with no immediate State admission.
     * admit(...) means lawful Act with immediate State admission.
     *
     * This function is pure relationship meaning: no provider calls, persistence, logging, coroutine
     * launch, or other runtime effect may happen while resolving.
     */
    protected abstract fun act(
        currentState: FamilyState,
        control: FamilyControl,
    ):
        Act<
            FamilyState,
            FamilyEffect
        >?

    protected fun stay(
        effect:
            FamilyEffect? =
            null,
    ):
        Act<
            FamilyState,
            FamilyEffect
        > =
        Act(
            admittedState =
                null,
            effect =
                effect,
        )

    protected fun admit(
        state:
            FamilyState,
        effect:
            FamilyEffect? =
            null,
    ):
        Act<
            FamilyState,
            FamilyEffect
        > =
        Act(
            admittedState =
                state,
            effect =
                effect,
        )

    open val displayState:
        DisplayState<FamilyState>
        get() =
            DisplayState.StateOnly(
                currentState,
            )

    /**
     * Availability and activation resolve through the exact same semantic relationship.
     */
    fun isAvailable(
        control:
            FamilyControl,
    ):
        Boolean =
        synchronized(
            stateLock,
        ) {
            act(
                currentState =
                    stateRelay.value,
                control =
                    control,
            ) !=
                null
        }

    /**
     * Apply one lawful Control.
     *
     * Ordering is part of the semantic contract:
     *
     *     act(CURRENT State, Control)
     *     -> admit immediate State when present
     *     -> return Act
     *
     * Family never realizes [Act.effect]. The concrete Connection/runtime owns that work.
     */
    fun activate(
        control:
            FamilyControl,
    ):
        Act<
            FamilyState,
            FamilyEffect
        >? =
        synchronized(
            stateLock,
        ) {
            val resolved =
                act(
                    currentState =
                        stateRelay.value,
                    control =
                        control,
                )
                    ?: return null

            resolved.admittedState
                ?.let(
                    ::setCurrentStateLocked,
                )

            resolved
        }

    /**
     * Publish already-reconciled semantic reality.
     *
     * This is intentionally not a current-State callback and not a second decision DSL. The caller
     * must already own provider/repository interpretation, stale-result gating, and construction of
     * the truthful Family State before calling observe(...).
     */
    fun observe(
        state:
            FamilyState,
    ):
        FamilyState =
        synchronized(
            stateLock,
        ) {
            setCurrentStateLocked(
                state,
            )
            stateRelay.value
        }

    private fun setCurrentStateLocked(
        state:
            FamilyState,
    ) {
        stateRelay.value =
            state
    }
}

// [VXG RealForever]
