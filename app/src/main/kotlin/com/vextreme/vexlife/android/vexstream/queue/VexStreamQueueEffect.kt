package com.vextreme.vexlife.android.vexstream.queue

import com.vextreme.vexlife.android.behavior.VexFamily

/** Runtime Queue transformations selected by semantic Acts. */
internal sealed interface VexStreamQueueEffect :
    VexFamily.Effect<
        VexStreamQueueFamily
    > {
    data object SeekPrevious :
        VexStreamQueueEffect

    data object SeekNext :
        VexStreamQueueEffect

    data object ShuffleFuture :
        VexStreamQueueEffect

    data object ClearQueue :
        VexStreamQueueEffect
}

// [VXG RealForever]
