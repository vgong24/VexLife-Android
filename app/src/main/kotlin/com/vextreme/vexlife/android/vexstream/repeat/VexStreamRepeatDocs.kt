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
 * [VexStreamRepeatState.Disabled] and [VexStreamRepeatState.Enabled] are provider-derived product
 * truth. The current product has one Repeat toggle, so Media3 Repeat-One and Repeat-All both project
 * as Enabled. From Disabled, Toggle requests queue-wide Repeat-All; from Enabled, Toggle requests
 * Repeat-Off. No repeat-scope chooser is invented.
 *
 * [Media3VexStreamRepeatConnection] re-reads current Media3 truth at activation time. Its Behavior
 * relationships use stay(): effect dispatch does not claim a provider mode change before Media3
 * observation reports it.
 *
 * Compose consumes [VexStreamRepeatExperience], not raw repeatMode.
 */
@Suppress("unused")
internal class VexStreamRepeatDocs private constructor()

// [VXG RealForever]
