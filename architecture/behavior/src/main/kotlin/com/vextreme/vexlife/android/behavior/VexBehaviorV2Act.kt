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
 * Family is carried by the surrounding Behavior generic scope, so target code does not repeat a
 * runtime Family argument.
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
 * Effect is begin(...); Save(...) is the operation inside that Effect.
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

/**
 * One authored Behavior Act.
 *
 * This class exists because the Origin Walk removed the earlier V2 Family.Act value hierarchy.
 * The Act that survived the removal test is the scenario itself:
 *
 *     CURRENT State + Control
 *     -> Effect
 *     -> immediate stay/admit
 *
 * [from] and [control] identify the input neighborhood. [effect] and [immediateDisposition] are the
 * consequence authored by that exact on(...) relationship.
 *
 * This proof currently matches exact Control values, which is sufficient for ThemeMode.Toggle.
 * Payload-bearing Control-shape matching (for example Click(songId)) remains a deliberate next
 * pressure rather than being hidden behind an unearned generic matcher abstraction.
 */
class VexBehaviorV2Act<
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
    Control : VexBehaviorFamily.Control<Family>,
> internal constructor(
    val from: KClass<out State>,
    val control: Control,
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
    internal fun matches(
        current: State,
        requestedControl: Control,
    ): Boolean =
        from.isInstance(
            current,
        ) &&
            control ==
                requestedControl &&
            whenCurrent(
                current,
            )
}

// [VXG RealForever]
