package com.vextreme.vexlife.android.vexstream.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class VexStreamPlayerStoreTest {
    @Test
    fun timelineUpdateDoesNotRebuildLargeQueueIdentity() {
        val store =
            VexStreamPlayerStore()
        val id =
            VexStreamPlayerPartner.Connection.Id(
                "connection-a",
            )
        val queueItems =
            VexStreamPlayerPartner.Signal.QueueItems(
                mediaIds =
                    (0 until 500)
                        .map { index ->
                            "song-$index"
                        },
            )
        val initial =
            available(
                queueItems =
                    queueItems,
                timeline =
                    timeline(
                        positionMs =
                            1_000L,
                    ),
            )

        store.receiveNow(
            observation(
                id =
                    id,
                signal =
                    VexStreamPlayerPartner.Signal
                        .Snapshot(
                            initial,
                        ),
            ),
        )

        store.receiveNow(
            observation(
                id =
                    id,
                signal =
                    timeline(
                        positionMs =
                            2_000L,
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
        assertEquals(
            2_000L,
            current.timeline.positionMs,
        )
    }

    @Test
    fun staleConnectionObservationCannotOverwriteCurrentPartnerTruth() {
        val store =
            VexStreamPlayerStore()
        val currentId =
            VexStreamPlayerPartner.Connection.Id(
                "current",
            )
        val staleId =
            VexStreamPlayerPartner.Connection.Id(
                "stale",
            )
        val initial =
            available(
                playback =
                    playback(
                        playRequested =
                            false,
                    ),
            )

        store.receiveNow(
            observation(
                id =
                    currentId,
                signal =
                    VexStreamPlayerPartner.Signal
                        .Snapshot(
                            initial,
                        ),
            ),
        )

        val admission =
            store.receiveNow(
                observation(
                    id =
                        staleId,
                    signal =
                        playback(
                            playRequested =
                                true,
                        ),
                ),
            )

        assertFalse(
            admission.accepted,
        )
        assertEquals(
            initial,
            store.current,
        )
    }

    @Test
    fun oldConnectionCloseCannotEraseNewerSnapshot() {
        val store =
            VexStreamPlayerStore()
        val first =
            VexStreamPlayerPartner.Connection.Id(
                "first",
            )
        val second =
            VexStreamPlayerPartner.Connection.Id(
                "second",
            )

        store.receiveNow(
            observation(
                id =
                    first,
                signal =
                    VexStreamPlayerPartner.Signal
                        .Snapshot(
                            available(
                                playback =
                                    playback(
                                        playRequested =
                                            false,
                                    ),
                            ),
                        ),
            ),
        )
        val newer =
            available(
                playback =
                    playback(
                        playRequested =
                            true,
                    ),
            )
        store.receiveNow(
            observation(
                id =
                    second,
                signal =
                    VexStreamPlayerPartner.Signal
                        .Snapshot(
                            newer,
                        ),
            ),
        )

        val afterOldClose =
            store.connectionClosed(
                first,
            )

        assertEquals(
            newer,
            afterOldClose,
        )
        assertTrue(
            afterOldClose is
                VexStreamPlayerPartner.Current.Available,
        )

        assertEquals(
            VexStreamPlayerPartner.Current.Unavailable,
            store.connectionClosed(
                second,
            ),
        )
    }

    private fun observation(
        id:
            VexStreamPlayerPartner.Connection.Id,
        signal:
            VexStreamPlayerPartner.Signal,
    ):
        VexStreamPlayerPartner.Connection.Observation =
        VexStreamPlayerPartner.Connection.Observation(
            connectionId =
                id,
            signal =
                signal,
            observedAtEpochMillis =
                123L,
        )

    private fun available(
        playback:
            VexStreamPlayerPartner.Signal.Playback =
            playback(
                playRequested =
                    false,
            ),
        queueItems:
            VexStreamPlayerPartner.Signal.QueueItems =
            VexStreamPlayerPartner.Signal.QueueItems(
                mediaIds =
                    emptyList(),
            ),
        timeline:
            VexStreamPlayerPartner.Signal.Timeline =
            timeline(
                positionMs =
                    0L,
            ),
    ):
        VexStreamPlayerPartner.Current.Available =
        VexStreamPlayerPartner.Current.Available(
            playback =
                playback,
            queueItems =
                queueItems,
            queueNavigation =
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
            repeat =
                VexStreamPlayerPartner.Signal.Repeat(
                    enabled =
                        false,
                    toggleAvailable =
                        true,
                ),
            currentMedia =
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
            timeline =
                timeline,
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
                VexStreamPlayerPartner
                    .PlaybackPhase.Ready,
            playPauseAvailable =
                true,
            restartAvailable =
                true,
        )

    private fun timeline(
        positionMs:
            Long,
    ):
        VexStreamPlayerPartner.Signal.Timeline =
        VexStreamPlayerPartner.Signal.Timeline(
            positionMs =
                positionMs,
            durationMs =
                10_000L,
            bufferedPositionMs =
                5_000L,
        )
}

// [VXG RealForever][VEXSTREAM][PLAYER-STORE][PROOF]
