package com.vextreme.vexlife.android.vexstream.queue

import com.vextreme.vexlife.android.behavior.VexFamily

internal data class VexStreamQueueControlProjection(
    val isAvailable:
        Boolean,
)

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

internal val VexStreamQueueProjection.previousAvailable:
    Boolean
    get() =
        displayState
            .previousAvailable

internal val VexStreamQueueProjection.nextAvailable:
    Boolean
    get() =
        displayState
            .nextAvailable

internal val VexStreamQueueProjection.shuffleAvailable:
    Boolean
    get() =
        displayState
            .shuffleAvailable

internal val VexStreamQueueProjection.clearAvailable:
    Boolean
    get() =
        displayState
            .clearAvailable

internal operator fun VexStreamQueueProjection.get(
    control:
        VexStreamQueueControl,
):
    VexStreamQueueControlProjection =
    VexStreamQueueControlProjection(
        isAvailable =
            displayState
                .isAvailable(
                    control,
                ),
    )

// [VXG RealForever][VEXSTREAM][QUEUE][DIRECT-FAMILY]
