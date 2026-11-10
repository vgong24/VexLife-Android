package com.vextreme.vexlife.android.vexstream.queue

import com.vextreme.vexlife.android.behavior.VexFamily

/**
 * Stable finite Queue affordance value.
 *
 * All four values belong to one maintained Queue-command relationship. The value still names the
 * exact semantic request, while the shared Control class lets VexFamily preserve mixed command
 * arrival order through one Queued realization lane.
 */
internal class VexStreamQueueControl private constructor(
    val name:
        String,
) :
    VexFamily.Control<
        VexStreamQueueFamily
    > {
    companion object {
        val Previous =
            VexStreamQueueControl(
                "Previous",
            )

        val Next =
            VexStreamQueueControl(
                "Next",
            )

        val Shuffle =
            VexStreamQueueControl(
                "Shuffle",
            )

        val Clear =
            VexStreamQueueControl(
                "Clear",
            )

        val entries:
            List<
                VexStreamQueueControl
            > =
            listOf(
                Previous,
                Next,
                Shuffle,
                Clear,
            )
    }

    override fun toString():
        String =
        name
}

// [VXG RealForever][VEXSTREAM][QUEUE][DIRECT-FAMILY]
