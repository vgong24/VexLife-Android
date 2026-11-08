@file:OptIn(com.vextreme.vexlife.android.execution.VexExecution.RelationshipFoundationApi::class)

package com.vextreme.vexlife.android.behavior

import com.vextreme.vexlife.android.execution.VexExecution
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import kotlin.reflect.KClass
import kotlinx.coroutines.Job
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
 * A caller may call [activate], bind the returned accepted Act to one Family-owned executable
 * [Act.Method] through [Act.launchWith], and let VexExecution provide Job custody while the Method
 * touches the rightful Store/provider boundary. Already-reconciled semantic reality returns through
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
 *        -> execution Approach
 *
 *     accepted Act + bound Method
 *     -> VexExecution custody
 *     -> Method realization
 *     -> VexMethod.Result.Success / Failure / Error
 *     -> rightful interpretation
 *
 *     outside/provider truth
 *     -> already-semantic State
 *     -> observe(State)
 *
 * VexMethod.Result reports realization outcome; it does not become later State admission or cancellation.
 *
 * RESPONSIBILITY FENCE
 * Family owns semantic RelationshipId composition because that identity is an invisible consequence
 * of the already-authored live Family + Control (+ typed ResourceId when needed). It does NOT own
 * VexExecution Job custody, Store/provider access, VexLogger, dependency resolution, lifecycle, or
 * presentation mechanics. The consequence's feature/domain meaning remains on the semantic owner;
 * VexExecution contributes custody/overlap mechanics without acquiring a peer feature vocabulary.
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
     * Family-owned semantic value that may remain recognizable across State, Control, Act,
     * executable Method, observation, or convergence without becoming an unscoped primitive.
     *
     * Parameter names what the value IS inside one Family's world. It does not mirror a Method name,
     * imply current State, or require every ordinary Kotlin argument to become a Parameter.
     */
    interface Parameter<
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
     * Effect is not resulting truth. When an Act carries an Effect, [Act.approach] authors how that
     * runtime consequence overlaps with other active work; the later realization boundary attempts
     * it and observed reality comes back separately as State.
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
     * [effect] is the optional requested runtime transformation.
     * [approach] is the one overlap law for that Effect and is authored here, not at execution time.
     *
     * Effect + Approach are inert semantic consequence data. They do not launch a coroutine, call a
     * Store, or log anything while [act] is being resolved. The caller of [activate] later hands the
     * already-authored consequence to VexExecution.
     */
    @ConsistentCopyVisibility
    data class Act<
        Family,
        State,
        Effect,
    > internal constructor(
        val admittedState:
            State?,
        val effect:
            Effect?,
        /**
         * Execution overlap law for [effect].
         *
         * null means this Act has no runtime Effect. Independent / Earliest / Latest / Queued are
         * meaningful only when [effect] exists.
         */
        val approach:
            VexExecution.Approach?,
        /**
         * Accepted maintained-work identity mechanically bound by [activate] from the triggering
         * live Family + Control (+ typed ResourceId when required).
         *
         * null means this Act has no maintained execution relationship (no Effect, or Independent).
         * Realization consumes this coordinate; callers do not reconstruct it from Control.
         */
        val relationshipId:
            VexExecution.RelationshipId? =
            null,
    ) where
        Family : VexFamily<
            Family,
            *,
            *,
            *
        >,
        State : VexFamily.State<Family>,
        Effect : VexFamily.Effect<Family> {
        init {
            require(
                (effect == null) ==
                    (approach == null),
            ) {
                "VexFamily Act Effect and Approach must either both exist or both be absent"
            }
        }

        /*
         * Acceptance binds the live Family execution owner after pure act(...) resolution without
         * putting runtime-owner identity into Act equality/copy semantics.
         */
        private var executionOwner:
            Any? =
            null

        /**
         * Family-owned specialization of the reusable [VexMethod] contract.
         *
         * Family kinship stays here because this Method realizes one accepted Family Act. The
         * executable/result contract itself is shared through [VexMethod].
         */
        interface Method<
            Family,
            out SuccessOutput,
            out FailureOutput,
        > :
            VexMethod<
                SuccessOutput,
                FailureOutput
            > where
            Family : VexFamily<
                Family,
                *,
                *,
                *
            >


        /**
         * Compiler-visible continuation from an accepted Act into its Method.
         *
         * This type owns no semantic meaning. It only prevents callers from re-supplying the
         * already-authored Approach, RelationshipId, or execution owner while selecting the exact
         * bound Method that will realize the consequence.
         */
        class MethodFollowUp<
            Family,
        > internal constructor(
            private val executionRoot:
                VexExecutionRoot,
            private val owner:
                Any,
            private val approach:
                VexExecution.Approach,
            private val relationshipId:
                VexExecution.RelationshipId?,
        ) where
            Family : VexFamily<
                Family,
                *,
                *,
                *
            > {
            fun <SuccessOutput, FailureOutput> method(
                method:
                    Method<
                        Family,
                        SuccessOutput,
                        FailureOutput
                    >,
                beforeOperation:
                    () -> Unit =
                    {},
                onResult:
                    (
                        VexMethod.Result<
                            SuccessOutput,
                            FailureOutput
                        >
                    ) -> Unit =
                    {},
            ):
                Job =
                VexExecution.begin(
                    executionRoot =
                        executionRoot,
                    owner =
                        owner,
                    approach =
                        approach,
                    relationshipId =
                        relationshipId,
                    operation =
                        method::invoke,
                    beforeOperation =
                        beforeOperation,
                    onResult = { result ->
                        onResult(
                            result,
                        )
                    },
                    onFailure = { error ->
                        onResult(
                            VexMethod.Result.Error(
                                cause =
                                    error,
                            ),
                        )
                    },
                )
        }

        /**
         * Continue one accepted Act through the execution relationship it already authored.
         *
         * null means this Act has no runtime consequence. The caller supplies only the
         * VexExecutionRoot and then one bound Family Method; Approach, RelationshipId, and the live
         * Family owner are consumed from this accepted Act rather than reconstructed downstream.
         */
        fun launchWith(
            executionRoot:
                VexExecutionRoot,
        ):
            MethodFollowUp<Family>? {
            if (
                effect ==
                null
            ) {
                return null
            }

            val authoredApproach =
                checkNotNull(
                    approach,
                ) {
                    "VexFamily Act Effect requires an authored execution Approach"
                }
            val owner =
                checkNotNull(
                    executionOwner,
                ) {
                    "Only an Act accepted by VexFamily.activate(...) may launch"
                }

            return MethodFollowUp(
                executionRoot =
                    executionRoot,
                owner =
                    owner,
                approach =
                    authoredApproach,
                relationshipId =
                    relationshipId,
            )
        }

        internal fun acceptedBy(
            owner:
                Any,
            relationshipId:
                VexExecution.RelationshipId?,
        ):
            Act<
                Family,
                State,
                Effect
            > =
            Act(
                admittedState =
                    admittedState,
                effect =
                    effect,
                approach =
                    approach,
                relationshipId =
                    relationshipId,
            )
                .also { accepted ->
                    accepted.executionOwner =
                        owner
                }

        /*
         * Approach answers only the overlap meaning of [effect]:
         *
         * Independent
         *     every accepted consequence remains independently meaningful
         *
         * Earliest
         *     an unfinished consequence for this relationship is already sufficient
         *
         * Latest
         *     the newest accepted consequence supersedes unfinished predecessors
         *
         * Queued
         *     every accepted consequence remains meaningful and order must be preserved
         *
         * This choice is orthogonal to immediate State admission. A same-State Act may be Latest;
         * a State-changing Act may be Independent, Earliest, Latest, or Queued according to the
         * relationship being authored. VexExecution consumes this choice; it does not re-author it.
         *
         * Earlier #207 source carried richer runtime-consequence grammar around later-State
         * admission. That responsibility is gone. Current Act therefore carries Effect + Approach
         * directly instead of retaining another wrapper around the same consequence pair.
         */
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
     * launch, or other runtime effect may happen while resolving. Effect + Approach are only inert
     * Act consequence data here.
     */
    protected abstract fun act(
        currentState: FamilyState,
        control: FamilyControl,
    ):
        Act<
            Family,
            FamilyState,
            FamilyEffect
        >?

    protected fun stay():
        Act<
            Family,
            FamilyState,
            FamilyEffect
        > =
        Act(
            admittedState =
                null,
            effect =
                null,
            approach =
                null,
            relationshipId =
                null,
        )

    protected fun stay(
        effect:
            FamilyEffect,
        approach:
            VexExecution.Approach =
            VexExecution.Approach.Independent,
    ):
        Act<
            Family,
            FamilyState,
            FamilyEffect
        > =
        Act(
            admittedState =
                null,
            effect =
                effect,
            approach =
                approach,
            relationshipId =
                null,
        )

    protected fun admit(
        state:
            FamilyState,
    ):
        Act<
            Family,
            FamilyState,
            FamilyEffect
        > =
        Act(
            admittedState =
                state,
            effect =
                null,
            approach =
                null,
            relationshipId =
                null,
        )

    protected fun admit(
        state:
            FamilyState,
        effect:
            FamilyEffect,
        approach:
            VexExecution.Approach =
            VexExecution.Approach.Independent,
    ):
        Act<
            Family,
            FamilyState,
            FamilyEffect
        > =
        Act(
            admittedState =
                state,
            effect =
                effect,
            approach =
                approach,
            relationshipId =
                null,
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
     *     -> bind maintained RelationshipId when the authored Approach requires one
     *     -> admit immediate State when present
     *     -> return accepted Act
     *
     * Family never realizes [Act.effect]. The caller hands that consequence to VexExecution after
     * activation; this keeps availability/act resolution side-effect free.
     */
    fun activate(
        control:
            FamilyControl,
    ):
        Act<
            Family,
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

            val acceptedRelationshipId =
                when (
                    resolved.approach
                ) {
                    VexExecution.Approach.Earliest,
                    VexExecution.Approach.Latest,
                    VexExecution.Approach.Queued,
                    ->
                        relationshipId(
                            control,
                        )

                    VexExecution.Approach.Independent,
                    null,
                    ->
                        null
                }

            val accepted =
                resolved.acceptedBy(
                    owner =
                        this,
                    relationshipId =
                        acceptedRelationshipId,
                )

            accepted.admittedState
                ?.let(
                    ::setCurrentStateLocked,
                )

            accepted
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
     * Ordinary Control payload is not identity. [activate] binds this coordinate onto the accepted
     * Act so realization consumes the authored relationship without reconstructing Control identity.
     */
    private fun relationshipId(
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
