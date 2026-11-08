package com.vextreme.vexlife.android.vexstream.player

import androidx.media3.common.C
import androidx.media3.common.Player
import com.vextreme.vexlife.android.behavior.VexPartner
import com.vextreme.vexlife.android.vexstream.queue.vexStreamQueueShuffleAvailable

/**
 * Media3 realization of one live VexStream Player Partner Connection.
 *
 * Media3 vocabulary terminates here. A full Snapshot establishes outside totality once; subsequent
 * Player.Events are converted only into the complete facet Signals implicated by that provider
 * occurrence. Queue item identity is therefore not rebuilt for ordinary Playback/Repeat/position
 * events.
 */
internal class Media3VexStreamPlayerConnection(
    private val player:
        Player,
    private val receiver:
        VexStreamPlayerObservationReceiver,
    private val clock:
        () -> Long = {
            System.currentTimeMillis()
        },
) :
    VexStreamPlayerPartner.Connection,
    AutoCloseable {
    override val id =
        VexStreamPlayerPartner.Connection.Id(
            "media3-player-" +
                System.identityHashCode(
                    player,
                ),
        )

    private var closed =
        false

    internal data class EventInput(
        val player:
            Player,
        val events:
            Player.Events,
    )

    private val snapshotConversion =
        VexPartner.Connection.Conversion<
            Player,
            VexStreamPlayerPartner,
            VexStreamPlayerPartner.Signal
        > { provider ->
            listOf(
                VexStreamPlayerPartner.Signal
                    .Snapshot(
                        provider
                            .toVexStreamPlayerCurrent(),
                    ),
            )
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
                eventConversion
                    .convert(
                        EventInput(
                            player =
                                player,
                            events =
                                events,
                        ),
                    )
                    .forEach(
                        ::publish,
                    )
            }
        }

    init {
        snapshotConversion
            .convert(
                player,
            )
            .forEach(
                ::publish,
            )

        player.addListener(
            listener,
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

        receiver.connectionClosed(
            id,
        )
    }

    private fun publish(
        signal:
            VexStreamPlayerPartner.Signal,
    ) {
        receiver.receiveNow(
            VexStreamPlayerPartner.Connection.Observation(
                connectionId =
                    id,
                signal =
                    signal,
                observedAtEpochMillis =
                    clock(),
            ),
        )
    }
}

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

        /*
         * Item identity can be large. Re-read it only when the provider says the timeline itself
         * changed; current-index/navigation changes have their own smaller Signal below.
         */
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

        if (
            events.contains(
                Player.EVENT_TIMELINE_CHANGED,
            ) ||
            events.contains(
                Player.EVENT_MEDIA_ITEM_TRANSITION,
            ) ||
            events.contains(
                Player.EVENT_POSITION_DISCONTINUITY,
            ) ||
            events.contains(
                Player.EVENT_PLAYBACK_STATE_CHANGED,
            )
        ) {
            add(
                player
                    .toVexStreamPlayerTimelineSignal(),
            )
        }
    }

private fun Player.toVexStreamPlayerCurrent():
    VexStreamPlayerPartner.Current.Available =
    VexStreamPlayerPartner.Current.Available(
        playback =
            toVexStreamPlayerPlaybackSignal(),
        queueItems =
            toVexStreamPlayerQueueItemsSignal(),
        queueNavigation =
            toVexStreamPlayerQueueNavigationSignal(),
        repeat =
            toVexStreamPlayerRepeatSignal(),
        currentMedia =
            toVexStreamPlayerCurrentMediaSignal(),
        timeline =
            toVexStreamPlayerTimelineSignal(),
    )

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
                    VexStreamPlayerPartner
                        .PlaybackPhase.Idle

                Player.STATE_BUFFERING ->
                    VexStreamPlayerPartner
                        .PlaybackPhase.Buffering

                Player.STATE_READY ->
                    VexStreamPlayerPartner
                        .PlaybackPhase.Ready

                Player.STATE_ENDED ->
                    VexStreamPlayerPartner
                        .PlaybackPhase.Ended

                else ->
                    error(
                        "unknown Media3 playbackState: $playbackState",
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
                        "unknown Media3 repeatMode: $repeatMode",
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

private fun Player.toVexStreamPlayerTimelineSignal():
    VexStreamPlayerPartner.Signal.Timeline {
    val durationMs =
        duration.takeIf {
            it !=
                C.TIME_UNSET &&
                it >
                0L
        } ?: 0L

    val positionMs =
        currentPosition
            .coerceAtLeast(
                0L,
            )
            .let {
                if (
                    durationMs >
                    0L
                ) {
                    it.coerceAtMost(
                        durationMs,
                    )
                } else {
                    it
                }
            }

    val bufferedMs =
        bufferedPosition
            .coerceAtLeast(
                0L,
            )
            .let {
                if (
                    durationMs >
                    0L
                ) {
                    it.coerceAtMost(
                        durationMs,
                    )
                } else {
                    it
                }
            }

    return VexStreamPlayerPartner.Signal.Timeline(
        positionMs =
            positionMs,
        durationMs =
            durationMs,
        bufferedPositionMs =
            bufferedMs,
    )
}

// [VXG RealForever][VEXSTREAM][PLAYER-PARTNER][MEDIA3-CONNECTION]
