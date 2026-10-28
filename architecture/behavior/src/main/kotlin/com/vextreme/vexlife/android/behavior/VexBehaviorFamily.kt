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

    /**
     * Canonical one-time member map for a concrete Family.
     *
     * The role types still live in their ordinary feature-owned source files. [Members] only lets
     * Kotlin recover those already-bound types from the Family value so authoring sites do not have
     * to repeat <Family, State, Control, Act, Effect> on every generic call.
     *
     * Preferred source shape keeps the Family as the visible namespace/root:
     *
     * object ThemeModeFamily : VexBehaviorFamily<ThemeModeFamily>, Members<...> {
     *     sealed interface State { ... }
     *     enum class Control { ... }
     *     sealed interface Act { ... }
     * }
     *
     * This gives self-identifying paths such as ThemeModeFamily.State.Dark and
     * ThemeModeFamily.Control.Toggle without a second registration catalog.
     */
    interface Members<
        Family,
        State,
        Control,
        Act,
        Effect,
    >
        where Family : VexBehaviorFamily<Family>,
              State : VexBehaviorFamily.State<Family>,
              Control : VexBehaviorFamily.Control<Family>,
              Act : VexBehaviorFamily.Act<Family>,
              Effect : VexBehaviorFamily.Effect<Family> {
        /** Complete Control vocabulary for the Family when a consumer needs exhaustive binding. */
        val controls: Set<Control>
    }
}

// [VXG RealForever]
