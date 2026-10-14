package com.vextreme.vexlife.android.behavior

import kotlin.reflect.KClass

/**
 * One authored cause/effect bond inside one exact Behavior family.
 */
class VexBehaviorRelationship<
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
    Act : VexBehaviorFamily.Act<Family>,
    Effect : VexBehaviorFamily.Effect<Family>,
> @PublishedApi internal constructor(
    val from: KClass<out State>,
    val act: Act,
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
            append(act)
            append(" / ")
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
 * Read-only semantic consequence of one Act from one current State.
 */
data class VexBehaviorResolution<
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
    Effect : VexBehaviorFamily.Effect<Family>,
>(
    val effect: Effect,
    val destination: KClass<out State>,
)
