package com.vextreme.vexlife.android.behavior

/**
 * Compile-time kinship contract for one Behavior vocabulary.
 *
 * V1 production consumers still use State / Control / Act / Effect as Family-bound vocabularies.
 * That compatibility surface remains here.
 *
 * The V2 Origin Walk now uses [VexBehaviorV2Family] for the live Family shape:
 *
 *     Family
 *       State
 *       Control
 *       DisplayState
 *       current
 *       acts
 *       activate(Control)
 *
 * V2 does not add another nested Family.Act value vocabulary. The authored State + Control
 * relationship is the Act itself.
 *
 * Kotlin nested types are declarations, not overridable associated-type slots. Concrete Families own
 * their nested State / Control / DisplayState regions directly; generic bounds protect Family
 * membership and [VexBehaviorV2Family] supplies the common live/currentness contract.
 */
interface VexBehaviorFamily<
    Self : VexBehaviorFamily<Self>,
> {
    /** Current truth belonging to one exact Behavior family. */
    interface State<
        Family : VexBehaviorFamily<Family>,
    >

    /** Semantic request/input belonging to one exact Behavior family. */
    interface Control<
        Family : VexBehaviorFamily<Family>,
    >

    /**
     * V1 compatibility marker for the older "Act as Family vocabulary" model.
     *
     * Do not introduce a V2 Family.Act merely to satisfy SCAE naming. In V2, the authored
     * State + Control -> Effect/consequence relationship is the Act.
     */
    interface Act<
        Family : VexBehaviorFamily<Family>,
    >

    /** Runtime transformation relationship belonging to one exact Behavior family. */
    interface Effect<
        Family : VexBehaviorFamily<Family>,
    >
}

// [VXG RealForever]
