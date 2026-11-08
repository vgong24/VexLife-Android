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
                VexStreamTrackPlaybackExecution(
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
                    realization =
                        realization(
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
                VexStreamTrackPlaybackExecution(
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
                    realization =
                        realization(
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
                VexStreamTrackPlaybackExecution(
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
                    realization =
                        realization(
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
                VexStreamTrackPlaybackExecution(
                    root =
                        root,
                    resolver =
                        VexStreamTrackPlaybackResolver(
                            ::media,
                        ),
                    realization =
                        realization(
                            onQueue = { _, media ->
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

    @Test
    fun playAdmissionRequiresEveryProviderCommandActuallyUsed() {
        assertTrue(
            vexStreamTrackPlayCommandsAvailable(
                changeMediaItemsAvailable = true,
                prepareAvailable = true,
                playAvailable = true,
                shuffleEnabled = false,
                setShuffleModeAvailable = false,
            ),
        )
        assertFalse(
            vexStreamTrackPlayCommandsAvailable(
                changeMediaItemsAvailable = false,
                prepareAvailable = true,
                playAvailable = true,
                shuffleEnabled = false,
                setShuffleModeAvailable = true,
            ),
        )
        assertFalse(
            vexStreamTrackPlayCommandsAvailable(
                changeMediaItemsAvailable = true,
                prepareAvailable = true,
                playAvailable = true,
                shuffleEnabled = true,
                setShuffleModeAvailable = false,
            ),
        )
    }

    @Test
    fun playNextStartsPlaybackOnlyWhenEmptyQueueCommandsAreAvailable() {
        assertEquals(
            VexStreamTrackQueueTarget.StartPlayback,
            vexStreamTrackQueueTarget(
                placement =
                    VexStreamTrackQueuePlacement.Next,
                mediaItemCount =
                    0,
                currentMediaItemIndex =
                    0,
                timelineAvailable =
                    true,
                setMediaItemAvailable =
                    true,
                changeMediaItemsAvailable =
                    false,
                prepareAvailable =
                    true,
                playAvailable =
                    true,
            ),
        )
    }

    @Test
    fun playNextNeedsTimelineAndInsertsAfterCurrentItem() {
        assertEquals(
            VexStreamTrackQueueTarget.Insert(
                index =
                    2,
            ),
            vexStreamTrackQueueTarget(
                placement =
                    VexStreamTrackQueuePlacement.Next,
                mediaItemCount =
                    4,
                currentMediaItemIndex =
                    1,
                timelineAvailable =
                    true,
                setMediaItemAvailable =
                    false,
                changeMediaItemsAvailable =
                    true,
                prepareAvailable =
                    false,
                playAvailable =
                    false,
            ),
        )
    }

    @Test
    fun addToQueueNeedsOnlyAppendAuthorityAfterSourceResolution() {
        assertEquals(
            VexStreamTrackQueueTarget.Append,
            vexStreamTrackQueueTarget(
                placement =
                    VexStreamTrackQueuePlacement.End,
                mediaItemCount =
                    null,
                currentMediaItemIndex =
                    null,
                timelineAvailable =
                    false,
                setMediaItemAvailable =
                    false,
                changeMediaItemsAvailable =
                    true,
                prepareAvailable =
                    false,
                playAvailable =
                    false,
            ),
        )
    }

    private fun realization(
        onPlay:
            suspend (
                ResolvedTrackPlay,
            ) -> Unit =
            {},
        onQueue:
            suspend (
                VexStreamTrackQueueRequest,
                VexStreamPlayerPartner.Media,
            ) -> Unit =
            { _, _ -> },
    ):
        VexStreamTrackPlaybackRealization =
        object :
            VexStreamTrackPlaybackRealization {
            override val isTrackPlayAvailable:
                Boolean =
                true

            override fun isTrackQueueAvailable(
                request:
                    VexStreamTrackQueueRequest,
            ):
                Boolean =
                true

            override suspend fun realizeTrackPlay(
                resolved:
                    ResolvedTrackPlay,
            ):
                Boolean {
                onPlay(
                    resolved,
                )
                return true
            }

            override suspend fun realizeTrackQueue(
                request:
                    VexStreamTrackQueueRequest,
                media:
                    VexStreamPlayerPartner.Media,
            ):
                Boolean {
                onQueue(
                    request,
                    media,
                )
                return true
            }
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
