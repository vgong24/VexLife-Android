package com.vextreme.vexlife.android.behavior

import kotlin.reflect.KClass

/**
 * One authored bond in a VexBehaviorBlueprint.
 *
 * The relationship is immutable after blueprint construction. Feature authors normally create these
 * through the blueprint DSL so related bonds remain visually adjacent in source.
 */
class VexBehaviorRelationship<
    State : Any,
    Intention : Any,
    Effect : Any,
> @PublishedApi internal constructor(
    val from: KClass<out State>,
    val intention: Intention,
    val effect: Effect,
    val to: KClass<out State>,
    val conditionName: String?,
    private val whenCurrent: (State) -> Boolean,
) {
    internal fun isAvailableFrom(
        current: State,
    ): Boolean =
        from.isInstance(current) &&
            whenCurrent(current)

    override fun toString(): String =
        buildString {
            append(from.simpleName)
            append(" --")
            append(effect)
            append("--> ")
            append(to.simpleName)
            if (conditionName != null) {
                append(" when ")
                append(conditionName)
            }
        }
}

/**
 * Read-only result of resolving one human intention from one current state.
 *
 * Resolution does not perform the effect and does not fabricate the destination state value.
 */
data class VexBehaviorResolution<
    State : Any,
    Effect : Any,
>(
    val effect: Effect,
    val destination: KClass<out State>,
)
