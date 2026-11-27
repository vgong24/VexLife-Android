package com.vextreme.vexlife.android.vexstream.playback

import com.vextreme.vexlife.android.behavior.VexFamily

/**
 * Stable finite Playback affordance value.
 *
 * [Command.PlayPause] is the human toggle. The listener sees current Playback before acting, so
 * "the other one" is the truthful meaning.
 *
 * [Command.Play] and [Command.Pause] are directional intent for callers whose request may have been
 * formed against older perception: a spoken or typed Vex request, or a future system surface. An
 * already-satisfied directional intent authors no Act. A toggle would invert it instead.
 *
 * All three values belong to one maintained Playback relationship, so the outer Control remains one
 * class. VexFamily derives RelationshipId from Control type plus enum entry name. One enum entry per
 * command would therefore split PlayPause / Play / Pause into independent relationships, and a newer
 * human toggle could not supersede an older directional request under Latest. This follows the
 * existing [com.vextreme.vexlife.android.vexstream.queue.VexStreamQueueControl] shape.
 */
internal class VexStreamPlaybackControl private constructor(
    val command:
        Command,
) :
    VexFamily.Control<
        VexStreamPlaybackFamily
    > {
    enum class Command {
        PlayPause,
        Play,
        Pause,
    }

    val name:
        String
        get() =
            command.name

    companion object {
        val PlayPause =
            VexStreamPlaybackControl(
                Command.PlayPause,
            )

        val Play =
            VexStreamPlaybackControl(
                Command.Play,
            )

        val Pause =
            VexStreamPlaybackControl(
                Command.Pause,
            )

        val entries:
            List<
                VexStreamPlaybackControl
            > =
            listOf(
                PlayPause,
                Play,
                Pause,
            )
    }

    override fun toString():
        String =
        name
}

// [VXG RealForever][VEXSTREAM][PLAYBACK][DIRECT-FAMILY]
