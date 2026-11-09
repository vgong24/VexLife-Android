package com.vextreme.vexlife.android.vexstream.playback

/**
 * # VexStream Playback
 *
 * Human question:
 *
 *     "What is Playback's current semantic truth, how does outside Player truth enter it, how does a
 *      Control become real work, and how do UI / Widget consumers receive coherent relationship
 *      current without becoming competing State owners?"
 *
 * ## One semantic owner
 *
 * [VexStreamPlaybackFamily] remains the one live Playback semantic owner:
 *
 *     CURRENT State + Control
 *         -> accepted Act
 *         -> admitted pending State when truthful
 *         -> Effect + authored execution Approach / RelationshipId
 *
 * Provider truth does not become Playback State by being observed.
 *
 * ## Outside Player -> Family
 *
 *     Media3 Player
 *         -> VexStreamPlayerPartner.Connection
 *         -> Conversion
 *         -> Signal / Observation
 *         -> VexStreamPlayerStore CURRENT
 *         -> VexStreamPlaybackAdmission
 *         -> Family.observe(reconciled State)
 *         -> exact admitted Playback Family CURRENT
 *
 * [Media3VexStreamPlayerConnection] is itself a [com.vextreme.vexlife.android.execution.VexExecution]
 * owner over the process execution root. Its queued Opened / Observed / Closed deliveries therefore
 * have Connection-lifetime custody instead of entering the root's direct-owner retention map.
 * close() removes the provider listener and queues terminal Closed behind earlier deliveries rather
 * than cancelling the lane and losing terminal ordering.
 *
 * ## Ordered Family admission
 *
 * [VexStreamPlaybackAdmission] is a feature-local serialization boundary, not another truth owner.
 * It keeps Family mutation plus immediate downstream relationship admission in one order across
 * provider reconciliation and Control activation.
 *
 * ## Family -> Player realization
 *
 * [VexStreamPlaybackConnectionFactory] gives UI and Widget callers the same Control path:
 *
 *     Control
 *         -> PartnerVexStreamPlaybackConnection
 *         -> Playback admission
 *         -> Family.activate(Control)
 *         -> exact admitted Family CURRENT
 *         -> consumer relationship admission(s)
 *         -> accepted Act.launchWith(VexExecutionRoot)
 *         -> Family-owned Method
 *         -> Player Partner request realization
 *         -> Media3 provider looper
 *
 * The accepted Act's Approach, RelationshipId and live Family execution owner are consumed rather
 * than discarded. Playback uses Latest overlap for Play/Pause/Restart, so a newer accepted request
 * cancels the older active relationship Job before the replacement realization proceeds.
 *
 * [Media3VexStreamPlayerControlConnection] re-enters the MediaController application looper inside
 * its suspend realization boundary. A short-lived Widget controller waits for the returned Job before
 * release; configuration-lived UI controllers may ignore the Job while the same execution custody
 * remains active.
 *
 * ## Consumer relationship currents
 *
 * Provider and Control origins feed two separate bounded consumers from the same exact admissions:
 *
 *     Player Store CURRENT + admitted Family CURRENT
 *          |                         |
 *          +-> VexStreamPlaybackWidgetRelay
 *          |      -> Widget SurfaceState -> Glance
 *          |
 *          +-> VexStreamPlaybackExperienceRelay
 *                 -> bounded UI read facts + Playback Projection
 *                 -> Compose Playback Experience
 *
 * Neither Relay samples Store and Family independently. ProviderRoundTrip is admitted only after
 * Store admission + Family reconciliation. FamilyCurrent is admitted only after Family.activate(...)
 * has admitted the semantic State.
 *
 * Widget Relay carries only remote-surface facts. Playback Experience Relay carries only UI playback
 * metadata, queue count/current index and the semantic Playback Projection. Full QueueItems remain
 * Player Store / Queue Experience-owned.
 *
 * ## Live progress
 *
 * Position / duration / buffered progress advances without a Player Partner occurrence, so it is not
 * retained in either relationship Relay. Compose layers the concrete Player Progress connection over
 * the coherent Playback Experience Relay current.
 *
 * ## Sustained Android realization
 *
 * VexStreamPlaybackService owns the sustained ExoPlayer / MediaSession realization while UI may be
 * absent. The Service borrows Playback Family CURRENT and hosts the live inbound Player Partner
 * Connection. Service teardown closes that Connection; it does not manufacture a second semantic
 * NoMedia path.
 *
 * ## Non-collapse
 *
 *     Partner != Store
 *     Store != Family
 *     Admission != State owner
 *     Family != Relay
 *     Widget Relay != Experience Relay
 *     SurfaceState != product State
 *     Service != semantic owner
 *     VexExecution != semantic owner
 *
 * Queue, Repeat, Seek, media source/loading and other consumer relationships remain separate
 * questions even though they meet on one music surface.
 */
// Documentation/navigation only.
@Suppress("unused")
internal class VexStreamPlaybackDocs private constructor()

// [VXG RealForever][VEXSTREAM][PLAYBACK][DIRECT-FAMILY]
