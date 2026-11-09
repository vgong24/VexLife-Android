package com.vextreme.vexlife.android.vexstream.repeat

/**
 * # VexStream Repeat
 *
 * Repeat is a process-shared Direct [VexStreamRepeatFamily], sibling to Playback and Queue.
 *
 *     Player Partner Repeat Signal
 *         -> Player Store
 *         -> Repeat Family State
 *
 *     Toggle Control
 *         -> accepted Act
 *         -> RequestEnableAll / RequestDisable Effect
 *         -> bound Method
 *         -> VexExecution (Latest)
 *         -> PlayerPartner.Connection.Request.setRepeatEnabled(...)
 *         -> external Source
 *
 * Repeat State is semantic/provider-derived truth. Effect dispatch uses stay(): it does not claim a
 * provider mode change before the next Player observation returns.
 *
 * [VexStreamRepeatExperience] consumes the process Family. It does not own another provider listener,
 * Port, or feature-local Connection.
 */
@Suppress("unused")
internal class VexStreamRepeatDocs private constructor()

// [VXG RealForever][VEXSTREAM][REPEAT][DIRECT-FAMILY]
