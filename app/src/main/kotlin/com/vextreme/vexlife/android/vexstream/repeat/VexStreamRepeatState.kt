package com.vextreme.vexlife.android.vexstream.repeat

import com.vextreme.vexlife.android.behavior.VexBehaviorFamily

/**
 * Provider-derived Repeat truth used by the current one-toggle product experience.
 *
 * Media3 REPEAT_MODE_ONE and REPEAT_MODE_ALL both project as Enabled because the current VexStream UI
 * asks only whether Repeat is active. Enabling from Disabled still requests queue-wide Repeat All.
 */
sealed interface VexStreamRepeatState :
    VexBehaviorFamily.State<
        VexStreamRepeatFamily
    > {
    data class Disabled(
        val toggleAvailable: Boolean,
    ) :
        VexStreamRepeatState

    data class Enabled(
        val toggleAvailable: Boolean,
    ) :
        VexStreamRepeatState
}

// [VXG RealForever]
