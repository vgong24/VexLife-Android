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

/**
 * Immediate semantic State consequence of one Act.
 *
 * Stay carries no destination value because the current State value remains authoritative.
 * Admit carries the concrete next State value. A class token alone is intentionally insufficient:
 * data-bearing State flavor must not be reconstructed by a later runtime mapping.
 */
data class VexBehaviorV2StateDisposition<
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
>(
    val kind: VexBehaviorV2StateDispositionKind,
    val admittedState: State?,
) {
    init {
        require(
            (kind == VexBehaviorV2StateDispositionKind.Admit) ==
                (admittedState != null),
        ) {
            "Admit requires a concrete State value; Stay must not carry one"
        }
    }
}

/**
 * One Success or Failure consequence from Save(...).results { ... }.
 *
 * Null means this result produces no later State admission. A non-null consequence is the concrete
 * semantic State value admitted later; this V2 grammar does not pretend to solve result-payload
 * mapping that has not yet been earned by a real consumer.
 */
data class VexBehaviorV2Result<
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
>(
    val admittedState: State?,
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
 *     -> optional Effect
 *     -> immediate stay/admit
 *
 * [from] and [control] identify the input neighborhood. [effect] is present only when this Act asks
 * reality/runtime to do something beyond semantic State admission. Null means no Effect was earned;
 * it is not an implicit no-op Effect. [immediateDisposition] carries the immediate State consequence
 * authored by this exact on(...) relationship.
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
        >?,
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
