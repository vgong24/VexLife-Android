package com.vextreme.vexlife.android.behavior

import kotlin.reflect.KClass

/**
 * V2 construction scope.
 *
 * The source shape intentionally stays close to V1. The new vocabulary is limited to begin(mode)
 * and optional later result admission so a future reader can compare versions without learning a
 * second SCAE architecture.
 */
class VexBehaviorBlueprintBuilderV2<
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
            VexBehaviorRelationshipV2<
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

    inline fun <reified Current : State> state(
        block:
            VexBehaviorStateScopeV2<
                Family,
                State,
                Act,
                Effect,
                Current
            >.() -> Unit = {},
    ) {
        stateTypes +=
            Current::class

        VexBehaviorStateScopeV2<
            Family,
            State,
            Act,
            Effect,
            Current
        >(
            from = Current::class,
            beginRelationship =
                ::beginRelationship,
            completeRelationship =
                ::completeRelationship,
        ).apply(block)
    }

    @PublishedApi
    internal fun beginRelationship(
        from: KClass<out State>,
        act: Act,
        effectPlan:
            VexBehaviorEffectPlanV2<
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
                effectPlan.executionMode
                    ?.let { mode ->
                        append(" begin[")
                        append(mode)
                        append("]")
                    }
                if (
                    conditionName !=
                    null
                ) {
                    append(
                        " when ",
                    )
                    append(
                        conditionName,
                    )
                }
            }

        return id
    }

    @PublishedApi
    internal fun completeRelationship(
        id: Int,
        relationship:
            VexBehaviorRelationshipV2<
                Family,
                State,
                Act,
                Effect
            >,
    ) {
        require(
            pendingRelationships.remove(
                id,
            ) != null,
        ) {
            "behavior V2 relationship was already completed"
        }

        authoredRelationships +=
            relationship
    }

    internal fun build():
        VexBehaviorBlueprintV2<
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
                pendingRelationships.values
                    .joinToString()
        }

        return VexBehaviorBlueprintV2(
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

class VexBehaviorStateScopeV2<
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
            VexBehaviorEffectPlanV2<
                State,
                Effect
            >,
            String?,
        ) -> Int,
    @PublishedApi
    internal val completeRelationship:
        (
            Int,
            VexBehaviorRelationshipV2<
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
    ): VexBehaviorDestinationV2<
        Family,
        State,
        Act,
        Effect,
        Current
    > =
        on(
            act = act,
            effect =
                VexBehaviorEffectPlanV2(
                    effect = effect,
                    executionMode = null,
                    results = null,
                ),
            conditionName = conditionName,
            whenCurrent = whenCurrent,
        )

    /**
     * Async V2 Effect plan formed by [begin].
     */
    fun on(
        act: Act,
        effect:
            VexBehaviorEffectPlanV2<
                State,
                Effect
            >,
        conditionName: String? = null,
        whenCurrent: (Current) -> Boolean = {
            true
        },
    ): VexBehaviorDestinationV2<
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

        return VexBehaviorDestinationV2(
            relationshipId =
                relationshipId,
            from = from,
            act = act,
            effectPlan = effect,
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
     * execution lane. The lane identity is intentionally not stored here; the concrete Effect owner
     * (for example FamilyStore<Family>) supplies that coordination boundary.
     */
    fun begin(
        effect: Effect,
        mode: VexBehaviorExecutionModeV2,
        block:
            VexBehaviorAsyncResultsBuilderV2<
                State
            >.() -> Unit,
    ): VexBehaviorEffectPlanV2<
        State,
        Effect
    > {
        val results =
            VexBehaviorAsyncResultsBuilderV2<
                State
            >()
                .apply(block)
                .build()

        return VexBehaviorEffectPlanV2(
            effect = effect,
            executionMode = mode,
            results = results,
        )
    }
}

/**
 * Completes the required immediate SCAE decision.
 *
 * V2 deliberately preserves V1's rule that every on(...) ends in exactly one stay/admit. Async result
 * scopes may optionally emit a later admission, but they do not replace this immediate decision.
 */
class VexBehaviorDestinationV2<
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
    internal val act: Act,
    @PublishedApi
    internal val effectPlan:
        VexBehaviorEffectPlanV2<
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
            VexBehaviorRelationshipV2<
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
            VexBehaviorStateDispositionV2(
                kind =
                    VexBehaviorStateDispositionKindV2
                        .Admit,
                stateKind =
                    Next::class,
            ),
        )
    }

    fun stay() {
        complete(
            VexBehaviorStateDispositionV2(
                kind =
                    VexBehaviorStateDispositionKindV2
                        .Stay,
                stateKind =
                    from,
            ),
        )
    }

    @PublishedApi
    internal fun complete(
        disposition:
            VexBehaviorStateDispositionV2<
                State
            >,
    ) {
        completeRelationship(
            relationshipId,
            VexBehaviorRelationshipV2(
                from = from,
                act = act,
                effectPlan = effectPlan,
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

class VexBehaviorAsyncResultsBuilderV2<
    State : Any,
> internal constructor() {
    private var successDeclared =
        false

    private var failureDeclared =
        false

    private var success:
        VexBehaviorAsyncResultV2<
            State
        >? =
        null

    private var failure:
        VexBehaviorAsyncResultV2<
            State
        >? =
        null

    fun success(
        block:
            VexBehaviorAsyncResultScopeV2<
                State
            >.() -> Unit = {},
    ) {
        require(!successDeclared) {
            "Success result was declared more than once"
        }

        successDeclared = true
        success =
            VexBehaviorAsyncResultScopeV2<
                State
            >()
                .apply(block)
                .build()
    }

    fun failure(
        block:
            VexBehaviorAsyncResultScopeV2<
                State
            >.() -> Unit = {},
    ) {
        require(!failureDeclared) {
            "Failure result was declared more than once"
        }

        failureDeclared = true
        failure =
            VexBehaviorAsyncResultScopeV2<
                State
            >()
                .apply(block)
                .build()
    }

    internal fun build():
        VexBehaviorAsyncResultsV2<
            State
        > {
        require(successDeclared) {
            "begin V2 requires an explicit Success result"
        }
        require(failureDeclared) {
            "begin V2 requires an explicit Failure result"
        }

        return VexBehaviorAsyncResultsV2(
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

class VexBehaviorAsyncResultScopeV2<
    State : Any,
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
        VexBehaviorAsyncResultV2<
            State
        > =
        VexBehaviorAsyncResultV2(
            admittedStateKind =
                admittedStateKind,
        )
}

// [VXG RealForever]
