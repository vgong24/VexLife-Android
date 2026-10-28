package com.vextreme.vexlife.android.behavior

import kotlin.reflect.KClass

/**
 * Exact Kotlin spelling of the Origin-Walk begin(mode = ...) relationship.
 *
 * This is semantic scheduling intent, not a coroutine/thread/transaction primitive.
 */
enum class VexBehaviorV2BeginMode {
    Replaceable,
    Sequenced,
}

enum class VexBehaviorV2StateDispositionKind {
    Stay,
    Admit,
}

data class VexBehaviorV2StateDisposition<
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
>(
    val kind: VexBehaviorV2StateDispositionKind,
    val stateKind: KClass<out State>,
)

/**
 * One Success or Failure result consequence.
 *
 * Null means that result has no later State admission. The Origin Walk intentionally does not
 * require a redundant stay() in a result block when no later State consequence exists.
 */
data class VexBehaviorV2Result<
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
>(
    val admittedStateKind: KClass<out State>?,
)

data class VexBehaviorV2Results<
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
>(
    val success:
        VexBehaviorV2Result<
            Family,
            State
        >,
    val failure:
        VexBehaviorV2Result<
            Family,
            State
        >,
)

/**
 * One exact operation(...) inside begin(...).
 *
 * [effect] is the existing SCAE Effect identity realized by this operation. V2 does not create a
 * fifth Family role for Operation; Operation is the execution relationship around that Effect.
 */
data class VexBehaviorV2Operation<
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
    Effect : VexBehaviorFamily.Effect<Family>,
> internal constructor(
    val effect: Effect,
    val results:
        VexBehaviorV2Results<
            Family,
            State
        >,
)

/**
 * The Origin-Walk Effect scope:
 *
 *     begin(mode = ...) {
 *         operation(...).results { ... }
 *     }
 */
data class VexBehaviorV2Begin<
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
    Effect : VexBehaviorFamily.Effect<Family>,
> internal constructor(
    val mode: VexBehaviorV2BeginMode,
    val operation:
        VexBehaviorV2Operation<
            Family,
            State,
            Effect
        >,
)

/**
 * One V2 cause/effect bond inside one exact Behavior family.
 */
class VexBehaviorV2Relationship<
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
    Act : VexBehaviorFamily.Act<Family>,
    Effect : VexBehaviorFamily.Effect<Family>,
> internal constructor(
    val from: KClass<out State>,
    val act: Act,
    val effect: Effect,
    val begin:
        VexBehaviorV2Begin<
            Family,
            State,
            Effect
        >?,
    val immediateDisposition:
        VexBehaviorV2StateDisposition<
            Family,
            State
        >,
    val conditionName: String?,
    private val whenCurrent: (State) -> Boolean,
) {
    internal fun isAvailableFrom(
        current: State,
    ): Boolean =
        from.isInstance(
            current,
        ) &&
            whenCurrent(
                current,
            )
}

data class VexBehaviorV2Resolution<
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
    Effect : VexBehaviorFamily.Effect<Family>,
>(
    val effect: Effect,
    val begin:
        VexBehaviorV2Begin<
            Family,
            State,
            Effect
        >?,
    val immediateDisposition:
        VexBehaviorV2StateDisposition<
            Family,
            State
        >,
)

// [VXG RealForever]
