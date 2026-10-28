package com.vextreme.vexlife.android.behavior

import kotlin.reflect.KClass

/**
 * Exact Kotlin spelling of the Origin-Walk begin(mode = ...) relationship.
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
 * One Success or Failure consequence from Save(...).results { ... }.
 *
 * Null means this result produces no later State admission.
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
 * FamilyStore Save(State) operation proven by the ThemeMode Origin Walk.
 *
 * Family is carried by the generic bound; Kotlin therefore does not need to repeat the Family as a
 * runtime argument when the surrounding Behavior is already bound to one exact Family.
 *
 * Origin:
 *     Save(ThemeMode, ThemeMode.Dark)
 *
 * Kotlin inside ThemeModeFamily:
 *     save(ThemeMode.Dark)
 */
data class VexBehaviorV2Save<
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
> internal constructor(
    val state: State,
    val results:
        VexBehaviorV2Results<
            Family,
            State
        >,
)

/**
 * SCAE Effect scope proven by the Origin Walk.
 *
 * The Effect is begin(...); Save(...) is the operation inside that Effect.
 */
data class VexBehaviorV2Begin<
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
> internal constructor(
    val mode: VexBehaviorV2BeginMode,
    val save:
        VexBehaviorV2Save<
            Family,
            State
        >,
) :
    VexBehaviorFamily.Effect<
        Family
    >

class VexBehaviorV2Relationship<
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
    Act : VexBehaviorFamily.Act<Family>,
> internal constructor(
    val from: KClass<out State>,
    val act: Act,
    val effect:
        VexBehaviorV2Begin<
            Family,
            State
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

data class VexBehaviorV2Resolution<
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
    Act : VexBehaviorFamily.Act<Family>,
>(
    val act: Act,
    val effect:
        VexBehaviorV2Begin<
            Family,
            State
        >,
    val immediateDisposition:
        VexBehaviorV2StateDisposition<
            Family,
            State
        >,
)

// [VXG RealForever]
