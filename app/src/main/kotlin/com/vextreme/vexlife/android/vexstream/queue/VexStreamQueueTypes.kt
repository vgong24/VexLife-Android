package com.vextreme.vexlife.android.vexstream.queue

import com.vextreme.vexlife.android.behavior.VexFamily

/**
 * Feature-facing name for the canonical Family projection.
 *
 * Projection identity stays with VexFamily; Queue contributes only its concrete DisplayState.
 */
internal typealias VexStreamQueueProjection =
    VexFamily.Projection<
        VexStreamQueueFamily,
        VexStreamQueueState,
        VexStreamQueueFamily.DisplayState
    >

// [VXG RealForever][VEXSTREAM][QUEUE][DIRECT-FAMILY]
