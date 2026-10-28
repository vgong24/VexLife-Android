package com.vextreme.vexlife.android.behavior

/**
 * Compile-time kinship contract for one Behavior vocabulary.
 *
 * The recursive bound forces every concrete family to identify itself:
 *
 * sealed interface PlaybackFamily : VexBehaviorFamily<PlaybackFamily>
 *
 * Generic architecture cannot collapse back to a raw VexBehaviorFamily without losing the bound.
 *
 * The foundation role markers intentionally remain open. A concrete Family may then contain its own
 * sealed role regions:
 *
 * object ThemeModeFamily : VexBehaviorFamily<ThemeModeFamily> {
 *     sealed interface State : VexBehaviorFamily.State<ThemeModeFamily>
 *     sealed interface Control : VexBehaviorFamily.Control<ThemeModeFamily>
 *     sealed interface Act : VexBehaviorFamily.Act<ThemeModeFamily>
 * }
 *
 * Kotlin nested types are declarations, not overridable associated-type slots. Therefore this
 * foundation cannot force an implementing Family to "override type State". The concrete Family root
 * owns that sealed region directly, while generic builders infer the role types from actual use.
 */
interface VexBehaviorFamily<
    Self : VexBehaviorFamily<Self>,
> {
    /** Current truth belonging to one exact Behavior family. */
    interface State<
        Family : VexBehaviorFamily<Family>,
    >

    /** Human-facing affordance or request belonging to one exact Behavior family. */
    interface Control<
        Family : VexBehaviorFamily<Family>,
    >

    /** Human/product verb belonging to one exact Behavior family. */
    interface Act<
        Family : VexBehaviorFamily<Family>,
    >

    /** Runtime transformation relationship belonging to one exact Behavior family. */
    interface Effect<
        Family : VexBehaviorFamily<Family>,
    >
}

// [VXG RealForever]
