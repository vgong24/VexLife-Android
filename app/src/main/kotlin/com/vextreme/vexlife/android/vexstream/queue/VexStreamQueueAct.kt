package com.vextreme.vexlife.android.vexstream.queue

import com.vextreme.vexlife.android.behavior.VexBehaviorFamily

/** Human/product verbs admitted by VexStream Queue. */
sealed interface VexStreamQueueAct :
    VexBehaviorFamily.Act<
        VexStreamQueueFamily
    > {
    data object Previous :
        VexStreamQueueAct

    data object Next :
        VexStreamQueueAct

    /** Randomize only the still-future queue while preserving already-played/current placement. */
    data object Shuffle :
        VexStreamQueueAct

    data object Clear :
        VexStreamQueueAct
}

// [VXG RealForever]
