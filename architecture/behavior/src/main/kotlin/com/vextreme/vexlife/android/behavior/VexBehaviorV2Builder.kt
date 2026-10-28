package com.vextreme.vexlife.android.behavior

import kotlin.reflect.KClass

/**
 * V2 Act-graph authoring scope produced by the Origin Walk.
 *
 * Singleton/exact Controls keep the smallest form:
 *
 *     state<State.Light> {
 *         on(Control.Toggle) {
 *             admit(State.Dark)
 *         }
 *     }
 *
 * Runtime payload Controls materialize the Act from the actual CURRENT State + incoming Control:
 *
 *     state<State.Ready> {
 *         on<Control.Seek>(
 *             whenCurrent = { current, seek ->
 *                 current.mediaId == seek.mediaId
 *             },
 *         ) { current, seek ->
 *             admit(
 *                 current.copy(
 *                     requestedPositionMs = seek.positionMs,
 *                 ),
 *             )
 *         }
 *     }
 *
 * The dynamic block is declarative authoring. Resolving availability may materialize it without
 * executing begin(...); real Operation execution happens only after activate(Control) selects the Act.
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
    internal val authoredRules =
        mutableListOf<
            VexBehaviorV2ActRule<
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
            completeRule =
                ::completeRule,
        )
            .apply(
                block,
            )
    }

    @PublishedApi
    internal fun completeRule(
        rule:
            VexBehaviorV2ActRule<
                Family,
                State,
                Control
            >,
    ) {
        authoredRules +=
            rule
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
            rules =
                authoredRules.toList(),
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
    internal val completeRule:
        (
            VexBehaviorV2ActRule<
                Family,
                State,
                Control
            >,
        ) -> Unit,
) {
    /**
     * Exact-value Control form.
     *
     * This preserves the compact singleton-Control language already earned by ThemeMode.
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
        val act =
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
            )
                .apply(
                    block,
                )
                .build()

        completeRule(
            VexBehaviorV2ActRule(
                from =
                    from,
                controlDescription =
                    control.toString(),
                conditionName =
                    conditionName,
                staticAct =
                    act,
                resolveAct = { current, requestedControl ->
                    if (
                        !from.isInstance(
                            current,
                        ) ||
                        requestedControl !=
                        control
                    ) {
                        null
                    } else {
                        @Suppress(
                            "UNCHECKED_CAST",
                        )
                        val typedCurrent =
                            current as
                                Current

                        act.takeIf {
                            whenCurrent(
                                typedCurrent,
                            )
                        }
                    }
                },
            ),
        )
    }

    /**
     * Runtime Control-shape form.
     *
     * The actual CURRENT State value and actual incoming payload Control value materialize the Act.
     * This is the minimum relationship needed by Seek/Select/Click-style Controls; it does not invent
     * provider observation or provider Operation semantics.
     */
    inline fun <
        reified RequestedControl : Control,
    > on(
        conditionName: String? = null,
        noinline whenCurrent:
            (
                Current,
                RequestedControl,
            ) -> Boolean = {
                _,
                _,
                ->
                true
            },
        noinline block:
            VexBehaviorV2ActScope<
                Family,
                State,
                Control,
                Current
            >.(
                Current,
                RequestedControl,
            ) -> Unit,
    ) {
        val requestedType =
            RequestedControl::class

        completeRule(
            VexBehaviorV2ActRule(
                from =
                    from,
                controlDescription =
                    requestedType.simpleName
                        ?: requestedType.toString(),
                conditionName =
                    conditionName,
                staticAct =
                    null,
                resolveAct = { current, requestedControl ->
                    if (
                        !from.isInstance(
                            current,
                        ) ||
                        !requestedType.isInstance(
                            requestedControl,
                        )
                    ) {
                        null
                    } else {
                        @Suppress(
                            "UNCHECKED_CAST",
                        )
                        val typedCurrent =
                            current as
                                Current

                        @Suppress(
                            "UNCHECKED_CAST",
                        )
                        val typedControl =
                            requestedControl as
                                RequestedControl

                        if (
                            !whenCurrent(
                                typedCurrent,
                                typedControl,
                            )
                        ) {
                            null
                        } else {
                            VexBehaviorV2ActScope<
                                Family,
                                State,
                                Control,
                                Current
                            >(
                                from =
                                    from,
                                control =
                                    typedControl,
                                conditionName =
                                    conditionName,
                            )
                                .apply {
                                    block(
                                        typedCurrent,
                                        typedControl,
                                    )
                                }
                                .build()
                        }
                    }
                },
            ),
        )
    }
}

class VexBehaviorV2ActScope<
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
    Control : VexBehaviorFamily.Control<Family>,
    Current : State,
> @PublishedApi internal constructor(
    private val from:
        KClass<Current>,
    private val control:
        Control,
    private val conditionName:
        String?,
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

    @PublishedApi
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
        )
    }
}

class VexBehaviorV2BeginScope<
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
> internal constructor(
    private val mode: VexBehaviorV2BeginMode,
) {
    private var operation:
        VexOperation<
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
            operation ==
                null,
        ) {
            "begin V2 currently admits one Operation"
        }

        return VexBehaviorV2SaveDestination(
            state =
                state,
            complete = { completed ->
                require(
                    operation ==
                        null,
                ) {
                    "begin Operation was already completed"
                }

                operation =
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
            operation =
                requireNotNull(
                    operation,
                ) {
                    "begin V2 requires one Operation; Save(...).results { ... } is the current patient zero"
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
