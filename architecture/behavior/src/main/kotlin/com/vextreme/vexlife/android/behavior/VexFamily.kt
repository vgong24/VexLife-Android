@file:OptIn(com.vextreme.vexlife.android.execution.VexExecution.RelationshipFoundationApi::class)

package com.vextreme.vexlife.android.behavior

import com.vextreme.vexlife.android.execution.VexExecution
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import com.vextreme.vexlife.android.logging.VexLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
 *     -> optional durable Save
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
        VexExecutionRoot? =
        null,
    private val logger:
        VexLogger.Sink =
        VexLogger.Sink.None,
    private val store:
        Store<FamilyState>? =
        null,
) where
    Family : VexFamily<
        Family,
        FamilyState,
        FamilyControl
    >,
    FamilyState : VexFamily.State<Family>,
    FamilyControl : VexFamily.Control<Family> {
    init {
        require(
            store ==
                null ||
                executionRoot !=
                null,
        ) {
            "A VexFamily with a Store requires an executionRoot"
        }
    }

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
     * Compile-time kinship marker for incoming Control belonging to one exact Family coordinate.
     *
     * Control remains the finite affordance/request vocabulary resolved by act(CURRENT State,
     * Control). Execution identity is not authored through Control in the current Direct Family
     * surface.
     */
    interface Control<
        Family,
    > where
        Family : VexFamily<
            Family,
            *,
            *
        >


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
        /**
         * Persist one Family State.
         *
         * Successful return means the Save completed. Failure is reported by throwing; Family does
         * not invent a second operation-result vocabulary when execution already owns failure.
         */
        suspend fun save(
            state: State,
        )
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
        internal val saveState:
            State?,
    )


    /*
     * One live State holder.
     *
     * Family.currentState and Family.states expose this same StateRelay truth. Family does not retain
     * a second semantic-State copy.
     */
    private val stateLock =
        Any()

    /*
     * One maintained Store.save queue per live Family.
     *
     * This is execution mechanics, not a feature-authored semantic identity. All accepted saves for
     * this Family share the same queue so a later accepted whole-State save cannot be overtaken by
     * an earlier save from a different Control.
     */
    private val saveRelationshipId:
        VexExecution.RelationshipId =
        object :
            VexExecution.RelationshipId {}

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
     * An optional saveState carried by stay()/admit(...) describes a durable consequence only; it
     * does not start that consequence here.
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
        saveState:
            FamilyState? =
            null,
    ):
        Act<FamilyState> =
        Act(
            admittedState =
                null,
            saveState =
                saveState,
        )

    /**
     * Authored Act outcome: admit this semantic State when activate(...) applies the Act.
     */
    protected fun admit(
        state: FamilyState,
        saveState:
            FamilyState? =
            null,
    ):
        Act<FamilyState> =
        Act(
            admittedState =
                state,
            saveState =
                saveState,
        )

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

            act.saveState
                ?.let { saveState ->
                    executeSave(
                        state =
                            saveState,
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
        state:
            FamilyState,
    ) {
        val stateStore =
            requireNotNull(
                store,
            ) {
                "Save was authored for " +
                    this::class.qualifiedName +
                    " but this live Family has no Store"
            }

        val saveExecutionRoot =
            requireNotNull(
                executionRoot,
            ) {
                "A VexFamily Save requires an executionRoot"
            }

        val operation =
            stateStore::save

        /*
         * Persist accepted Save consequences in acceptance order. This is execution mechanics, not
         * another Family semantic law: Family has already admitted immediate State before reaching
         * this point, and Save completion has no State-writing callback.
         */
        VexExecution.beginQueued(
            executionRoot =
                saveExecutionRoot,
            owner =
                this::class,
            relationshipId =
                saveRelationshipId,
            operation =
                operation,
            argument =
                state,
            beforeOperation = {
                recordAcceptedFact(
                    VexLogger.Event.Family.OperationBegan(
                        family =
                            this::class,
                        operation =
                            operation,
                        state =
                            state::class,
                    ),
                )
            },
            onResult = {
                recordAcceptedFact(
                    VexLogger.Event.Family.OperationSuccess(
                        family =
                            this::class,
                        operation =
                            operation,
                        state =
                            state::class,
                    ),
                )
            },
            onFailure = { failure ->
                recordAcceptedFact(
                    VexLogger.Event.Family.OperationFailure(
                        family =
                            this::class,
                        operation =
                            operation,
                        state =
                            state::class,
                        failure =
                            failure::class,
                    ),
                )
            },
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
