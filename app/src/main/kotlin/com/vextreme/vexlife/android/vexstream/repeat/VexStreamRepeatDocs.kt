package com.vextreme.vexlife.android.vexstream.repeat

/**
 * # VexStream Repeat
 *
 * Search this symbol when asking:
 *
 * "Is Repeat active for the current playback context, and can I toggle it?"
 *
 * [VexStreamRepeatFamily] is the one finite semantic owner. Disabled/Enabled are Family State; the
 * current product intentionally projects Media3 Repeat-One and Repeat-All as Enabled because there
 * is only one Repeat toggle.
 *
 * Toggle authors [VexStreamRepeatEffect.RequestEnableAll] or
 * [VexStreamRepeatEffect.RequestDisable] as a Latest runtime consequence without claiming the
 * provider already changed. Later outside Player Repeat Signal returns through the shared Player
 * Store and [VexStreamRepeatFamily.observeStore].
 *
 * Concrete finite requests use
 * com.vextreme.vexlife.android.vexstream.player.Media3ControllerConnection, which re-checks current
 * Media3 command capability at dispatch. [VexStreamRepeatExperience] is only the Compose membrane.
 *
 * Repeat remains separate from Queue order and Playback Play/Pause meaning.
 */
@Suppress("unused")
internal class VexStreamRepeatDocs private constructor()

// [VXG RealForever][VEXSTREAM][REPEAT][DIRECT-FAMILY]
