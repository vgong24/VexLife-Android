package com.vextreme.vexlife.android.vexstream.repeat

/**
 * # VexStream Repeat
 *
 * Question: is Repeat active for the current playback context, and can it be toggled?
 *
 * [VexStreamRepeatFamily] owns Repeat State / Control / Effect meaning.
 * [Media3VexStreamRepeatConnection] owns the concrete Media3 provider relationship:
 *
 *     Media3 observation -> Family.observe(State)
 *     Family Effect      -> immediate repeatMode request
 *
 * The request does not certify provider outcome, and the immediate assignment does not earn a
 * VexExecution Job. Compose consumes [VexStreamRepeatExperience].
 */
@Suppress("unused")
internal class VexStreamRepeatDocs private constructor()

// [VXG RealForever][VEXSTREAM][REPEAT][DIRECT-FAMILY]
