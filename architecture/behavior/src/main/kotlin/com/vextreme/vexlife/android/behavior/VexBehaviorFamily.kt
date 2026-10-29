package com.vextreme.vexlife.android.behavior

/**
 * Compile-time kinship contract for one Behavior vocabulary.
 *
 * The recursive bound forces every concrete family to identify itself:
 *
 * sealed interface PlaybackFamily : VexBehaviorFamily<PlaybackFamily>
 *
 * Generic architecture cannot collapse back to a raw VexBehaviorFamily without losing the bound.
 */
interface VexBehaviorFamily<
    Self : VexBehaviorFamily<Self>,
> {
    /** Current truth belonging to one exact Behavior family. */
    interface State<
        Family : VexBehaviorFamily<Family>,
    >

    /** Human-facing affordance belonging to one exact Behavior family. */
    interface Control<
        Family : VexBehaviorFamily<Family>,
    >

    /** Human/product verb belonging to one exact Behavior family. */
    interface Act<
        Family : VexBehaviorFamily<Family>,
    >

    /** Runtime transformation verb belonging to one exact Behavior family. */
    interface Effect<
        Family : VexBehaviorFamily<Family>,
    >
}

// [VXG RealForever]
