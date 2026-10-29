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
abstract class VexFamily<
    State : Any,
    Control : Any,
>(
    initialState: State,
    private val executionRoot:
        VexExecutionRoot,
    private val logger:
        VexLogger.Sink,
) {
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
        out State,
    > {
        data class StateOnly<
            State,
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
        State : Any,
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
     * One lawful outcome authored by [act] for an exact current State + incoming Control pair.
     *
     * Act belongs to Family. It is intentionally not another Vex root.
     */
    class Act<
        State : Any,
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
            State : Any,
        > internal constructor(
            internal val mode:
                Mode,
            internal val save:
                Save<State>,
        ) {
            /**
             * Provisional adapter to the current VexExecution implementation.
             *
             * These names are not the final execution contract. VexExecution owns the already
             * accepted launch / Earliest / Latest / Queued relationship and this adapter disappears
             * when that source conformance lands.
             */
            enum class Mode {
                Replaceable,
                Sequenced,
            }

            internal data class Result<
                State,
            >(
                val admittedState:
                    State?,
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
                State : Any,
            >(
                val state:
                    State,
                val results:
                    Results<State>,
            )

            class Scope<
                State : Any,
            > internal constructor(
                private val mode:
                    Mode,
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
                        mode =
                            mode,
                        save =
                            requireNotNull(
                                save,
                            ) {
                                "begin requires one Save"
                            },
                    )

                class SaveDestination<
                    State : Any,
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
                    State : Any,
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
                    State : Any,
                > internal constructor() {
                    private var admittedState:
                        State? =
                        null

                    fun admit(
                        state: State,
                    ) {
                        require(
                            admittedState ==
                                null,
                        ) {
                            "one result may admit at most one later State"
                        }

                        admittedState =
                            state
                    }

                    internal fun build():
                        Result<State> =
                        Result(
                            admittedState =
                                admittedState,
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
    private val stateRelay =
        MutableStateFlow(
            initialState,
        )

    val states:
        StateFlow<State> =
        stateRelay.asStateFlow()

    val currentState:
        State
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
     */
    protected abstract fun act(
        currentState: State,
        control: Control,
    ):
        Act<State>?

    /**
     * Authored Act outcome: keep current semantic State.
     */
    protected fun stay(
        begin:
            Act.Begin<State>? =
            null,
    ):
        Act<State> =
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
        Act<State>? =
        null

    /**
     * Authored Act outcome: admit this semantic State when activate(...) applies the Act.
     */
    protected fun admit(
        state: State,
        begin:
            Act.Begin<State>? =
            null,
    ):
        Act<State> =
        Act(
            admittedState =
                state,
            begin =
                begin,
        )

    /**
     * Optional authored execution consequence carried by an Act.
     */
    protected fun begin(
        mode: Act.Begin.Mode,
        block:
            Act.Begin.Scope<State>.() -> Unit,
    ):
        Act.Begin<State> =
        Act.Begin.Scope<State>(
            mode =
                mode,
        )
            .apply(
                block,
            )
            .build()

    open val store:
        Store<State>? =
        null

    /**
     * Current display classification for this Family.
     *
     * The default is complete and intentionally boring: no richer display classification has been
     * earned beyond current semantic State. Override only when real consumers would otherwise repeat
     * the same derived interpretation of State.
     */
    open val displayState:
        DisplayState<State>
        get() =
            DisplayState.StateOnly(
                currentState,
            )

    /**
     * Informative availability from the same direct act(currentState, control) relationship used by
     * activate(...). A Control is available exactly when current State + that Control produces a
     * lawful Act; noAct() means unavailable. The returned Act is inspected but not applied.
     */
    fun isAvailable(
        control: Control,
    ):
        Boolean =
        act(
            currentState =
                currentState,
            control =
                control,
        ) !=
            null

    /**
     * Runtime entrance.
     *
     * This is the explicit callsite another reader can follow:
     *
     *     activate(control)
     *     -> act(currentState, control)
     */
    fun activate(
        control: Control,
    ):
        Boolean {
        val before =
            currentState

        val act =
            act(
                currentState =
                    before,
                control =
                    control,
            )
                ?: return false

        logger.record(
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
                ::setCurrentState,
            )

        act.begin
            ?.let(
                ::execute,
            )

        return true
    }

    /**
     * The write side of Family current State.
     *
     * StateRelay remains the single live holder.
     */
    private fun setCurrentState(
        state: State,
    ) {
        val before =
            currentState

        stateRelay.value =
            state

        if (
            before !=
            currentState
        ) {
            logger.record(
                VexLogger.Event.Family.StateAdmitted(
                    family =
                        this::class,
                    fromState =
                        before::class,
                    toState =
                        currentState::class,
                ),
            )
        }
    }

    private fun execute(
        begin:
            Act.Begin<State>,
    ) {
        executeSave(
            mode =
                begin.mode,
            save =
                begin.save,
        )
    }

    private fun executeSave(
        mode: Act.Begin.Mode,
        save:
            Act.Begin.Save<State>,
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
            logger.record(
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
                        logger.record(
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
                        logger.record(
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
                logger.record(
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
            mode
        ) {
            Act.Begin.Mode.Replaceable ->
                VexExecution.beginReplaceable(
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

            Act.Begin.Mode.Sequenced ->
                VexExecution.beginSequenced(
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
        }
    }

    private fun applyLaterResult(
        result:
            Act.Begin.Result<State>,
    ) {
        result.admittedState
            ?.let(
                ::setCurrentState,
            )
    }
}

// [VXG RealForever]
