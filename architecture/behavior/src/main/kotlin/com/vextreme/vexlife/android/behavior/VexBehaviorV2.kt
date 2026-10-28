package com.vextreme.vexlife.android.behavior

import com.vextreme.vexlife.android.execution.VexExecution
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import kotlin.reflect.KClass

interface VexBehaviorV2DisplayState<
    out State,
> {
    val state: State

    data class Empty<
        State,
    >(
        override val state: State,
    ) :
        VexBehaviorV2DisplayState<
            State
        >
}

class VexBehaviorV2<
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
    Control : VexBehaviorFamily.Control<Family>,
> internal constructor(
    val family: Family,
    val behaviorRef: String,
    val states: Set<KClass<out State>>,
    val acts:
        List<
            VexBehaviorV2Act<
                Family,
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
            "behavior V2 requires at least one State"
        }

        acts.forEach { act ->
            require(
                act.from in
                    states,
            ) {
                "Act source is not a registered State: " +
                    act.from
            }

            act.immediateDisposition
                .admittedState
                ?.let { admitted ->
                    require(
                        admitted::class in
                            states,
                    ) {
                        "Act immediate State is not registered: " +
                            admitted::class
                    }
                }

            act.effect
                ?.operation
                ?.let { operation ->
                    when (
                        operation
                    ) {
                        is VexBehaviorV2Save ->
                            require(
                                operation
                                    .state
                                    .let {
                                        it::class in states
                                    },
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
        }
    }

    internal fun resolve(
        current: State,
        control: Control,
    ):
        VexBehaviorV2Act<
            Family,
            State,
            Control
        >? {
        val available =
            acts.filter { act ->
                act.matches(
                    current =
                        current,
                    requestedControl =
                        control,
                )
            }

        require(
            available.size <=
                1,
        ) {
            "behavior " +
                behaviorRef +
                " has ambiguous Acts for current " +
                current::class.simpleName +
                " + Control " +
                control
        }

        return available.singleOrNull()
    }

    fun describe():
        String =
        buildString {
            appendLine(
                behaviorRef +
                    " [V2]",
            )

            acts.forEach { act ->
                append(
                    act.from.simpleName,
                )
                append(
                    " --on(",
                )
                append(
                    act.control,
                )
                append(
                    ")",
                )

                val effect =
                    act.effect

                if (
                    effect ==
                    null
                ) {
                    append(
                        " / NO_EFFECT",
                    )
                } else {
                    append(
                        " / begin[",
                    )
                    append(
                        effect.mode,
                    )
                    append(
                        "] ",
                    )

                    when (
                        val operation =
                            effect.operation
                    ) {
                        is VexBehaviorV2Save -> {
                            append(
                                "Save(",
                            )
                            append(
                                operation.state,
                            )
                            append(
                                ")",
                            )
                        }

                        else ->
                            append(
                                operation::class.simpleName
                                    ?: "Operation",
                            )
                    }
                }

                append(
                    "--> ",
                )
                append(
                    act.immediateDisposition.kind,
                )
                act.immediateDisposition
                    .admittedState
                    ?.let { admitted ->
                        append(
                            " ",
                        )
                        append(
                            admitted,
                        )
                    }

                effect
                    ?.operation
                    ?.results
                    ?.let { results ->
                        append(
                            " ; Success=",
                        )
                        append(
                            results.success
                                .admittedState
                                ?.toString()
                                ?: "NO_STATE_CHANGE",
                        )
                        append(
                            " ; Failure=",
                        )
                        append(
                            results.failure
                                .admittedState
                                ?.toString()
                                ?: "NO_STATE_CHANGE",
                        )
                    }

                act.conditionName
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
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
    Control : VexBehaviorFamily.Control<Family>,
> vexActs(
    family: Family,
    behaviorRef: String,
    block:
        VexBehaviorV2Builder<
            Family,
            State,
            Control
        >.() -> Unit,
):
    VexBehaviorV2<
        Family,
        State,
        Control
    > =
    VexBehaviorV2Builder<
        Family,
        State,
        Control
    >(
        behaviorRef =
            behaviorRef,
    )
        .apply(
            block,
        )
        .build(
            family,
        )

@Deprecated(
    message =
        "V2 Family authors should expose required acts with vexActs(...).",
)
fun <
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
    Control : VexBehaviorFamily.Control<Family>,
> vexBehaviorV2(
    family: Family,
    behaviorRef: String,
    block:
        VexBehaviorV2Builder<
            Family,
            State,
            Control
        >.() -> Unit,
):
    VexBehaviorV2<
        Family,
        State,
        Control
    > =
    vexActs(
        family =
            family,
        behaviorRef =
            behaviorRef,
        block =
            block,
    )

/**
 * Live V2 Family.
 *
 * begin(mode) is the execution phase; no ThemeModeExecution/FamilyExecution sibling is introduced.
 * Store remains visible on the Family. The existing VexExecution foundation is only the coroutine
 * machinery underneath begin.
 */
abstract class VexBehaviorV2Family<
    Family : VexBehaviorV2Family<
        Family,
        State,
        Control
    >,
    State : VexBehaviorFamily.State<Family>,
    Control : VexBehaviorFamily.Control<Family>,
>(
    initialState: State,
    private val executionRoot:
        VexExecutionRoot,
    protected val logger:
        VexFamilyLogger =
        VexFamilyLogger.None,
) :
    VexBehaviorFamily<
        Family
    > {
    @Volatile
    private var currentState:
        State =
        initialState

    val current:
        State
        get() =
            currentState

    abstract val acts:
        VexBehaviorV2<
            Family,
            State,
            Control
        >

    open val store:
        VexFamilyStore<
            Family,
            State
        >? =
        null

    open val displayState:
        VexBehaviorV2DisplayState<
            State
        >
        get() =
            VexBehaviorV2DisplayState.Empty(
                current,
            )

    fun activate(
        control: Control,
    ):
        VexBehaviorV2Act<
            Family,
            State,
            Control
        >? {
        check(
            acts.family ===
                this,
        ) {
            "acts must belong to this live Family instance"
        }

        val before =
            current

        val act =
            acts.resolve(
                current =
                    before,
                control =
                    control,
            )
                ?: return null

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

        act.effect
            ?.let(
                ::begin,
            )

        return act
    }

    private fun applyDisposition(
        disposition:
            VexBehaviorV2StateDisposition<
                Family,
                State
            >,
    ) {
        when (
            disposition.kind
        ) {
            VexBehaviorV2StateDispositionKind.Stay ->
                Unit

            VexBehaviorV2StateDispositionKind.Admit ->
                admit(
                    requireNotNull(
                        disposition.admittedState,
                    ),
                )
        }
    }

    private fun admit(
        state: State,
    ) {
        val before =
            currentState

        currentState =
            state

        if (
            before !=
            state
        ) {
            logger.record(
                VexFamilyLogEvent.StateAdmitted(
                    family =
                        this::class,
                    fromState =
                        before::class,
                    toState =
                        state::class,
                ),
            )
        }
    }

    private fun begin(
        effect:
            VexBehaviorV2Begin<
                Family,
                State
            >,
    ) {
        when (
            val operation =
                effect.operation
        ) {
            is VexBehaviorV2Save ->
                beginSave(
                    mode =
                        effect.mode,
                    save =
                        operation,
                )

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
        mode: VexBehaviorV2BeginMode,
        save:
            VexBehaviorV2Save<
                Family,
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

        val operation =
            familyStore::save

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
                VexOperationResult,
            ) -> Unit = { result ->
                when (
                    result
                ) {
                    VexOperationResult.Success -> {
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

                    is VexOperationResult.Failure -> {
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
            VexBehaviorV2BeginMode.Replaceable ->
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

            VexBehaviorV2BeginMode.Sequenced ->
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
            VexBehaviorV2Result<
                Family,
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
