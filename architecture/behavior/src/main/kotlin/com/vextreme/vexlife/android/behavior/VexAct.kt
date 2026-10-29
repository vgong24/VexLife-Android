package com.vextreme.vexlife.android.behavior

enum class VexBeginMode {
    Replaceable,
    Sequenced,
}

/**
 * One authored State + Control outcome.
 *
 * This is the direct return value of Family.act(currentState, control). It carries only the semantic
 * State admission and optional execution consequence that Family.activate(...) needs to apply.
 */
class VexAct<
    State : Any,
> internal constructor(
    internal val admittedState:
        State?,
    internal val begin:
        VexBegin<State>?,
)

internal data class VexResult<
    State,
>(
    val admittedState:
        State?,
)

internal data class VexResults<
    State,
>(
    val success:
        VexResult<State>,
    val failure:
        VexResult<State>,
)

internal data class VexSave<
    State : Any,
> internal constructor(
    val state:
        State,
    override val results:
        VexResults<State>,
) :
    VexStoreOperation<State>

/**
 * Optional execution consequence authored by an Act.
 */
class VexBegin<
    State : Any,
> internal constructor(
    val mode:
        VexBeginMode,
    internal val operation:
        VexOperation<State>,
)

// [VXG RealForever]
