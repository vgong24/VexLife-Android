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
 * - can I reshuffle the still-future queue?
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
 * [VexStreamQueueControl.Previous], [VexStreamQueueControl.Next],
 * [VexStreamQueueControl.Shuffle], and [VexStreamQueueControl.Clear].
 *
 * ## What does Shuffle mean?
 *
 * Shuffle is a one-shot Queue action, not a selected/persistent mode.
 *
 * [VexStreamQueueEffect.ShuffleFuture] preserves already-played items and the current item, then
 * randomizes only the future queue. The Control is available only when at least two future items
 * exist and Media3 currently permits playlist mutation.
 *
 * The relationship uses stay(): queue order changes, but the admitted State kind remains NonEmpty
 * until provider observation supplies the next current projection.
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
 * The accepted Behavior kernel does not carry arbitrary runtime request payloads through finite
 * Controls, so this family does not invent a generic payload framework merely to absorb those
 * presentation mechanics.
 *
 * Repeat is now the sibling
 * [com.vextreme.vexlife.android.vexstream.repeat.VexStreamRepeatDocs] family. Playback remains the
 * sibling [com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackDocs] family. Queue does
 * not copy either repeat or play/pause truth.
 */
@Suppress("unused")
internal class VexStreamQueueDocs private constructor()

// [VXG RealForever]
