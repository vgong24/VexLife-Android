
package com.vextreme.vexlife.android.vexstream.playback

/**
 * # VexStream Playback
 *
 * Search this symbol when asking:
 *
 * "How does VexStream Playback fit together?"
 *
 * This class is documentation/navigation only. It owns no playback state, provider, lifecycle, or
 * semantic behavior.
 *
 * ## What can Playback truthfully be?
 *
 * Start at [VexStreamPlaybackState].
 *
 * Compiler kinship is declared by [VexStreamPlaybackFamily].
 *
 * ## What can the human interact with?
 *
 * See [VexStreamPlaybackControl].
 *
 * A Control is stable affordance identity. Its current meaning is resolved from State.
 *
 * ## What does a Control mean right now?
 *
 * See [VexStreamPlaybackAct].
 *
 * One PlayPause Control may currently mean Play or Pause.
 *
 * ## What should runtime attempt?
 *
 * See [VexStreamPlaybackEffect].
 *
 * Effect is attempted runtime meaning, not provider success.
 *
 * ## Where are the relationships authored?
 *
 * See [vexStreamPlaybackBehavior].
 *
 * Read it as:
 *
 * State + Act -> Effect -> admitted State kind.
 *
 * ## Who talks to Media3?
 *
 * See [Media3VexStreamPlaybackConnection].
 *
 * It implements the reusable
 * [com.vextreme.vexlife.android.behavior.VexPortFlow] boundary while keeping an ordinary concrete
 * Connection name.
 *
 * The generic two-way contract is [com.vextreme.vexlife.android.behavior.VexPort].
 *
 * ## Who keeps the Connection alive on Android?
 *
 * See [VexStreamPlaybackConnectionViewModel].
 *
 * The ViewModel owns Android configuration lifetime only. MediaSession/Player remain playback truth
 * owners.
 *
 * ## How is provider latency reconciled?
 *
 * See [VexStreamPlaybackPending].
 *
 * Local admitted intent may be published before Media3 acknowledgement without pretending provider
 * convergence already happened.
 *
 * ## How does semantic meaning become UI meaning?
 *
 * See [VexStreamPlaybackControlPresentation] and [present].
 *
 * Presentation maps projected semantic meaning into icon/enabled treatment. It is not another State
 * owner.
 *
 * ## What types should normal callers use?
 *
 * Prefer:
 *
 * - [VexStreamPlaybackConnection]
 * - [VexStreamPlaybackConnectionFlow]
 * - [VexStreamPlaybackProjection]
 *
 * These preserve the full Family-bound generic contract underneath without making ordinary Compose
 * code read the complete generic equation.
 *
 * ## What is intentionally still outside this family?
 *
 * Queue, seek, shuffle, repeat, media loading/source selection, generic UIElement binding, and
 * higher convergence are not silently owned by Playback merely because they are nearby.
 *
 * If one of those becomes an independently meaningful behavioral question, it may earn a sibling
 * package/family rather than making Playback grow forever.
 */
@Suppress("unused")
internal class VexStreamPlaybackDocs private constructor()

// [VXG RealForever]
