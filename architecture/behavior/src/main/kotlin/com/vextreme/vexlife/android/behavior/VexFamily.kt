@file:OptIn(com.vextreme.vexlife.android.execution.VexExecution.RelationshipFoundationApi::class)

package com.vextreme.vexlife.android.behavior

import com.vextreme.vexlife.android.execution.VexExecution
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import com.vextreme.vexlife.android.humanvetting.VettedWithHuman
import com.vextreme.vexlife.android.logging.VexLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.reflect.KClass

/**
 * Live Family runtime for one semantic State and Control vocabulary.
 *
 * [Family] is the concrete VexFamily root itself, not an arbitrary marker. The root bound closes
 * compile-time kinship so State and Control cannot agree on a fake coordinate that is not the
 * Family that actually owns them.
 *
 * Human-visible runtime call:
 *
 *     activate(control)
 *     -> act(currentState, control)
 *     -> when (currentState)
 *     -> when (control)
 *     -> stay() or admit(State)
 *     -> optional begin(...)
 *     -> StateRelay / execution
 *
 * The concrete Family owns [act]. State and Control traversal use native Kotlin sealed-type when
 * expressions; there is no custom state/on branching language between activate(...) and feature
 * meaning.
 */
@VettedWithHuman("VexFamily", "2026-10-29T22:46:00-07:00", "Victor Approved")
abstract class VexFamily<
    Family,
    FamilyState,
    FamilyControl,
>(
    initialState: FamilyState,
    private val executionRoot:
        VexExecutionRoot,
    private val logger:
        VexLogger.Sink,
) where
    Family : VexFamily<
        Family,
        FamilyState,
        FamilyControl
    >,
    FamilyState : VexFamily.State<Family>,
    FamilyControl : VexFamily.Control<Family> {
    /**
     * Compile-time kinship marker for semantic State belonging to one exact Family coordinate.
     */
    @VettedWithHuman("VexFamily.State", "2026-10-29T23:10:25-07:00", "Victor Approved")
    interface State<
        Family,
    > where
        Family : VexFamily<
            Family,
            *,
            *
        >

    /**
     * Typed resource identity contributed by a Control when Control kind alone is not enough to
     * distinguish maintained execution relationships.
     *
     * ResourceId deliberately has no shared String/value payload. The concrete ID type owns its own
     * representation and equality; Family preserves that typed object instead of flattening it.
     *
     * Equality and hashCode must remain stable for as long as the ID participates in a live
     * relationship. Prefer immutable value identity. A mutable ID whose equality/hash changes while
     * active would corrupt relationship lookup/custody rather than represent a new semantic request.
     */
    interface ResourceId<
        Family,
    > where
        Family : VexFamily<
            Family,
            *,
            *
        >

    /**
     * Compile-time kinship marker for incoming Control belonging to one exact Family coordinate.
     *
     * Human-vetted relationship identity law:
     *
     *     live Family
     *     + exact Control identity
     *     + optional stable resourceId chosen by that Control
     *     = Family relationship identity
     *
     * Control identity is always present. [resourceId] only narrows repeated semantic resources for
     * that same Control kind (for example Play(trackA) vs Play(trackB)). Ordinary Control payload is
     * not automatically identity. Two different Controls may expose equal resource values without
     * colliding because the Control identity remains part of the relationship.
     *
     * This law does not vet VexExecution custody, InstanceId/Handle/Status, or any downstream
     * execution representation.
     */
    @VettedWithHuman("VexFamily.Control", "2026-10-29", "Victor Approved")
    interface Control<
        Family,
    > where
        Family : VexFamily<
            Family,
            *,
            *
        > {
        /**
         * Optional stable semantic resource identity for this Control's maintained relationship.
         *
         * Null means the exact Control identity is sufficient. A non-null value narrows only this
         * Control's relationship; it is never an ID by itself.
         */
        @VettedWithHuman("VexFamily.Control.resourceId", "2026-10-29", "Victor Approved")
        val resourceId:
            ResourceId<Family>?
            get() =
                null
    }

    /**
     * Exact Family-maintained execution identity.
     *
     * The live Family instance scopes identity. Control kind is always present. A typed ResourceId
     * optionally narrows repeated instances of that same Control. The actual ResourceId object is
     * retained so its concrete type/equality cannot collapse into a shared String convention.
     */
    internal class RelationshipId<
        Family,
    > internal constructor(
        private val family:
            VexFamily<
                Family,
                *,
                *
            >,
        internal val controlType:
            KClass<*>,
        internal val controlName:
            String,
        internal val resourceId:
            ResourceId<*>?,
    ) :
        VexExecution.RelationshipId where
        Family : VexFamily<
            Family,
            *,
            *
        > {
        override fun equals(
            other:
                Any?,
        ):
            Boolean =
            other is
                RelationshipId<*> &&
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

    /**
     * Family-owned display classification.
     *
     * DisplayState is not another semantic truth owner. It is the bounded outward interpretation of
     * [currentState] when a consumer has earned a repeated display distinction.
     *
     * [StateOnly] is the complete default contract: current semantic State already carries all
     * display meaning this Family has earned. It is not a placeholder and it is not an instruction
     * to invent another ViewState / UiState / PresentationState owner.
     */
    @VettedWithHuman("VexFamily.DisplayState", "2026-10-29T22:46:00-07:00", "Victor Approved")
    interface DisplayState<
        out State : VexFamily.State<*>,
    > {
        @VettedWithHuman("VexFamily.DisplayState.StateOnly", "2026-10-29T22:46:00-07:00", "Victor Approved")
        data class StateOnly<
            State : VexFamily.State<*>,
        >(
            val state: State,
        ) :
            DisplayState<
                State
            >
    }

    /**
     * Exact Store capability the current Family runtime consumes.
     *
     * Store is a Family child, not an independent Vex root. A concrete Family may extend this
     * locally when it truly needs more storage behavior, but the shared Family contract requires
     * only the operation Family itself currently executes: save(State).
     *
     * Durable Store truth is not another live Family State holder.
     */
    interface Store<
        State : VexFamily.State<*>,
    > {
        suspend fun save(
            state: State,
        ):
            WriteResult

        sealed interface WriteResult {
            data object Success :
                WriteResult

            data class Failure(
                val cause:
                    Throwable? =
                    null,
            ) :
                WriteResult
        }
    }

    /**
     * Earned specialization when durable Store truth is exactly Family State.
     *
     * This restores the ThemeMode pressure already proved by the Origin Walk without claiming every
     * Family Store is State-shaped. Observable durable State is a StateRelay dependency; it is not
     * another live semantic-State holder.
     */
    interface StateStore<
        State : VexFamily.State<*>,
    > :
        Store<State> {
        val observableState:
            Flow<State>

        suspend fun get():
            State

        suspend fun forget():
            Store.WriteResult
    }

    /**
     * One lawful outcome authored by [act] for an exact current State + incoming Control pair.
     *
     * Act belongs to Family. It is intentionally not another Vex root.
     */
    class Act<
        State : VexFamily.State<*>,
    > internal constructor(
        internal val admittedState:
            State?,
        internal val begin:
            Begin<State>?,
    ) {
        /**
         * Optional execution consequence opened by an Act.
         *
         * Begin is not a standalone package capability. Its constructors are closed; concrete
         * Families receive the protected VexFamily.begin(...) opening only because they are
         * authoring an Act.
         */
        class Begin<
            State : VexFamily.State<*>,
        > internal constructor(
            internal val approach:
                VexExecution.Approach,
            internal val save:
                Save<State>,
        ) {

            internal class Result<
                State,
            > internal constructor(
                internal val admission:
                    (
                        (
                            State,
                        ) -> State
                    )?,
            )

            internal data class Results<
                State,
            >(
                val success:
                    Result<State>,
                val failure:
                    Result<State>,
            )

            internal data class Save<
                State : VexFamily.State<*>,
            >(
                val state:
                    State,
                val results:
                    Results<State>,
            )

            class Scope<
                State : VexFamily.State<*>,
            > internal constructor(
                private val approach:
                    VexExecution.Approach,
            ) {
                private var save:
                    Save<State>? =
                    null

                fun save(
                    state: State,
                ):
                    SaveDestination<State> {
                    require(
                        save ==
                            null,
                    ) {
                        "begin currently admits one Save"
                    }

                    return SaveDestination(
                        state =
                            state,
                        complete = { completed ->
                            require(
                                save ==
                                    null,
                            ) {
                                "begin Save was already completed"
                            }

                            save =
                                completed
                        },
                    )
                }

                internal fun build():
                    Begin<State> =
                    Begin(
                        approach =
                            approach,
                        save =
                            requireNotNull(
                                save,
                            ) {
                                "begin requires one Save"
                            },
                    )

                class SaveDestination<
                    State : VexFamily.State<*>,
                > internal constructor(
                    private val state:
                        State,
                    private val complete:
                        (
                            Save<State>,
                        ) -> Unit,
                ) {
                    fun results(
                        block:
                            ResultsScope<State>.() -> Unit,
                    ) {
                        complete(
                            Save(
                                state =
                                    state,
                                results =
                                    ResultsScope<State>()
                                        .apply(
                                            block,
                                        )
                                        .build(),
                            ),
                        )
                    }
                }

                class ResultsScope<
                    State : VexFamily.State<*>,
                > internal constructor() {
                    private var success:
                        Result<State>? =
                        null

                    private var failure:
                        Result<State>? =
                        null

                    fun success(
                        block:
                            ResultScope<State>.() -> Unit = {},
                    ) {
                        require(
                            success ==
                                null,
                        ) {
                            "Success result was declared more than once"
                        }

                        success =
                            ResultScope<State>()
                                .apply(
                                    block,
                                )
                                .build()
                    }

                    fun failure(
                        block:
                            ResultScope<State>.() -> Unit = {},
                    ) {
                        require(
                            failure ==
                                null,
                        ) {
                            "Failure result was declared more than once"
                        }

                        failure =
                            ResultScope<State>()
                                .apply(
                                    block,
                                )
                                .build()
                    }

                    internal fun build():
                        Results<State> =
                        Results(
                            success =
                                requireNotNull(
                                    success,
                                ) {
                                    "results requires explicit Success"
                                },
                            failure =
                                requireNotNull(
                                    failure,
                                ) {
                                    "results requires explicit Failure"
                                },
                        )
                }

                class ResultScope<
                    State : VexFamily.State<*>,
                > internal constructor() {
                    private var admission:
                        (
                            (
                                State,
                            ) -> State
                        )? =
                        null

                    /**
                     * Authored unconditional later State after this execution result.
                     *
                     * The State is still admitted through the Family StateRelay boundary when the
                     * result actually arrives.
                     */
                    fun admit(
                        state: State,
                    ) {
                        admit {
                            state
                        }
                    }

                    /**
                     * Authored later-State decision against exact CURRENT Family State.
                     *
                     * Use this when a delayed result is only lawful while some feature-owned
                     * currentness condition still holds. Family supplies CURRENT State and
                     * serialization; the feature supplies the meaning of "still current".
                     */
                    fun admit(
                        decide:
                            (
                                currentState: State,
                            ) -> State,
                    ) {
                        require(
                            admission ==
                                null,
                        ) {
                            "one result may admit at most one later State"
                        }

                        admission =
                            decide
                    }

                    internal fun build():
                        Result<State> =
                        Result(
                            admission =
                                admission,
                        )
                }
            }
        }
    }

    /*
     * One live State holder.
     *
     * Family.currentState and Family.states expose this same StateRelay truth. Family does not retain
     * a second semantic-State copy.
     */
    private val stateLock =
        Any()

    @VettedWithHuman("VexFamily.stateRelay", "2026-10-29T22:46:00-07:00", "Victor Approved")
    private val stateRelay =
        MutableStateFlow(
            initialState,
        )

    @VettedWithHuman("VexFamily.states", "2026-10-29T22:46:00-07:00", "Victor Approved")
    val states:
        StateFlow<FamilyState> =
        stateRelay.asStateFlow()

    @VettedWithHuman("VexFamily.currentState", "2026-10-29T22:46:00-07:00", "Victor Approved")
    val currentState:
        FamilyState
        get() =
            states.value

    /**
     * Feature-owned State + Control meaning.
     *
     * A concrete Family implements this function directly. State is traversed first because current
     * State is the truth against which the incoming Control receives meaning.
     *
     * Both State and Control traversal stay exhaustive at this semantic boundary. Do not use an
     * else branch to absorb future vocabulary: a new State or Control should create compiler-visible
     * relationship work. Controls with the exact same reviewed outcome may share one branch.
     *
     * Use [noAct] for a reviewed State + Control pair that has no lawful Act.
     *
     * This is relationship resolution, not execution. The same function is used by isAvailable(...)
     * and activate(...), so implementations must not mutate Family State, invoke providers/Store,
     * launch work, log accepted facts, or otherwise cause an external effect while resolving.
     * begin(...) authors a consequence description only; it does not start that consequence here.
     */
    protected abstract fun act(
        currentState: FamilyState,
        control: FamilyControl,
    ):
        Act<FamilyState>?

    /**
     * Authored Act outcome: keep current semantic State.
     */
    protected fun stay(
        begin:
            Act.Begin<FamilyState>? =
            null,
    ):
        Act<FamilyState> =
        Act(
            admittedState =
                null,
            begin =
                begin,
        )

    /**
     * Reviewed absence of a lawful Act for this exact current State + incoming Control pair.
     *
     * This intentionally returns the nullable representation used by act(...), but concrete Family
     * source does not communicate semantic absence with a naked null.
     */
    protected fun noAct():
        Act<FamilyState>? =
        null

    /**
     * Authored Act outcome: admit this semantic State when activate(...) applies the Act.
     */
    protected fun admit(
        state: FamilyState,
        begin:
            Act.Begin<FamilyState>? =
            null,
    ):
        Act<FamilyState> =
        Act(
            admittedState =
                state,
            begin =
                begin,
        )

    /**
     * Optional independent execution consequence carried by an Act.
     */
    protected fun begin(
        block:
            Act.Begin.Scope<FamilyState>.() -> Unit,
    ):
        Act.Begin<FamilyState> =
        buildBegin(
            approach =
                VexExecution.Approach.Independent,
            block =
                block,
        )

    /**
     * Earliest maintained relationship for the triggering Control.
     *
     * Relationship identity comes from the live Family + Control name + optional typed ResourceId.
     */
    protected fun beginEarliest(
        block:
            Act.Begin.Scope<FamilyState>.() -> Unit,
    ):
        Act.Begin<FamilyState> =
        buildBegin(
            approach =
                VexExecution.Approach.Earliest,
            block =
                block,
        )

    /**
     * Latest maintained relationship for the triggering Control.
     */
    protected fun beginLatest(
        block:
            Act.Begin.Scope<FamilyState>.() -> Unit,
    ):
        Act.Begin<FamilyState> =
        buildBegin(
            approach =
                VexExecution.Approach.Latest,
            block =
                block,
        )

    /**
     * Queued maintained relationship for the triggering Control.
     */
    protected fun beginQueued(
        block:
            Act.Begin.Scope<FamilyState>.() -> Unit,
    ):
        Act.Begin<FamilyState> =
        buildBegin(
            approach =
                VexExecution.Approach.Queued,
            block =
                block,
        )

    private fun buildBegin(
        approach:
            VexExecution.Approach,
        block:
            Act.Begin.Scope<FamilyState>.() -> Unit,
    ):
        Act.Begin<FamilyState> =
        Act.Begin.Scope<FamilyState>(
            approach =
                approach,
        )
            .apply(
                block,
            )
            .build()

    open val store:
        Store<FamilyState>? =
        null

    /**
     * Observe one durable Family-State Store through the same StateRelay currentness boundary.
     *
     * Execution owns only maintained Flow custody. Store owns durable State. The concrete Family
     * owns how stored State combines with exact CURRENT State. The decision re-enters through
     * [admitCurrent], so no session/presentation observer or second current-State holder is created.
     */
    protected fun observeStore(
        stateStore:
            StateStore<FamilyState>,
        decide:
            (
                currentState: FamilyState,
                storedState: FamilyState,
            ) -> FamilyState,
    ):
        VexExecution.Handle =
        VexExecution.observe(
            executionRoot =
                executionRoot,
            owner =
                this::class,
            source =
                stateStore::observableState,
        ) { storedState ->
            admitCurrent { currentState ->
                decide(
                    currentState,
                    storedState,
                )
            }
        }

    /**
     * Current display classification for this Family.
     *
     * The default is complete and intentionally boring: no richer display classification has been
     * earned beyond current semantic State. Override only when real consumers would otherwise repeat
     * the same derived interpretation of State.
     */
    @VettedWithHuman("VexFamily.displayState", "2026-10-29T22:46:00-07:00", "Victor Approved")
    open val displayState:
        DisplayState<FamilyState>
        get() =
            DisplayState.StateOnly(
                currentState,
            )

    /**
     * Informative availability from the same direct act(currentState, control) relationship used by
     * activate(...). A Control is available exactly when current State + that Control produces a
     * lawful Act; noAct() means unavailable.
     *
     * Availability resolves and inspects the Act only. It never admits State and never starts an
     * authored Begin. This is why concrete act(...) must remain relationship-pure.
     */
    fun isAvailable(
        control: FamilyControl,
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
     * Runtime entrance.
     *
     * This is the explicit callsite another reader can follow:
     *
     *     activate(control)
     *     -> act(currentState, control)
     */
    fun activate(
        control: FamilyControl,
    ):
        Boolean =
        synchronized(
            stateLock,
        ) {
            val before =
                stateRelay.value

            val act =
                act(
                    currentState =
                        before,
                    control =
                        control,
                )
                    ?: return false

            recordAcceptedFact(
                VexLogger.Event.Family.ControlAccepted(
                    family =
                        this::class,
                    control =
                        control::class,
                    currentState =
                        before::class,
                ),
            )

            act.admittedState
                ?.let(
                    ::setCurrentStateLocked,
                )

            act.begin
                ?.let { begin ->
                    execute(
                        begin =
                            begin,
                        control =
                            control,
                    )
                }

            true
        }

    /**
     * Concrete-Family semantic admission for already-reconciled outside truth.
     *
     * Keep the public/source-specific name on the concrete Family (for example providerReconciled).
     * This protected seam only supplies exact CURRENT State + serialization; it does not decide what
     * an observation means and it must not be used from act(...), which remains relationship-pure.
     */
    protected fun admitCurrent(
        decide:
            (
                currentState: FamilyState,
            ) -> FamilyState,
    ):
        FamilyState =
        synchronized(
            stateLock,
        ) {
            setCurrentStateLocked(
                decide(
                    stateRelay.value,
                ),
            )
            stateRelay.value
        }

    /**
     * One serialized Family StateRelay decision.
     *
     * The caller supplies meaning; this boundary supplies exact CURRENT State and one-at-a-time
     * admission. StateRelay remains the single live holder.
     */
    private fun updateCurrentState(
        decide:
            (
                currentState: FamilyState,
            ) -> FamilyState,
    ) {
        synchronized(
            stateLock,
        ) {
            setCurrentStateLocked(
                decide(
                    stateRelay.value,
                ),
            )
        }
    }

    /**
     * Commit one already-resolved next State while [stateLock] is held.
     */
    @VettedWithHuman("VexFamily.setCurrentStateLocked", "2026-10-29T22:46:00-07:00", "Victor Approved")
    private fun setCurrentStateLocked(
        state: FamilyState,
    ) {
        val before =
            stateRelay.value

        stateRelay.value =
            state

        if (
            before !=
            stateRelay.value
        ) {
            recordAcceptedFact(
                VexLogger.Event.Family.StateAdmitted(
                    family =
                        this::class,
                    fromState =
                        before::class,
                    toState =
                        stateRelay.value::class,
                ),
            )
        }
    }

    private fun execute(
        begin:
            Act.Begin<FamilyState>,
        control:
            FamilyControl,
    ) {
        executeSave(
            approach =
                begin.approach,
            control =
                control,
            save =
                begin.save,
        )
    }

    private fun executeSave(
        approach:
            VexExecution.Approach,
        control:
            FamilyControl,
        save:
            Act.Begin.Save<FamilyState>,
    ) {
        val stateStore =
            requireNotNull(
                store,
            ) {
                "Save was authored for " +
                    this::class.qualifiedName +
                    " but this live Family has no Store"
            }

        val operation =
            stateStore::save

        val beforeOperation = {
            recordAcceptedFact(
                VexLogger.Event.Family.OperationBegan(
                    family =
                        this::class,
                    operation =
                        operation,
                    state =
                        save.state::class,
                ),
            )
        }

        val onResult:
            (
                Store.WriteResult,
            ) -> Unit = { result ->
                when (
                    result
                ) {
                    Store.WriteResult.Success -> {
                        recordAcceptedFact(
                            VexLogger.Event.Family.OperationSuccess(
                                family =
                                    this::class,
                                operation =
                                    operation,
                                state =
                                    save.state::class,
                            ),
                        )

                        applyLaterResult(
                            save.results.success,
                        )
                    }

                    is Store.WriteResult.Failure -> {
                        recordAcceptedFact(
                            VexLogger.Event.Family.OperationFailure(
                                family =
                                    this::class,
                                operation =
                                    operation,
                                state =
                                    save.state::class,
                                failure =
                                    result.cause
                                        ?.let { cause ->
                                            cause::class
                                        },
                            ),
                        )

                        applyLaterResult(
                            save.results.failure,
                        )
                    }
                }
            }

        val onFailure:
            (
                Throwable,
            ) -> Unit = { failure ->
                recordAcceptedFact(
                    VexLogger.Event.Family.OperationFailure(
                        family =
                            this::class,
                        operation =
                            operation,
                        state =
                            save.state::class,
                        failure =
                            failure::class,
                    ),
                )

                applyLaterResult(
                    save.results.failure,
                )
            }

        when (
            approach
        ) {
            VexExecution.Approach.Independent ->
                VexExecution.begin(
                    executionRoot =
                        executionRoot,
                    owner =
                        this::class,
                    operation =
                        operation,
                    argument =
                        save.state,
                    beforeOperation =
                        beforeOperation,
                    onResult =
                        onResult,
                    onFailure =
                        onFailure,
                )

            VexExecution.Approach.Earliest ->
                VexExecution.beginEarliest(
                    executionRoot =
                        executionRoot,
                    owner =
                        this::class,
                    relationshipId =
                        relationshipId(
                            control,
                        ),
                    operation =
                        operation,
                    argument =
                        save.state,
                    beforeOperation =
                        beforeOperation,
                    onResult =
                        onResult,
                    onFailure =
                        onFailure,
                )

            VexExecution.Approach.Latest ->
                VexExecution.beginLatest(
                    executionRoot =
                        executionRoot,
                    owner =
                        this::class,
                    relationshipId =
                        relationshipId(
                            control,
                        ),
                    operation =
                        operation,
                    argument =
                        save.state,
                    beforeOperation =
                        beforeOperation,
                    onResult =
                        onResult,
                    onFailure =
                        onFailure,
                )

            VexExecution.Approach.Queued ->
                VexExecution.beginQueued(
                    executionRoot =
                        executionRoot,
                    owner =
                        this::class,
                    relationshipId =
                        relationshipId(
                            control,
                        ),
                    operation =
                        operation,
                    argument =
                        save.state,
                    beforeOperation =
                        beforeOperation,
                    onResult =
                        onResult,
                    onFailure =
                        onFailure,
                )
        }
    }

    /**
     * Forms the Family relationship identity from the human-vetted equation:
     *
     *     this live Family + exact Control identity + optional Control.resourceId
     *
     * The optional resource never replaces Control identity. Control payload that is not deliberately
     * exposed as [Control.resourceId] does not silently participate in relationship equality.
     *
     * This marker vets only Family's semantic identity formation. The returned type's downstream
     * VexExecution custody/coordination role remains outside this approval.
     */
    @VettedWithHuman("VexFamily.relationshipId", "2026-10-29", "Victor Approved")
    internal fun relationshipId(
        control:
            FamilyControl,
    ):
        RelationshipId<Family> {
        val controlType =
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

        return RelationshipId(
            family =
                this,
            controlType =
                controlType,
            controlName =
                controlName,
            resourceId =
                control.resourceId,
        )
    }

    /**
     * Best-effort observation of an already-authored/accepted Family fact.
     *
     * Logger projection is deliberately non-authoritative. A Sink failure must not reject a lawful
     * Control, suppress State admission or Store execution, or convert a successful operation into
     * the Family-authored failure result.
     */
    private fun recordAcceptedFact(
        event:
            VexLogger.Event,
    ) {
        try {
            logger.record(
                event,
            )
        } catch (_: Throwable) {
            // Observation failure is not Family semantic failure.
        }
    }

    private fun applyLaterResult(
        result:
            Act.Begin.Result<FamilyState>,
    ) {
        result.admission
            ?.let(
                ::updateCurrentState,
            )
    }
}

// [VXG RealForever]
