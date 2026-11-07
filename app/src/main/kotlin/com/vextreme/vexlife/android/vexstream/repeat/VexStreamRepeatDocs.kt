package com.vextreme.vexlife.android.vexstream.repeat

/**
 * # VexStream Repeat
 *
 * Search this symbol when asking:
 *
 * "Is Repeat active for the current playback context, and can I toggle it?"
 *
 * Repeat is a sibling of Playback and Queue. It is not Queue ordering truth and it is not Play/Pause
 * truth merely because all three are displayed near the player.
 *
 * ## Semantic owner
 *
 * [VexStreamRepeatFamily] is the Direct Family owner.
 *
 * Its [VexStreamRepeatFamily.State.Disabled] and [VexStreamRepeatFamily.State.Enabled] values are
 * provider-derived product truth. The current product has one Repeat toggle, so Media3 Repeat-One
 * and Repeat-All both become Enabled. From Disabled, Toggle requests queue-wide Repeat-All; from
 * Enabled, Toggle requests Repeat-Off. No repeat-scope chooser is invented.
 *
 * ## Provider boundary
 *
 * [Media3VexStreamRepeatConnection] is still a real Connection because Media3 is independently-owned
 * provider truth. It does not own Repeat semantics.
 *
 * Activation:
 *
 *     re-read Media3 truth
 *     -> Family.observe(State)
 *     -> Family.activate(Control.Toggle)
 *     -> accepted Family Effect
 *     -> synchronous Media3 repeatMode assignment
 *
 * The assignment creates no unfinished asynchronous work, so no VexExecution Job is invented.
 * Requesting the provider mutation does not claim the resulting Repeat State. Player observation
 * later returns provider truth through Family.observe(...).
 *
 * ## UI boundary
 *
 * Compose consumes [VexStreamRepeatExperience]. The experience projects only the current Family
 * State and carries Controls back to the existing Media3 Connection.
 */
@Suppress("unused")
internal class VexStreamRepeatDocs private constructor()

// [VXG RealForever][VEXSTREAM][REPEAT][DIRECT-FAMILY]
