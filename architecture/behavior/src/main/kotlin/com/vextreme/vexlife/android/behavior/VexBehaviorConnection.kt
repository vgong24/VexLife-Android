package com.vextreme.vexlife.android.behavior

import kotlinx.coroutines.flow.StateFlow

/**
 * Reusable two-way semantic connection to one Behavior.
 *
 * Read side:
 *   [currentProjection] tells a consumer what the Behavior means now.
 *
 * Write side:
 *   [activate] submits one finite Control identity. The implementation must resolve that Control
 *   against its CURRENT State before choosing Act / Effect; callers never send a stale rendered Act.
 *
 * This interface deliberately does not expose Effect or provider APIs.
 */
interface VexBehaviorConnection<
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
 * Observable Behavior connection for owners that publish an always-current semantic stream.
 *
 * StateFlow is chosen here because Behavior projection is current truth, not a fire-and-forget event
 * stream. Consumers can always read [currentProjection] through [projections.value].
 *
 * This is an optional stronger contract. A local feature may use VexBehaviorConnection directly or
 * simply pass one immutable Projection + callback without implementing this interface.
 */
interface VexObservableBehaviorConnection<
    State : Any,
    Control,
    Act : Any,
> :
    VexBehaviorConnection<
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
