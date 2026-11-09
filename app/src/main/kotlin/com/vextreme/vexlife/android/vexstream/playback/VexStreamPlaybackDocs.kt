package com.vextreme.vexlife.android.vexstream.playback

/**
 * # VexStream Playback
 *
 * Human question:
 *
 *     "What is Playback's current semantic truth, what may the person request, and how do outside
 *      Player truth, sustained Service realization, UI, and Widget consequences stay connected
 *      without becoming competing State owners?"
 *
 * ## One semantic owner
 *
 * [VexStreamPlaybackFamily] remains the one live Playback semantic owner:
 *
 *     CURRENT State + Control
 *         -> accepted Act
 *         -> admitted pending State when truthful
 *         -> Effect
 *
 * Provider truth does not become Playback State by being observed. The Player Partner path admits
 * outside truth first, then reconciles it through the exact current Family:
 *
 *     Media3 Player
 *         -> VexStream Player Partner Connection
 *         -> Signal / Observation
 *         -> Player Store CURRENT
 *         -> Playback admission
 *         -> Family.observe(reconciled State)
 *
 * ## Ordered admission
 *
 * [VexStreamPlaybackAdmission] is a feature-local serialization boundary, not another truth owner.
 * It keeps Family mutation plus the immediate post-admission relationship consequence in one order
 * across both provider reconciliation and Control activation.
 *
 * ## Control / realization return path
 *
 * [VexStreamPlaybackConnectionFactory] gives UI and Widget callers the same construction path:
 *
 *     Control
 *         -> PartnerVexStreamPlaybackConnection
 *         -> Playback admission
 *         -> Family.activate(Control)
 *         -> exact admitted Family CURRENT
 *         -> bounded relationship consequence
 *         -> accepted Effect
 *         -> Player Partner request realization
 *         -> Media3
 *
 * Provider acknowledgement returns independently through the Player Partner observation path above.
 * The concrete request connection remains provider-specific; the Family does not call Media3.
 *
 * ## Sustained Android realization
 *
 * VexStreamPlaybackService owns the sustained ExoPlayer / MediaSession realization while UI may be
 * absent. The Service borrows Playback Family CURRENT and hosts the live inbound Player Partner
 * Connection. Service teardown closes that Connection; it does not manufacture a second semantic
 * NoMedia path.
 *
 * ## Widget relationship
 *
 * VexStream Playback is the patient zero for the rooted VexWidget relationship:
 *
 *     Player Store bounded facts ----\
 *                                    -> Playback Widget.Relay
 *     admitted Playback Family -----/          |
 *                                               v
 *                                      Widget SurfaceState
 *                                               |
 *                                               v
 *                                      Glance Projection
 *
 * Provider-origin Widget Relay admission happens only after Store admission + Family reconciliation.
 * Control-origin admission happens only after Family.activate(...) has admitted its semantic State.
 * The Relay carries only the Widget facts it needs; full QueueItems remain Store-owned.
 *
 * Widget durable SurfaceState is remote presentation custody, not Playback State. Artwork-availability
 * backfill remains a Widget-owned presentation transform rather than synthetic product truth.
 *
 * ## UI
 *
 * [VexStreamPlaybackExperience] observes Family State and combines it with provider/read metadata
 * from [VexStreamPlaybackReadState] for display. [VexStreamPlaybackDisplayState] and [present] are
 * projections only; neither owns Playback truth.
 *
 * The UI read/display composition is still a separate consumer relationship from Widget Relay and
 * should not be silently treated as transactionally paired Store + Family CURRENT.
 *
 * ## Provider latency
 *
 * Pending Play/Pause/Restart intent is represented in the shared Family State. A stale Media3
 * callback cannot erase a newer admitted Control; [reconcileVexStreamPlaybackObservation] compares
 * provider reality with the exact Family current inside the Playback admission gate before
 * [VexStreamPlaybackFamily.observe].
 *
 * ## Sibling boundaries
 *
 * Queue, Repeat, Seek, media source/loading, Widget remote-surface custody, Player Partner outside
 * truth, and higher convergence remain separate questions. Playback does not absorb them merely
 * because they meet in one music experience.
 */
// Documentation/navigation only.
@Suppress("unused")
internal class VexStreamPlaybackDocs private constructor()

// [VXG RealForever][VEXSTREAM][PLAYBACK][DIRECT-FAMILY]
