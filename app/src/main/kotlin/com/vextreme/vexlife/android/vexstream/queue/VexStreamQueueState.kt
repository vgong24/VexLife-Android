package com.vextreme.vexlife.android.vexstream.queue

import com.vextreme.vexlife.android.behavior.VexBehaviorFamily

/**
 * Current Queue truth needed by Previous / Next behavior.
 *
 * Item identity, queue-row selection payloads, reorder state, shuffle and repeat remain with their
 * current owners. This family carries only the truth required to answer the neighboring-item
 * question shared by Mini Player and Context Dock.
 */
sealed interface VexStreamQueueState :
    VexBehaviorFamily.State<
        VexStreamQueueFamily
    > {
    /** There is no current ordered media context to navigate. */
    data object Empty :
        VexStreamQueueState

    /** The queue has media; provider truth decides which neighboring moves are currently available. */
    data class NonEmpty(
        val previousAvailable: Boolean,
        val nextAvailable: Boolean,
    ) :
        VexStreamQueueState
}

// [VXG RealForever]
