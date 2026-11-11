package com.vextreme.vexlife.android.vexstream.player

import com.vextreme.vexlife.android.execution.VexExecutionRoot
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackFamily
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackPhase
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackState
import com.vextreme.vexlife.android.vexstream.playback.observeVexStreamPlaybackPlayerStore
import com.vextreme.vexlife.android.vexstream.queue.VexStreamQueueFamily
import com.vextreme.vexlife.android.vexstream.queue.VexStreamQueueState
import com.vextreme.vexlife.android.vexstream.queue.observeVexStreamQueuePlayerStore
import com.vextreme.vexlife.android.vexstream.repeat.VexStreamRepeatFamily
import com.vextreme.vexlife.android.vexstream.repeat.VexStreamRepeatState
import com.vextreme.vexlife.android.vexstream.repeat.observeVexStreamRepeatPlayerStore
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Test

class VexStreamPlayerFamilySeedTest {
    @Test
    fun familyObservationSeedsRetainedPlayerTruthBeforeAsyncCollectionStarts() {
        val store =
            VexStreamPlayerStore()
        val connectionId =
            VexStreamPlayerPartner.Connection.Id(
                value =
                    "seed-test",
                occurrence =
                    1L,
            )

        store.connectionOpened(
            connectionId =
                connectionId,
            observations =
                observations(
                    connectionId,
                ),
        )

        val playback =
            VexStreamPlaybackFamily()
        val queue =
            VexStreamQueueFamily()
        val repeat =
            VexStreamRepeatFamily()
        val root =
            VexExecutionRoot()

        try {
            observeVexStreamPlaybackPlayerStore(
                family =
                    playback,
                store =
                    store,
                executionRoot =
                    root,
            )
            observeVexStreamQueuePlayerStore(
                family =
                    queue,
                store =
                    store,
                executionRoot =
                    root,
            )
            observeVexStreamRepeatPlayerStore(
                family =
                    repeat,
                store =
                    store,
                executionRoot =
                    root,
            )

            assertEquals(
                VexStreamPlaybackState.Media(
                    playRequested =
                        true,
                    phase =
                        VexStreamPlaybackPhase.Ready,
                    playPauseAvailable =
                        true,
                ),
                playback.currentState,
            )
            assertEquals(
                VexStreamQueueState.NonEmpty(
                    previousAvailable =
                        true,
                    nextAvailable =
                        true,
                    shuffleAvailable =
                        false,
                    clearAvailable =
                        true,
                ),
                queue.currentState,
            )
            assertEquals(
                VexStreamRepeatState.Enabled(
                    toggleAvailable =
                        true,
                ),
                repeat.currentState,
            )
        } finally {
            root.forceStopAll(
                "seed test complete",
            )
        }
    }

    @Test
    fun liveObservationContinuesAfterSynchronousSeed() =
        runBlocking {
            val store =
                VexStreamPlayerStore()
            val connectionId =
                VexStreamPlayerPartner.Connection.Id(
                    value =
                        "live-test",
                    occurrence =
                        1L,
                )
            store.connectionOpened(
                connectionId =
                    connectionId,
                observations =
                    observations(
                        connectionId,
                    ),
            )

            val playback =
                VexStreamPlaybackFamily()
            val queue =
                VexStreamQueueFamily()
            val repeat =
                VexStreamRepeatFamily()
            val root =
                VexExecutionRoot()

            try {
                observeVexStreamPlaybackPlayerStore(
                    family =
                        playback,
                    store =
                        store,
                    executionRoot =
                        root,
                )
                observeVexStreamQueuePlayerStore(
                    family =
                        queue,
                    store =
                        store,
                    executionRoot =
                        root,
                )
                observeVexStreamRepeatPlayerStore(
                    family =
                        repeat,
                    store =
                        store,
                    executionRoot =
                        root,
                )

                store.receive(
                    observations =
                        listOf(
                            VexStreamPlayerPartner.Connection.Observation(
                                connectionId =
                                    connectionId,
                                signal =
                                    VexStreamPlayerPartner.Signal.Playback(
                                        hasMedia =
                                            true,
                                        playRequested =
                                            false,
                                        phase =
                                            VexStreamPlayerPartner.PlaybackPhase.Ready,
                                        playPauseAvailable =
                                            true,
                                        restartAvailable =
                                            true,
                                    ),
                                observedAtEpochMillis =
                                    2L,
                            ),
                            VexStreamPlayerPartner.Connection.Observation(
                                connectionId =
                                    connectionId,
                                signal =
                                    VexStreamPlayerPartner.Signal.QueueNavigation(
                                        currentIndex =
                                            1,
                                        previousAvailable =
                                            false,
                                        nextAvailable =
                                            true,
                                        changeMediaItemsAvailable =
                                            false,
                                    ),
                                observedAtEpochMillis =
                                    2L,
                            ),
                            VexStreamPlayerPartner.Connection.Observation(
                                connectionId =
                                    connectionId,
                                signal =
                                    VexStreamPlayerPartner.Signal.Repeat(
                                        enabled =
                                            false,
                                        toggleAvailable =
                                            false,
                                    ),
                                observedAtEpochMillis =
                                    2L,
                            ),
                        ),
                )

                withTimeout(
                    2_000L,
                ) {
                    while (
                        playback.currentState !=
                            VexStreamPlaybackState.Media(
                                playRequested =
                                    false,
                                phase =
                                    VexStreamPlaybackPhase.Ready,
                                playPauseAvailable =
                                    true,
                            ) ||
                        queue.currentState !=
                            VexStreamQueueState.NonEmpty(
                                previousAvailable =
                                    false,
                                nextAvailable =
                                    true,
                                shuffleAvailable =
                                    false,
                                clearAvailable =
                                    false,
                            ) ||
                        repeat.currentState !=
                            VexStreamRepeatState.Disabled(
                                toggleAvailable =
                                    false,
                            )
                    ) {
                        yield()
                    }
                }
            } finally {
                root.joinStopAll(
                    "live observation seed test complete",
                )
            }
        }

    private fun observations(
        connectionId:
            VexStreamPlayerPartner.Connection.Id,
    ):
        List<
            VexStreamPlayerPartner.Connection.Observation
        > {
        val signals =
            listOf<
                VexStreamPlayerPartner.Signal
            >(
                VexStreamPlayerPartner.Signal.Playback(
                    hasMedia =
                        true,
                    playRequested =
                        true,
                    phase =
                        VexStreamPlayerPartner.PlaybackPhase.Ready,
                    playPauseAvailable =
                        true,
                    restartAvailable =
                        true,
                ),
                VexStreamPlayerPartner.Signal.QueueItems(
                    items =
                        listOf(
                            VexStreamPlayerPartner.Signal.QueueItems.Item(
                                mediaId =
                                    "track.a",
                                title =
                                    "A",
                                artist =
                                    "Artist",
                            ),
                            VexStreamPlayerPartner.Signal.QueueItems.Item(
                                mediaId =
                                    "track.b",
                                title =
                                    "B",
                                artist =
                                    "Artist",
                            ),
                            VexStreamPlayerPartner.Signal.QueueItems.Item(
                                mediaId =
                                    "track.c",
                                title =
                                    "C",
                                artist =
                                    "Artist",
                            ),
                        ),
                ),
                VexStreamPlayerPartner.Signal.QueueNavigation(
                    currentIndex =
                        1,
                    previousAvailable =
                        true,
                    nextAvailable =
                        true,
                    changeMediaItemsAvailable =
                        true,
                ),
                VexStreamPlayerPartner.Signal.Repeat(
                    enabled =
                        true,
                    toggleAvailable =
                        true,
                ),
                VexStreamPlayerPartner.Signal.CurrentMedia(
                    mediaId =
                        "track.b",
                    title =
                        "B",
                    artist =
                        "Artist",
                    artworkUri =
                        null,
                ),
            )

        return signals.map { signal ->
            VexStreamPlayerPartner.Connection.Observation(
                connectionId =
                    connectionId,
                signal =
                    signal,
                observedAtEpochMillis =
                    1L,
            )
        }
    }
}

// [VXG RealForever][VEXSTREAM][PLAYER-STORE][FAMILY-SEED][PROOF]
