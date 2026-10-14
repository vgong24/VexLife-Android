package com.vextreme.vexlife.android.behavior

import kotlin.reflect.KClass

/**
 * Construction scope whose source shape is intended to resemble the behavior graph.
 *
 * Declaration is mutable only while the blueprint is being formed. Build freezes it into an
 * immutable VexBehaviorBlueprint.
 */
class VexBehaviorBlueprintBuilder<
    State : Any,
    Intention : Any,
    Effect : Any,
> internal constructor(
    private val behaviorRef: String,
) {
    @PublishedApi
    internal val stateTypes =
        linkedSetOf<KClass<out State>>()

    @PublishedApi
    internal val authoredRelationships =
        mutableListOf<VexBehaviorRelationship<State, Intention, Effect>>()

    /** Admit one state kind into this behavior. */
    inline fun <reified Current : State> state() {
        stateTypes +=
            Current::class
    }

    /**
     * Keep every relationship selected by one human intention visually together.
     */
    fun intention(
        intention: Intention,
        block: VexBehaviorIntentionScope<State, Intention, Effect>.() -> Unit,
    ) {
        VexBehaviorIntentionScope(
            intention = intention,
            addRelationship = authoredRelationships::add,
        ).apply(block)
    }

    internal fun build():
        VexBehaviorBlueprint<State, Intention, Effect> =
        VexBehaviorBlueprint(
            behaviorRef = behaviorRef,
            states = stateTypes.toSet(),
            relationships = authoredRelationships.toList(),
        )
}

/**
 * Relationships that share one human intention.
 *
 * This scope makes cycles and bonds first-class source structure rather than unrelated val
 * declarations.
 */
class VexBehaviorIntentionScope<
    State : Any,
    Intention : Any,
    Effect : Any,
> internal constructor(
    @PublishedApi
    internal val intention: Intention,
    @PublishedApi
    internal val addRelationship:
        (VexBehaviorRelationship<State, Intention, Effect>) -> Unit,
) {
    /** One directional relationship between two state kinds. */
    inline fun <
        reified From : State,
        reified To : State,
    > move(
        effect: Effect,
        conditionName: String? = null,
        noinline whenCurrent: (From) -> Boolean = {
            true
        },
    ) {
        addRelationship(
            VexBehaviorRelationship(
                from = From::class,
                intention = intention,
                effect = effect,
                to = To::class,
                conditionName = conditionName,
                whenCurrent = { current ->
                    @Suppress("UNCHECKED_CAST")
                    whenCurrent(current as From)
                },
            ),
        )
    }

    /**
     * Two opposite relationships authored as one visible bond.
     *
     * This is useful for behavior such as Play/Pause where the relationship makes sense as a cycle.
     */
    inline fun <
        reified Left : State,
        reified Right : State,
    > cycle(
        forward: Effect,
        backward: Effect,
        forwardConditionName: String? = null,
        backwardConditionName: String? = null,
        noinline forwardWhen: (Left) -> Boolean = {
            true
        },
        noinline backwardWhen: (Right) -> Boolean = {
            true
        },
    ) {
        move<Left, Right>(
            effect = forward,
            conditionName = forwardConditionName,
            whenCurrent = forwardWhen,
        )
        move<Right, Left>(
            effect = backward,
            conditionName = backwardConditionName,
            whenCurrent = backwardWhen,
        )
    }

    /**
     * Relationship whose state kind remains the same while its data may change.
     *
     * Queue Next or Previous are examples: Positioned stays Positioned while index data changes.
     */
    inline fun <
        reified Current : State,
    > stay(
        effect: Effect,
        conditionName: String? = null,
        noinline whenCurrent: (Current) -> Boolean = {
            true
        },
    ) {
        move<Current, Current>(
            effect = effect,
            conditionName = conditionName,
            whenCurrent = whenCurrent,
        )
    }
}
