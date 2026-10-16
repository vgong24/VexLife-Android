package com.vextreme.vexlife.android.vexstream.queue

/**
 * # VexStream Queue
 *
 * Search this symbol when asking:
 *
 * "Can I move to the previous or next item in the current VexStream queue?"
 *
 * This class is documentation/navigation only. It owns no Media3 queue, player lifetime, rendered
 * queue-item identity, or Compose state.
 *
 * ## What is true?
 *
 * [VexStreamQueueState] distinguishes an empty queue from a non-empty queue and carries only the
 * provider-derived neighboring-item availability needed by this behavior.
 *
 * ## What can the listener request?
 *
 * [VexStreamQueueControl.Previous] and [VexStreamQueueControl.Next].
 *
 * ## What do those Controls mean?
 *
 * [VexStreamQueueAct.Previous] and [VexStreamQueueAct.Next].
 *
 * ## What should runtime attempt?
 *
 * [VexStreamQueueEffect.SeekPrevious] and [VexStreamQueueEffect.SeekNext].
 *
 * ## Who talks to Media3?
 *
 * [Media3VexStreamQueueConnection]. It re-reads current Media3 truth at activation time before
 * resolving a Control, so stale UI availability never becomes command authority.
 *
 * ## What should Compose depend on?
 *
 * [VexStreamQueueExperience]. Mini Player and Context Dock consume the same Queue Projection and
 * activate the same Controls instead of each interpreting MediaController predicates themselves.
 *
 * ## What is deliberately NOT in this family?
 *
 * Queue-row selection still carries an index request payload. Reorder carries dynamic item identity.
 * Shuffle, repeat and clear are separate questions. The accepted Behavior kernel explicitly holds a
 * generic request-payload abstraction, so this slice does not invent one merely to absorb nearby
 * Queue mechanics.
 *
 * Playback remains the sibling [com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackDocs]
 * family. Queue does not copy play/pause truth.
 */
@Suppress("unused")
internal class VexStreamQueueDocs private constructor()

// [VXG RealForever]
