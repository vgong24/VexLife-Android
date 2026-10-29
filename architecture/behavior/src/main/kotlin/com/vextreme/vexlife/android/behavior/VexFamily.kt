package com.vextreme.vexlife.android.behavior

import com.vextreme.vexlife.android.execution.VexExecution
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import kotlin.reflect.KClass
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
 * Authored State + Control relationships for one Family.
 *
 * Feature code creates this with [vexActs] and reads the grammar as:
 *
 *     state<State.X> {
 *         on<Control.Y> { currentState, control ->
 *             ...
 *         }
 *     }
 *
 * Matching machinery is intentionally internal. Family code should not call a resolver API.
 */
class VexActs<
    State : Any,
    Control : Any,
> internal constructor(
    val behaviorRef: String,
    val states:
        Set<
            KClass<out State>
        >,
    private val rules:
        List<
            VexActRule<
                State,
                Control
            >
        >,
) {
    init {
        require(
            behaviorRef.isNotBlank(),
        ) {
            "behaviorRef must not be blank"
        }
        require(
            states.isNotEmpty(),
        ) {
            "acts require at least one State"
        }

        rules.forEach { rule ->
            require(
                rule.from in
                    states,
            ) {
                "Act source is not a registered State: " +
                    rule.from
            }
        }
    }

    internal operator fun invoke(
        currentState: State,
        control: Control,
    ):
        VexAct<
            State,
            Control
        >? {
        val available =
            rules.mapNotNull { rule ->
                rule.match(
                    currentState =
                        currentState,
                    control =
                        control,
                )
                    ?.let(
                        ::validateAct,
                    )
            }

        require(
            available.size <=
                1,
        ) {
            "acts " +
                behaviorRef +
                " has ambiguous State + Control meaning for " +
                currentState::class.simpleName +
                " + " +
                control
        }

        return available.singleOrNull()
    }

    private fun validateAct(
        act:
            VexAct<
                State,
                Control
            >,
    ):
        VexAct<
            State,
            Control
        > {
        require(
            act.from in
                states,
        ) {
            "Act source is not a registered State: " +
                act.from
        }

        act.immediateDisposition
            .admittedState
            ?.let { admittedState ->
                require(
                    admittedState::class in
                        states,
                ) {
                    "Act immediate State is not registered: " +
                        admittedState::class
                }
            }

        act.begin
            ?.operation
            ?.let { operation ->
                if (
                    operation is
                        VexSave<
                            *
                        >
                ) {
                    @Suppress(
                        "UNCHECKED_CAST",
                    )
                    val save =
                        operation as
                            VexSave<
                                State
                            >

                    require(
                        save.state::class in
                            states,
                    ) {
                        "Save State is not registered in " +
                            behaviorRef
                    }
                }

                listOf(
                    operation.results.success,
                    operation.results.failure,
                ).forEach { result ->
                    result.admittedState
                        ?.let { laterState ->
                            require(
                                laterState::class in
                                    states,
                            ) {
                                "later admitted State is not registered: " +
                                    laterState::class
                            }
                        }
                }
            }

        return act
    }

    fun describe():
        String =
        buildString {
            appendLine(
                behaviorRef,
            )

            rules.forEach { rule ->
                append(
                    rule.from.simpleName,
                )
                append(
                    " --on<",
                )
                append(
                    rule.controlDescription,
                )
                append(
                    ">()",
                )

                rule.conditionName
                    ?.let { name ->
                        append(
                            " when ",
                        )
                        append(
                            name,
                        )
                    }

                appendLine()
            }
        }.trimEnd()
}

fun <
    State : Any,
    Control : Any,
> vexActs(
    behaviorRef: String,
    block:
        VexActsBuilder<
            State,
            Control
        >.() -> Unit,
):
    VexActs<
        State,
        Control
    > =
    VexActsBuilder<
        State,
        Control
    >(
        behaviorRef =
            behaviorRef,
    )
        .apply(
            block,
        )
        .build()

/**
 * Live Family runtime for one semantic State and Control vocabulary.
 *
 * The generics bind the concrete State and Control contracts. They do not create another Family
 * type hierarchy.
 *
 * Human-facing circuit:
 *
 *     currentState + Control
 *     -> state<State> { on<Control> { currentState, control -> ... } }
 *     -> stay() or admit(State)
 *     -> optional begin(...)
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
     * Family exposes the same StateRelay truth as currentState and states; it does not retain a
     * second copy of semantic State.
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

    protected abstract val acts:
        VexActs<
            State,
            Control
        >

    open val store:
        VexFamilyStore? =
        null

    open val displayState:
        VexDisplayState<
            State
        >
        get() =
            VexDisplayState.Empty(
                currentState,
            )

    /**
     * Informative availability from the same CURRENT State + Control grammar used by [activate].
     *
     * An earlier availability read never becomes authority for a later activation.
     */
    fun isAvailable(
        control: Control,
    ):
        Boolean =
        acts(
            currentState =
                currentState,
            control =
                control,
        ) !=
            null

    /**
     * Apply the Act authored for this exact CURRENT State + Control pair.
     *
     * Returns false when no Act is authored for the pair. The internal Act carrier is never exposed
     * as Family API.
     */
    fun activate(
        control: Control,
    ):
        Boolean {
        val before =
            currentState

        val act =
            acts(
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

        applyDisposition(
            act.immediateDisposition,
        )

        act.begin
            ?.let(
                ::begin,
            )

        return true
    }

    private fun applyDisposition(
        disposition:
            VexStateDisposition<
                State
            >,
    ) {
        disposition
            .admittedState
            ?.let(
                ::admit,
            )
    }

    /**
     * Family-owned semantic admission seam.
     *
     * External observations must first pass through their rightful reconciliation/currentness
     * boundary. Callers do not receive a generic setCurrent(...).
     */
    protected fun admit(
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

    private fun begin(
        begin:
            VexBegin<
                State
            >,
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
                        VexSave<
                            State
                        >

                beginSave(
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

    private fun beginSave(
        mode:
            VexBeginMode,
        save:
            VexSave<
                State
            >,
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
                VexFamilyStateStore<
                    *
                >
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
                VexFamilyStateStore<
                    State
                >

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

    /**
     * Current patient-zero later consequence.
     *
     * This deliberately does not claim a reusable stale-result guard. A richer Family may receive
     * newer Control/provider truth while an earlier Operation is running; that currentness law
     * remains open pressure.
     */
    private fun applyLaterResult(
        result:
            VexResult<
                State
            >,
    ) {
        result.admittedState
            ?.let(
                ::admit,
            )
    }
}

// [VXG RealForever]
