package com.vextreme.vexlife.android.vexstream.queue

import com.vextreme.vexlife.android.behavior.VexBehaviorFamily

/** Human/product verbs admitted by the neighboring-item Queue behavior. */
sealed interface VexStreamQueueAct :
    VexBehaviorFamily.Act<
        VexStreamQueueFamily
    > {
    data object Previous :
        VexStreamQueueAct

    data object Next :
        VexStreamQueueAct
}

// [VXG RealForever]
