package com.vextreme.vexlife.android.vexstream.repeat

import com.vextreme.vexlife.android.behavior.VexFamily

/**
 * Stable finite Repeat affordance value.
 *
 * [Command.Toggle] is the human affordance, interpreted against Repeat truth the listener can see.
 *
 * [Command.Enable] and [Command.Disable] are directional intent for callers whose request may have
 * been formed against older perception, such as a spoken "repeat on". An already-satisfied
 * directional intent authors no Act. Enable keeps the current product meaning: from Disabled it
 * requests queue-wide Repeat All.
 *
 * All three values belong to one maintained Repeat relationship, so the outer Control remains one
 * class. VexFamily derives RelationshipId from Control type plus enum entry name, so enum entries
 * would split Toggle / Enable / Disable into independent relationships under Latest.
 */
internal class VexStreamRepeatControl private constructor(
    val command:
        Command,
) :
    VexFamily.Control<
        VexStreamRepeatFamily
    > {
    enum class Command {
        Toggle,
        Enable,
        Disable,
    }

    val name:
        String
        get() =
            command.name

    companion object {
        val Toggle =
            VexStreamRepeatControl(
                Command.Toggle,
            )

        val Enable =
            VexStreamRepeatControl(
                Command.Enable,
            )

        val Disable =
            VexStreamRepeatControl(
                Command.Disable,
            )

        val entries:
            List<
                VexStreamRepeatControl
            > =
            listOf(
                Toggle,
                Enable,
                Disable,
            )
    }

    override fun toString():
        String =
        name
}

// [VXG RealForever][VEXSTREAM][REPEAT][DIRECT-FAMILY]
