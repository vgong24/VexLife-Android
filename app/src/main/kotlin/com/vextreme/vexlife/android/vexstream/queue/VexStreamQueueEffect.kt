package com.vextreme.vexlife.android.vexstream.queue

import com.vextreme.vexlife.android.behavior.VexBehaviorFamily

/** Runtime Queue transformations selected by semantic Acts. */
sealed interface VexStreamQueueEffect :
    VexBehaviorFamily.Effect<
        VexStreamQueueFamily
    > {
    data object SeekPrevious :
        VexStreamQueueEffect

    data object SeekNext :
        VexStreamQueueEffect
}

// [VXG RealForever]
