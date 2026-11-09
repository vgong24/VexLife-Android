package com.vextreme.vexlife.android.vexstream.playback

/**
 * # VexStream Playback
 *
 * ## Semantic owner
 *
 * [VexStreamPlaybackFamily] owns Playback State / Control / Act / Effect meaning.
 *
 *     CURRENT State + Control
 *         -> accepted Act
 *         -> optional admitted pending State
 *         -> Effect + Approach / RelationshipId
 *
 * Provider reality remains outside Family until it has crossed the Player Partner and Player Store.
 *
 * ## Outside Player -> Store -> Family
 *
 *     Media3 Player
 *         -> VexStreamPlayerPartner.Connection
 *         -> Conversion
 *         -> Signal
 *         -> Observation
 *         -> VexStreamPlayerObservationReceiver
 *         -> VexStreamPlayerStore CURRENT
 *         -> VexExecution.observe(store::playbackSignals, owner = Playback Family)
 *         -> VexStreamPlaybackFamily.observePlayer(...)
 *         -> Family.observe(reconciled State)
 *
 * The Receiver ends at the Store boundary. It does not mutate Family or fan out presentation
 * callbacks.
 *
 * Playback has a real provider-vs-Control currentness question: a stale Media3 statement must not
 * erase a newer pending Play/Pause intent. Following the accepted concrete-Family currentness
 * pattern, [VexStreamPlaybackFamily] keeps that reconciliation fence locally around
 * [VexStreamPlaybackFamily.activatePlayback] and [VexStreamPlaybackFamily.observePlayer]. There is
 * no separate admission/coordinator owner.
 *
 * ## Control -> accepted Act -> Player Partner request
 *
 *     Control
 *         -> PartnerVexStreamPlaybackConnection
 *         -> Playback Family.activatePlayback(Control)
 *         -> accepted Act
 *         -> Act.launchWith(VexExecutionRoot)
 *         -> bound VexFamily.Act.Method
 *         -> VexStreamPlaybackEffectConnection
 *         -> Media3 provider looper
 *
 * The accepted Act's authored Approach, RelationshipId and live Family execution owner survive into
 * VexExecution. Playback's Latest relationship therefore governs actual overlapping request Jobs.
 *
 * ## Inbound execution
 *
 * [Media3VexStreamPlayerConnection] uses the existing direct VexExecution entry:
 *
 *     VexExecution.beginQueued(
 *         executionRoot,
 *         owner = exact live Player Connection,
 *         relationshipId = inbound ordering relationship,
 *         ...
 *     )
 *
 * It does not introduce a second execution-owner hierarchy. Opened / Observed / Closed remain queued
 * in arrival order for that live Connection.
 *
 * ## Widget
 *
 * [com.vextreme.vexlife.android.widget.vexstream.VexStreamPlaybackWidgetRelay] uses the established
 * Compound Relay dependency mechanism to observe Player Store CURRENT and Playback Family CURRENT.
 * It owns only its bounded remote-surface relationship current; it does not mutate either owner or
 * invent transaction/origin admission vocabulary.
 *
 *     Player Store CURRENT ----\
 *                              -> Playback Widget Relay -> SurfaceState -> Glance
 *     Playback Family CURRENT -/
 *
 * Full QueueItems remain Player Store / Queue-owned; the Widget input carries only the metadata and
 * navigation availability required by the remote surface.
 *
 * ## Compose
 *
 * [VexStreamPlaybackExperience] observes Playback Family semantic State while
 * [VexStreamPlaybackReadState] observes retained Player Store read truth and the concrete live
 * Progress connection. Compose does not create a second Playback relationship-current owner merely
 * because it reads both sources.
 *
 * If a demonstrated consumer failure later proves that UI requires a distinct cross-owner
 * relationship current, that requirement must earn a Relay separately.
 *
 * ## Sustained realization
 *
 * VexStreamPlaybackService owns sustained ExoPlayer / MediaSession realization while borrowing
 * Playback Family CURRENT. Service lifetime is not Playback truth and does not manufacture a second
 * State owner.
 *
 * ## Non-collapse
 *
 *     Partner != Store
 *     Store != Family
 *     Family != Execution
 *     Family != Relay
 *     Relay != source truth
 *     SurfaceState != product State
 *     Service != semantic owner
 */
// Documentation/navigation only.
@Suppress("unused")
internal class VexStreamPlaybackDocs private constructor()

// [VXG RealForever][VEXSTREAM][PLAYBACK][DIRECT-FAMILY]
