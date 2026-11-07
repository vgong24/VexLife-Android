package com.vextreme.vexlife.android.vexstream.repeat

import androidx.media3.common.Player
import androidx.media3.session.MediaController
import kotlinx.coroutines.flow.StateFlow

/**
 * Media3 provider boundary for the VexStream Repeat Family.
 *
 * Family owns Repeat meaning. Media3 owns provider truth. This Connection owns only the concrete
 * relationship between them:
 *
 * provider observation -> semantic Family State
 * accepted Family Effect -> synchronous Media3 request
 *
 * No VexExecution Job is created because assigning Media3 repeatMode is immediate and there is no
 * unfinished work for execution custody to maintain.
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

    val currentState:
        VexStreamRepeatFamily.State
        get() =
            family.currentState

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

    /**
     * Re-read exact provider truth before resolving the incoming semantic Control.
     *
     * The accepted Effect is realized synchronously. The request does not claim a new semantic
     * Repeat State; only a later Media3 observation enters Family.observe(...).
     */
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
