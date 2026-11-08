package com.vextreme.vexlife.android.vexstream.player

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class VexStreamPlayerStoreTest {
    @Test
    fun oneProviderOccurrenceConvergesSeveralFacetSignalsIntoOneCurrentEmission() =
        runBlocking {
            val store =
                VexStreamPlayerStore()
            val id =
                id(
                    value =
                        "connection-a",
                    occurrence =
                        1L,
                )
            val emissions =
                mutableListOf<
                    VexStreamPlayerPartner.Current
                >()
            val collector =
                launch(
                    start =
                        CoroutineStart.UNDISPATCHED,
                ) {
                    store
                        .states
                        .drop(
                            1,
                        )
                        .take(
                            2,
                        )
                        .toList(
                            emissions,
                        )
                }

            store.connectionOpened(
                connectionId =
                    id,
                observations =
                    observations(
                        id =
                            id,
                        signals =
                            completeSignals(),
                    ),
            )
            yield()

            store.receive(
                observations(
                    id =
                        id,
                    signals =
                        listOf(
                            playback(
                                playRequested =
                                    true,
                            ),
                            VexStreamPlayerPartner.Signal.Repeat(
                                enabled =
                                    true,
                                toggleAvailable =
                                    true,
                            ),
                        ),
                ),
            )
            yield()

            collector.join()

            assertEquals(
                2,
                emissions.size,
            )
            val second =
                emissions
                    .last() as
                    VexStreamPlayerPartner.Current.Available
            assertTrue(
                second.playback.playRequested,
            )
            assertTrue(
                second.repeat.enabled,
            )
        }

    @Test
    fun playbackUpdateRetainsExactLargeQueueIdentity() {
        val store =
            VexStreamPlayerStore()
        val id =
            id(
                value =
                    "connection-a",
                occurrence =
                    1L,
            )
        val queueItems =
            VexStreamPlayerPartner.Signal.QueueItems(
                items =
                    (0 until 500)
                        .map { index ->
                            VexStreamPlayerPartner.Signal.QueueItems.Item(
                                mediaId =
                                    "song-$index",
                                title =
                                    "Song $index",
                                artist =
                                    "Artist",
                            )
                        },
            )

        store.connectionOpened(
            connectionId =
                id,
            observations =
                observations(
                    id =
                        id,
                    signals =
                        completeSignals(
                            queueItems =
                                queueItems,
                        ),
                ),
        )

        store.receive(
            observations(
                id =
                    id,
                signals =
                    listOf(
                        playback(
                            playRequested =
                                true,
                        ),
                    ),
            ),
        )

        val current =
            store.current as
                VexStreamPlayerPartner.Current.Available

        assertSame(
            queueItems,
            current.queueItems,
        )
        assertSame(
            queueItems.mediaIds,
            current.queueItems.mediaIds,
        )
    }

    @Test
    fun staleConnectionObservationCannotOverwriteCurrentPartnerTruth() {
        val store =
            VexStreamPlayerStore()
        val currentId =
            id(
                value =
                    "current",
                occurrence =
                    2L,
            )
        val staleId =
            id(
                value =
                    "stale",
                occurrence =
                    1L,
            )
        val initialPlayback =
            playback(
                playRequested =
                    false,
            )

        store.connectionOpened(
            connectionId =
                currentId,
            observations =
                observations(
                    id =
                        currentId,
                    signals =
                        completeSignals(
                            playback =
                                initialPlayback,
                        ),
                ),
        )

        val admission =
            store.receive(
                observations(
                    id =
                        staleId,
                    signals =
                        listOf(
                            playback(
                                playRequested =
                                    true,
                            ),
                        ),
                ),
            )

        assertFalse(
            admission.accepted,
        )
        assertEquals(
            initialPlayback,
            (
                store.current as
                    VexStreamPlayerPartner.Current.Available
            ).playback,
        )
    }

    @Test
    fun lateOlderConnectionOpenCannotReplaceNewerTruthEvenAfterNewerClose() {
        val store =
            VexStreamPlayerStore()
        val older =
            id(
                value =
                    "older",
                occurrence =
                    1L,
            )
        val newer =
            id(
                value =
                    "newer",
                occurrence =
                    2L,
            )

        store.connectionOpened(
            connectionId =
                newer,
            observations =
                observations(
                    id =
                        newer,
                    signals =
                        completeSignals(),
                ),
        )
        store.connectionClosed(
            newer,
        )

        val staleOpen =
            store.connectionOpened(
                connectionId =
                    older,
                observations =
                    observations(
                        id =
                            older,
                        signals =
                            completeSignals(
                                playback =
                                    playback(
                                        playRequested =
                                            true,
                                    ),
                            ),
                    ),
            )

        assertFalse(
            staleOpen.accepted,
        )
        assertEquals(
            VexStreamPlayerPartner.Current.Unavailable,
            store.current,
        )
    }

    @Test
    fun oldConnectionCloseCannotEraseNewerConnectionCurrent() {
        val store =
            VexStreamPlayerStore()
        val first =
            id(
                value =
                    "first",
                occurrence =
                    1L,
            )
        val second =
            id(
                value =
                    "second",
                occurrence =
                    2L,
            )

        store.connectionOpened(
            connectionId =
                first,
            observations =
                observations(
                    id =
                        first,
                    signals =
                        completeSignals(),
                ),
        )

        val newerPlayback =
            playback(
                playRequested =
                    true,
            )
        store.connectionOpened(
            connectionId =
                second,
            observations =
                observations(
                    id =
                        second,
                    signals =
                        completeSignals(
                            playback =
                                newerPlayback,
                        ),
                ),
        )

        val oldClose =
            store.connectionClosed(
                first,
            )

        assertFalse(
            oldClose.accepted,
        )
        assertEquals(
            newerPlayback,
            (
                store.current as
                    VexStreamPlayerPartner.Current.Available
            ).playback,
        )

        val currentClose =
            store.connectionClosed(
                second,
            )

        assertTrue(
            currentClose.accepted,
        )
        assertEquals(
            VexStreamPlayerPartner.Current.Unavailable,
            store.current,
        )
    }

    private fun id(
        value:
            String,
        occurrence:
            Long,
    ):
        VexStreamPlayerPartner.Connection.Id =
        VexStreamPlayerPartner.Connection.Id(
            value =
                value,
            occurrence =
                occurrence,
        )

    private fun observations(
        id:
            VexStreamPlayerPartner.Connection.Id,
        signals:
            List<
                VexStreamPlayerPartner.Signal
            >,
    ):
        List<
            VexStreamPlayerPartner.Connection.Observation
        > =
        signals
            .map { signal ->
                VexStreamPlayerPartner.Connection.Observation(
                    connectionId =
                        id,
                    signal =
                        signal,
                    observedAtEpochMillis =
                        123L,
                )
            }

    private fun completeSignals(
        playback:
            VexStreamPlayerPartner.Signal.Playback =
            playback(
                playRequested =
                    false,
            ),
        queueItems:
            VexStreamPlayerPartner.Signal.QueueItems =
            VexStreamPlayerPartner.Signal.QueueItems(
                items =
                    listOf(
                        VexStreamPlayerPartner.Signal.QueueItems.Item(
                            mediaId =
                                "song-a",
                            title =
                                "Song A",
                            artist =
                                "Artist",
                        ),
                    ),
            ),
    ):
        List<
            VexStreamPlayerPartner.Signal
        > =
        listOf(
            playback,
            queueItems,
            VexStreamPlayerPartner.Signal.QueueNavigation(
                currentIndex =
                    if (
                        queueItems.mediaIds.isEmpty()
                    ) {
                        -1
                    } else {
                        0
                    },
                previousAvailable =
                    false,
                nextAvailable =
                    false,
                shuffleAvailable =
                    false,
                clearAvailable =
                    queueItems.mediaIds.isNotEmpty(),
            ),
            VexStreamPlayerPartner.Signal.Repeat(
                enabled =
                    false,
                toggleAvailable =
                    true,
            ),
            VexStreamPlayerPartner.Signal.CurrentMedia(
                mediaId =
                    queueItems.mediaIds
                        .firstOrNull(),
                title =
                    "",
                artist =
                    "",
                artworkUri =
                    null,
            ),
        )

    private fun playback(
        playRequested:
            Boolean,
    ):
        VexStreamPlayerPartner.Signal.Playback =
        VexStreamPlayerPartner.Signal.Playback(
            hasMedia =
                true,
            playRequested =
                playRequested,
            phase =
                VexStreamPlayerPartner.PlaybackPhase.Ready,
            playPauseAvailable =
                true,
            restartAvailable =
                true,
        )
}

// [VXG RealForever][VEXSTREAM][PLAYER-STORE][PROOF]
