package com.vextreme.vexlife.android.behavior

import com.vextreme.vexlife.android.execution.VexExecution
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import kotlin.reflect.KClass

/**
 * Required minimum Family display/projectable-output role.
 *
 * A feature Family is not reviewed as an isolated class. It participates in a larger fractal
 * application ecosystem: UI, VexBoard, widgets, diagnostics and later consumers may all need the
 * Family's outward meaning without reinterpreting raw semantic State independently.
 *
 * Mandatory therefore does NOT mean every Family must invent a second presentation truth. The
 * requirement is that a fresh human/AI/consumer can always find one bounded, Family-owned output
 * coordinate.
 *
 * The removal test is ecosystem-aware:
 *
 *     remove DisplayState
 *     -> semantic State still exists locally
 *     -> but the designated State -> human/projectable-output relationship disappears
 *     -> downstream consumers must invent that ownership again
 *
 * Empty(current) is the structural zero for that relationship: it adds no new semantic fact, while
 * preserving where outward/projectable meaning belongs until richer presentation pressure is earned.
 *
 * The type parameter preserves Family/State association, but concrete display projections do not
 * need to expose the full semantic State.
 */
interface VexBehaviorV2DisplayState<
    out State,
> {
    data class Empty<
        State,
    >(
        val state: State,
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
    private val rules:
        List<
            VexBehaviorV2ActRule<
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

        rules.forEach { rule ->
            require(
                rule.from in
                    states,
            ) {
                "Act source is not a registered State: " +
                    rule.from
            }

            rule.staticAct
                ?.let(
                    ::validateAct,
                )
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
            rules.mapNotNull { rule ->
                rule.resolve(
                    current =
                        current,
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
            "behavior " +
                behaviorRef +
                " has ambiguous Acts for current " +
                current::class.simpleName +
                " + Control " +
                control
        }

        return available.singleOrNull()
    }

    private fun validateAct(
        act:
            VexBehaviorV2Act<
                Family,
                State,
                Control
            >,
    ):
        VexBehaviorV2Act<
            Family,
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
                    is VexBehaviorV2Save<*, *> ->
                        require(
                            operation
                                .state
                                .let {
                                    it::class in
                                        states
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

        return act
    }

    fun describe():
        String =
        buildString {
            appendLine(
                behaviorRef +
                    " [V2]",
            )

            rules.forEach { rule ->
                val act =
                    rule.staticAct

                if (
                    act ==
                    null
                ) {
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
                        ">() / RUNTIME_ACT",
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
                    return@forEach
                }

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
                        is VexBehaviorV2Save<*, *> -> {
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
 * Current live V2 Family patient-zero runtime.
 *
 * Stable authoring rules live above this runtime: Family-root vocabulary, structural Act,
 * concrete admission, optional Effect and begin(mode) execution.
 *
 * This implementation now proves locally-owned CURRENT State + Control activation, runtime payload
 * Act materialization, same-law Control availability, State-store Save, and a protected semantic
 * admission seam that a concrete Family may use only after its own rightful external reconciliation.
 *
 * It is still NOT a universal currentness runtime. Concurrent admission serialization, observable UI
 * State, provider Operation realization, async-result staleness and multi-instance execution identity
 * remain open pressure.
 *
 * begin(mode) remains the semantic execution phase; no ThemeModeExecution/FamilyExecution sibling is
 * introduced. VexExecution is coroutine machinery underneath that already-authored boundary.
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
        VexFamilyLogger,
) :
    VexBehaviorFamily<
        Family
    > {
    /**
     * Patient-zero locally-owned semantic State.
     *
     * @Volatile provides cross-thread visibility only. It is NOT the eventual serialization,
     * provider-reconciliation, transition-generation, or Compose-observation contract.
     */
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
            Family
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

    /**
     * Informative availability from the same CURRENT State + Control relationship used by activate.
     *
     * No second UI availability table is introduced. Activation always re-resolves CURRENT State, so
     * an availability read never becomes authority for a later activation.
     */
    fun isAvailable(
        control: Control,
    ): Boolean =
        acts.resolve(
            current =
                current,
            control =
                control,
        ) !=
            null

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

    /**
     * Family-owned semantic admission seam.
     *
     * activate(Control) uses this after an authored Act. A concrete Family may also use it after its
     * own rightful external-truth reconciliation (for example provider observation + pending intent).
     *
     * This is protected rather than public on purpose:
     * - UI/provider callers do not receive a generic setCurrent(...);
     * - external observations must first pass through the concrete Family's owned reconciliation;
     * - the same State-admission/accountant path is reused once semantic truth is earned.
     *
     * This does not yet solve concurrent admission serialization or stale async-result generations.
     */
    protected fun admit(
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
            is VexBehaviorV2Save<*, *> -> {
                @Suppress(
                    "UNCHECKED_CAST",
                )
                val familySave =
                    operation as
                        VexBehaviorV2Save<
                            Family,
                            State
                        >

                beginSave(
                    mode =
                        effect.mode,
                    save =
                        familySave,
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

        val stateStore =
            familyStore as?
                VexFamilyStateStore<
                    *,
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
                    Family,
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

    /**
     * Current ThemeMode patient-zero later consequence.
     *
     * This intentionally does not claim a reusable stale-result guard. A richer Family may receive a
     * newer Control or provider observation while an earlier Operation is running; Playback pressure
     * must earn the transition/result currentness relationship before this becomes universal.
     */
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


/**
 * Input side of one Compound Family circuit.
 *
 * Incoming observations may be partial. [latest] owns how one partial observation is currentized
 * against the already-retained complete input. [deepCopy] owns detachment only when this input
 * contains mutable-backed data; immutable inputs may keep the default identity snapshot.
 *
 * Relationship calculation does NOT belong here. Once [latest] returns, process(...) should be able
 * to read one complete current input without consulting previous Output or another hidden cache.
 */
interface VexBehaviorV2CompoundInputState<
    Self : VexBehaviorV2CompoundInputState<Self>,
> {
    fun latest(
        incoming: Self,
    ): Self

    @Suppress("UNCHECKED_CAST")
    fun deepCopy(): Self =
        this as Self
}

/**
 * Current consequence of one complete Compound Family relationship.
 *
 * Output is not previous-process memory and is not another owner's State. It answers only what the
 * complete latest input means now.
 */
interface VexBehaviorV2CompoundOutputState<
    Self : VexBehaviorV2CompoundOutputState<Self>,
> {
    @Suppress("UNCHECKED_CAST")
    fun deepCopy(): Self =
        this as Self
}

/**
 * CURRENT semantic State for a Compound Family.
 *
 * The Family owns this whole relationship State:
 *
 *     inputState
 *       complete current facts needed for calculation
 *
 *     outputState
 *       current consequence of those facts
 *
 *     revision
 *       current-state lineage; advances when Input or Output meaning changes
 *
 * Revision is not an event ledger.
 */
data class VexBehaviorV2CompoundState<
    Family,
    InputState,
    OutputState,
>(
    val inputState: InputState,
    val outputState: OutputState,
    val revision: Long = 0L,
) :
    VexBehaviorFamily.State<
        Family
    >
    where Family :
        VexBehaviorFamily<
            Family
        >,
          InputState :
        VexBehaviorV2CompoundInputState<
            InputState
        >,
          OutputState :
        VexBehaviorV2CompoundOutputState<
            OutputState
        > {
    init {
        require(
            revision >=
                0L,
        ) {
            "Compound Family revision must be non-negative"
        }
    }
}

/**
 * Exact committed Output consequence produced by one Compound Family State transition.
 *
 * A receipt exists only when Output meaning changed. Input-only currentness movement still advances
 * the Compound State revision, but it does not fabricate another Act consequence.
 */
data class VexBehaviorV2CommittedOutput<
    OutputState,
>(
    val compoundRevision: Long,
    val outputState: OutputState,
) where OutputState :
    VexBehaviorV2CompoundOutputState<
        OutputState
    >

/**
 * Family specialization for relationships formed from several CURRENT truths.
 *
 * This is still a V2 Family. The specialization adds exactly the reusable circuit discovered by the
 * Origin Walk:
 *
 *     incoming partial Input
 *     + retained CURRENT Input
 *     -> Input.latest(incoming)
 *     -> complete latestStateInput
 *     -> process(latestStateInput)
 *     -> Output
 *     -> COMMIT Family.current CompoundState
 *     -> Act only when Output meaning changed
 *
 * Concrete Families author relationship meaning in [process]. They do not re-author retained-current
 * plumbing, Compound State assembly, revision movement, or output-change detection.
 *
 * [act] runs only AFTER the next Compound State is already CURRENT and after the process/commit lock
 * is released. An Act may therefore request another rightful Family/owner to change and feed the
 * observed resulting truth back through [observe] without recursive half-committed processing.
 *
 * This specialization does not claim that every Family is Compound. A simple local Family continues
 * to use [VexBehaviorV2Family] directly.
 */
abstract class VexBehaviorV2CompoundFamily<
    Family,
    InputState,
    OutputState,
    Control,
>(
    initialInputState: InputState,
    initialOutputState: OutputState,
    executionRoot: VexExecutionRoot,
    logger: VexFamilyLogger,
) :
    VexBehaviorV2Family<
        Family,
        VexBehaviorV2CompoundState<
            Family,
            InputState,
            OutputState
        >,
        Control
    >(
        initialState =
            VexBehaviorV2CompoundState(
                inputState =
                    initialInputState.deepCopy(),
                outputState =
                    initialOutputState.deepCopy(),
            ),
        executionRoot =
            executionRoot,
        logger =
            logger,
    )
    where Family :
        VexBehaviorV2CompoundFamily<
            Family,
            InputState,
            OutputState,
            Control
        >,
          InputState :
        VexBehaviorV2CompoundInputState<
            InputState
        >,
          OutputState :
        VexBehaviorV2CompoundOutputState<
            OutputState
        >,
          Control :
        VexBehaviorFamily.Control<
            Family
        > {
    private val compoundProcessLock =
        Any()

    val currentInput:
        InputState
        get() =
            current
                .inputState
                .deepCopy()

    val currentOutput:
        OutputState
        get() =
            current
                .outputState
                .deepCopy()

    /**
     * The one concrete relationship-calculation override.
     *
     * Read this method as:
     *
     *     here is everything CURRENT
     *     -> what does it mean?
     *     -> return the Output consequence
     *
     * Do not mutate another owner, execute an Effect, consult previous Output as hidden input memory,
     * or re-currentize partial observations here.
     */
    protected abstract fun process(
        latestStateInput: InputState,
    ): OutputState

    /**
     * React to one already-CURRENT Output consequence.
     *
     * Pure projection/convergence Families may keep the default no-op. A Family that owns a lawful
     * consequence may override this and request change through the rightful target owner's semantic
     * boundary. Resulting target truth must return as NEW input through [observe].
     */
    protected open fun act(
        committed:
            VexBehaviorV2CommittedOutput<
                OutputState
            >,
    ) {
    }

    /**
     * Admit one partial observation into this Family's current relationship.
     *
     * Exactly one process/commit decision is serialized at a time. The Act runs after commit and
     * outside the lock so a resulting observation may safely re-enter this Family as new input.
     */
    protected fun observe(
        inputState: InputState,
    ):
        VexBehaviorV2CommittedOutput<
            OutputState
        >? {
        val committed =
            synchronized(
                compoundProcessLock,
            ) {
                val retained =
                    current

                val admittedPatch =
                    inputState.deepCopy()

                val processingCurrentInput =
                    retained
                        .inputState
                        .deepCopy()

                val latestStateInput =
                    processingCurrentInput
                        .latest(
                            admittedPatch,
                        )
                        .deepCopy()

                val latestStateOutput =
                    process(
                        latestStateInput
                            .deepCopy(),
                    )
                        .deepCopy()

                val inputChanged =
                    latestStateInput !=
                        retained.inputState
                val outputChanged =
                    latestStateOutput !=
                        retained.outputState

                if (
                    !inputChanged &&
                    !outputChanged
                ) {
                    return@synchronized null
                }

                val nextState =
                    VexBehaviorV2CompoundState<
                        Family,
                        InputState,
                        OutputState
                    >(
                        inputState =
                            if (
                                inputChanged
                            ) {
                                latestStateInput
                                    .deepCopy()
                            } else {
                                retained
                                    .inputState
                            },
                        outputState =
                            if (
                                outputChanged
                            ) {
                                latestStateOutput
                                    .deepCopy()
                            } else {
                                retained
                                    .outputState
                            },
                        revision =
                            retained.revision +
                                1L,
                    )

                /*
                 * COMMIT — Family.current moves before any Act can observe this consequence.
                 */
                admit(
                    nextState,
                )

                if (
                    outputChanged
                ) {
                    VexBehaviorV2CommittedOutput(
                        compoundRevision =
                            nextState.revision,
                        outputState =
                            nextState
                                .outputState
                                .deepCopy(),
                    )
                } else {
                    null
                }
            }

        committed
            ?.let(
                ::act,
            )

        return committed
    }
}

// [VXG RealForever]
