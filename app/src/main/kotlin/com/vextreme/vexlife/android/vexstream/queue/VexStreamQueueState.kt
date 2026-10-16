package com.vextreme.vexlife.android.vexstream.queue

import com.vextreme.vexlife.android.behavior.VexBehaviorFamily

/**
 * Current Queue truth needed by the admitted Queue controls.
 *
 * Item identity, queue-row selection payloads, reorder state and repeat remain with their current
 * owners. This family carries only provider truth required by Previous / Next / Shuffle / Clear.
 */
sealed interface VexStreamQueueState :
    VexBehaviorFamily.State<
        VexStreamQueueFamily
    > {
    /** There is no current ordered media context to navigate, reshuffle or clear. */
    data object Empty :
        VexStreamQueueState

    /** The queue has media; provider truth decides which current Queue controls are available. */
    data class NonEmpty(
        val previousAvailable: Boolean,
        val nextAvailable: Boolean,
        val shuffleAvailable: Boolean,
        val clearAvailable: Boolean,
    ) :
        VexStreamQueueState
}

// [VXG RealForever]
