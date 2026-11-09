package com.vextreme.vexlife.android.vexstream.queue

/**
 * # VexStream Queue
 *
 * Queue is a process-shared Direct [VexStreamQueueFamily]. It owns the semantic question:
 *
 *     "What do Previous / Next / Shuffle / Clear mean for the currently observed queue?"
 *
 * Player Partner owns the external music Source/Connection vocabulary. Player Store owns retained
 * queue payload/current. Queue Family observes the Store's bounded QueueSnapshot and owns only Queue
 * State / Control / Effect meaning.
 *
 *     Player Partner Signal
 *         -> Player Store QueueSnapshot
 *         -> Queue Family State
 *
 *     Queue Control
 *         -> accepted Family Act
 *         -> bound Method
 *         -> VexExecution
 *         -> PlayerPartner.Connection.Request
 *         -> external Source
 *
 * Queue Effects use stay(): an attempted provider mutation does not claim the resulting provider
 * queue before Player observation returns through Store.
 *
 * [VexStreamQueueExperience] exposes the Family projection plus the exact Store-owned queue payload
 * required by presentation. It owns neither truth.
 *
 * Queue-row Play / Reorder are deliberately different. They carry one exact current provider
 * occurrence and require an immediate provider-currentness result while a listener selects or drags
 * a row. They therefore address the actual Player Partner request Connection directly rather than
 * inventing feature-local Connection interfaces or forcing payload requests into finite Family
 * Controls.
 *
 * Queue-row currentness uses [com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerPartner.QueueOccurrence]
 * directly. The concrete Player Connection rechecks media ID / occurrence / index and provider
 * command authority before mutation; Queue does not rename that Partner-owned coordinate.
 *
 * Repeat and Playback remain sibling semantic Families; Queue does not copy their truth.
 */
@Suppress("unused")
internal class VexStreamQueueDocs private constructor()

// [VXG RealForever][VEXSTREAM][QUEUE][DIRECT-FAMILY]
