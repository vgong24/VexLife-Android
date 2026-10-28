package com.vextreme.vexlife.android.behavior

import kotlin.reflect.KClass

/**
 * How repeated later-completing episodes participate in the rightful owner's execution lane.
 *
 * This is semantic scheduling intent, not a coroutine/thread/transaction primitive.
 */
enum class VexBehaviorV2ExecutionMode {
    Replaceable,
    Sequenced,
}

enum class VexBehaviorV2StateDispositionKind {
    Stay,
    Admit,
}

/**
 * One immediate or later State disposition inside one exact Behavior family.
 *
 * Family remains present in the type equation so V2 helper types do not weaken the compiler-bound
 * kinship already established by VexBehaviorFamily.
 */
data class VexBehaviorV2StateDisposition<
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
>(
    val kind: VexBehaviorV2StateDispositionKind,
    val stateKind: KClass<out State>,
)

data class VexBehaviorV2AsyncResult<
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
>(
    /**
     * Null means this result produces no additional State admission.
     *
     * Result scopes deliberately do not require a redundant stay(); stay/admit remains mandatory
     * only for the immediate on(...) decision.
     */
    val admittedStateKind: KClass<out State>?,
)

data class VexBehaviorV2AsyncResults<
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
>(
    val success:
        VexBehaviorV2AsyncResult<
            Family,
            State
        >,
    val failure:
        VexBehaviorV2AsyncResult<
            Family,
            State
        >,
)

data class VexBehaviorV2EffectPlan<
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
    Effect : VexBehaviorFamily.Effect<Family>,
> internal constructor(
    val effect: Effect,
    val executionMode: VexBehaviorV2ExecutionMode?,
    val results:
        VexBehaviorV2AsyncResults<
            Family,
            State
        >?,
) {
    val isAsync: Boolean
        get() =
            executionMode !=
                null
}

/**
 * One V2 cause/effect bond inside one exact Behavior family.
 *
 * V1 keeps one immediate admitted State kind. V2 keeps that immediate decision explicit while also
 * allowing the Effect plan to describe a later result admission.
 */
class VexBehaviorV2Relationship<
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
    Act : VexBehaviorFamily.Act<Family>,
    Effect : VexBehaviorFamily.Effect<Family>,
> internal constructor(
    val from: KClass<out State>,
    val act: Act,
    val effectPlan:
        VexBehaviorV2EffectPlan<
            Family,
            State,
            Effect
        >,
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

/**
 * Read-only V2 semantic consequence of one Act from one current State.
 */
data class VexBehaviorV2Resolution<
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
    Effect : VexBehaviorFamily.Effect<Family>,
>(
    val effect: Effect,
    val immediateDisposition:
        VexBehaviorV2StateDisposition<
            Family,
            State
        >,
    val executionMode: VexBehaviorV2ExecutionMode?,
    val results:
        VexBehaviorV2AsyncResults<
            Family,
            State
        >?,
)

// [VXG RealForever]
