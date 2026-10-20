package com.vextreme.vexlife.android.behavior

import kotlinx.coroutines.flow.StateFlow

/**
 * Reusable two-way semantic port for one exact Behavior family.
 *
 * The F-bound makes the family coordinate concrete. The role bounds prevent State / Control / Act
 * positions from being swapped or supplied from another family.
 *
 * This two-way Port is intentionally invariant. activate() consumes Control, so using out Control
 * plus @UnsafeVariance would disable the compiler protection this contract exists to provide.
 *
 * Scope of currentness:
 * - activate(Control) re-resolves semantic meaning against current State at activation time;
 * - VexPort does NOT admit or reject later asynchronous provider results;
 * - request-generation, revision, latest-request, Relay, or other stale-completion gates remain the
 *   responsibility of the concrete owner/Connection that performs async work.
 *
 * Concurrency is also intentionally not prescribed here. A concrete Connection must preserve the
 * serialization / locking / dispatcher guarantees required by its provider or domain owner.
 */
interface VexPort<
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
    Control : VexBehaviorFamily.Control<Family>,
    Act : VexBehaviorFamily.Act<Family>,
> {
    val currentProjection:
        VexBehaviorProjection<
            Family,
            State,
            Control,
            Act
        >

    fun activate(
        control: Control,
    )
}

/**
 * Live current-value form of [VexPort].
 */
interface VexPortFlow<
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
    Control : VexBehaviorFamily.Control<Family>,
    Act : VexBehaviorFamily.Act<Family>,
> :
    VexPort<
        Family,
        State,
        Control,
        Act
    > {
    val projections:
        StateFlow<
            VexBehaviorProjection<
                Family,
                State,
                Control,
                Act
            >
        >

    override val currentProjection:
        VexBehaviorProjection<
            Family,
            State,
            Control,
            Act
        >
        get() =
            projections.value
}
