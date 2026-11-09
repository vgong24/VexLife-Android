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
    override val currentProjection:
        VexStreamRepeatProjection
        get() =
            (
                store.current as?
                    VexStreamPlayerStore.Snapshot.Available
            )
                ?.repeat
                .toVexStreamRepeatProjection()

    override fun activate(
        control:
            VexStreamRepeatControl,
    ) {
        val signal =
            (
                store.current as?
                    VexStreamPlayerStore.Snapshot.Available
            )
                ?.repeat
        val current =
            signal
                .toVexStreamRepeatState()
        val behavior =
            vexStreamRepeatBehavior()
        val controls =
            behavior
                .vexStreamRepeatControls()
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
}

internal fun VexStreamPlayerPartner.Signal.Repeat?.toVexStreamRepeatState():
    VexStreamRepeatState =
    if (
        this?.enabled ==
        true
    ) {
        VexStreamRepeatState.Enabled(
            toggleAvailable =
                toggleAvailable,
        )
    } else {
        VexStreamRepeatState.Disabled(
            toggleAvailable =
                this?.toggleAvailable ==
                    true,
        )
    }

internal fun VexStreamPlayerPartner.Signal.Repeat?.toVexStreamRepeatProjection():
    VexStreamRepeatProjection {
    val behavior =
        vexStreamRepeatBehavior()

    return behavior
        .vexStreamRepeatControls()
        .projection(
            toVexStreamRepeatState(),
        )
}

// [VXG RealForever][VEXSTREAM][REPEAT][PLAYER-PARTNER]
