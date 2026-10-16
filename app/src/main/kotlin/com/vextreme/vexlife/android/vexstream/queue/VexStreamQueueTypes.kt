package com.vextreme.vexlife.android.vexstream.queue

import com.vextreme.vexlife.android.behavior.VexBehaviorProjection
import com.vextreme.vexlife.android.behavior.VexPort

/** Feature-local names keep foundation generic weight out of ordinary Queue callers. */
internal typealias VexStreamQueueConnection =
    VexPort<
        VexStreamQueueFamily,
        VexStreamQueueState,
        VexStreamQueueControl,
        VexStreamQueueAct
    >

internal typealias VexStreamQueueProjection =
    VexBehaviorProjection<
        VexStreamQueueFamily,
        VexStreamQueueState,
        VexStreamQueueControl,
        VexStreamQueueAct
    >

// [VXG RealForever]
