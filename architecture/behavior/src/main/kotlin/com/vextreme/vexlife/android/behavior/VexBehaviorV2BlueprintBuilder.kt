package com.vextreme.vexlife.android.behavior

import kotlin.reflect.KClass

/**
 * V2 construction scope.
 *
 * Generic ordering mirrors V1:
 *
 *     Family, State, Act, Effect
 *
 * The new vocabulary is limited to begin(mode) and optional later result admission.
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
        effectPlan:
            VexBehaviorV2EffectPlan<
                Family,
                State,
                Effect
            >,
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
                    effectPlan.effect,
                )

                effectPlan
                    .executionMode
                    ?.let { mode ->
                        append(
                            " begin[",
                        )
                        append(
                            mode,
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
            VexBehaviorV2EffectPlan<
                Family,
                State,
                Effect
            >,
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
    /**
     * V1-compatible direct Effect. No async execution/result contract is added.
     */
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
        on(
            act =
                act,
            effect =
                VexBehaviorV2EffectPlan(
                    effect =
                        effect,
                    executionMode =
                        null,
                    results =
                        null,
                ),
            conditionName =
                conditionName,
            whenCurrent =
                whenCurrent,
        )

    /**
     * Async V2 Effect plan formed by begin(...).
     */
    fun on(
        act: Act,
        effect:
            VexBehaviorV2EffectPlan<
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
    > {
        val relationshipId =
            beginRelationship(
                from,
                act,
                effect,
                conditionName,
            )

        return VexBehaviorV2Destination(
            relationshipId =
                relationshipId,
            from =
                from,
            act =
                act,
            effectPlan =
                effect,
            conditionName =
                conditionName,
            whenCurrent =
                whenCurrent,
            completeRelationship =
                completeRelationship,
        )
    }

    /**
     * Declare later-completing Effect work and how repeated episodes participate in their shared
     * execution lane. Lane identity remains owned by the concrete Effect owner.
     */
    fun begin(
        effect: Effect,
        mode: VexBehaviorV2ExecutionMode,
        block:
            VexBehaviorV2AsyncResultsBuilder<
                Family,
                State
            >.() -> Unit,
    ): VexBehaviorV2EffectPlan<
        Family,
        State,
        Effect
    > {
        val results =
            VexBehaviorV2AsyncResultsBuilder<
                Family,
                State
            >()
                .apply(
                    block,
                )
                .build()

        return VexBehaviorV2EffectPlan(
            effect =
                effect,
            executionMode =
                mode,
            results =
                results,
        )
    }
}

/**
 * Completes the required immediate SCAE decision.
 *
 * Async result scopes may optionally emit a later admission, but they do not replace this immediate
 * stay/admit decision.
 */
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
    internal val effectPlan:
        VexBehaviorV2EffectPlan<
            Family,
            State,
            Effect
        >,
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
                effectPlan =
                    effectPlan,
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

class VexBehaviorV2AsyncResultsBuilder<
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
> internal constructor() {
    private var successDeclared =
        false

    private var failureDeclared =
        false

    private var success:
        VexBehaviorV2AsyncResult<
            Family,
            State
        >? =
        null

    private var failure:
        VexBehaviorV2AsyncResult<
            Family,
            State
        >? =
        null

    fun success(
        block:
            VexBehaviorV2AsyncResultScope<
                Family,
                State
            >.() -> Unit = {},
    ) {
        require(
            !successDeclared,
        ) {
            "Success result was declared more than once"
        }

        successDeclared =
            true
        success =
            VexBehaviorV2AsyncResultScope<
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
            VexBehaviorV2AsyncResultScope<
                Family,
                State
            >.() -> Unit = {},
    ) {
        require(
            !failureDeclared,
        ) {
            "Failure result was declared more than once"
        }

        failureDeclared =
            true
        failure =
            VexBehaviorV2AsyncResultScope<
                Family,
                State
            >()
                .apply(
                    block,
                )
                .build()
    }

    internal fun build():
        VexBehaviorV2AsyncResults<
            Family,
            State
        > {
        require(
            successDeclared,
        ) {
            "begin V2 requires an explicit Success result"
        }
        require(
            failureDeclared,
        ) {
            "begin V2 requires an explicit Failure result"
        }

        return VexBehaviorV2AsyncResults(
            success =
                requireNotNull(
                    success,
                ),
            failure =
                requireNotNull(
                    failure,
                ),
        )
    }
}

class VexBehaviorV2AsyncResultScope<
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
            "async result may admit at most one later State"
        }

        admittedStateKind =
            Next::class
    }

    internal fun build():
        VexBehaviorV2AsyncResult<
            Family,
            State
        > =
        VexBehaviorV2AsyncResult(
            admittedStateKind =
                admittedStateKind,
        )
}

// [VXG RealForever]
