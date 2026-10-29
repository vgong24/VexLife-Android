package com.vextreme.vexlife.android.behavior

import com.vextreme.vexlife.android.execution.VexExecution
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Family-owned projectable output.
 *
 * DisplayState is not another semantic truth owner. It is the bounded outward meaning of
 * [VexFamily.currentState].
 */
interface VexDisplayState<
    out State,
> {
    data class Empty<
        State,
    >(
        val state: State,
    ) :
        VexDisplayState<
            State
        >
}

/**
 * Live Family runtime for one semantic State and Control vocabulary.
 *
 * Human-visible runtime call:
 *
 *     activate(control)
 *     -> act(currentState, control)
 *     -> state<State> / on<Control>
 *     -> stay() or admit(State)
 *     -> optional begin(...)
 *     -> StateRelay / execution
 *
 * The concrete Family owns [act]. There is no separately constructed Acts table or hidden invoke
 * operator between activate(...) and the feature's State + Control logic.
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
     * State-shape helper for the direct [act] function.
     *
     * This is a runtime type branch, not construction-time rule registration.
     */
    protected inline fun <
        reified Current : State,
    > state(
        currentState: State,
        control: Control,
        block:
            (
                currentState: Current,
                control: Control,
            ) ->
                VexAct<State>?,
    ):
        VexAct<State>? =
        if (
            currentState is
                Current
        ) {
            block(
                currentState,
                control,
            )
        } else {
            null
        }

    /**
     * Payload-Control helper for the direct [act] function.
     */
    protected inline fun <
        reified RequestedControl : Control,
    > on(
        control: Control,
        block:
            (
                control: RequestedControl,
            ) ->
                VexAct<State>,
    ):
        VexAct<State>? =
        if (
            control is
                RequestedControl
        ) {
            block(
                control,
            )
        } else {
            null
        }

    /**
     * Exact singleton/value Control helper for the direct [act] function.
     */
    protected fun on(
        expectedControl: Control,
        control: Control,
        block:
            () ->
                VexAct<State>,
    ):
        VexAct<State>? =
        if (
            control ==
                expectedControl
        ) {
            block()
        } else {
            null
        }

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
            VexBeginScope<State>.() -> Unit,
    ):
        VexBegin<State> =
        VexBeginScope<State>(
            mode =
                mode,
        )
            .apply(
                block,
            )
            .build()

    open val store:
        VexFamilyStore? =
        null

    open val displayState:
        VexDisplayState<State>
        get() =
            VexDisplayState.Empty(
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
        when (
            val operation =
                begin.operation
        ) {
            is VexSave<*> -> {
                @Suppress(
                    "UNCHECKED_CAST",
                )
                val save =
                    operation as
                        VexSave<State>

                executeSave(
                    mode =
                        begin.mode,
                    save =
                        save,
                )
            }

            else ->
                error(
                    "begin has no runtime realization for Operation " +
                        (
                            operation::class.qualifiedName
                                ?: operation::class.simpleName
                                ?: "Unknown"
                        ),
                )
        }
    }

    private fun executeSave(
        mode: VexBeginMode,
        save:
            VexSave<State>,
    ) {
        val familyStore =
            requireNotNull(
                store,
            ) {
                "Save was authored for " +
                    this::class.qualifiedName +
                    " but this live Family has no Store"
            }

        val stateStore =
            familyStore as?
                VexFamilyStateStore<*>
                ?: error(
                    "Save requires a Family State Store; " +
                        this::class.qualifiedName +
                        ".Store does not expose VexFamilyStateStore",
                )

        @Suppress(
            "UNCHECKED_CAST",
        )
        val typedStateStore =
            stateStore as
                VexFamilyStateStore<State>

        val operation =
            typedStateStore::save

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
                VexStoreWriteResult,
            ) -> Unit = { result ->
                when (
                    result
                ) {
                    VexStoreWriteResult.Success -> {
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

                    is VexStoreWriteResult.Failure -> {
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
            VexResult<State>,
    ) {
        result.admittedState
            ?.let(
                ::setCurrentState,
            )
    }
}

// [VXG RealForever]
