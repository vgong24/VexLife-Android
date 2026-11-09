package com.vextreme.vexlife.android.vexstream.track

import com.vextreme.vexlife.android.data.vexstream.VexStreamMediaSha256
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrack
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackRef
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackState
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerPartner
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VexStreamTrackPlaybackTest {
    @Test
    fun executionResolvesVexMediaThenHandsItToPlayerRealization() =
        runBlocking {
            val executor =
                Executors.newSingleThreadExecutor { runnable ->
                    Thread(
                        runnable,
                        "vex-track-worker",
                    )
                }
            val dispatcher =
                executor.asCoroutineDispatcher()
            val root =
                VexExecutionRoot(
                    dispatcher =
                        dispatcher,
                )
            val resolverThreads =
                CopyOnWriteArrayList<String>()
            val realized =
                CopyOnWriteArrayList<String>()
            val execution =
                VexStreamTrackPlayback(
                    root =
                        root,
                    resolver =
                        VexStreamTrackPlaybackResolver { state ->
                            resolverThreads +=
                                Thread.currentThread().name
                            media(
                                state,
                            )
                        },
                    connection =
                        requestConnection(
                            onPlay = { resolved ->
                                realized +=
                                    resolved.items.map {
                                        it.mediaId
                                    }
                            },
                        ),
                )
            val alpha =
                track(
                    "track.alpha",
                )
            val beta =
                track(
                    "track.beta",
                )

            try {
                assertTrue(
                    execution.play(
                        VexStreamTrackPlayRequest(
                            selected =
                                beta,
                            contextTracks =
                                listOf(
                                    alpha,
                                    beta,
                                ),
                        ),
                    ),
                )

                withTimeout(
                    2_000L,
                ) {
                    while (
                        realized.isEmpty()
                    ) {
                        yield()
                    }
                }

                assertEquals(
                    2,
                    resolverThreads.size,
                )
                assertTrue(
                    resolverThreads.all {
                        it.startsWith(
                            "vex-track-worker",
                        )
                    },
                )
                assertEquals(
                    listOf(
                        "track.alpha",
                        "track.beta",
                    ),
                    realized.toList(),
                )
            } finally {
                execution.shutdown()
                root.joinStopAll(
                    "test complete",
                )
                dispatcher.close()
                executor.shutdownNow()
            }
        }

    @Test
    fun selectedArtworkPreparationPrecedesPlayerRealization() =
        runBlocking {
            val executor =
                Executors.newSingleThreadExecutor()
            val dispatcher =
                executor.asCoroutineDispatcher()
            val root =
                VexExecutionRoot(
                    dispatcher =
                        dispatcher,
                )
            val events =
                CopyOnWriteArrayList<String>()
            val execution =
                VexStreamTrackPlayback(
                    root =
                        root,
                    resolver =
                        object :
                            VexStreamTrackPlaybackResolver {
                            override fun media(
                                state:
                                    VexStreamTrackState,
                            ):
                                VexStreamPlayerPartner.Media {
                                events +=
                                    "resolve:" +
                                        state.track
                                            .trackRef
                                            .value
                                return media(
                                    state,
                                )
                            }

                            override suspend fun prepareArtwork(
                                state:
                                    VexStreamTrackState,
                            ) {
                                events +=
                                    "prepare:" +
                                        state.track
                                            .trackRef
                                            .value
                            }
                        },
                    connection =
                        requestConnection(
                            onPlay = { resolved ->
                                events +=
                                    "realize:" +
                                        resolved.items[
                                            resolved.selectedIndex
                                        ].mediaId
                            },
                        ),
                )
            val selected =
                track(
                    "track.selected",
                )

            try {
                execution.play(
                    VexStreamTrackPlayRequest(
                        selected =
                            selected,
                        contextTracks =
                            listOf(
                                selected,
                            ),
                    ),
                )

                withTimeout(
                    2_000L,
                ) {
                    while (
                        events.none {
                            it.startsWith(
                                "realize:",
                            )
                        }
                    ) {
                        yield()
                    }
                }

                assertTrue(
                    events.indexOf(
                        "prepare:track.selected",
                    ) <
                        events.indexOf(
                            "realize:track.selected",
                        ),
                )
            } finally {
                execution.shutdown()
                root.joinStopAll(
                    "test complete",
                )
                dispatcher.close()
                executor.shutdownNow()
            }
        }

    @Test
    fun newerPlaySelectionSupersedesPreparedOlderSelection() =
        runBlocking {
            val executor =
                Executors.newSingleThreadExecutor()
            val dispatcher =
                executor.asCoroutineDispatcher()
            val root =
                VexExecutionRoot(
                    dispatcher =
                        dispatcher,
                )
            val firstResolverEntered =
                CountDownLatch(
                    1,
                )
            val releaseFirstResolver =
                CountDownLatch(
                    1,
                )
            val realized =
                CopyOnWriteArrayList<String>()
            val execution =
                VexStreamTrackPlayback(
                    root =
                        root,
                    resolver =
                        VexStreamTrackPlaybackResolver { state ->
                            if (
                                state.track
                                    .trackRef
                                    .value ==
                                "track.first"
                            ) {
                                firstResolverEntered
                                    .countDown()
                                check(
                                    releaseFirstResolver.await(
                                        2,
                                        TimeUnit.SECONDS,
                                    ),
                                )
                            }
                            media(
                                state,
                            )
                        },
                    connection =
                        requestConnection(
                            onPlay = { resolved ->
                                realized +=
                                    resolved.items[
                                        resolved.selectedIndex
                                    ].mediaId
                            },
                        ),
                )

            try {
                execution.play(
                    VexStreamTrackPlayRequest(
                        selected =
                            track(
                                "track.first",
                            ),
                        contextTracks =
                            listOf(
                                track(
                                    "track.first",
                                ),
                            ),
                    ),
                )
                assertTrue(
                    firstResolverEntered.await(
                        2,
                        TimeUnit.SECONDS,
                    ),
                )
                execution.play(
                    VexStreamTrackPlayRequest(
                        selected =
                            track(
                                "track.second",
                            ),
                        contextTracks =
                            listOf(
                                track(
                                    "track.second",
                                ),
                            ),
                    ),
                )
                releaseFirstResolver
                    .countDown()

                withTimeout(
                    2_000L,
                ) {
                    while (
                        realized.isEmpty()
                    ) {
                        yield()
                    }
                }

                assertEquals(
                    listOf(
                        "track.second",
                    ),
                    realized.toList(),
                )
            } finally {
                execution.shutdown()
                root.joinStopAll(
                    "test complete",
                )
                dispatcher.close()
                executor.shutdownNow()
            }
        }

    @Test
    fun queueRequestsRemainOrdered() =
        runBlocking {
            val root =
                VexExecutionRoot()
            val realized =
                CopyOnWriteArrayList<String>()
            val execution =
                VexStreamTrackPlayback(
                    root =
                        root,
                    resolver =
                        VexStreamTrackPlaybackResolver(
                            ::media,
                        ),
                    connection =
                        requestConnection(
                            onQueue = { media ->
                                realized +=
                                    media.mediaId
                            },
                        ),
                )

            try {
                execution.queue(
                    VexStreamTrackQueueRequest(
                        track =
                            track(
                                "track.queue.first",
                            ),
                        placement =
                            VexStreamTrackQueuePlacement.End,
                    ),
                )
                execution.queue(
                    VexStreamTrackQueueRequest(
                        track =
                            track(
                                "track.queue.second",
                            ),
                        placement =
                            VexStreamTrackQueuePlacement.End,
                    ),
                )

                withTimeout(
                    2_000L,
                ) {
                    while (
                        realized.size <
                        2
                    ) {
                        yield()
                    }
                }

                assertEquals(
                    listOf(
                        "track.queue.first",
                        "track.queue.second",
                    ),
                    realized.toList(),
                )
            } finally {
                execution.shutdown()
                root.joinStopAll(
                    "test complete",
                )
            }
        }

    @Test
    fun playTargetFollowsResolvedContextInsteadOfRenderedIndex() {
        val alpha =
            track(
                "track.alpha",
            )
        val beta =
            track(
                "track.beta",
            )
        val gamma =
            track(
                "track.gamma",
            )
        val request =
            VexStreamTrackPlayRequest(
                selected =
                    beta,
                contextTracks =
                    listOf(
                        alpha,
                        beta,
                        gamma,
                    ),
            )

        assertEquals(
            0,
            vexStreamTrackPlayTargetIndex(
                request =
                    request,
                resolvedMediaIds =
                    listOf(
                        "track.beta",
                        "track.gamma",
                    ),
            ),
        )
        assertNull(
            vexStreamTrackPlayTargetIndex(
                request =
                    request,
                resolvedMediaIds =
                    listOf(
                        "track.alpha",
                        "track.gamma",
                    ),
            ),
        )
    }

    @Test
    fun sourceAvailabilityUsesProjectedTrackTruthWithoutByteVerification() {
        assertTrue(
            vexStreamTrackSourceProjectedAvailable(
                track(
                    "track.hub",
                ),
            ),
        )
        assertFalse(
            vexStreamTrackSourceProjectedAvailable(
                track(
                    ref =
                        "track.unavailable",
                    hubAvailable =
                        false,
                ),
            ),
        )
    }

    private fun requestConnection(
        onPlay:
            suspend (
                ResolvedTrackPlay,
            ) -> Unit =
            {},
        onQueue:
            suspend (
                VexStreamPlayerPartner.Media,
            ) -> Unit =
            {},
    ):
        VexStreamPlayerPartner.Connection.Request =
        object :
            VexStreamPlayerPartner.Connection.Request {
            override val id =
                VexStreamPlayerPartner.Connection.Id(
                    value =
                        "track-test-controller",
                    occurrence =
                        1L,
                )

            override val mediaPlaybackAvailable =
                true

            override suspend fun playMedia(
                items:
                    List<
                        VexStreamPlayerPartner.Media
                    >,
                selectedIndex:
                    Int,
            ):
                Boolean {
                onPlay(
                    ResolvedTrackPlay(
                        items =
                            items,
                        selectedIndex =
                            selectedIndex,
                    ),
                )
                return true
            }

            override fun mediaQueueAvailable(
                placement:
                    VexStreamPlayerPartner.QueuePlacement,
            ) =
                true

            override suspend fun queueMedia(
                media:
                    VexStreamPlayerPartner.Media,
                placement:
                    VexStreamPlayerPartner.QueuePlacement,
            ):
                Boolean {
                onQueue(
                    media,
                )
                return true
            }

            override suspend fun play() = Unit
            override suspend fun pause() = Unit
            override suspend fun restartCurrent() = Unit
            override suspend fun previous() = Unit
            override suspend fun next() = Unit
            override suspend fun shuffleFuture() = Unit
            override suspend fun clearQueue() = Unit
            override suspend fun setRepeatEnabled(
                enabled:
                    Boolean,
            ) = Unit

            override fun playQueueOccurrence(
                occurrence:
                    VexStreamPlayerPartner.QueueOccurrence,
            ) =
                true

            override fun moveQueueOccurrence(
                occurrence:
                    VexStreamPlayerPartner.QueueOccurrence,
                move:
                    VexStreamPlayerPartner.QueueMove,
            ) =
                true

            override fun seek(
                mediaId:
                    String,
                mediaItemIndex:
                    Int,
                positionMs:
                    Long,
            ) =
                true
        }

    private fun media(
        state:
            VexStreamTrackState,
    ):
        VexStreamPlayerPartner.Media =
        VexStreamPlayerPartner.Media(
            mediaId =
                state.track
                    .trackRef
                    .value,
            sourceRef =
                "vexstream://media/" +
                    state.track
                        .trackRef
                        .value,
            mimeType =
                "audio/mpeg",
            title =
                state.track.title,
            artist =
                state.track.artist,
            album =
                state.track.album,
            artworkRef =
                null,
            playbackStartMs =
                0L,
            playbackEndMs =
                null,
        )

    private fun track(
        ref:
            String,
        hubAvailable:
            Boolean =
            true,
    ):
        VexStreamTrackState =
        VexStreamTrackState(
            track =
                VexStreamTrack(
                    trackRef =
                        VexStreamTrackRef(
                            ref,
                        ),
                    mediaSha256 =
                        VexStreamMediaSha256(
                            "a".repeat(
                                64,
                            ),
                        ),
                    mediaByteLength =
                        1L,
                    title =
                        ref,
                    artist =
                        "Vex",
                ),
            hubAvailable =
                hubAvailable,
        )
}

// [VXG RealForever][VEXSTREAM][TRACK][PLAYER-PARTNER]
