package com.vextreme.vexlife.android.behavior

import kotlin.reflect.KClass

/**
 * Construction scope whose source shape is intended to resemble the behavior itself.
 *
 * Each State block is a semantic neighborhood:
 *
 * state<Playing> {
 *     on(Pause, PausePlayback).to<Paused>()
 *     on(FastForward, StartFastForward).to<FastForwarding>()
 * }
 *
 * The feature authors each relationship once. Incoming neighborhoods and whole-graph views are
 * derived from those same declarations.
 */
class VexBehaviorBlueprintBuilder<
    State : Any,
    Act : Any,
    Effect : Any,
> internal constructor(
    private val behaviorRef: String,
) {
    @PublishedApi
    internal val stateTypes =
        linkedSetOf<KClass<out State>>()

    @PublishedApi
    internal val authoredRelationships =
        mutableListOf<VexBehaviorRelationship<State, Act, Effect>>()

    /**
     * Admit one State kind and declare the Acts that have meaning from that current State.
     */
    inline fun <reified Current : State> state(
        block: VexBehaviorStateScope<State, Act, Effect, Current>.() -> Unit = {},
    ) {
        stateTypes +=
            Current::class

        VexBehaviorStateScope<State, Act, Effect, Current>(
            from = Current::class,
            addRelationship = authoredRelationships::add,
        ).apply(block)
    }

    internal fun build():
        VexBehaviorBlueprint<State, Act, Effect> =
        VexBehaviorBlueprint(
            behaviorRef = behaviorRef,
            states = stateTypes.toSet(),
            relationships = authoredRelationships.toList(),
        )
}

/**
 * One current State's outward-facing behavioral neighborhood.
 *
 * An Act is meaningful here only when this scope declares a relationship for it.
 */
class VexBehaviorStateScope<
    State : Any,
    Act : Any,
    Effect : Any,
    Current : State,
> @PublishedApi internal constructor(
    @PublishedApi
    internal val from: KClass<Current>,
    @PublishedApi
    internal val addRelationship:
        (VexBehaviorRelationship<State, Act, Effect>) -> Unit,
) {
    /**
     * Begin one relationship from this current State.
     *
     * [effect] is deliberately separate from [act]: a UI/human action names the cause while the
     * provider-facing Effect names what runtime meaning should be attempted.
     */
    fun on(
        act: Act,
        effect: Effect,
        conditionName: String? = null,
        whenCurrent: (Current) -> Boolean = {
            true
        },
    ): VexBehaviorDestination<State, Act, Effect, Current> =
        VexBehaviorDestination(
            from = from,
            act = act,
            effect = effect,
            conditionName = conditionName,
            whenCurrent = whenCurrent,
            addRelationship = addRelationship,
        )
}

/**
 * Destination half of an Act authored from one exact current State.
 */
class VexBehaviorDestination<
    State : Any,
    Act : Any,
    Effect : Any,
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
    internal val whenCurrent: (Current) -> Boolean,
    @PublishedApi
    internal val addRelationship:
        (VexBehaviorRelationship<State, Act, Effect>) -> Unit,
) {
    /** Finish the relationship at another admitted State kind. */
    inline fun <reified Next : State> to() {
        addRelationship(
            VexBehaviorRelationship(
                from = from,
                act = act,
                effect = effect,
                to = Next::class,
                conditionName = conditionName,
                whenCurrent = { current ->
                    @Suppress("UNCHECKED_CAST")
                    whenCurrent(current as Current)
                },
            ),
        )
    }

    /**
     * Finish the relationship at the same State kind while allowing data inside that State to
     * change. Queue position movement is one example.
     */
    fun stay() {
        addRelationship(
            VexBehaviorRelationship(
                from = from,
                act = act,
                effect = effect,
                to = from,
                conditionName = conditionName,
                whenCurrent = { current ->
                    @Suppress("UNCHECKED_CAST")
                    whenCurrent(current as Current)
                },
            ),
        )
    }
}
