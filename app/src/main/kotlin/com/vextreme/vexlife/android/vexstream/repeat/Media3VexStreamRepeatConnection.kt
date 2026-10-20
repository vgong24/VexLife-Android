package com.vextreme.vexlife.android.vexstream.repeat

import androidx.media3.common.Player
import androidx.media3.session.MediaController

/**
 * Media3 realization of the current VexStream Repeat question.
 *
 * Activation re-reads provider truth before resolving Toggle. The relationship uses stay(): asking
 * Media3 to change repeat mode does not itself certify the next provider State.
 */
internal class Media3VexStreamRepeatConnection(
    private val controller: MediaController,
) :
    VexStreamRepeatConnection {
    private val behavior =
        vexStreamRepeatBehavior()

    private val controls =
        behavior.vexStreamRepeatControls()

    override val currentProjection:
        VexStreamRepeatProjection
        get() =
            controls.projection(
                controller
                    .toVexStreamRepeatState(),
            )

    override fun activate(
        control: VexStreamRepeatControl,
    ) {
        val current =
            controller
                .toVexStreamRepeatState()

        val act =
            controls.resolve(
                current = current,
                control = control,
            )?.act
                ?: return

        val relationship =
            behavior.resolve(
                current = current,
                act = act,
            )
                ?: return

        when (
            relationship.effect
        ) {
            VexStreamRepeatEffect.RequestEnableAll ->
                controller.repeatMode =
                    Player.REPEAT_MODE_ALL

            VexStreamRepeatEffect.RequestDisable ->
                controller.repeatMode =
                    Player.REPEAT_MODE_OFF
        }
    }
}

private fun Player.toVexStreamRepeatState():
    VexStreamRepeatState =
    vexStreamRepeatState(
        repeatMode = repeatMode,
        toggleAvailable =
            isCommandAvailable(
                Player.COMMAND_SET_REPEAT_MODE,
            ),
    )

internal fun vexStreamRepeatState(
    repeatMode: Int,
    toggleAvailable: Boolean,
): VexStreamRepeatState =
    when (repeatMode) {
        Player.REPEAT_MODE_OFF ->
            VexStreamRepeatState.Disabled(
                toggleAvailable =
                    toggleAvailable,
            )

        Player.REPEAT_MODE_ONE,
        Player.REPEAT_MODE_ALL ->
            VexStreamRepeatState.Enabled(
                toggleAvailable =
                    toggleAvailable,
            )

        else ->
            error(
                "unknown Media3 repeatMode: " +
                    repeatMode,
            )
    }

// [VXG RealForever]
