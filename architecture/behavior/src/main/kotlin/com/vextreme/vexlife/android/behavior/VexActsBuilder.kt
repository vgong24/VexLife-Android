package com.vextreme.vexlife.android.behavior

import kotlin.reflect.KClass

/**
 * Human-readable Act authoring grammar.
 *
 *     state<State.Idle> {
 *         on<Control.Load> { currentState, control ->
 *             admit(...)
 *             begin(...) { ... }
 *         }
 *     }
 *
 * State traversal is explicit. Control payloads stay visible. Internal matching is not part of the
 * feature-facing language.
 */
class VexActsBuilder<
    State : Any,
    Control : Any,
> internal constructor(
    private val behaviorRef:
        String,
) {
    @PublishedApi
    internal val stateTypes =
        linkedSetOf<
            KClass<out State>
        >()

    @PublishedApi
    internal val authoredRules =
        mutableListOf<
            VexActRule<
                State,
                Control
            >
        >()

    inline fun <
        reified Current : State,
    > state(
        block:
            VexStateScope<
                State,
                Control,
                Current
            >.() -> Unit = {},
    ) {
        stateTypes +=
            Current::class

        VexStateScope<
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
            VexActRule<
                State,
                Control
            >,
    ) {
        authoredRules +=
            rule
    }

    internal fun build():
        VexActs<
            State,
            Control
        > =
        VexActs(
            behaviorRef =
                behaviorRef,
            states =
                stateTypes.toSet(),
            rules =
                authoredRules.toList(),
        )
}

class VexStateScope<
    State : Any,
    Control : Any,
    Current : State,
> @PublishedApi internal constructor(
    @PublishedApi
    internal val from:
        KClass<Current>,
    @PublishedApi
    internal val completeRule:
        (
            VexActRule<
                State,
                Control
            >,
        ) -> Unit,
) {
    /**
     * Exact Control-value form.
     *
     * The lambda still receives the exact CURRENT State value and incoming Control so the causal
     * inputs remain visible at the authoring site.
     */
    fun on(
        control:
            Control,
        conditionName:
            String? =
            null,
        whenCurrent:
            (
                currentState: Current,
                control: Control,
            ) -> Boolean = {
                _,
                _,
                ->
                true
            },
        block:
            VexActScope<
                State,
                Control,
                Current
            >.(
                currentState: Current,
                control: Control,
            ) -> Unit,
    ) {
        completeRule(
            VexActRule(
                from =
                    from,
                controlDescription =
                    control.toString(),
                conditionName =
                    conditionName,
                matchAct = { currentState, requestedControl ->
                    if (
                        !from.isInstance(
                            currentState,
                        ) ||
                        requestedControl !=
                        control
                    ) {
                        null
                    } else {
                        @Suppress(
                            "UNCHECKED_CAST",
                        )
                        val typedCurrentState =
                            currentState as
                                Current

                        if (
                            !whenCurrent(
                                typedCurrentState,
                                requestedControl,
                            )
                        ) {
                            null
                        } else {
                            VexActScope<
                                State,
                                Control,
                                Current
                            >(
                                from =
                                    from,
                                control =
                                    requestedControl,
                                conditionName =
                                    conditionName,
                            )
                                .apply {
                                    block(
                                        typedCurrentState,
                                        requestedControl,
                                    )
                                }
                                .build()
                        }
                    }
                },
            ),
        )
    }

    /**
     * Runtime Control-shape form for payload Controls.
     */
    inline fun <
        reified RequestedControl : Control,
    > on(
        conditionName:
            String? =
            null,
        noinline whenCurrent:
            (
                currentState: Current,
                control: RequestedControl,
            ) -> Boolean = {
                _,
                _,
                ->
                true
            },
        noinline block:
            VexActScope<
                State,
                Control,
                Current
            >.(
                currentState: Current,
                control: RequestedControl,
            ) -> Unit,
    ) {
        val requestedType =
            RequestedControl::class

        completeRule(
            VexActRule(
                from =
                    from,
                controlDescription =
                    requestedType.simpleName
                        ?: requestedType.toString(),
                conditionName =
                    conditionName,
                matchAct = { currentState, control ->
                    if (
                        !from.isInstance(
                            currentState,
                        ) ||
                        !requestedType.isInstance(
                            control,
                        )
                    ) {
                        null
                    } else {
                        @Suppress(
                            "UNCHECKED_CAST",
                        )
                        val typedCurrentState =
                            currentState as
                                Current

                        @Suppress(
                            "UNCHECKED_CAST",
                        )
                        val typedControl =
                            control as
                                RequestedControl

                        if (
                            !whenCurrent(
                                typedCurrentState,
                                typedControl,
                            )
                        ) {
                            null
                        } else {
                            VexActScope<
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
                                        typedCurrentState,
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

class VexActScope<
    State : Any,
    Control : Any,
    Current : State,
> @PublishedApi internal constructor(
    private val from:
        KClass<Current>,
    private val control:
        Control,
    private val conditionName:
        String?,
) {
    private var begin:
        VexBegin<
            State
        >? =
        null

    private var immediateDisposition:
        VexStateDisposition<
            State
        >? =
        null

    fun admit(
        state:
            State,
    ) {
        require(
            immediateDisposition ==
                null,
        ) {
            "on(Control) may declare only one immediate stay() or admit(state)"
        }

        immediateDisposition =
            VexStateDisposition(
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
            VexStateDisposition(
                admittedState =
                    null,
            )
    }

    fun begin(
        mode:
            VexBeginMode,
        block:
            VexBeginScope<
                State
            >.() -> Unit,
    ) {
        require(
            begin ==
                null,
        ) {
            "one Act currently admits at most one begin(...)"
        }

        begin =
            VexBeginScope<
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
        VexAct<
            State,
            Control
        > {
        val immediate =
            requireNotNull(
                immediateDisposition,
            ) {
                "on(Control) requires exactly one immediate stay() or admit(state)"
            }

        return VexAct(
            from =
                from,
            control =
                control,
            begin =
                begin,
            immediateDisposition =
                immediate,
            conditionName =
                conditionName,
        )
    }
}

class VexBeginScope<
    State : Any,
> internal constructor(
    private val mode:
        VexBeginMode,
) {
    private var operation:
        VexOperation<
            State
        >? =
        null

    fun save(
        state:
            State,
    ):
        VexSaveDestination<
            State
        > {
        require(
            operation ==
                null,
        ) {
            "begin currently admits one Operation"
        }

        return VexSaveDestination(
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
        VexBegin<
            State
        > =
        VexBegin(
            mode =
                mode,
            operation =
                requireNotNull(
                    operation,
                ) {
                    "begin requires one Operation; Save(...).results { ... } is the current patient zero"
                },
        )
}

class VexSaveDestination<
    State : Any,
> internal constructor(
    private val state:
        State,
    private val complete:
        (
            VexSave<
                State
            >,
        ) -> Unit,
) {
    fun results(
        block:
            VexResultsScope<
                State
            >.() -> Unit,
    ) {
        complete(
            VexSave(
                state =
                    state,
                results =
                    VexResultsScope<
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

class VexResultsScope<
    State : Any,
> internal constructor() {
    private var success:
        VexResult<
            State
        >? =
        null

    private var failure:
        VexResult<
            State
        >? =
        null

    fun success(
        block:
            VexResultScope<
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
            VexResultScope<
                State
            >()
                .apply(
                    block,
                )
                .build()
    }

    fun failure(
        block:
            VexResultScope<
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
            VexResultScope<
                State
            >()
                .apply(
                    block,
                )
                .build()
    }

    internal fun build():
        VexResults<
            State
        > =
        VexResults(
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

class VexResultScope<
    State : Any,
> internal constructor() {
    private var admittedState:
        State? =
        null

    fun admit(
        state:
            State,
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
        VexResult<
            State
        > =
        VexResult(
            admittedState =
                admittedState,
        )
}

// [VXG RealForever]
