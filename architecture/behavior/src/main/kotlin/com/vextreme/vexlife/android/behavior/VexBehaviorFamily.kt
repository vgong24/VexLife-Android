package com.vextreme.vexlife.android.behavior

/**
 * Compile-time kinship contract for one Behavior vocabulary.
 *
 * V1 production consumers still use State / Control / Act / Effect as Family-bound vocabularies.
 * That compatibility surface remains here.
 *
 * The V2 Origin Walk has now separated what is stable from what is still a runtime pressure.
 *
 * Stable semantic/authoring direction:
 *
 *     Family
 *       State
 *       Control
 *       DisplayState role
 *       acts
 *       activate(Control)
 *
 *     Act
 *       CURRENT State + Control
 *       -> immediate stay/admit
 *       -> optional begin(mode) execution episode
 *
 * V2 does not add another nested Family.Act value vocabulary. The authored State + Control
 * relationship is the Act itself, and admission carries the concrete State value.
 *
 * The current live Family implementation also carries current State, optional Store access and
 * execution mechanics. Those runtime ownership details remain patient-zero candidates until
 * externally observed truth, payload-bearing Controls, observable UI currentness and multiple Family
 * instances have completed their own Origin pressure.
 *
 * Kotlin nested types are declarations, not overridable associated-type slots. Concrete Families own
 * their nested State / Control / DisplayState / Store regions directly; generic bounds protect Family
 * membership without implying that every Family owns every external truth it consumes.
 */
interface VexBehaviorFamily<
    Self : VexBehaviorFamily<Self>,
> {
    interface State<
        Family : VexBehaviorFamily<Family>,
    >

    interface Control<
        Family : VexBehaviorFamily<Family>,
    >

    /**
     * V1 compatibility marker for the older "Act as Family vocabulary" model.
     *
     * Do not introduce a V2 Family.Act merely to satisfy SCAE naming.
     */
    interface Act<
        Family : VexBehaviorFamily<Family>,
    >

    interface Effect<
        Family : VexBehaviorFamily<Family>,
    >
}

// [VXG RealForever]
