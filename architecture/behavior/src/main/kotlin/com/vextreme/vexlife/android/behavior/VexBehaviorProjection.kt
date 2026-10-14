package com.vextreme.vexlife.android.behavior

/**
 * Current semantic projection of one finite Control.
 *
 * [act] is null when the Control has no meaning from the current State. Presentation decides whether
 * that appears disabled, hidden, replaced, animated, or otherwise rendered.
 */
data class VexBehaviorControlProjection<
    Control : Any,
    Act : Any,
>(
    val control: Control,
    val act: Act?,
) {
    val available: Boolean
        get() =
            act != null
}

/**
 * Immutable UI/convergence-facing result of one Behavior at one observed State.
 *
 * This value contains no Effect implementation and performs no mutation. It can therefore be shared
 * with one Composable, many Composables, tests, previews, or a higher convergence owner without
 * transferring Behavior or provider ownership.
 */
data class VexBehaviorProjection<
    State : Any,
    Control,
    Act : Any,
>(
    val state: State,
    val controls:
        Map<
            Control,
            VexBehaviorControlProjection<
                Control,
                Act
            >
        >,
) where Control : Enum<Control> {
    /**
     * Exact projection for one finite Control.
     *
     * Construction of the owning Control blueprint guarantees every enum member is represented.
     */
    operator fun get(
        control: Control,
    ): VexBehaviorControlProjection<Control, Act> =
        controls.getValue(control)
}
