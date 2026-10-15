package com.vextreme.vexlife.android.behavior

import kotlin.reflect.KClass

/**
 * One authored cause/effect bond inside one exact Behavior family.
 *
 * [admittedStateKind] is the immediate semantic State kind admitted when the Effect is accepted for
 * dispatch. It is NOT a promise about the provider's eventual observed outcome.
 *
 * Example:
 *
 * Idle + Locate -> RequestCurrentLocation -> admit Finding
 *
 * The provider may later report Ready, Denied, Unavailable, Error, or another truthful observed
 * State without violating this relationship.
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
    val admittedStateKind: KClass<out State>,
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
            append(admittedStateKind.simpleName)
            if (conditionName != null) {
                append(" when ")
                append(conditionName)
            }
        }
}

/**
 * Read-only semantic consequence of one Act from one current State.
 *
 * [admittedStateKind] describes immediate semantic admission topology only. A concrete Connection
 * may need runtime data to construct the admitted State value, and later provider observations may
 * truthfully produce other State kinds.
 */
data class VexBehaviorResolution<
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
    Effect : VexBehaviorFamily.Effect<Family>,
>(
    val effect: Effect,
    val admittedStateKind: KClass<out State>,
) {
    /**
     * Compatibility name from the earlier experiment. Prefer [admittedStateKind].
     */
    @Deprecated(
        message =
            "destination implied eventual outcome; use admittedStateKind for immediate semantic admission",
        replaceWith =
            ReplaceWith(
                "admittedStateKind",
            ),
    )
    val destination: KClass<out State>
        get() =
            admittedStateKind
}
