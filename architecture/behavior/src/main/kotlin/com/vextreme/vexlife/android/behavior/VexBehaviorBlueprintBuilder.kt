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
            addRelationship =
                authoredRelationships::add,
        ).apply(block)
    }

    internal fun build():
        VexBehaviorBlueprint<
            Family,
            State,
            Act,
            Effect
        > =
        VexBehaviorBlueprint(
            behaviorRef = behaviorRef,
            states = stateTypes.toSet(),
            relationships =
                authoredRelationships.toList(),
        )
}

class VexBehaviorStateScope<
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
    Act : VexBehaviorFamily.Act<Family>,
    Effect : VexBehaviorFamily.Effect<Family>,
    Current : State,
> @PublishedApi internal constructor(
    @PublishedApi
    internal val from: KClass<Current>,
    @PublishedApi
    internal val addRelationship:
        (
            VexBehaviorRelationship<
                Family,
                State,
                Act,
                Effect
            >
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
    > =
        VexBehaviorDestination(
            from = from,
            act = act,
            effect = effect,
            conditionName = conditionName,
            whenCurrent = whenCurrent,
            addRelationship = addRelationship,
        )
}

class VexBehaviorDestination<
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
    Act : VexBehaviorFamily.Act<Family>,
    Effect : VexBehaviorFamily.Effect<Family>,
    Current : State,
> @PublishedApi internal constructor(
    @PublishedApi
    internal val from: KClass<Current>,
    @PublishedApi
    internal val act: Act,
    @PublishedApi
    internal val effect: Effect,
    @PublishedApi
    internal val conditionName: String?,
    @PublishedApi
    internal val whenCurrent:
        (Current) -> Boolean,
    @PublishedApi
    internal val addRelationship:
        (
            VexBehaviorRelationship<
                Family,
                State,
                Act,
                Effect
            >
        ) -> Unit,
) {
    inline fun <reified Next : State> to() {
        addRelationship(
            VexBehaviorRelationship(
                from = from,
                act = act,
                effect = effect,
                to = Next::class,
                conditionName = conditionName,
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

    fun stay() {
        addRelationship(
            VexBehaviorRelationship(
                from = from,
                act = act,
                effect = effect,
                to = from,
                conditionName = conditionName,
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
