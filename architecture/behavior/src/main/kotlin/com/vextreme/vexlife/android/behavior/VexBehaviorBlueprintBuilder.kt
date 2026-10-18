package com.vextreme.vexlife.android.behavior

import kotlin.reflect.KClass

/**
 * Construction scope whose source shape resembles one exact family's behavior graph.
 */
class VexBehaviorBlueprintBuilder<
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
            VexBehaviorRelationship<
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
            VexBehaviorStateScope<
                Family,
                State,
                Act,
                Effect,
                Current
            >.() -> Unit = {},
    ) {
        stateTypes +=
            Current::class

        VexBehaviorStateScope<
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
        effect: Effect,
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
            VexBehaviorRelationship<
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
            "behavior relationship was already completed: " +
                relationship
        }

        authoredRelationships +=
            relationship
    }

    internal fun build():
        VexBehaviorBlueprint<
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
                " has relationship(s) without a terminal .stay() or .admit<State>(): " +
                pendingRelationships.values
                    .joinToString()
        }

        return VexBehaviorBlueprint(
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

class VexBehaviorStateScope<
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
            String?,
        ) -> Int,
    @PublishedApi
    internal val completeRelationship:
        (
            Int,
            VexBehaviorRelationship<
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
    ): VexBehaviorDestination<
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

        return VexBehaviorDestination(
            relationshipId =
                relationshipId,
            from = from,
            act = act,
            effect = effect,
            conditionName =
                conditionName,
            whenCurrent =
                whenCurrent,
            completeRelationship =
                completeRelationship,
        )
    }
}

/**
 * Completes one authored relationship with its immediate admitted State KIND.
 *
 * Every [VexBehaviorStateScope.on] must terminate in exactly one [stay] or [admit]. Construction
 * fails if a relationship is silently left unfinished.
 */
class VexBehaviorDestination<
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
    internal val effect: Effect,
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
            VexBehaviorRelationship<
                Family,
                State,
                Act,
                Effect
            >,
        ) -> Unit,
) {
    /**
     * Admit another semantic State kind immediately after the Effect is accepted for dispatch.
     */
    inline fun <
        reified Next : State,
    > admit() {
        completeRelationship(
            relationshipId,
            VexBehaviorRelationship(
                from = from,
                act = act,
                effect = effect,
                admittedStateKind =
                    Next::class,
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

    @Deprecated(
        message =
            "Use admit(); the relationship declares immediate semantic admission, not final provider outcome",
        replaceWith =
            ReplaceWith(
                "admit<Next>()",
            ),
    )
    inline fun <
        reified Next : State,
    > to() {
        admit<Next>()
    }

    /**
     * Admit the same State kind while its data/flavor may change.
     */
    fun stay() {
        completeRelationship(
            relationshipId,
            VexBehaviorRelationship(
                from = from,
                act = act,
                effect = effect,
                admittedStateKind =
                    from,
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
