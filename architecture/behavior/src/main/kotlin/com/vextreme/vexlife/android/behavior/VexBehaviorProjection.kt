package com.vextreme.vexlife.android.behavior

/**
 * Current semantic projection of one Control from one exact Behavior family.
 */
data class VexBehaviorControlProjection<
    Family : VexBehaviorFamily<Family>,
    Control : VexBehaviorFamily.Control<Family>,
    Act : VexBehaviorFamily.Act<Family>,
>(
    val control: Control,
    val act: Act?,
) {
    val available: Boolean
        get() =
            act != null
}

/**
 * Immutable UI/convergence-facing result of one exact Behavior family.
 *
 * Family is a compile-time coordinate only. It prevents a State, Control and Act from unrelated
 * families from being assembled into one apparently valid Projection.
 */
data class VexBehaviorProjection<
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
    Control : VexBehaviorFamily.Control<Family>,
    Act : VexBehaviorFamily.Act<Family>,
>(
    val state: State,
    val controls:
        Map<
            Control,
            VexBehaviorControlProjection<
                Family,
                Control,
                Act
            >
        >,
) {
    operator fun get(
        control: Control,
    ): VexBehaviorControlProjection<
        Family,
        Control,
        Act
    > =
        controls.getValue(
            control,
        )
}
