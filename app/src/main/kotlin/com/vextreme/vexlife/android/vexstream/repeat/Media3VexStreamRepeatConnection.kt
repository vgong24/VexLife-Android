package com.vextreme.vexlife.android.vexstream.repeat

import androidx.media3.common.Player
import androidx.media3.session.MediaController
import kotlinx.coroutines.flow.StateFlow

/**
 * Media3 provider bridge for Repeat.
 * Observation enters Family State; accepted Family Effects become immediate repeatMode requests.
 */
internal class Media3VexStreamRepeatConnection(
    private val controller:
        MediaController,
) :
    AutoCloseable {
    private val family =
        VexStreamRepeatFamily(
            initialState =
                controller
                    .toVexStreamRepeatState(),
        )

    val states:
        StateFlow<
            VexStreamRepeatFamily.State
        >
        get() =
            family.states

    private val listener =
        object :
            Player.Listener {
            override fun onEvents(
                player:
                    Player,
                events:
                    Player.Events,
            ) {
                observe(
                    player,
                )
            }
        }

    init {
        controller.addListener(
            listener,
        )
    }

    /** Re-read provider truth, resolve Toggle, then realize its Effect synchronously. */
    fun activate(
        control:
            VexStreamRepeatFamily.Control,
    ) {
        observe(
            controller,
        )

        val effect =
            family
                .activate(
                    control,
                )
                ?.effect
                ?: return

        when (
            effect
        ) {
            VexStreamRepeatFamily.Effect.RequestEnableAll ->
                controller.repeatMode =
                    Player.REPEAT_MODE_ALL

            VexStreamRepeatFamily.Effect.RequestDisable ->
                controller.repeatMode =
                    Player.REPEAT_MODE_OFF
        }
    }

    override fun close() {
        controller.removeListener(
            listener,
        )
    }

    private fun observe(
        player:
            Player,
    ) {
        family.observe(
            player
                .toVexStreamRepeatState(),
        )
    }
}

private fun Player.toVexStreamRepeatState():
    VexStreamRepeatFamily.State =
    vexStreamRepeatState(
        repeatMode =
            repeatMode,
        toggleAvailable =
            isCommandAvailable(
                Player.COMMAND_SET_REPEAT_MODE,
            ),
    )

internal fun vexStreamRepeatState(
    repeatMode:
        Int,
    toggleAvailable:
        Boolean,
):
    VexStreamRepeatFamily.State =
    when (
        repeatMode
    ) {
        Player.REPEAT_MODE_OFF ->
            VexStreamRepeatFamily.State.Disabled(
                toggleAvailable =
                    toggleAvailable,
            )

        Player.REPEAT_MODE_ONE,
        Player.REPEAT_MODE_ALL,
        ->
            VexStreamRepeatFamily.State.Enabled(
                toggleAvailable =
                    toggleAvailable,
            )

        else ->
            error(
                "unknown Media3 repeatMode: " +
                    repeatMode,
            )
    }

// [VXG RealForever][VEXSTREAM][REPEAT][MEDIA3-CONNECTION]
