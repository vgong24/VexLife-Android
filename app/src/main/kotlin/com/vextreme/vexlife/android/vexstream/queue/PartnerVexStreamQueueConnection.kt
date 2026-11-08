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
    override val currentProjection:
        VexStreamQueueProjection
        get() =
            store
                .currentQueue
                .toVexStreamQueueProjection()

    override fun activate(
        control:
            VexStreamQueueControl,
    ) {
        val current =
            store
                .currentQueue
                .toVexStreamQueueState()
        val behavior =
            vexStreamQueueBehavior()
        val controls =
            behavior
                .vexStreamQueueControls()
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

internal fun VexStreamPlayerPartner.QueueCurrent?.toVexStreamQueueState():
    VexStreamQueueState =
    this
        ?.let { queue ->
            vexStreamQueueState(
                mediaItemCount =
                    queue.items
                        .mediaIds
                        .size,
                previousAvailable =
                    queue.navigation
                        .previousAvailable,
                nextAvailable =
                    queue.navigation
                        .nextAvailable,
                shuffleAvailable =
                    queue.navigation
                        .shuffleAvailable,
                clearAvailable =
                    queue.navigation
                        .clearAvailable,
            )
        }
        ?: VexStreamQueueState
            .Empty

internal fun VexStreamPlayerPartner.QueueCurrent?.toVexStreamQueueProjection():
    VexStreamQueueProjection {
    val behavior =
        vexStreamQueueBehavior()

    return behavior
        .vexStreamQueueControls()
        .projection(
            toVexStreamQueueState(),
        )
}

// [VXG RealForever][VEXSTREAM][QUEUE][PLAYER-PARTNER]
