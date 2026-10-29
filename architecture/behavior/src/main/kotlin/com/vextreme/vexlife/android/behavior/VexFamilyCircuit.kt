package com.vextreme.vexlife.android.behavior

/**
 * Minimum projectable output role for every live Family.
 *
 * Empty(current) adds no second truth. It keeps one predictable Family-owned place where a UI,
 * VexBoard or another consumer can ask what the Family currently exposes.
 */
interface VexFamilyDisplayState<
    out State,
> {
    data class Empty<
        State,
    >(
        val current:
            State,
    ) :
        VexFamilyDisplayState<
            State
        >
}

/**
 * Smallest live Family root.
 *
 * A Family owns one CURRENT semantic State. More specific Family forms add only the responsibilities
 * they actually need.
 */
abstract class VexFamily<
    Family,
    State,
>(
    initialState:
        State,
) :
    VexBehaviorFamily<
        Family
    >
    where Family :
        VexFamily<
            Family,
            State
        >,
          State :
        VexBehaviorFamily.State<
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

    open val displayState:
        VexFamilyDisplayState<
            State
        >
        get() =
            VexFamilyDisplayState.Empty(
                current =
                    current,
            )

    /**
     * Family-owned State admission.
     *
     * This stays protected so external callers never receive a generic setCurrent(...). A concrete
     * Family form must first establish why the next State is lawful/current.
     */
    protected fun admit(
        state:
            State,
    ) {
        currentState =
            state
    }
}

/**
 * Immediate semantic result of one CURRENT State + Control relationship.
 *
 * Act is the resolved relationship meaning. It is not another Family vocabulary hierarchy.
 */
sealed interface VexFamilyAct<
    out State,
> {
    data object Stay :
        VexFamilyAct<
            Nothing
        >

    data class Admit<
        State,
    >(
        val state:
            State,
    ) :
        VexFamilyAct<
            State
        >
}

/**
 * Family form for a semantic request/Control boundary.
 *
 * The same [resolveAct] law powers both availability and activation:
 *
 *     CURRENT State + Control
 *     -> Act?
 *
 * Activation always re-reads CURRENT State. An earlier availability result never becomes authority.
 */
abstract class VexControlFamily<
    Family,
    State,
    Control,
>(
    initialState:
        State,
) :
    VexFamily<
        Family,
        State
    >(
        initialState =
            initialState,
    )
    where Family :
        VexControlFamily<
            Family,
            State,
            Control
        >,
          State :
        VexBehaviorFamily.State<
            Family
        >,
          Control :
        VexBehaviorFamily.Control<
            Family
        > {
    protected abstract fun resolveAct(
        current:
            State,
        control:
            Control,
    ):
        VexFamilyAct<
            State
        >?

    fun isAvailable(
        control:
            Control,
    ):
        Boolean =
        resolveAct(
            current =
                current,
            control =
                control,
        ) !=
            null

    fun activate(
        control:
            Control,
    ):
        VexFamilyAct<
            State
        >? {
        val act =
            resolveAct(
                current =
                    current,
                control =
                    control,
            )
                ?: return null

        when (
            act
        ) {
            VexFamilyAct.Stay ->
                Unit

            is VexFamilyAct.Admit ->
                admit(
                    act.state,
                )
        }

        return act
    }
}

/**
 * Input side of one Compound Family.
 *
 * Incoming observations may be partial. [latest] owns how a partial observation becomes one complete
 * CURRENT input when combined with retained input.
 *
 * If an Input contains mutable-backed data, override [deepCopy] and detach it here.
 */
interface VexCompoundInputState<
    Self : VexCompoundInputState<
        Self
    >,
> {
    fun latest(
        incoming:
            Self,
    ):
        Self

    @Suppress(
        "UNCHECKED_CAST",
    )
    fun deepCopy():
        Self =
        this as
            Self
}

/**
 * Current consequence of one complete Compound relationship.
 *
 * Output is not previous-process memory and is not another Family's State.
 */
interface VexCompoundOutputState<
    Self : VexCompoundOutputState<
        Self
    >,
> {
    @Suppress(
        "UNCHECKED_CAST",
    )
    fun deepCopy():
        Self =
        this as
            Self
}

/**
 * CURRENT semantic State contract for one Compound Family.
 *
 * Concrete Families give this State a readable name such as MultiCompoundState.
 */
interface VexCompoundFamilyState<
    Family,
    InputState,
    OutputState,
> :
    VexBehaviorFamily.State<
        Family
    >
    where Family :
        VexFamily<
            Family,
            *
        >,
          InputState :
        VexCompoundInputState<
            InputState
        >,
          OutputState :
        VexCompoundOutputState<
            OutputState
        > {
    val inputState:
        InputState

    val outputState:
        OutputState

    /**
     * CURRENT relationship lineage.
     *
     * This advances when retained Input or Output meaning changes. It is not an event ledger.
     */
    val revision:
        Long
}

/**
 * One already-committed Output consequence.
 *
 * A receipt exists only when Output meaning changed. Input-only currentness movement may advance
 * Family.current.revision without fabricating another Act.
 */
data class VexCommittedOutput<
    OutputState,
>(
    val compoundRevision:
        Long,
    val outputState:
        OutputState,
) where OutputState :
    VexCompoundOutputState<
        OutputState
    >

/**
 * Family form for a CURRENT relationship between several observed truths.
 *
 * Generic infrastructure owns:
 *
 *     partial observation
 *     -> Input.latest(...)
 *     -> process(...)
 *     -> concrete Compound State assembly
 *     -> COMMIT Family.current
 *     -> Act only when Output meaning changed
 *
 * The concrete Family owns:
 *
 *     Input.latest(...) current-fact formation
 *     process(...) relationship meaning
 *     domain-readable Input / Output / Compound State types
 *     observation entry points
 *     act(...) meaning after a committed Output consequence
 */
abstract class VexCompoundFamily<
    Family,
    State,
    InputState,
    OutputState,
>(
    initialInputState:
        InputState,
    initialOutputState:
        OutputState,
    private val stateFactory:
        (
            inputState:
                InputState,
            outputState:
                OutputState,
            revision:
                Long,
        ) ->
            State,
) :
    VexFamily<
        Family,
        State
    >(
        initialState =
            stateFactory(
                initialInputState.deepCopy(),
                initialOutputState.deepCopy(),
                0L,
            ),
    )
    where Family :
        VexCompoundFamily<
            Family,
            State,
            InputState,
            OutputState
        >,
          State :
        VexCompoundFamilyState<
            Family,
            InputState,
            OutputState
        >,
          InputState :
        VexCompoundInputState<
            InputState
        >,
          OutputState :
        VexCompoundOutputState<
            OutputState
        > {
    private val processLock =
        Any()

    init {
        require(
            current.inputState ==
                initialInputState
        ) {
            "Compound stateFactory must preserve initial Input meaning"
        }
        require(
            current.outputState ==
                initialOutputState
        ) {
            "Compound stateFactory must preserve initial Output meaning"
        }
        require(
            current.revision ==
                0L
        ) {
            "Compound stateFactory must initialize revision at 0"
        }
    }

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
     * One relationship calculation surface.
     *
     * Read it as:
     *
     *     here is everything CURRENT
     *     -> what does it mean?
     *     -> return Output
     */
    protected abstract fun process(
        latestStateInput:
            InputState,
    ):
        OutputState

    /**
     * React to an Output consequence only after that Output is already carried by Family.current.
     *
     * Pure projection/convergence Families may keep the default no-op.
     */
    protected open fun act(
        committed:
            VexCommittedOutput<
                OutputState
            >,
    ) {
    }

    /**
     * Admit one partial observation into the Family relationship.
     *
     * process + commit is serialized. act(...) runs after commit and outside the lock so an observed
     * target result may safely re-enter this Family as NEW input.
     */
    protected fun observe(
        inputState:
            InputState,
    ):
        VexCommittedOutput<
            OutputState
        >? {
        val committed =
            synchronized(
                processLock,
            ) {
                val retained =
                    current

                val latestStateInput =
                    retained
                        .inputState
                        .deepCopy()
                        .latest(
                            inputState.deepCopy(),
                        )
                        .deepCopy()

                val latestStateOutput =
                    process(
                        latestStateInput.deepCopy(),
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

                val committedInput =
                    if (
                        inputChanged
                    ) {
                        latestStateInput.deepCopy()
                    } else {
                        retained.inputState
                    }

                val committedOutput =
                    if (
                        outputChanged
                    ) {
                        latestStateOutput.deepCopy()
                    } else {
                        retained.outputState
                    }

                val nextRevision =
                    retained.revision +
                        1L

                val nextState =
                    stateFactory(
                        committedInput,
                        committedOutput,
                        nextRevision,
                    )

                require(
                    nextState.inputState ==
                        committedInput
                ) {
                    "Compound stateFactory must preserve committed Input meaning"
                }
                require(
                    nextState.outputState ==
                        committedOutput
                ) {
                    "Compound stateFactory must preserve committed Output meaning"
                }
                require(
                    nextState.revision ==
                        nextRevision
                ) {
                    "Compound stateFactory must preserve framework revision"
                }

                /*
                 * COMMIT before Act.
                 */
                admit(
                    nextState,
                )

                if (
                    outputChanged
                ) {
                    VexCommittedOutput(
                        compoundRevision =
                            nextRevision,
                        outputState =
                            committedOutput.deepCopy(),
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
