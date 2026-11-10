package com.vextreme.vexlife.android.vexstream.queue

/**
 * # VexStream Queue
 *
 * Search this symbol when asking:
 *
 * "What can I truthfully do with the current ordered VexStream queue?"
 *
 * [VexStreamQueueFamily] owns the finite Previous / Next / Shuffle / Clear semantic question.
 * [VexStreamQueueState] carries only the outside truth those Controls need.
 *
 * Each accepted finite Control authors its exact [VexStreamQueueEffect] as an Independent runtime
 * consequence and keeps semantic State unchanged until later outside Player truth is observed.
 *
 * Outside queue/currentness enters through the shared VexStream Player Partner / Player Store rather
 * than a Queue-owned Media3 semantic Connection. Concrete finite requests are handed to
 * com.vextreme.vexlife.android.vexstream.player.Media3ControllerConnection, which re-checks current
 * Media3 capability at dispatch.
 *
 * [VexStreamQueueExperience] is the Compose membrane over the one process-shared Family. It does not
 * own provider truth.
 *
 * Queue-row Play and Reorder remain typed Queue-local payload/currentness boundaries:
 * [VexStreamQueuePlayRequest] / [Media3VexStreamQueuePlayConnection] and
 * [VexStreamQueueReorderRequest] / [Media3VexStreamQueueReorderConnection].
 * They are intentionally not absorbed into the finite Player Partner merely because they use Media3.
 *
 * Shuffle remains one-shot future-queue mutation. [vexStreamQueueShuffleAvailable] and
 * [vexStreamQueueShuffleTargetOrder] preserve already-played/current prefix and require truthful
 * playlist-mutation capability.
 *
 * Repeat and Playback remain sibling Families; Queue does not copy their truth.
 */
@Suppress("unused")
internal class VexStreamQueueDocs private constructor()

// [VXG RealForever][VEXSTREAM][QUEUE][DIRECT-FAMILY]
