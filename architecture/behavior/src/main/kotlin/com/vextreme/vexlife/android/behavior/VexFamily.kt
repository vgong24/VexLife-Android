@file:OptIn(com.vextreme.vexlife.android.execution.VexExecution.RelationshipFoundationApi::class)

package com.vextreme.vexlife.android.behavior

import com.vextreme.vexlife.android.execution.VexExecution
import kotlin.reflect.KClass
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
 * A caller may call [activate], realize the returned [Act.begin] through VexExecution and the
 * rightful Store/provider boundary, then return already-reconciled semantic reality through
 * [observe]. Dependency construction may create a Family, but Family does not know that a dependency
 * system exists.
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
 * Family owns semantic RelationshipId composition because that identity is an invisible consequence
 * of the already-authored live Family + Control (+ typed ResourceId when needed). It does NOT own
 * VexExecution Job custody, Store/provider access, VexLogger, dependency resolution, lifecycle, or
 * presentation mechanics. Do not create a second State holder, runtime/binding object, or
 * <Feature>Execution mirror merely to restate an Act's already-authored consequence.
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
     * Typed resource identity contributed only when Control kind alone does not distinguish the
     * maintained execution relationship.
     *
     * The concrete ID object/equality is retained; payload that does not identify the relationship
     * stays ordinary Control payload.
     */
    interface ResourceId<
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
     * Control kind is the default relationship distinction. A Control implements [Resource] only
     * when one exact typed child ID is genuinely part of that relationship.
     */
    interface Control<
        Family,
    > where
        Family : VexFamily<
            Family,
            *,
            *,
            *
        > {
        interface Resource<
            Family,
            out Id,
        > :
            Control<Family> where
            Family : VexFamily<
                Family,
                *,
                *,
                *
            >,
            Id : ResourceId<Family> {
            val resourceId:
                Id
        }
    }

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
     * [begin] is an inert description of the optional runtime transformation and its overlap law.
     *
     * Begin is authored here because Independent / Earliest / Latest / Queued describe the
     * relationship of THIS Act. It does not launch a coroutine, call a Store, or log anything while
     * [act] is being resolved. The caller of [activate] later hands this already-authored consequence
     * to VexExecution.
     */
    @ConsistentCopyVisibility
    data class Act<
        State : VexFamily.State<*>,
        Effect : VexFamily.Effect<*>,
    > internal constructor(
        val admittedState:
            State?,
        val begin:
            Begin<Effect>?,
    ) {
        val effect:
            Effect?
            get() =
                begin
                    ?.effect

        /**
         * Execution relationship authored by the Act, not an executor and not another Effect layer.
         */
        @ConsistentCopyVisibility
        data class Begin<
            Effect : VexFamily.Effect<*>,
        > internal constructor(
            val approach:
                VexExecution.Approach,
            val effect:
                Effect,
        )
    }

    private class FamilyRelationshipId(
        private val family:
            Any,
        private val controlType:
            KClass<*>,
        private val controlName:
            String,
        private val resourceId:
            ResourceId<*>?,
    ) :
        VexExecution.RelationshipId {
        override fun equals(
            other:
                Any?,
        ):
            Boolean =
            other is
                FamilyRelationshipId &&
                family ===
                other.family &&
                controlType ==
                other.controlType &&
                controlName ==
                other.controlName &&
                resourceId ==
                other.resourceId

        override fun hashCode():
            Int {
            var result =
                System.identityHashCode(
                    family,
                )
            result =
                31 *
                    result +
                    controlType.hashCode()
            result =
                31 *
                    result +
                    controlName.hashCode()
            result =
                31 *
                    result +
                    (
                        resourceId
                            ?.hashCode()
                            ?: 0
                    )
            return result
        }
    }

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
     * launch, or other runtime effect may happen while resolving. [begin], [beginEarliest],
     * [beginLatest], and [beginQueued] only author inert Act consequence data here.
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
        begin:
            Act.Begin<FamilyEffect>? =
            null,
    ):
        Act<
            FamilyState,
            FamilyEffect
        > =
        Act(
            admittedState =
                null,
            begin =
                begin,
        )

    protected fun admit(
        state:
            FamilyState,
        begin:
            Act.Begin<FamilyEffect>? =
            null,
    ):
        Act<
            FamilyState,
            FamilyEffect
        > =
        Act(
            admittedState =
                state,
            begin =
                begin,
        )

    /** Author an independent runtime consequence without starting it during [act]. */
    protected fun begin(
        effect:
            FamilyEffect,
    ):
        Act.Begin<FamilyEffect> =
        Act.Begin(
            approach =
                VexExecution.Approach.Independent,
            effect =
                effect,
        )

    /** Author Earliest overlap semantics for this Act; execution remains deferred to the caller. */
    protected fun beginEarliest(
        effect:
            FamilyEffect,
    ):
        Act.Begin<FamilyEffect> =
        Act.Begin(
            approach =
                VexExecution.Approach.Earliest,
            effect =
                effect,
        )

    /** Author Latest overlap semantics for this Act; execution remains deferred to the caller. */
    protected fun beginLatest(
        effect:
            FamilyEffect,
    ):
        Act.Begin<FamilyEffect> =
        Act.Begin(
            approach =
                VexExecution.Approach.Latest,
            effect =
                effect,
        )

    /** Author Queued overlap semantics for this Act; execution remains deferred to the caller. */
    protected fun beginQueued(
        effect:
            FamilyEffect,
    ):
        Act.Begin<FamilyEffect> =
        Act.Begin(
            approach =
                VexExecution.Approach.Queued,
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
     *     -> return Act, including any authored Begin
     *
     * Family never realizes [Act.begin]. The caller hands that consequence to VexExecution after
     * activation; this keeps availability/act resolution side-effect free.
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
     * Mechanical active-work identity for one accepted Control relationship.
     *
     * This is deliberately an invisible consequence of semantic structure already authored here:
     *
     *     live Family instance
     *     + Control kind/name
     *     + optional typed Control.ResourceId
     *
     * Ordinary Control payload is not identity. Callers pass this value to VexExecution when
     * realizing [Act.begin]; they must not build a second feature relationship key beside it.
     */
    fun relationshipId(
        control:
            FamilyControl,
    ):
        VexExecution.RelationshipId {
        val controlType:
            KClass<*> =
            control::class
        val controlName =
            (
                control as?
                    Enum<*>
            )
                ?.name
                ?: requireNotNull(
                    controlType.simpleName,
                ) {
                    "VexFamily Control requires a named class or enum entry"
                }
        val resourceId =
            (
                control as?
                    Control.Resource<
                        *,
                        *
                    >
            )
                ?.resourceId

        return FamilyRelationshipId(
            family =
                this,
            controlType =
                controlType,
            controlName =
                controlName,
            resourceId =
                resourceId,
        )
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
