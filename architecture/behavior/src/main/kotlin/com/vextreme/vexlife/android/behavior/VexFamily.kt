@file:OptIn(com.vextreme.vexlife.android.execution.VexExecution.RelationshipFoundationApi::class)

package com.vextreme.vexlife.android.behavior

import com.vextreme.vexlife.android.execution.VexExecution
import com.vextreme.vexlife.android.execution.VexExecutionRoot
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
 *     -> optional saveLatest(State)
 *     -> StateRelay / execution
 *
 * The concrete Family owns [act]. State and Control traversal use native Kotlin sealed-type when
 * expressions; there is no custom state/on branching language between activate(...) and feature
 * meaning.
 */
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
     * Exact Control identity is always part of the Family-owned relationship identity. A Control
     * that needs narrower repeated-resource identity exposes [resourceId] with its exact ResourceId
     * type.
     */
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
         * Control identity is always present. A non-null resourceId narrows repeated resources for
         * this exact Control kind; ordinary Control payload does not silently become identity.
         */
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
    interface DisplayState<
        out State : VexFamily.State<*>,
    > {
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
        /**
         * Current durable State followed by subsequent durable changes.
         *
         * Store observation is input to Family meaning; it is never another live Family State owner.
         */
        val observableState:
            Flow<State>
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
        internal val save:
            Save<State>?,
    ) {
        /**
         * The one execution consequence Direct Family has actually earned in production today.
         *
         * Save does not author later Family State. Save success/failure are execution/storage facts.
         * If a future result changes Family meaning, that result must return as a typed Control and
         * re-enter act(CURRENT State, Control).
         */
        class Save<
            State : VexFamily.State<*>,
        > internal constructor(
            internal val state:
                State,
        )
    }

    /*
     * One live State holder.
     *
     * Family.currentState and Family.states expose this same StateRelay truth. Family does not retain
     * a second semantic-State copy.
     */
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
     * Feature-owned State + Control meaning.
     *
     * A concrete Family implements this function directly. State is traversed first because current
     * State is the truth against which the incoming Control receives meaning.
     *
     * null means this exact current State + Control has no lawful Act. stay() means the Control is
     * lawful while immediate semantic State remains CURRENT. admit(State) means the returned Act
     * carries that immediate semantic State admission.
     *
     * This is relationship resolution, not execution. The same function is used by isAvailable(...)
     * and activate(...), so implementations must not mutate Family State, invoke providers/Store,
     * launch work, log accepted facts, or otherwise cause an external effect while resolving.
     * saveLatest(...) authors a Save consequence only; it does not start that consequence here.
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
        save:
            Act.Save<FamilyState>? =
            null,
    ):
        Act<FamilyState> =
        Act(
            admittedState =
                null,
            save =
                save,
        )

    /**
     * Authored Act outcome: admit this semantic State when activate(...) applies the Act.
     */
    protected fun admit(
        state: FamilyState,
        save:
            Act.Save<FamilyState>? =
            null,
    ):
        Act<FamilyState> =
        Act(
            admittedState =
                state,
            save =
                save,
        )

    /**
     * Author the currently earned durable consequence: persist this Family State using Latest
     * relationship semantics for the triggering Control.
     *
     * Save completion cannot author Family State from this path. Any future semantically meaningful
     * completion must return as a typed Control and re-enter act(CURRENT State, Control).
     */
    protected fun saveLatest(
        state: FamilyState,
    ):
        Act.Save<FamilyState> =
        Act.Save(
            state =
                state,
        )

    open val store:
        Store<FamilyState>? =
        null

    /**
     * Observe durable Family-State truth as typed Family input.
     *
     * Store owns durable State. Execution owns only observation lifetime. The concrete Family turns
     * each observed State into a typed Control, and activate(...) re-resolves that Control against
     * exact CURRENT semantic State. Store observation therefore gets no second State-writing door.
     */
    protected fun observeStore(
        stateStore:
            StateStore<FamilyState>,
        toControl:
            (
                storedState: FamilyState,
            ) -> FamilyControl,
    ) {
        VexExecution.observe(
            executionRoot =
                executionRoot,
            owner =
                this::class,
            source =
                stateStore::observableState,
        ) { storedState ->
            activate(
                toControl(
                    storedState,
                ),
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
    open val displayState:
        DisplayState<FamilyState>
        get() =
            DisplayState.StateOnly(
                currentState,
            )

    /**
     * Informative availability from the same direct act(currentState, control) relationship used by
     * activate(...). A Control is available exactly when current State + that Control produces a
     * lawful Act; null means unavailable.
     *
     * Availability resolves and inspects the Act only. It never admits State and never starts an
     * authored Save. This is why concrete act(...) must remain relationship-pure.
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

            act.save
                ?.let { save ->
                    executeSave(
                        save =
                            save,
                        control =
                            control,
                    )
                }

            true
        }

    /**
     * Commit one already-resolved next State while [stateLock] is held.
     */
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

    private fun executeSave(
        save:
            Act.Save<FamilyState>,
        control:
            FamilyControl,
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
            beforeOperation = {
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
            },
            onResult = { result ->
                when (result) {
                    Store.WriteResult.Success ->
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

                    is Store.WriteResult.Failure ->
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
                }
            },
            onFailure = { failure ->
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
            },
        )
    }

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
     * Control, suppress State admission, or suppress Store execution.
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


}

// [VXG RealForever]
