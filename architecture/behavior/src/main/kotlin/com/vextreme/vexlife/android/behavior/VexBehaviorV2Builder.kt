package com.vextreme.vexlife.android.behavior

import kotlin.reflect.KClass

/**
 * V2 Act-graph authoring scope produced by the ThemeMode Origin Walk.
 *
 * The complete Act is now one contiguous on(Control) { ... } block:
 *
 *     state<Current> {
 *         on(Control) {
 *             stay() / admit(nextState)
 *
 *             optional:
 *             begin(mode = Replaceable | Sequenced) {
 *                 save(...)
 *                     .results {
 *                         success { optional later admit(...) }
 *                         failure { optional later admit(...) }
 *                     }
 *             }
 *         }
 *     }
 *
 * Direct stay/admit inside on { ... } is the immediate semantic disposition. Nesting under
 * begin(...).results { ... } is the already-earned later result scope. No additional "immediate"
 * vocabulary is required.
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
            completeAct =
                ::completeAct,
        )
            .apply(
                block,
            )
    }

    @PublishedApi
    internal fun completeAct(
        act:
            VexBehaviorV2Act<
                Family,
                State,
                Control
            >,
    ) {
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
        > =
        VexBehaviorV2(
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
    internal val completeAct:
        (
            VexBehaviorV2Act<
                Family,
                State,
                Control
            >,
        ) -> Unit,
) {
    /**
     * Author one complete Act for this CURRENT State + Control relationship.
     *
     * The block must declare exactly one immediate stay()/admit(state). It may additionally declare
     * one begin(mode) Effect episode. Keeping both scopes inside this block preserves the causal read
     * without moving Effect meaning into another handler or function.
     */
    fun on(
        control: Control,
        conditionName: String? = null,
        whenCurrent: (Current) -> Boolean = {
            true
        },
        block:
            VexBehaviorV2ActScope<
                Family,
                State,
                Control,
                Current
            >.() -> Unit,
    ) {
        completeAct(
            VexBehaviorV2ActScope<
                Family,
                State,
                Control,
                Current
            >(
                from =
                    from,
                control =
                    control,
                conditionName =
                    conditionName,
                whenCurrent =
                    whenCurrent,
            )
                .apply(
                    block,
                )
                .build(),
        )
    }
}

class VexBehaviorV2ActScope<
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
    Control : VexBehaviorFamily.Control<Family>,
    Current : State,
> internal constructor(
    private val from:
        KClass<Current>,
    private val control:
        Control,
    private val conditionName:
        String?,
    private val whenCurrent:
        (Current) -> Boolean,
) {
    private var effect:
        VexBehaviorV2Begin<
            Family,
            State
        >? =
        null

    private var immediateDisposition:
        VexBehaviorV2StateDisposition<
            Family,
            State
        >? =
        null

    fun admit(
        state: State,
    ) {
        require(
            immediateDisposition ==
                null,
        ) {
            "on(Control) may declare only one immediate stay() or admit(state)"
        }

        immediateDisposition =
            VexBehaviorV2StateDisposition(
                kind =
                    VexBehaviorV2StateDispositionKind.Admit,
                admittedState =
                    state,
            )
    }

    fun stay() {
        require(
            immediateDisposition ==
                null,
        ) {
            "on(Control) may declare only one immediate stay() or admit(state)"
        }

        immediateDisposition =
            VexBehaviorV2StateDisposition(
                kind =
                    VexBehaviorV2StateDispositionKind.Stay,
                admittedState =
                    null,
            )
    }

    fun begin(
        mode: VexBehaviorV2BeginMode,
        block:
            VexBehaviorV2BeginScope<
                Family,
                State
            >.() -> Unit,
    ) {
        require(
            effect ==
                null,
        ) {
            "one V2 Act currently admits at most one begin(...) Effect"
        }

        effect =
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

    internal fun build():
        VexBehaviorV2Act<
            Family,
            State,
            Control
        > {
        val immediate =
            requireNotNull(
                immediateDisposition,
            ) {
                "on(Control) requires exactly one immediate stay() or admit(state)"
            }

        return VexBehaviorV2Act(
            from =
                from,
            control =
                control,
            effect =
                effect,
            immediateDisposition =
                immediate,
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
        )
    }
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

    fun save(
        state: State,
    ):
        VexBehaviorV2SaveDestination<
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
    private val state:
        State,
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

// [VXG RealForever]
