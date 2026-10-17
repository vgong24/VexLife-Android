package com.vextreme.vexlife.android.vexstream.repeat

import com.vextreme.vexlife.android.behavior.VexBehaviorFamily

/** Human/product verbs admitted by VexStream Repeat. */
sealed interface VexStreamRepeatAct :
    VexBehaviorFamily.Act<
        VexStreamRepeatFamily
    > {
    data object Enable :
        VexStreamRepeatAct

    data object Disable :
        VexStreamRepeatAct
}

// [VXG RealForever]
