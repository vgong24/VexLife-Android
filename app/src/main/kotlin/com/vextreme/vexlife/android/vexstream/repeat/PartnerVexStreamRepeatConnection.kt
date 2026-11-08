package com.vextreme.vexlife.android.vexstream.repeat

import com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerPartner
import com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerStore

/**
 * Repeat semantic meaning resolved from retained Player Partner CURRENT, then realized outward.
 */
internal class PartnerVexStreamRepeatConnection(
    private val store:
        VexStreamPlayerStore,
    private val effectConnection:
        VexStreamRepeatEffectConnection,
) :
    VexStreamRepeatConnection {
    private val behavior =
        vexStreamRepeatBehavior()

    private val controls =
        behavior
            .vexStreamRepeatControls()

    override val currentProjection:
        VexStreamRepeatProjection
        get() =
            controls.projection(
                currentState(),
            )

    override fun activate(
        control:
            VexStreamRepeatControl,
    ) {
        val current =
            currentState()
        val act =
            controls.resolve(
                current =
                    current,
                control =
                    control,
            )
                ?.act
                ?: return
        val relationship =
            behavior.resolve(
                current =
                    current,
                act =
                    act,
            )
                ?: return

        effectConnection.realize(
            relationship.effect,
        )
    }

    private fun currentState():
        VexStreamRepeatState {
        val signal =
            (
                store.current as?
                    VexStreamPlayerPartner.Current.Available
            )
                ?.repeat

        return if (
            signal?.enabled ==
            true
        ) {
            VexStreamRepeatState.Enabled(
                toggleAvailable =
                    signal.toggleAvailable,
            )
        } else {
            VexStreamRepeatState.Disabled(
                toggleAvailable =
                    signal?.toggleAvailable ==
                        true,
            )
        }
    }
}

// [VXG RealForever][VEXSTREAM][REPEAT][PLAYER-PARTNER]
