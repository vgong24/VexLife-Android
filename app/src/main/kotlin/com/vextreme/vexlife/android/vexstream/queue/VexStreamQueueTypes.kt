package com.vextreme.vexlife.android.vexstream.queue

internal data class VexStreamQueueControlProjection(
    val isAvailable:
        Boolean,
)

internal data class VexStreamQueueProjection(
    val state:
        VexStreamQueueState,
    val previousAvailable:
        Boolean,
    val nextAvailable:
        Boolean,
    val shuffleAvailable:
        Boolean,
    val clearAvailable:
        Boolean,
) {
    operator fun get(
        control:
            VexStreamQueueControl,
    ):
        VexStreamQueueControlProjection =
        VexStreamQueueControlProjection(
            isAvailable =
                when (
                    control
                ) {
                    VexStreamQueueControl.Previous ->
                        previousAvailable

                    VexStreamQueueControl.Next ->
                        nextAvailable

                    VexStreamQueueControl.Shuffle ->
                        shuffleAvailable

                    VexStreamQueueControl.Clear ->
                        clearAvailable
                },
        )
}

// [VXG RealForever][VEXSTREAM][QUEUE][DIRECT-FAMILY]
