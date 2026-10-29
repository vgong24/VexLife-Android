package com.vextreme.vexlife.android.behavior

import com.vextreme.vexlife.android.execution.VexExecution
import com.vextreme.vexlife.android.execution.VexExecutionRoot
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
    protected val logger:
        VexFamilyLogger,
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
     * A concrete Family implements this function directly. Returning null means no Act is authored
     * for the exact CURRENT State + incoming Control pair.
     */
    protected abstract fun act(
        currentState: State,
        control: Control,
    ):
        VexAct<State>?

    /**
     * Authored Act outcome: keep current semantic State.
     */
    protected fun stay(
        begin:
            VexBegin<State>? =
            null,
    ):
        VexAct<State> =
        VexAct(
            admittedState =
                null,
            begin =
                begin,
        )

    /**
     * Authored Act outcome: admit this semantic State when activate(...) applies the Act.
     */
    protected fun admit(
        state: State,
        begin:
            VexBegin<State>? =
            null,
    ):
        VexAct<State> =
        VexAct(
            admittedState =
                state,
            begin =
                begin,
        )

    /**
     * Optional authored execution consequence carried by an Act.
     */
    protected fun begin(
        mode: VexBeginMode,
        block:
            VexBegin.Scope<State>.() -> Unit,
    ):
        VexBegin<State> =
        VexBegin.Scope<State>(
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
     * Informative availability from the same direct act(currentState, control) function used by
     * activate(...). The returned Act is inspected but not applied.
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
            VexFamilyLogEvent.ControlAccepted(
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
                VexFamilyLogEvent.StateAdmitted(
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
            VexBegin<State>,
    ) {
        executeSave(
            mode =
                begin.mode,
            save =
                begin.save,
        )
    }

    private fun executeSave(
        mode: VexBeginMode,
        save:
            VexBegin.Save<State>,
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
                VexFamilyLogEvent.OperationBegan(
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
                            VexFamilyLogEvent.OperationSuccess(
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
                            VexFamilyLogEvent.OperationFailure(
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
                    VexFamilyLogEvent.OperationFailure(
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
            VexBeginMode.Replaceable ->
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

            VexBeginMode.Sequenced ->
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
            VexBegin.Result<State>,
    ) {
        result.admittedState
            ?.let(
                ::setCurrentState,
            )
    }
}

// [VXG RealForever]
