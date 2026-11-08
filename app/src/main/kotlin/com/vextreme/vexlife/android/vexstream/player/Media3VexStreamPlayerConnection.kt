@file:OptIn(com.vextreme.vexlife.android.execution.VexExecution.RelationshipFoundationApi::class)

package com.vextreme.vexlife.android.vexstream.player

import androidx.media3.common.Player
import com.vextreme.vexlife.android.behavior.VexPartner
import com.vextreme.vexlife.android.execution.VexExecution
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import com.vextreme.vexlife.android.vexstream.queue.vexStreamQueueShuffleAvailable
import java.util.concurrent.atomic.AtomicLong

internal class Media3VexStreamPlayerConnection(
    private val player:
        Player,
    private val receiver:
        VexStreamPlayerObservationReceiver,
    private val executionRoot:
        VexExecutionRoot,
    private val clock:
        () -> Long = {
            System.currentTimeMillis()
        },
) :
    VexStreamPlayerPartner.Connection,
    AutoCloseable {
    private object InboundAdmission :
        VexExecution.RelationshipId

    private sealed interface Delivery {
        data class Opened(
            val observations:
                List<
                    VexStreamPlayerPartner.Connection.Observation
                >,
        ) :
            Delivery

        data class Observed(
            val observations:
                List<
                    VexStreamPlayerPartner.Connection.Observation
                >,
        ) :
            Delivery

        data object Closed :
            Delivery
    }

    internal data class EventInput(
        val player:
            Player,
        val events:
            Player.Events,
    )

    private val occurrence =
        nextConnectionOccurrence
            .getAndIncrement()

    override val id =
        VexStreamPlayerPartner.Connection.Id(
            value =
                "media3-player-" +
                    occurrence,
            occurrence =
                occurrence,
        )

    private var closed =
        false

    private val fullConversion =
        VexPartner.Connection.Conversion<
            Player,
            VexStreamPlayerPartner,
            VexStreamPlayerPartner.Signal
        > { provider ->
            provider
                .toVexStreamPlayerSignals()
        }

    private val eventConversion =
        VexPartner.Connection.Conversion<
            EventInput,
            VexStreamPlayerPartner,
            VexStreamPlayerPartner.Signal
        > { input ->
            input
                .toVexStreamPlayerSignals()
        }

    private val listener =
        object :
            Player.Listener {
            override fun onEvents(
                player:
                    Player,
                events:
                    Player.Events,
            ) {
                val signals =
                    eventConversion
                        .convert(
                            EventInput(
                                player =
                                    player,
                                events =
                                    events,
                            ),
                        )

                if (
                    signals.isNotEmpty()
                ) {
                    enqueue(
                        Delivery.Observed(
                            observations =
                                observations(
                                    signals,
                                ),
                        ),
                    )
                }
            }
        }

    init {
        player.addListener(
            listener,
        )

        enqueue(
            Delivery.Opened(
                observations =
                    observations(
                        fullConversion
                            .convert(
                                player,
                            ),
                    ),
            ),
        )
    }

    override fun close() {
        if (
            closed
        ) {
            return
        }

        closed =
            true
        player.removeListener(
            listener,
        )
        enqueue(
            Delivery.Closed,
        )
    }

    private fun observations(
        signals:
            List<
                VexStreamPlayerPartner.Signal
            >,
    ):
        List<
            VexStreamPlayerPartner.Connection.Observation
        > {
        val observedAt =
            clock()

        return signals
            .map { signal ->
                VexStreamPlayerPartner.Connection.Observation(
                    connectionId =
                        id,
                    signal =
                        signal,
                    observedAtEpochMillis =
                        observedAt,
                )
            }
    }

    private fun enqueue(
        delivery:
            Delivery,
    ) {
        VexExecution.beginQueued(
            executionRoot =
                executionRoot,
            owner =
                this,
            relationshipId =
                InboundAdmission,
            operation =
                ::admitForExecution,
            argument =
                delivery,
        )
    }

    private suspend fun admitForExecution(
        delivery:
            Delivery,
    ) {
        when (
            delivery
        ) {
            is Delivery.Opened ->
                receiver.connectionOpened(
                    connectionId =
                        id,
                    observations =
                        delivery.observations,
                )

            is Delivery.Observed ->
                receiver.receive(
                    delivery.observations,
                )

            Delivery.Closed ->
                receiver.connectionClosed(
                    id,
                )
        }
    }

    companion object {
        private val nextConnectionOccurrence =
            AtomicLong(
                1L,
            )
    }
}

private fun Player.toVexStreamPlayerSignals():
    List<
        VexStreamPlayerPartner.Signal
    > =
    listOf(
        toVexStreamPlayerPlaybackSignal(),
        toVexStreamPlayerQueueItemsSignal(),
        toVexStreamPlayerQueueNavigationSignal(),
        toVexStreamPlayerRepeatSignal(),
        toVexStreamPlayerCurrentMediaSignal(),
    )

private fun Media3VexStreamPlayerConnection.EventInput.toVexStreamPlayerSignals():
    List<
        VexStreamPlayerPartner.Signal
    > =
    buildList {
        if (
            events.contains(
                Player.EVENT_PLAYBACK_STATE_CHANGED,
            ) ||
            events.contains(
                Player.EVENT_PLAY_WHEN_READY_CHANGED,
            ) ||
            events.contains(
                Player.EVENT_AVAILABLE_COMMANDS_CHANGED,
            ) ||
            events.contains(
                Player.EVENT_TIMELINE_CHANGED,
            )
        ) {
            add(
                player
                    .toVexStreamPlayerPlaybackSignal(),
            )
        }

        if (
            events.contains(
                Player.EVENT_TIMELINE_CHANGED,
            )
        ) {
            add(
                player
                    .toVexStreamPlayerQueueItemsSignal(),
            )
        }

        if (
            events.contains(
                Player.EVENT_TIMELINE_CHANGED,
            ) ||
            events.contains(
                Player.EVENT_MEDIA_ITEM_TRANSITION,
            ) ||
            events.contains(
                Player.EVENT_AVAILABLE_COMMANDS_CHANGED,
            )
        ) {
            add(
                player
                    .toVexStreamPlayerQueueNavigationSignal(),
            )
        }

        if (
            events.contains(
                Player.EVENT_REPEAT_MODE_CHANGED,
            ) ||
            events.contains(
                Player.EVENT_AVAILABLE_COMMANDS_CHANGED,
            )
        ) {
            add(
                player
                    .toVexStreamPlayerRepeatSignal(),
            )
        }

        if (
            events.contains(
                Player.EVENT_TIMELINE_CHANGED,
            ) ||
            events.contains(
                Player.EVENT_MEDIA_ITEM_TRANSITION,
            ) ||
            events.contains(
                Player.EVENT_MEDIA_METADATA_CHANGED,
            )
        ) {
            add(
                player
                    .toVexStreamPlayerCurrentMediaSignal(),
            )
        }
    }

private fun Player.toVexStreamPlayerPlaybackSignal():
    VexStreamPlayerPartner.Signal.Playback =
    VexStreamPlayerPartner.Signal.Playback(
        hasMedia =
            mediaItemCount >
                0,
        playRequested =
            playWhenReady,
        phase =
            when (
                playbackState
            ) {
                Player.STATE_IDLE ->
                    VexStreamPlayerPartner.PlaybackPhase.Idle

                Player.STATE_BUFFERING ->
                    VexStreamPlayerPartner.PlaybackPhase.Buffering

                Player.STATE_READY ->
                    VexStreamPlayerPartner.PlaybackPhase.Ready

                Player.STATE_ENDED ->
                    VexStreamPlayerPartner.PlaybackPhase.Ended

                else ->
                    error(
                        "unknown Media3 playbackState: " +
                            playbackState,
                    )
            },
        playPauseAvailable =
            isCommandAvailable(
                Player.COMMAND_PLAY_PAUSE,
            ),
        restartAvailable =
            isCommandAvailable(
                Player.COMMAND_PLAY_PAUSE,
            ) &&
                isCommandAvailable(
                    Player.COMMAND_SEEK_TO_DEFAULT_POSITION,
                ),
    )

private fun Player.toVexStreamPlayerQueueItemsSignal():
    VexStreamPlayerPartner.Signal.QueueItems =
    VexStreamPlayerPartner.Signal.QueueItems(
        mediaIds =
            (0 until mediaItemCount)
                .map { index ->
                    getMediaItemAt(
                        index,
                    ).mediaId
                },
    )

private fun Player.toVexStreamPlayerQueueNavigationSignal():
    VexStreamPlayerPartner.Signal.QueueNavigation =
    VexStreamPlayerPartner.Signal.QueueNavigation(
        currentIndex =
            if (
                mediaItemCount >
                0
            ) {
                currentMediaItemIndex
            } else {
                -1
            },
        previousAvailable =
            isCommandAvailable(
                Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
            ) &&
                hasPreviousMediaItem(),
        nextAvailable =
            isCommandAvailable(
                Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
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
                        Player.COMMAND_CHANGE_MEDIA_ITEMS,
                    ),
            ),
        clearAvailable =
            isCommandAvailable(
                Player.COMMAND_CHANGE_MEDIA_ITEMS,
            ),
    )

private fun Player.toVexStreamPlayerRepeatSignal():
    VexStreamPlayerPartner.Signal.Repeat =
    VexStreamPlayerPartner.Signal.Repeat(
        enabled =
            when (
                repeatMode
            ) {
                Player.REPEAT_MODE_OFF ->
                    false

                Player.REPEAT_MODE_ONE,
                Player.REPEAT_MODE_ALL ->
                    true

                else ->
                    error(
                        "unknown Media3 repeatMode: " +
                            repeatMode,
                    )
            },
        toggleAvailable =
            isCommandAvailable(
                Player.COMMAND_SET_REPEAT_MODE,
            ),
    )

private fun Player.toVexStreamPlayerCurrentMediaSignal():
    VexStreamPlayerPartner.Signal.CurrentMedia {
    val current =
        currentMediaItem

    return VexStreamPlayerPartner.Signal.CurrentMedia(
        mediaId =
            current
                ?.mediaId,
        title =
            current
                ?.mediaMetadata
                ?.title
                ?.toString()
                .orEmpty(),
        artist =
            current
                ?.mediaMetadata
                ?.artist
                ?.toString()
                .orEmpty(),
        artworkUri =
            current
                ?.mediaMetadata
                ?.artworkUri
                ?.toString(),
    )
}

// [VXG RealForever][VEXSTREAM][PLAYER-PARTNER][MEDIA3-CONNECTION]
