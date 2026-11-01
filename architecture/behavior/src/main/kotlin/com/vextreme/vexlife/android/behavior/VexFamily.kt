package com.vextreme.vexlife.android.behavior

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Semantic owner for one exact State / Control / Effect vocabulary.
 *
 * WHAT THIS IS
 * Family answers one question: what does this Control mean against exact CURRENT semantic State?
 * It owns that meaning and the one current semantic State holder.
 *
 * WHAT IT RELATES TO
 * A concrete feature runtime may call [activate], realize the returned [Act.effect] through its
 * Store/provider boundary, report accepted/observed facts to VexLogger, and later return already-
 * reconciled semantic reality through [observe]. Dependency construction may create a Family, but
 * Family does not know that a dependency system exists.
 *
 * WHAT IT ADDRESSES / WHY THIS SHAPE EXISTS
 * Keeping State + Control -> Act in one synchronous semantic owner prevents provider mechanics,
 * coroutine timing, persistence, logging, and UI presentation from becoming alternate places where
 * product meaning is decided. Effect is returned rather than executed so requested transformation
 * remains distinct from resulting truth.
 *
 * Core loop:
 *
 *     Control + CURRENT State
 *     -> Act?
 *     -> admitted State? + Effect?
 *
 * RESPONSIBILITY FENCE
 * Do not move Store/provider access, VexExecution, RelationshipId construction, VexLogger, dependency
 * resolution, lifecycle ownership, or presentation mechanics into Family. Do not create a second
 * State holder or generic runtime/binding layer that restates this semantic owner. A reusable layer
 * is earned only by repeated real consumers, not by making this patient-zero shape universal.
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
     * Effect is not resulting truth. A concrete feature runtime may attempt the Effect; observed reality
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
     * The exact Family + State coordinates remain in the reusable type so display interpretation
     * cannot silently detach from its semantic owner.
     *
     * DisplayState is derived outward interpretation of State, never another truth owner.
     */
    interface DisplayState<
        Family,
        State,
    > where
        Family : VexFamily<
            Family,
            State,
            *,
            *
        >,
        State : VexFamily.State<Family> {
        data class StateOnly<
            Family,
            State,
        >(
            val state:
                State,
        ) :
            DisplayState<
                Family,
                State
            > where
            Family : VexFamily<
                Family,
                State,
                *,
                *
            >,
            State : VexFamily.State<Family>
    }

    /**
     * Immutable outward snapshot of one exact Family State and the DisplayState derived from that
     * same State value.
     *
     * Projection is not another truth owner. The Family coordinate prevents unrelated State /
     * DisplayState pairs from being assembled into one apparently valid projection.
     */
    data class Projection<
        Family,
        State,
        out Display,
    >(
        val state:
            State,
        val displayState:
            Display,
    ) where
        Family : VexFamily<
            Family,
            State,
            *,
            *
        >,
        State : VexFamily.State<Family>,
        Display : DisplayState<
            Family,
            State
        >

    /**
     * One lawful outcome resolved from exact CURRENT State + Control.
     *
     * [admittedState] is immediate semantic State meaning.
     * [effect] is the optional runtime transformation request to realize after that admission.
     */
    @ConsistentCopyVisibility
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

    /**
     * Derive outward display meaning from one exact semantic State snapshot.
     *
     * Concrete Families override this seam when richer repeated display interpretation is earned.
     */
    open fun displayStateFor(
        state:
            FamilyState,
    ):
        DisplayState<
            Family,
            FamilyState
        > =
        DisplayState.StateOnly<
            Family,
            FamilyState
        >(
            state,
        )

    /**
     * Bind one exact State snapshot to the DisplayState derived from that same value.
     */
    open fun projectionFor(
        state:
            FamilyState,
    ):
        Projection<
            Family,
            FamilyState,
            DisplayState<
                Family,
                FamilyState
            >
        > =
        Projection(
            state =
                state,
            displayState =
                displayStateFor(
                    state,
                ),
        )

    open val currentProjection:
        Projection<
            Family,
            FamilyState,
            DisplayState<
                Family,
                FamilyState
            >
        >
        get() =
            synchronized(
                stateLock,
            ) {
                projectionFor(
                    stateRelay.value,
                )
            }

    open val displayState:
        DisplayState<
            Family,
            FamilyState
        >
        get() =
            currentProjection
                .displayState

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
     * Family never realizes [Act.effect]. A concrete feature runtime owns that work.
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
