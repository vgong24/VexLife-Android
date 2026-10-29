package com.vextreme.vexlife.android.behavior

import kotlin.reflect.KClass

enum class VexBeginMode {
    Replaceable,
    Sequenced,
}

internal data class VexStateDisposition<
    State,
>(
    val admittedState:
        State?,
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
        VexResult<
            State
        >,
    val failure:
        VexResult<
            State
        >,
)

internal data class VexSave<
    State : Any,
> internal constructor(
    val state:
        State,
    override val results:
        VexResults<
            State
        >,
) :
    VexStoreOperation<
        State
    >

internal data class VexBegin<
    State : Any,
> internal constructor(
    val mode:
        VexBeginMode,
    val operation:
        VexOperation<
            State
        >,
)

internal class VexAct<
    State : Any,
    Control : Any,
> internal constructor(
    val from:
        KClass<out State>,
    val control:
        Control,
    val begin:
        VexBegin<
            State
        >?,
    val immediateDisposition:
        VexStateDisposition<
            State
        >,
    val conditionName:
        String?,
)

/**
 * Internal matcher behind the visible state<State> { on<Control> { ... } } grammar.
 *
 * This is implementation machinery, not a Family vocabulary noun.
 */
@PublishedApi
internal class VexActRule<
    State : Any,
    Control : Any,
> @PublishedApi internal constructor(
    val from:
        KClass<out State>,
    val controlDescription:
        String,
    val conditionName:
        String?,
    private val matchAct:
        (
            currentState: State,
            control: Control,
        ) ->
            VexAct<
                State,
                Control
            >?,
) {
    internal fun match(
        currentState:
            State,
        control:
            Control,
    ):
        VexAct<
            State,
            Control
        >? =
        matchAct(
            currentState,
            control,
        )
}

// [VXG RealForever]
