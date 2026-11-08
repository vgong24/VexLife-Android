package com.vextreme.vexlife.android.behavior

/**
 * Rooted contract for one sustained realization that remains aware of rightful CURRENT truth.
 *
 * WHAT THIS IS
 * VexService binds one exact Service coordinate to the exact current vocabulary that sustained
 * realization participates in.
 *
 * [Current] is borrowed/read-only truth from its rightful owner. It may be a Family State, Relay
 * OutputState, Session State, Partner-derived projection, or another already-owned current value.
 * Implementing VexService does not transfer ownership of that truth and does not authorize a second
 * mutable Service State.
 *
 * WHAT IT RELATES TO
 *
 *     rightful CURRENT truth
 *          ↕
 *     sustained realization
 *          ↕
 *     platform/provider reality
 *
 * A concrete Service may use VexExecution for asynchronous work, a Relay for compound current
 * meaning, or platform-specific lifecycle APIs for realization. Those remain their own rightful
 * contracts rather than becoming mandatory VexService children.
 *
 * WHY THIS ROOT EXISTS
 * Without one recursive Service coordinate, the relationship between an exact sustained realization
 * and the exact current truth it participates in must be reconstructed from Android class names,
 * repositories, coroutine fields, or convention. The recursive binding gives that relationship the
 * same compiler-visible kinship discipline used by VexFamily and VexPartner without cloning their
 * semantics.
 *
 * RESPONSIBILITY FENCE
 * VexService does not own Family State, Relay relationship truth, Partner identity, provider truth,
 * dependency construction, VexExecution Job custody, lifecycle history, generic Entry/Admission, or
 * generic Retention policy. Those behaviors remain concrete until repeated consumers earn a smaller
 * truthful shared contract.
 */
interface VexService<
    Service,
    Current,
> where
    Service : VexService<
        Service,
        Current
    > {

    /**
     * Exact CURRENT truth this live Service is presently aware of.
     *
     * This property must read/reuse the rightful existing owner. It must not publish or maintain a
     * second Service-owned copy merely to satisfy this contract.
     */
    val current:
        Current
}

// [VXG RealForever][VEX-SERVICE][STANDARD-TEMPLATE]
