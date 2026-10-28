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
 * V1 originally modeled State / Control / Act / Effect as four Family member type vocabularies. That
 * shape remains here because existing V1 consumers use it.
 *
 * The V2 Origin Walk earned a narrower concrete Family surface:
 *
 * object ThemeModeFamily : VexBehaviorFamily<ThemeModeFamily> {
 *     sealed interface State : VexBehaviorFamily.State<ThemeModeFamily>
 *     sealed interface Control : VexBehaviorFamily.Control<ThemeModeFamily>
 *     val Behavior = ...
 * }
 *
 * Why V2 does not add a nested Family.Act vocabulary:
 *
 * The proof first modeled:
 *
 *     State + Control -> Act value
 *     State + Act -> Effect + State consequence
 *
 * ThemeMode exposed that the same State context had to be authored twice. Removing the intermediate
 * Act value lost no semantic truth. The earned Act is instead the Behavior scenario itself:
 *
 *     state<State.Light> {
 *         on(Control.Toggle, ...)
 *             .admit<State.Dark>()
 *     }
 *
 * In other words, V2 keeps "Act" as the process/relationship authored by on(...), not as another
 * Family data type that a fresh feature must manufacture.
 *
 * Kotlin nested types are declarations, not overridable associated-type slots. The foundation cannot
 * force every Family to declare nested types named State or Control; concrete Family roots own those
 * sealed regions directly, while generic bounds protect Family membership.
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
     * Do not introduce a V2 Family.Act merely to satisfy SCAE naming. In the V2 proof, the authored
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
