package com.vextreme.vexlife.android.vexstream.repeat

import com.vextreme.vexlife.android.behavior.VexBehaviorFamily

/** Media3 repeat requests selected by semantic Acts. */
sealed interface VexStreamRepeatEffect :
    VexBehaviorFamily.Effect<
        VexStreamRepeatFamily
    > {
    data object RequestEnableAll :
        VexStreamRepeatEffect

    data object RequestDisable :
        VexStreamRepeatEffect
}

// [VXG RealForever]
