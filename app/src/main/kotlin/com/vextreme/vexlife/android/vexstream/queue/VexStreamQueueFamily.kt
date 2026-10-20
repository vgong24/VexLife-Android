package com.vextreme.vexlife.android.vexstream.queue

import com.vextreme.vexlife.android.behavior.VexBehaviorFamily

/**
 * Compile-time family identity for the VexStream Queue behavior.
 *
 * Queue is independent from Playback: it answers whether a listener can move to a neighboring item
 * in the current ordered media context. It does not own play/pause truth or media-source selection.
 */
sealed interface VexStreamQueueFamily :
    VexBehaviorFamily<
        VexStreamQueueFamily
    >

// [VXG RealForever]
