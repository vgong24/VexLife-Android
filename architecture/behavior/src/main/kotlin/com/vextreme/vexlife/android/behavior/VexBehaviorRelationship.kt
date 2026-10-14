package com.vextreme.vexlife.android.behavior

import kotlin.reflect.KClass

/**
 * One authored cause/effect bond in a VexBehaviorBlueprint.
 *
 * The current State is the causal context. [act] names what is requested from that context,
 * [effect] names what runtime meaning should be attempted, and [to] names the expected next State
 * kind if that effect converges.
 *
 * Runtime/provider code still owns whether the effect actually succeeds and which State is
 * subsequently observed.
 */
class VexBehaviorRelationship<
    State : Any,
    Act : Any,
    Effect : Any,
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
 *
 * Resolution does not perform [effect] and does not fabricate the destination State value.
 */
data class VexBehaviorResolution<
    State : Any,
    Effect : Any,
>(
    val effect: Effect,
    val destination: KClass<out State>,
)
