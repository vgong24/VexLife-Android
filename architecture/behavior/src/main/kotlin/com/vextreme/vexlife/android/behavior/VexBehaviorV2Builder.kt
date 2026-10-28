package com.vextreme.vexlife.android.behavior

import kotlin.reflect.KClass

/**
 * V2 authoring scope produced by the ThemeMode Origin Walk.
 *
 * The important outcome of the POC is not another builder class. It is the source sentence the
 * builder protects:
 *
 *     state<Current> {
 *         on(Control)
 *             .stay() / .admit(nextState)
 *
 *         on(Control, Effect)
 *             .stay() / .admit(nextState)
 *     }
 *
 * Effect is earned per Act. A purely semantic State admission must not invent a no-op Effect merely
 * to satisfy builder symmetry.
 *
 * That complete relationship is the Act. There is intentionally no separate Family.Act value to
 * maintain or route through.
 */
class VexBehaviorV2Builder<
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
    Control : VexBehaviorFamily.Control<Family>,
> internal constructor(
    private val behaviorRef: String,
) {
    @PublishedApi
    internal val stateTypes =
        linkedSetOf<
            KClass<out State>
        >()

    @PublishedApi
    internal val authoredActs =
        mutableListOf<
            VexBehaviorV2Act<
                Family,
                State,
                Control
            >
        >()

    private val pendingActs =
        linkedMapOf<
            Int,
            String
        >()

    private var nextActId =
        0

    inline fun <
        reified Current : State,
    > state(
        block:
            VexBehaviorV2StateScope<
                Family,
                State,
                Control,
                Current
            >.() -> Unit = {},
    ) {
        stateTypes +=
            Current::class

        VexBehaviorV2StateScope<
            Family,
            State,
            Control,
            Current
        >(
            from =
                Current::class,
            beginAct =
                ::beginAct,
            completeAct =
                ::completeAct,
        ).apply(
            block,
        )
    }

    @PublishedApi
    internal fun beginAct(
        from: KClass<out State>,
        control: Control,
        effect:
            VexBehaviorV2Begin<
                Family,
                State
            >?,
        conditionName: String?,
    ): Int {
        val id =
            nextActId++

        pendingActs[id] =
            buildString {
                append(
                    from.simpleName,
                )
                append(
                    " + ",
                )
                append(
                    control,
                )

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
                        "] Save(",
                    )
                    append(
                        effect
                            .save
                            .state,
                    )
                    append(
                        ")",
                    )
                }

                conditionName
                    ?.let { name ->
                        append(
                            " when ",
                        )
                        append(
                            name,
                        )
                    }
            }

        return id
    }

    @PublishedApi
    internal fun completeAct(
        id: Int,
        act:
            VexBehaviorV2Act<
                Family,
                State,
                Control
            >,
    ) {
        require(
            pendingActs.remove(
                id,
            ) !=
                null,
        ) {
            "behavior V2 Act was already completed"
        }

        authoredActs +=
            act
    }

    internal fun build(
        family: Family,
    ):
        VexBehaviorV2<
            Family,
            State,
            Control
        > {
        require(
            pendingActs.isEmpty(),
        ) {
            "behavior " +
                behaviorRef +
                " has Act(s) without a terminal .stay() or .admit(state): " +
                pendingActs.values
                    .joinToString()
        }

        return VexBehaviorV2(
            family =
                family,
            behaviorRef =
                behaviorRef,
            states =
                stateTypes.toSet(),
            acts =
                authoredActs.toList(),
        )
    }
}

class VexBehaviorV2StateScope<
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
    Control : VexBehaviorFamily.Control<Family>,
    Current : State,
> @PublishedApi internal constructor(
    @PublishedApi
    internal val from:
        KClass<Current>,
    @PublishedApi
    internal val beginAct:
        (
            KClass<out State>,
            Control,
            VexBehaviorV2Begin<
                Family,
                State
            >?,
            String?,
        ) -> Int,
    @PublishedApi
    internal val completeAct:
        (
            Int,
            VexBehaviorV2Act<
                Family,
                State,
                Control
            >,
        ) -> Unit,
) {
    /**
     * Begins an Act whose only consequence is semantic State admission/stay.
     *
     * Why this API takes Control directly:
     * the POC first introduced a separate Control->Act mapper, then authored the same State again in
     * the Act->Effect graph. ThemeMode proved that translation layer redundant. The on(...) scenario
     * itself is now the Act.
     *
     * No Effect is manufactured here. Use the Effect-bearing overload only when the Act actually
     * asks a runtime/provider/store to do something.
     */
    fun on(
        control: Control,
        conditionName: String? = null,
        whenCurrent: (Current) -> Boolean = {
            true
        },
    ): VexBehaviorV2Destination<
        Family,
        State,
        Control,
        Current
    > {
        val actId =
            beginAct(
                from,
                control,
                null,
                conditionName,
            )

        return VexBehaviorV2Destination(
            actId =
                actId,
            from =
                from,
            control =
                control,
            effect =
                null,
            conditionName =
                conditionName,
            whenCurrent =
                whenCurrent,
            completeAct =
                completeAct,
        )
    }

    fun on(
        control: Control,
        effect:
            VexBehaviorV2Begin<
                Family,
                State
            >,
        conditionName: String? = null,
        whenCurrent: (Current) -> Boolean = {
            true
        },
    ): VexBehaviorV2Destination<
        Family,
        State,
        Control,
        Current
    > {
        val actId =
            beginAct(
                from,
                control,
                effect,
                conditionName,
            )

        return VexBehaviorV2Destination(
            actId =
                actId,
            from =
                from,
            control =
                control,
            effect =
                effect,
            conditionName =
                conditionName,
            whenCurrent =
                whenCurrent,
            completeAct =
                completeAct,
        )
    }

    fun begin(
        mode: VexBehaviorV2BeginMode,
        block:
            VexBehaviorV2BeginScope<
                Family,
                State
            >.() -> Unit,
    ): VexBehaviorV2Begin<
        Family,
        State
    > =
        VexBehaviorV2BeginScope<
            Family,
            State
        >(
            mode =
                mode,
        )
            .apply(
                block,
            )
            .build()
}

class VexBehaviorV2BeginScope<
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
> internal constructor(
    private val mode: VexBehaviorV2BeginMode,
) {
    private var save:
        VexBehaviorV2Save<
            Family,
            State
        >? =
        null

    /**
     * Kotlin target form of Origin Save(Family, State).
     *
     * Family is already fixed by this scope, so callers provide only the State value.
     */
    fun save(
        state: State,
    ): VexBehaviorV2SaveDestination<
        Family,
        State
    > {
        require(
            save ==
                null,
        ) {
            "begin V2 currently admits one Save operation"
        }

        return VexBehaviorV2SaveDestination(
            state =
                state,
            complete = { completed ->
                require(
                    save ==
                        null,
                ) {
                    "Save was already completed"
                }

                save =
                    completed
            },
        )
    }

    internal fun build():
        VexBehaviorV2Begin<
            Family,
            State
        > =
        VexBehaviorV2Begin(
            mode =
                mode,
            save =
                requireNotNull(
                    save,
                ) {
                    "begin V2 requires Save(...).results { ... }"
                },
        )
}

class VexBehaviorV2SaveDestination<
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
> internal constructor(
    private val state: State,
    private val complete:
        (
            VexBehaviorV2Save<
                Family,
                State
            >,
        ) -> Unit,
) {
    fun results(
        block:
            VexBehaviorV2ResultsScope<
                Family,
                State
            >.() -> Unit,
    ) {
        complete(
            VexBehaviorV2Save(
                state =
                    state,
                results =
                    VexBehaviorV2ResultsScope<
                        Family,
                        State
                    >()
                        .apply(
                            block,
                        )
                        .build(),
            ),
        )
    }
}

class VexBehaviorV2ResultsScope<
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
> internal constructor() {
    private var success:
        VexBehaviorV2Result<
            Family,
            State
        >? =
        null

    private var failure:
        VexBehaviorV2Result<
            Family,
            State
        >? =
        null

    fun success(
        block:
            VexBehaviorV2ResultScope<
                Family,
                State
            >.() -> Unit = {},
    ) {
        require(
            success ==
                null,
        ) {
            "Success result was declared more than once"
        }

        success =
            VexBehaviorV2ResultScope<
                Family,
                State
            >()
                .apply(
                    block,
                )
                .build()
    }

    fun failure(
        block:
            VexBehaviorV2ResultScope<
                Family,
                State
            >.() -> Unit = {},
    ) {
        require(
            failure ==
                null,
        ) {
            "Failure result was declared more than once"
        }

        failure =
            VexBehaviorV2ResultScope<
                Family,
                State
            >()
                .apply(
                    block,
                )
                .build()
    }

    internal fun build():
        VexBehaviorV2Results<
            Family,
            State
        > =
        VexBehaviorV2Results(
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

class VexBehaviorV2ResultScope<
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
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
        VexBehaviorV2Result<
            Family,
            State
        > =
        VexBehaviorV2Result(
            admittedState =
                admittedState,
        )
}

class VexBehaviorV2Destination<
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
    Control : VexBehaviorFamily.Control<Family>,
    Current : State,
> @PublishedApi internal constructor(
    @PublishedApi
    internal val actId:
        Int,
    @PublishedApi
    internal val from:
        KClass<Current>,
    @PublishedApi
    internal val control:
        Control,
    @PublishedApi
    internal val effect:
        VexBehaviorV2Begin<
            Family,
            State
        >?,
    @PublishedApi
    internal val conditionName:
        String?,
    @PublishedApi
    internal val whenCurrent:
        (Current) -> Boolean,
    @PublishedApi
    internal val completeAct:
        (
            Int,
            VexBehaviorV2Act<
                Family,
                State,
                Control
            >,
        ) -> Unit,
) {
    /**
     * Immediately admit one concrete semantic State value.
     *
     * The Act already knows this semantic consequence, so carrying only a State class would force a
     * later runtime owner to reconstruct information Behavior should have preserved.
     */
    fun admit(
        state: State,
    ) {
        complete(
            VexBehaviorV2StateDisposition(
                kind =
                    VexBehaviorV2StateDispositionKind.Admit,
                admittedState =
                    state,
            ),
        )
    }

    fun stay() {
        complete(
            VexBehaviorV2StateDisposition(
                kind =
                    VexBehaviorV2StateDispositionKind.Stay,
                admittedState =
                    null,
            ),
        )
    }

    @PublishedApi
    internal fun complete(
        disposition:
            VexBehaviorV2StateDisposition<
                Family,
                State
            >,
    ) {
        completeAct(
            actId,
            VexBehaviorV2Act(
                from =
                    from,
                control =
                    control,
                effect =
                    effect,
                immediateDisposition =
                    disposition,
                conditionName =
                    conditionName,
                whenCurrent = { current ->
                    @Suppress(
                        "UNCHECKED_CAST",
                    )
                    whenCurrent(
                        current as Current,
                    )
                },
            ),
        )
    }
}

// [VXG RealForever]
