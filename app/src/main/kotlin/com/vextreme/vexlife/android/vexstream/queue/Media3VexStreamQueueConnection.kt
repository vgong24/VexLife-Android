package com.vextreme.vexlife.android.vexstream.queue

import androidx.media3.common.Player
import androidx.media3.session.MediaController
import kotlinx.coroutines.flow.StateFlow

/** Media3 provider bridge for the finite Queue Family. */
internal class Media3VexStreamQueueConnection(
    private val controller:
        MediaController,
) :
    AutoCloseable {
    private val family =
        VexStreamQueueFamily(
            initialState =
                controller
                    .toVexStreamQueueState(),
        )

    val states:
        StateFlow<
            VexStreamQueueFamily.State
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

    fun activate(
        control:
            VexStreamQueueFamily.Control,
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
            VexStreamQueueFamily.Effect.SeekPrevious ->
                controller
                    .seekToPreviousMediaItem()

            VexStreamQueueFamily.Effect.SeekNext ->
                controller
                    .seekToNextMediaItem()

            VexStreamQueueFamily.Effect.ShuffleFuture ->
                controller
                    .shuffleVexStreamFutureQueue()

            VexStreamQueueFamily.Effect.ClearQueue ->
                controller
                    .clearMediaItems()
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
                .toVexStreamQueueState(),
        )
    }
}

internal fun Player.toVexStreamQueueState():
    VexStreamQueueFamily.State =
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
        shuffleAvailable =
            vexStreamQueueShuffleAvailable(
                mediaItemCount =
                    mediaItemCount,
                currentIndex =
                    currentMediaItemIndex,
                canChangeMediaItems =
                    isCommandAvailable(
                        Player
                            .COMMAND_CHANGE_MEDIA_ITEMS,
                    ),
            ),
        clearAvailable =
            isCommandAvailable(
                Player
                    .COMMAND_CHANGE_MEDIA_ITEMS,
            ),
    )

internal fun vexStreamQueueState(
    mediaItemCount:
        Int,
    previousAvailable:
        Boolean,
    nextAvailable:
        Boolean,
    shuffleAvailable:
        Boolean,
    clearAvailable:
        Boolean,
):
    VexStreamQueueFamily.State {
    require(
        mediaItemCount >=
            0,
    ) {
        "mediaItemCount must not be negative"
    }

    if (
        mediaItemCount ==
        0
    ) {
        return VexStreamQueueFamily.State.Empty
    }

    return VexStreamQueueFamily.State.NonEmpty(
        previousAvailable =
            previousAvailable,
        nextAvailable =
            nextAvailable,
        shuffleAvailable =
            shuffleAvailable,
        clearAvailable =
            clearAvailable,
    )
}

// [VXG RealForever][VEXSTREAM][QUEUE][MEDIA3-CONNECTION]
