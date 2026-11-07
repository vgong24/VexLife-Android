package com.vextreme.vexlife.android.vexstream.queue

/**
 * # VexStream Queue
 *
 * Question: what can the listener truthfully do with the current ordered Media3 queue?
 *
 * [VexStreamQueueFamily] owns the finite Previous / Next / Shuffle / Clear semantic controls.
 * [Media3VexStreamQueueConnection] owns provider observation and immediate provider realization:
 *
 *     Media3 observation -> Family.observe(State)
 *     Family Effect      -> immediate Media3 command
 *
 * State carries the current provider-derived availability facts. Requesting an Effect never claims
 * the provider outcome; Clear therefore remains NonEmpty until Media3 later observes Empty.
 *
 * Queue-row Play and Reorder stay separate typed request boundaries because they carry one current
 * provider occurrence coordinate:
 *
 *     VexStreamQueueOccurrence
 *     -> VexStreamQueuePlayRequest
 *     -> Media3VexStreamQueuePlayConnection
 *
 *     VexStreamQueueOccurrence + Earlier/Later
 *     -> VexStreamQueueReorderRequest
 *     -> Media3VexStreamQueueReorderConnection
 *
 * Those request coordinates are current provider identity, not durable Queue-item identity.
 * [VexStreamQueueExperience] is the Compose-facing Family State / Control membrane.
 */
@Suppress("unused")
internal class VexStreamQueueDocs private constructor()

// [VXG RealForever][VEXSTREAM][QUEUE][DIRECT-FAMILY]
