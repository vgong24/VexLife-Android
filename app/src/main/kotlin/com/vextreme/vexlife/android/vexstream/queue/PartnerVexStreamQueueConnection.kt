package com.vextreme.vexlife.android.vexstream.queue

import com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerPartner
import com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerStore

/**
 * Queue semantic meaning resolved from retained Player Partner CURRENT, then realized outward.
 */
internal class PartnerVexStreamQueueConnection(
    private val store:
        VexStreamPlayerStore,
    private val effectConnection:
        VexStreamQueueEffectConnection,
) :
    VexStreamQueueConnection {
    private val behavior =
        vexStreamQueueBehavior()

    private val controls =
        behavior
            .vexStreamQueueControls()

    override val currentProjection:
        VexStreamQueueProjection
        get() =
            controls.projection(
                currentState(),
            )

    override fun activate(
        control:
            VexStreamQueueControl,
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
        VexStreamQueueState {
        val queue =
            store.currentQueue
                ?: return VexStreamQueueState
                    .Empty

        return queue
            .toState()
    }
}

internal fun VexStreamPlayerPartner.QueueCurrent.toState():
    VexStreamQueueState =
    vexStreamQueueState(
        mediaItemCount =
            items.mediaIds.size,
        previousAvailable =
            navigation.previousAvailable,
        nextAvailable =
            navigation.nextAvailable,
        shuffleAvailable =
            navigation.shuffleAvailable,
        clearAvailable =
            navigation.clearAvailable,
    )

// [VXG RealForever][VEXSTREAM][QUEUE][PLAYER-PARTNER]
