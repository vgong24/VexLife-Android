package com.vextreme.vexlife.android.vexstream.repeat

import com.vextreme.vexlife.android.behavior.VexBehaviorProjection
import com.vextreme.vexlife.android.behavior.VexPort

/** Feature-local names keep foundation generic weight out of ordinary Repeat callers. */
internal typealias VexStreamRepeatConnection =
    VexPort<
        VexStreamRepeatFamily,
        VexStreamRepeatState,
        VexStreamRepeatControl,
        VexStreamRepeatAct
    >

internal typealias VexStreamRepeatProjection =
    VexBehaviorProjection<
        VexStreamRepeatFamily,
        VexStreamRepeatState,
        VexStreamRepeatControl,
        VexStreamRepeatAct
    >

// [VXG RealForever]
