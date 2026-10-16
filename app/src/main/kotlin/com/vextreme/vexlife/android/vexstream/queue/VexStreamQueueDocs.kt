package com.vextreme.vexlife.android.vexstream.queue

/**
 * # VexStream Queue
 *
 * Search this symbol when asking:
 *
 * "What can I do with the current ordered VexStream queue?"
 *
 * Current admitted questions are:
 * - can I move to the previous item?
 * - can I move to the next item?
 * - can I clear the current queue?
 *
 * This class is documentation/navigation only. It owns no Media3 queue, player lifetime, rendered
 * queue-item identity, or Compose state.
 *
 * ## What is true?
 *
 * [VexStreamQueueState] distinguishes Empty from NonEmpty and carries only provider-derived
 * availability required by the admitted Queue controls.
 *
 * ## What can the listener request?
 *
 * [VexStreamQueueControl.Previous], [VexStreamQueueControl.Next], and
 * [VexStreamQueueControl.Clear].
 *
 * ## What does Clear mean before provider observation?
 *
 * Clear selects [VexStreamQueueEffect.ClearQueue] but the relationship deliberately uses stay().
 * Effect attempts the mutation; State does not claim Empty until Media3 actually reports no items.
 *
 * ## Who talks to Media3?
 *
 * [Media3VexStreamQueueConnection]. It re-reads current Media3 truth at activation time before
 * resolving a Control, so stale UI availability never becomes command authority.
 *
 * ## What should Compose depend on?
 *
 * [VexStreamQueueExperience]. Mini Player, Context Dock, and Queue presentation consume the same
 * Queue Projection / Controls where those controls apply.
 *
 * ## What is deliberately NOT in this family?
 *
 * Queue-row selection still carries an index request payload. Reorder carries dynamic item identity.
 * Shuffle and repeat remain separate questions. The accepted Behavior kernel explicitly holds a
 * generic request-payload abstraction, so this family does not invent one merely to absorb nearby
 * Queue mechanics.
 *
 * Playback remains the sibling [com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackDocs]
 * family. Queue does not copy play/pause truth.
 */
@Suppress("unused")
internal class VexStreamQueueDocs private constructor()

// [VXG RealForever]
