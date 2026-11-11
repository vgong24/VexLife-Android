package com.vextreme.vexlife.android.vexstream.repeat

import com.vextreme.vexlife.android.behavior.VexFamily

/** Media3 repeat requests selected by semantic Acts. */
internal sealed interface VexStreamRepeatEffect :
    VexFamily.Effect<
        VexStreamRepeatFamily
    > {
    data object RequestEnableAll :
        VexStreamRepeatEffect

    data object RequestDisable :
        VexStreamRepeatEffect
}

// [VXG RealForever]
