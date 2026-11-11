package com.vextreme.vexlife.android.vexstream.queue

import com.vextreme.vexlife.android.behavior.VexFamily

/**
 * Stable finite Queue affordance value.
 *
 * All four values belong to one maintained Queue-command relationship, so the outer Control remains
 * one class. [Command] keeps the finite semantic vocabulary compiler-exhaustive without splitting
 * Previous / Next / Shuffle / Clear into separate VexFamily relationship identities.
 */
internal class VexStreamQueueControl private constructor(
    val command:
        Command,
) :
    VexFamily.Control<
        VexStreamQueueFamily
    > {
    enum class Command {
        Previous,
        Next,
        Shuffle,
        Clear,
    }

    val name:
        String
        get() =
            command.name

    companion object {
        val Previous =
            VexStreamQueueControl(
                Command.Previous,
            )

        val Next =
            VexStreamQueueControl(
                Command.Next,
            )

        val Shuffle =
            VexStreamQueueControl(
                Command.Shuffle,
            )

        val Clear =
            VexStreamQueueControl(
                Command.Clear,
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
