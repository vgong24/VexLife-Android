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
 * Family Store Save(State) Operation.
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
