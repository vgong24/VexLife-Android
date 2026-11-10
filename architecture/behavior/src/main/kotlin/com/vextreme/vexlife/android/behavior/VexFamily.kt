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
 * [act] authors pure semantic meaning as an [Act]. [activate] accepts that exact authored Act into
 * this live Family as [Act.Accepted], applies immediate State, binds the maintained relationship
 * coordinate when required, and exposes [Act.Accepted.realizationBegin] only while that authored
 * consequence still requires a new runtime occurrence. A caller binds that exact Effect to one
 * Family-owned executable [Act.Method] and uses [Act.Accepted.launch] to cross into VexExecution
 * custody. Already-reconciled semantic reality returns through [observe]. Dependency construction
 * may create a Family, but Family does not know that a dependency system exists.
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
 *     -> authored Act?
 *        -> admitted State? + Begin?
 *           -> Effect + execution Approach
 *     -> Act.Accepted
 *        -> maintained RelationshipId when required
 *        -> realizationBegin?   # live remaining realization
 *
 *     Act.Accepted + bound Method
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
     * Effect is not resulting truth. [Act.Begin] may author how that Effect overlaps with other
     * active work; the later realization boundary attempts it and observed reality comes back
     * separately as State.
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
     * [begin] is an inert description of the optional runtime consequence and its overlap law.
     *
     * Begin is authored here because Independent / Earliest / Latest / Queued describe the
     * relationship of THIS Act. It says that runtime consequence remains after the semantic decision;
     * it does not launch a coroutine, call a Store, or log anything while [act] is being resolved.
     * [activate] later accepts this Act into the live Family before any realization may enter
     * VexExecution custody.
     */
    @ConsistentCopyVisibility
    data class Act<
        Family,
        State,
        Effect,
    > internal constructor(
        val admittedState:
            State?,
        val begin:
            Begin<
                Family,
                Effect
            >?,
    ) where
        Family : VexFamily<
            Family,
            *,
            *,
            *
        >,
        State : VexFamily.State<Family>,
        Effect : VexFamily.Effect<Family> {
        /**
         * Family-owned specialization of the reusable [VexMethod] contract.
         *
         * A Method gives one exact authored Effect its concrete means of realization, such as a
         * Store or live Connection, without re-authoring the Effect, Approach, RelationshipId, or
         * Family owner. Family kinship stays here because this Method realizes one accepted Family
         * Act. The executable/result contract itself is shared through [VexMethod].
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
            > {
            /**
             * Exact accepted Family Effect this Method realizes.
             *
             * Method construction binds realization to the Effect object already authored by
             * [Begin]. Callers do not reconstruct Effect payload into a second semantic input.
             */
            val effect:
                VexFamily.Effect<Family>
        }

        /**
         * Live accepted form of one authored [Act].
         *
         * [authored] remains the exact pure State / Control consequence resolved by [act]. Acceptance
         * adds the live Family owner, maintained [relationshipId] when required, and the current
         * question of whether that authored Begin still requires a new runtime occurrence.
         *
         * [realizationBegin] is deliberately separate from [authored].begin. Earliest may already be
         * covered by an earlier unresolved realization, and Latest may be superseded before custody;
         * neither live disposition rewrites what the pure Act originally authored.
         */
        class Accepted<
            Family,
            State,
            Effect,
        > internal constructor(
            val authored:
                Act<
                    Family,
                    State,
                    Effect
                >,
            /**
             * Maintained-work identity bound by [activate] from the live Family + triggering Control
             * (+ typed ResourceId when that Control relationship requires one).
             *
             * This coordinate belongs to acceptance, not pure [Act] resolution. Ordinary Control
             * payload is not identity, and realization consumes this value rather than reconstructing
             * the relationship downstream.
             */
            val relationshipId:
                VexExecution.RelationshipId?,
            private val executionOwner:
                Any,
            private val realizationBeginProvider:
                () ->
                    Begin<
                        Family,
                        Effect
                    >?,
            private val realizationAdmission:
                (
                    VexExecutionRoot,
                    (
                        Begin<
                            Family,
                            Effect
                        >
                    ) -> Job
                ) -> Job?,
        ) where
            Family : VexFamily<
                Family,
                *,
                *,
                *
            >,
            State : VexFamily.State<Family>,
            Effect : VexFamily.Effect<Family> {

            /**
             * Live preparation view of the authored Begin that still requires a new occurrence.
             *
             * This view may become null before custody (for example Latest supersession). [launch]
             * independently re-checks the live admission boundary atomically; callers must not treat
             * an earlier non-null read as custody authority.
             */
            val realizationBegin:
                Begin<
                    Family,
                    Effect
                >?
                get() =
                    realizationBeginProvider()

            /**
             * Entrust the remaining realization of this accepted Act to execution custody.
             *
             * The accepted boundary preserves authored provenance and verifies that [method] carries
             * the exact Effect still requiring realization. VexExecution forms and owns the actual
             * Job occurrence. null means the accepted Act no longer requires a new occurrence (for
             * example lawful Latest supersession or Earliest coverage); it is not a fabricated Job.
             */
            fun <SuccessOutput, FailureOutput> launch(
                executionRoot:
                    VexExecutionRoot,
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
                Job? {
                checkNotNull(
                    authored.begin,
                ) {
                    "Only an accepted Act whose authored Act has Begin may launch a Method"
                }

                val job =
                    realizationAdmission(
                        executionRoot,
                    ) { currentBegin ->
                        check(
                            method.effect ===
                                currentBegin.effect,
                        ) {
                            "Act Method must carry the exact accepted realization Effect"
                        }

                        VexExecution.admit(
                            executionRoot =
                                executionRoot,
                            owner =
                                executionOwner,
                            approach =
                                currentBegin.approach,
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

                job
                    ?.start()

                return job
            }
        }

        internal fun acceptedBy(
            owner:
                Any,
            relationshipId:
                VexExecution.RelationshipId?,
            realizationBeginProvider:
                () ->
                    Begin<
                        Family,
                        Effect
                    >?,
            realizationAdmission:
                (
                    VexExecutionRoot,
                    (
                        Begin<
                            Family,
                            Effect
                        >
                    ) -> Job
                ) -> Job?,
        ):
            Accepted<
                Family,
                State,
                Effect
            > =
            Accepted(
                authored =
                    this,
                relationshipId =
                    relationshipId,
                executionOwner =
                    owner,
                realizationBeginProvider =
                    realizationBeginProvider,
                realizationAdmission =
                    realizationAdmission,
            )

        /**
         * Execution relationship authored by the Act, not an executor and not another Effect layer.
         *
         * Approach answers only the overlap meaning of this consequence:
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
         * This choice is orthogonal to immediate State admission. stay(...) does not imply one
         * Approach and admit(State) does not imply another. A same-State Act may be Latest; a
         * State-changing Act may be Independent, Earliest, Latest, or Queued according to the
         * relationship being authored.
         *
         * Earlier #207 source let Begin author success/failure-specific later-State admission.
         * The current Family keeps that semantic decision removed: Begin authors only Effect +
         * overlap meaning. [VexMethod.Result] reports the later realization outcome, but does not
         * prescribe State. Outside truth is still interpreted by its rightful owner and
         * already-semantic truth re-enters through [observe].
         */
        @ConsistentCopyVisibility
        data class Begin<
            Family,
            Effect,
        > internal constructor(
            val approach:
                VexExecution.Approach,
            val effect:
                Effect,
        ) where
            Family : VexFamily<
                Family,
                *,
                *,
                *
            >,
            Effect : VexFamily.Effect<Family>
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

    /*
     * Acceptance-side continuity only. VexExecution remains the Job-custody owner.
     *
     * A binding is the private one-shot admission state for one accepted Begin. A lane retains only
     * unresolved accepted realizations for one maintained RelationshipId so Approach meaning cannot
     * be reordered between activate(...) and launch(...).
     */
    private class RealizationBinding {
        var admitted:
            Boolean =
            false

        var superseded:
            Boolean =
            false
    }

    private class AcceptanceLane(
        val approach:
            VexExecution.Approach,
    ) {
        val bindings:
            MutableList<
                RealizationBinding
            > =
            mutableListOf()

        /*
         * Runtime continuity only. While one or more bindings still have Job custody, overlapping
         * admissions must stay inside the same root so one Approach does not split across custody maps.
         */
        var custodyRoot:
            VexExecutionRoot? =
            null
    }

    private val relationshipLock =
        Any()

    private val acceptanceLanes =
        linkedMapOf<
            VexExecution.RelationshipId,
            AcceptanceLane
        >()


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
            Family,
            FamilyState,
            FamilyEffect
        >?

    protected fun stay(
        begin:
            Act.Begin<
                Family,
                FamilyEffect
            >? =
            null,
    ):
        Act<
            Family,
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
            Act.Begin<
                Family,
                FamilyEffect
            >? =
            null,
    ):
        Act<
            Family,
            FamilyState,
            FamilyEffect
        > =
        Act(
            admittedState =
                state,
            begin =
                begin,
        )

    /** Author a consequence where every accepted occurrence remains independently meaningful. */
    protected fun begin(
        effect:
            FamilyEffect,
    ):
        Act.Begin<
            Family,
            FamilyEffect
        > =
        Act.Begin(
            approach =
                VexExecution.Approach.Independent,
            effect =
                effect,
        )

    /** Author a relationship where one unfinished earliest consequence is already sufficient. */
    protected fun beginEarliest(
        effect:
            FamilyEffect,
    ):
        Act.Begin<
            Family,
            FamilyEffect
        > =
        Act.Begin(
            approach =
                VexExecution.Approach.Earliest,
            effect =
                effect,
        )

    /** Author a relationship where the newest accepted consequence supersedes unfinished predecessors. */
    protected fun beginLatest(
        effect:
            FamilyEffect,
    ):
        Act.Begin<
            Family,
            FamilyEffect
        > =
        Act.Begin(
            approach =
                VexExecution.Approach.Latest,
            effect =
                effect,
        )

    /** Author a relationship where every accepted consequence remains meaningful in arrival order. */
    protected fun beginQueued(
        effect:
            FamilyEffect,
    ):
        Act.Begin<
            Family,
            FamilyEffect
        > =
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
     *     -> bind maintained RelationshipId when Begin requires one
     *     -> preserve accepted overlap order until realization enters custody
     *     -> bind the live Family as execution owner
     *     -> admit immediate State when present
     *     -> return accepted Act
     *
     * Acceptance turns pure semantic meaning into this live Family's current responsibility without
     * rewriting [Act.begin]. [Act.Accepted.realizationBegin] separately reports whether that authored
     * consequence still requires a new runtime occurrence: Latest supersedes older pending
     * realizations, Queued preserves pending acceptance order, and Earliest may already be covered by
     * an earlier unresolved realization. A caller may bind only the remaining realization Begin to a
     * concrete Method and hand it to VexExecution through [Act.Accepted.launch].
     */
    fun activate(
        control:
            FamilyControl,
    ):
        Act.Accepted<
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

            val authoredBegin =
                resolved.begin
            var acceptedRelationshipId:
                VexExecution.RelationshipId? =
                null
            var realizationBinding:
                RealizationBinding? =
                null

            when (
                val approach =
                    authoredBegin
                        ?.approach
            ) {
                null ->
                    Unit

                VexExecution.Approach.Independent ->
                    realizationBinding =
                        RealizationBinding(
                        )

                VexExecution.Approach.Earliest,
                VexExecution.Approach.Latest,
                VexExecution.Approach.Queued,
                -> {
                    val relationshipId =
                        relationshipId(
                            control,
                        )
                    acceptedRelationshipId =
                        relationshipId

                    synchronized(
                        relationshipLock,
                    ) {
                        val existing =
                            acceptanceLanes[
                                relationshipId
                            ]
                        if (
                            existing !=
                            null
                        ) {
                            check(
                                existing.approach ==
                                    approach,
                            ) {
                                "Accepted relationship is already unresolved as " +
                                    existing.approach +
                                    " and cannot also be " +
                                    approach
                            }
                        }

                        val lane =
                            existing
                                ?: AcceptanceLane(
                                    approach =
                                        approach,
                                )
                                    .also { created ->
                                        acceptanceLanes[
                                            relationshipId
                                        ] =
                                            created
                                    }

                        when (
                            approach
                        ) {
                            VexExecution.Approach.Earliest ->
                                if (
                                    lane.bindings.isEmpty()
                                ) {
                                    realizationBinding =
                                        RealizationBinding(
                                        )
                                            .also(
                                                lane.bindings::add,
                                            )
                                }

                            VexExecution.Approach.Latest -> {
                                lane.bindings
                                    .filter { binding ->
                                        !binding.admitted
                                    }
                                    .forEach { binding ->
                                        binding.superseded =
                                            true
                                    }
                                lane.bindings
                                    .removeAll { binding ->
                                        !binding.admitted
                                    }

                                realizationBinding =
                                    RealizationBinding(
                                    )
                                        .also(
                                            lane.bindings::add,
                                        )
                            }

                            VexExecution.Approach.Queued ->
                                realizationBinding =
                                    RealizationBinding(
                                    )
                                        .also(
                                            lane.bindings::add,
                                        )

                            VexExecution.Approach.Independent ->
                                error(
                                    "Independent realization does not use a maintained acceptance lane",
                                )
                        }
                    }
                }
            }

            val realizationBeginProvider:
                () ->
                    Act.Begin<
                        Family,
                        FamilyEffect
                    >? =
                if (
                    authoredBegin ==
                        null ||
                    realizationBinding ==
                        null
                ) {
                    {
                        null
                    }
                } else {
                    val binding =
                        checkNotNull(
                            realizationBinding,
                        )
                    val begin =
                        authoredBegin

                    {
                        synchronized(
                            relationshipLock,
                        ) {
                            if (
                                !binding.admitted &&
                                !binding.superseded
                            ) {
                                begin
                            } else {
                                null
                            }
                        }
                    }
                }

            val realizationAdmission:
                (
                    VexExecutionRoot,
                    (
                        Act.Begin<
                            Family,
                            FamilyEffect
                        >
                    ) -> Job
                ) -> Job? =
                if (
                    realizationBinding !=
                        null &&
                    authoredBegin !=
                        null
                ) {
                    val binding =
                        checkNotNull(
                            realizationBinding,
                        )
                    val begin =
                        authoredBegin

                    { executionRoot, formJob ->
                        admitRealization(
                            binding =
                                binding,
                            relationshipId =
                                acceptedRelationshipId,
                            begin =
                                begin,
                            executionRoot =
                                executionRoot,
                            formJob =
                                formJob,
                        )
                    }
                } else {
                    { _, _ ->
                        null
                    }
                }

            val accepted =
                resolved.acceptedBy(
                    owner =
                        this,
                    relationshipId =
                        acceptedRelationshipId,
                    realizationBeginProvider =
                        realizationBeginProvider,
                    realizationAdmission =
                        realizationAdmission,
                )

            resolved.admittedState
                ?.let(
                    ::setCurrentStateLocked,
                )

            accepted
        }

    /**
     * Admit one accepted Begin to runtime custody exactly once.
     *
     * This is mechanical acceptance-side continuity only. VexExecution still owns the resulting Job
     * and active overlap mechanics.
     */
    private fun admitRealization(
        binding:
            RealizationBinding,
        relationshipId:
            VexExecution.RelationshipId?,
        begin:
            Act.Begin<
                Family,
                FamilyEffect
            >,
        executionRoot:
            VexExecutionRoot,
        formJob:
            (
                Act.Begin<
                    Family,
                    FamilyEffect
                >
            ) -> Job,
    ):
        Job? =
        synchronized(
            relationshipLock,
        ) {
            check(
                !binding.admitted,
            ) {
                "Accepted Act realization has already entered execution custody"
            }
            if (
                binding.superseded
            ) {
                return@synchronized null
            }

            relationshipId
                ?.let { relationshipId ->
                    val lane =
                        checkNotNull(
                            acceptanceLanes[
                                relationshipId
                            ],
                        ) {
                            "Accepted Act realization is missing its acceptance lane"
                        }
                    check(
                        lane.approach ==
                            begin.approach,
                    ) {
                        "Accepted Act realization Approach no longer matches its acceptance lane"
                    }

                    if (
                        begin.approach ==
                        VexExecution.Approach.Queued
                    ) {
                        val firstPending =
                            lane.bindings
                                .firstOrNull { candidate ->
                                    !candidate.admitted &&
                                        !candidate.superseded
                                }
                        check(
                            firstPending ===
                                binding,
                        ) {
                            "Queued accepted consequence cannot enter custody before an earlier accepted consequence"
                        }
                    }

                    if (
                        begin.approach ==
                        VexExecution.Approach.Latest
                    ) {
                        val latestPending =
                            lane.bindings
                                .lastOrNull { candidate ->
                                    !candidate.admitted &&
                                        !candidate.superseded
                                }
                        check(
                            latestPending ===
                                binding,
                        ) {
                            "Latest accepted consequence is no longer the newest pending realization"
                        }
                    }

                    if (
                        begin.approach ==
                        VexExecution.Approach.Earliest
                    ) {
                        check(
                            lane.bindings
                                .firstOrNull() ===
                                binding,
                        ) {
                            "Earliest accepted consequence is no longer the unresolved earliest realization"
                        }
                    }

                    lane.custodyRoot
                        ?.let { activeRoot ->
                            check(
                                activeRoot ===
                                    executionRoot,
                            ) {
                                "Overlapping accepted realizations must share one VexExecutionRoot"
                            }
                        }
                        ?: run {
                            lane.custodyRoot =
                                executionRoot
                        }
                }

            /*
             * Mark admission before the lazy Job is started so a re-entrant acceptance cannot treat
             * this binding as still pending. A pre-custody failure rolls the flag/root back.
             */
            binding.admitted =
                true

            val job =
                try {
                    formJob(
                        begin,
                    )
                } catch (
                    failure:
                        Throwable,
                ) {
                    binding.admitted =
                        false

                    relationshipId
                        ?.let { relationshipId ->
                            acceptanceLanes[
                                relationshipId
                            ]
                                ?.let { lane ->
                                    if (
                                        lane.bindings
                                            .none { candidate ->
                                                candidate.admitted
                                            }
                                    ) {
                                        lane.custodyRoot =
                                            null
                                    }
                                }
                        }

                    throw failure
                }

            job.invokeOnCompletion {
                synchronized(
                    relationshipLock,
                ) {
                    relationshipId
                        ?.let { relationshipId ->
                            acceptanceLanes[
                                relationshipId
                            ]
                                ?.let { lane ->
                                    lane.bindings.remove(
                                        binding,
                                    )
                                    if (
                                        lane.bindings
                                            .none { candidate ->
                                                candidate.admitted
                                            }
                                    ) {
                                        lane.custodyRoot =
                                            null
                                    }
                                    if (
                                        lane.bindings.isEmpty()
                                    ) {
                                        acceptanceLanes.remove(
                                            relationshipId,
                                        )
                                    }
                                }
                        }
                }
            }

            job
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
