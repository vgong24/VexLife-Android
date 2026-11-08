package com.vextreme.vexlife.android.vexstream.playback

/**
 * # VexStream Playback
 *
 * Human question:
 *
 *     "What is Playback's current semantic truth, what may the person request, and how does Media3
 *      realization stay connected when UI, Service, Widget, or Relay are the current consumer?"
 *
 * ## One semantic owner
 *
 * [VexStreamPlaybackFamily] is the one live Playback semantic owner:
 *
 *     CURRENT State + Control
 *         -> accepted Act
 *         -> admitted pending State when truthful
 *         -> Effect
 *
 *     Media3/provider observation
 *         -> reconciled Playback State
 *         -> Family.observe(State)
 *
 * Its StateFlow is process-shareable. Foreground UI, background Service, Widget projection, and
 * process-lived convergence therefore do not construct separate Playback semantic State.
 *
 * ## Android realization
 *
 * [Media3VexStreamPlaybackConnection] receives the same Family and one MediaController. It re-reads
 * provider truth, activates the Family Control, realizes the accepted Effect, and reconciles provider
 * truth back into that same Family.
 *
 * [VexStreamPlaybackConnectionViewModel] owns only the UI MediaController connection across Android
 * configuration recreation. It resolves the Family from the process composition root rather than
 * creating another Family.
 *
 * VexStreamPlaybackService owns the sustained Media3 Player/MediaSession realization while UI may be
 * absent. Its provider observer reconciles Player events into the same process Family.
 *
 * ## UI
 *
 * [VexStreamPlaybackExperience] observes Family State and combines it with provider read metadata
 * from [VexStreamPlaybackReadState] for display. [VexStreamPlaybackDisplayState] and [present] are
 * projections only; neither owns Playback truth.
 *
 * ## Provider latency
 *
 * Pending Play/Pause/Restart intent is represented in the shared Family State. A stale Media3
 * callback cannot erase a newer admitted Control; [reconcileVexStreamPlaybackObservation] compares
 * provider reality with the exact current Family State before [VexStreamPlaybackFamily.observe].
 *
 * ## Sibling boundaries
 *
 * Queue, Repeat, Seek, media source/loading, and higher convergence remain separate questions.
 * Playback does not absorb them merely because they share Media3 or the player surface.
 */
// Documentation/navigation only.
@Suppress("unused")
internal class VexStreamPlaybackDocs private constructor()

// [VXG RealForever][VEXSTREAM][PLAYBACK][DIRECT-FAMILY]
