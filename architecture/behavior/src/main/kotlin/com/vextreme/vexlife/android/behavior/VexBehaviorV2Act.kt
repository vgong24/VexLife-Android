package com.vextreme.vexlife.android.behavior

import kotlin.reflect.KClass

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
 * Family State Store Save(State) Operation.
 *
 * Save remains common-language intention even though begin(...) owns a generic Operation slot.
 */
data class VexBehaviorV2Save<
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
> internal constructor(
    val state: State,
    override val results:
        VexBehaviorV2Results<
            Family,
            State
        >,
) :
    VexStoreOperation<
        Family,
        State
    >

/**
 * begin(mode) is the execution phase. [operation] is the work performed inside that phase.
 */
data class VexBehaviorV2Begin<
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
> internal constructor(
    val mode: VexBehaviorV2BeginMode,
    val operation:
        VexOperation<
            Family,
            State
        >,
) :
    VexBehaviorFamily.Effect<
        Family
    >

/**
 * One resolved Act for the exact CURRENT State value + incoming Control value that activated it.
 *
 * Static singleton Controls may reuse one pre-authored instance. Runtime payload Controls materialize
 * a new Act from the actual State/Control values at resolution time.
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
)

/**
 * Authored relationship capable of resolving/materializing one Act from runtime values.
 *
 * Keeping this internal preserves the public language as State + Control -> Act rather than exposing
 * another Family vocabulary noun merely to implement dynamic matching.
 */
@PublishedApi
internal class VexBehaviorV2ActRule<
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
    Control : VexBehaviorFamily.Control<Family>,
> @PublishedApi internal constructor(
    val from: KClass<out State>,
    val controlDescription: String,
    val conditionName: String?,
    val staticAct:
        VexBehaviorV2Act<
            Family,
            State,
            Control
        >?,
    private val resolveAct:
        (
            State,
            Control,
        ) ->
            VexBehaviorV2Act<
                Family,
                State,
                Control
            >?,
) {
    internal fun resolve(
        current: State,
        control: Control,
    ):
        VexBehaviorV2Act<
            Family,
            State,
            Control
        >? =
        resolveAct(
            current,
            control,
        )
}

// [VXG RealForever]
