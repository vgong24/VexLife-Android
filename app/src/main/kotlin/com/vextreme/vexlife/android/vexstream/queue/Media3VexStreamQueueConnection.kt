package com.vextreme.vexlife.android.vexstream.queue

import androidx.media3.common.Player
import androidx.media3.session.MediaController

/**
 * Media3 realization of the VexStream neighboring-item Queue behavior.
 *
 * Activation always re-reads current Media3 truth before resolving the Control. UI projection is
 * therefore informative, not execution authority.
 */
internal class Media3VexStreamQueueConnection(
    private val controller: MediaController,
) :
    VexStreamQueueConnection {
    private val behavior =
        vexStreamQueueBehavior()

    private val controls =
        behavior.vexStreamQueueControls()

    override val currentProjection:
        VexStreamQueueProjection
        get() =
            controls.projection(
                controller
                    .toVexStreamQueueState(),
            )

    override fun activate(
        control: VexStreamQueueControl,
    ) {
        val current =
            controller
                .toVexStreamQueueState()

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
            VexStreamQueueEffect.SeekPrevious ->
                controller
                    .seekToPreviousMediaItem()

            VexStreamQueueEffect.SeekNext ->
                controller
                    .seekToNextMediaItem()
        }
    }
}

private fun Player.toVexStreamQueueState():
    VexStreamQueueState =
    vexStreamQueueState(
        mediaItemCount =
            mediaItemCount,
        previousAvailable =
            isCommandAvailable(
                Player
                    .COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
            ) &&
                hasPreviousMediaItem(),
        nextAvailable =
            isCommandAvailable(
                Player
                    .COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
            ) &&
                hasNextMediaItem(),
    )

internal fun vexStreamQueueState(
    mediaItemCount: Int,
    previousAvailable: Boolean,
    nextAvailable: Boolean,
): VexStreamQueueState {
    require(
        mediaItemCount >= 0,
    ) {
        "mediaItemCount must not be negative"
    }

    if (
        mediaItemCount == 0
    ) {
        return VexStreamQueueState.Empty
    }

    return VexStreamQueueState.NonEmpty(
        previousAvailable =
            previousAvailable,
        nextAvailable =
            nextAvailable,
    )
}

// [VXG RealForever]
