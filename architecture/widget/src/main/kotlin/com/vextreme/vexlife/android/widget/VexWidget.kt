package com.vextreme.vexlife.android.widget

/**
 * Root coordinate for one remotely hosted interactive Vex surface.
 *
 * WHAT THIS IS
 * Widget owns the remote-surface responsibility: bounded durable presentation custody, render-facing
 * projection and stable interaction doorway identity over truth owned elsewhere.
 *
 * WHAT IT RELATES TO
 *
 *     VexWidget
 *     -> Relay
 *     -> SurfaceState
 *     -> Projection
 *     -> Action
 *        -> Ref
 *
 * A concrete Widget may use a Relay when it must converge bounded CURRENT from independent rightful
 * owners. Relay remains its own relationship mechanism; the Widget root only makes that relationship
 * compiler-visible as part of the remote surface.
 *
 * RESPONSIBILITY FENCE
 * Widget does not own Family semantic State, Partner/provider truth, Service lifetime, VexExecution
 * Job custody or generic Relay semantics. SurfaceState is remote presentation custody, not a second
 * product truth.
 */
interface VexWidget<
    Widget,
> where
    Widget : VexWidget<Widget> {
    /** Durable remote-surface snapshot, not product/domain State. */
    interface SurfaceState

    /** Already-derived render-facing facts for one remote surface. */
    interface Projection

    /** Relationship child used when this Widget converges independently-owned CURRENT truths. */
    interface Relay<
        Widget,
    > where
        Widget : VexWidget<Widget> {
        /** Bounded current meaning emitted by one concrete Widget Relay. */
        interface Output
    }

    /** Stable interaction doorway owned by one Widget surface. */
    interface Action {
        /** Semantic identity of the doorway; not a Family Act or Effect. */
        interface Ref {
            val value:
                String
        }
    }
}

// [VXG RealForever][VEX-WIDGET][ROOT]
