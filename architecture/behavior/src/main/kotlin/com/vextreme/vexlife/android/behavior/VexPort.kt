package com.vextreme.vexlife.android.behavior

import kotlinx.coroutines.flow.StateFlow

/**
 * Reusable two-way semantic port for one Behavior.
 *
 * Read side:
 *   [currentProjection] tells a consumer what the Behavior means now.
 *
 * Write side:
 *   [activate] submits one finite Control identity. The implementation must resolve that Control
 *   against its CURRENT State before choosing Act / Effect; callers never send a stale rendered Act.
 *
 * A Port deliberately does not expose Effect or provider APIs.
 */
interface VexPort<
    State : Any,
    Control,
    Act : Any,
> where Control : Enum<Control> {
    /** Current immutable semantic result. */
    val currentProjection:
        VexBehaviorProjection<
            State,
            Control,
            Act
        >

    /**
     * Activate one Control against current truth.
     *
     * Implementations own re-resolution, Effect selection, provider realization, and subsequent
     * State observation.
     */
    fun activate(
        control: Control,
    )
}

/**
 * Live current-value form of [VexPort].
 *
 * StateFlow is appropriate here because a PortFlow publishes current semantic truth, not a
 * fire-and-forget event stream. Consumers can always read [currentProjection] through
 * [projections.value].
 *
 * A local feature that does not need observation over time can implement [VexPort] directly or pass
 * one immutable Projection + Control callback without using this stronger contract.
 */
interface VexPortFlow<
    State : Any,
    Control,
    Act : Any,
> :
    VexPort<
        State,
        Control,
        Act
    >
    where Control : Enum<Control> {
    val projections:
        StateFlow<
            VexBehaviorProjection<
                State,
                Control,
                Act
            >
        >

    override val currentProjection:
        VexBehaviorProjection<
            State,
            Control,
            Act
        >
        get() =
            projections.value
}
