package com.vextreme.vexlife.android.behavior

import kotlin.reflect.KClass

/**
 * V2 construction scope using the Origin-Walk vocabulary directly.
 */
class VexBehaviorV2BlueprintBuilder<
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
    Act : VexBehaviorFamily.Act<Family>,
    Effect : VexBehaviorFamily.Effect<Family>,
> internal constructor(
    private val behaviorRef: String,
) {
    @PublishedApi
    internal val stateTypes =
        linkedSetOf<
            KClass<out State>
        >()

    @PublishedApi
    internal val authoredRelationships =
        mutableListOf<
            VexBehaviorV2Relationship<
                Family,
                State,
                Act,
                Effect
            >
        >()

    private val pendingRelationships =
        linkedMapOf<
            Int,
            String
        >()

    private var nextRelationshipId =
        0

    inline fun <
        reified Current : State,
    > state(
        block:
            VexBehaviorV2StateScope<
                Family,
                State,
                Act,
                Effect,
                Current
            >.() -> Unit = {},
    ) {
        stateTypes +=
            Current::class

        VexBehaviorV2StateScope<
            Family,
            State,
            Act,
            Effect,
            Current
        >(
            from =
                Current::class,
            beginRelationship =
                ::beginRelationship,
            completeRelationship =
                ::completeRelationship,
        ).apply(
            block,
        )
    }

    @PublishedApi
    internal fun beginRelationship(
        from: KClass<out State>,
        act: Act,
        effect: Effect,
        begin:
            VexBehaviorV2Begin<
                Family,
                State,
                Effect
            >?,
        conditionName: String?,
    ): Int {
        val id =
            nextRelationshipId++

        pendingRelationships[id] =
            buildString {
                append(
                    from.simpleName,
                )
                append(
                    " + ",
                )
                append(
                    act,
                )
                append(
                    " / ",
                )
                append(
                    effect,
                )

                begin
                    ?.let {
                        append(
                            " begin[",
                        )
                        append(
                            it.mode,
                        )
                        append(
                            "]",
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
    internal fun completeRelationship(
        id: Int,
        relationship:
            VexBehaviorV2Relationship<
                Family,
                State,
                Act,
                Effect
            >,
    ) {
        require(
            pendingRelationships.remove(
                id,
            ) !=
                null,
        ) {
            "behavior V2 relationship was already completed"
        }

        authoredRelationships +=
            relationship
    }

    internal fun build():
        VexBehaviorV2Blueprint<
            Family,
            State,
            Act,
            Effect
        > {
        require(
            pendingRelationships
                .isEmpty(),
        ) {
            "behavior " +
                behaviorRef +
                " V2 has relationship(s) without a terminal .stay() or .admit<State>(): " +
                pendingRelationships
                    .values
                    .joinToString()
        }

        return VexBehaviorV2Blueprint(
            behaviorRef =
                behaviorRef,
            states =
                stateTypes.toSet(),
            relationships =
                authoredRelationships
                    .toList(),
        )
    }
}

class VexBehaviorV2StateScope<
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
    Act : VexBehaviorFamily.Act<Family>,
    Effect : VexBehaviorFamily.Effect<Family>,
    Current : State,
> @PublishedApi internal constructor(
    @PublishedApi
    internal val from:
        KClass<Current>,
    @PublishedApi
    internal val beginRelationship:
        (
            KClass<out State>,
            Act,
            Effect,
            VexBehaviorV2Begin<
                Family,
                State,
                Effect
            >?,
            String?,
        ) -> Int,
    @PublishedApi
    internal val completeRelationship:
        (
            Int,
            VexBehaviorV2Relationship<
                Family,
                State,
                Act,
                Effect
            >,
        ) -> Unit,
) {
    fun on(
        act: Act,
        effect: Effect,
        conditionName: String? = null,
        whenCurrent: (Current) -> Boolean = {
            true
        },
    ): VexBehaviorV2Destination<
        Family,
        State,
        Act,
        Effect,
        Current
    > =
        destination(
            act = act,
            effect = effect,
            begin = null,
            conditionName = conditionName,
            whenCurrent = whenCurrent,
        )

    fun on(
        act: Act,
        effect:
            VexBehaviorV2Begin<
                Family,
                State,
                Effect
            >,
        conditionName: String? = null,
        whenCurrent: (Current) -> Boolean = {
            true
        },
    ): VexBehaviorV2Destination<
        Family,
        State,
        Act,
        Effect,
        Current
    > =
        destination(
            act = act,
            effect =
                effect
                    .operation
                    .effect,
            begin =
                effect,
            conditionName =
                conditionName,
            whenCurrent =
                whenCurrent,
        )

    private fun destination(
        act: Act,
        effect: Effect,
        begin:
            VexBehaviorV2Begin<
                Family,
                State,
                Effect
            >?,
        conditionName: String?,
        whenCurrent: (Current) -> Boolean,
    ): VexBehaviorV2Destination<
        Family,
        State,
        Act,
        Effect,
        Current
    > {
        val relationshipId =
            beginRelationship(
                from,
                act,
                effect,
                begin,
                conditionName,
            )

        return VexBehaviorV2Destination(
            relationshipId =
                relationshipId,
            from =
                from,
            act =
                act,
            effect =
                effect,
            begin =
                begin,
            conditionName =
                conditionName,
            whenCurrent =
                whenCurrent,
            completeRelationship =
                completeRelationship,
        )
    }

    fun begin(
        mode: VexBehaviorV2BeginMode,
        block:
            VexBehaviorV2BeginScope<
                Family,
                State,
                Effect
            >.() -> Unit,
    ): VexBehaviorV2Begin<
        Family,
        State,
        Effect
    > =
        VexBehaviorV2BeginScope<
            Family,
            State,
            Effect
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
    Effect : VexBehaviorFamily.Effect<Family>,
> internal constructor(
    private val mode: VexBehaviorV2BeginMode,
) {
    private var operation:
        VexBehaviorV2Operation<
            Family,
            State,
            Effect
        >? =
        null

    fun operation(
        effect: Effect,
    ): VexBehaviorV2OperationDestination<
        Family,
        State,
        Effect
    > {
        require(
            operation ==
                null,
        ) {
            "begin V2 currently admits one operation; another operation was already declared"
        }

        return VexBehaviorV2OperationDestination(
            effect =
                effect,
            complete = { completed ->
                require(
                    operation ==
                        null,
                ) {
                    "operation was already completed"
                }
                operation =
                    completed
            },
        )
    }

    internal fun build():
        VexBehaviorV2Begin<
            Family,
            State,
            Effect
        > =
        VexBehaviorV2Begin(
            mode =
                mode,
            operation =
                requireNotNull(
                    operation,
                ) {
                    "begin V2 requires one operation(...).results { ... }"
                },
        )
}

class VexBehaviorV2OperationDestination<
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
    Effect : VexBehaviorFamily.Effect<Family>,
> internal constructor(
    private val effect: Effect,
    private val complete:
        (
            VexBehaviorV2Operation<
                Family,
                State,
                Effect
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
            VexBehaviorV2Operation(
                effect =
                    effect,
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
    @PublishedApi
    internal var admittedStateKind:
        KClass<out State>? =
        null

    inline fun <
        reified Next : State,
    > admit() {
        require(
            admittedStateKind ==
                null,
        ) {
            "one result may admit at most one later State"
        }

        admittedStateKind =
            Next::class
    }

    internal fun build():
        VexBehaviorV2Result<
            Family,
            State
        > =
        VexBehaviorV2Result(
            admittedStateKind =
                admittedStateKind,
        )
}

class VexBehaviorV2Destination<
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
    Act : VexBehaviorFamily.Act<Family>,
    Effect : VexBehaviorFamily.Effect<Family>,
    Current : State,
> @PublishedApi internal constructor(
    @PublishedApi
    internal val relationshipId:
        Int,
    @PublishedApi
    internal val from:
        KClass<Current>,
    @PublishedApi
    internal val act:
        Act,
    @PublishedApi
    internal val effect:
        Effect,
    @PublishedApi
    internal val begin:
        VexBehaviorV2Begin<
            Family,
            State,
            Effect
        >?,
    @PublishedApi
    internal val conditionName:
        String?,
    @PublishedApi
    internal val whenCurrent:
        (Current) -> Boolean,
    @PublishedApi
    internal val completeRelationship:
        (
            Int,
            VexBehaviorV2Relationship<
                Family,
                State,
                Act,
                Effect
            >,
        ) -> Unit,
) {
    inline fun <
        reified Next : State,
    > admit() {
        complete(
            VexBehaviorV2StateDisposition(
                kind =
                    VexBehaviorV2StateDispositionKind
                        .Admit,
                stateKind =
                    Next::class,
            ),
        )
    }

    fun stay() {
        complete(
            VexBehaviorV2StateDisposition(
                kind =
                    VexBehaviorV2StateDispositionKind
                        .Stay,
                stateKind =
                    from,
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
        completeRelationship(
            relationshipId,
            VexBehaviorV2Relationship(
                from =
                    from,
                act =
                    act,
                effect =
                    effect,
                begin =
                    begin,
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
