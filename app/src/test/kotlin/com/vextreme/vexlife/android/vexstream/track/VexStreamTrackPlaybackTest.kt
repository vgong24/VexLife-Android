package com.vextreme.vexlife.android.vexstream.track

import androidx.media3.common.MediaItem
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import com.vextreme.vexlife.android.data.vexstream.VexStreamMediaSha256
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrack
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackRef
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VexStreamTrackPlaybackTest {
    @Test
    fun trackExecutionResolvesOnWorkerAndCommitsOnProviderDispatcher(): Unit =
        runBlocking {
            val workerExecutor =
                Executors.newSingleThreadExecutor { runnable ->
                    Thread(
                        runnable,
                        "vex-track-worker",
                    )
                }
            val providerExecutor =
                Executors.newSingleThreadExecutor { runnable ->
                    Thread(
                        runnable,
                        "vex-media-provider",
                    )
                }
            val workerDispatcher =
                workerExecutor.asCoroutineDispatcher()
            val providerDispatcher =
                providerExecutor.asCoroutineDispatcher()
            val root =
                VexExecutionRoot(
                    dispatcher =
                        workerDispatcher,
                )
            val resolverThreads =
                CopyOnWriteArrayList<String>()
            val committedMediaIds =
                CopyOnWriteArrayList<String>()
            val commitThreads =
                CopyOnWriteArrayList<String>()
            val execution =
                VexStreamTrackPlaybackExecution(
                    root = root,
                    providerDispatcher =
                        providerDispatcher,
                    resolver =
                        VexStreamTrackMediaItemResolver { state ->
                            resolverThreads +=
                                Thread.currentThread().name
                            MediaItem.Builder()
                                .setMediaId(
                                    state.track
                                        .trackRef
                                        .value,
                                )
                                .build()
                        },
                    commitPlay = { resolved ->
                        commitThreads +=
                            Thread.currentThread().name
                        committedMediaIds +=
                            resolved.items.map {
                                it.mediaId
                            }
                        true
                    },
                    commitQueue = { _, _ ->
                        true
                    },
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
                            selected = beta,
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
                        commitThreads.isEmpty()
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
                    1,
                    commitThreads.size,
                )
                assertTrue(
                    commitThreads.all {
                        it.startsWith(
                            "vex-media-provider",
                        )
                    },
                )
                assertFalse(
                    resolverThreads.any {
                        workerThread ->
                            commitThreads.any {
                                providerThread ->
                                workerThread ==
                                    providerThread
                            }
                    },
                )
                assertEquals(
                    listOf(
                        "track.alpha",
                        "track.beta",
                    ),
                    committedMediaIds.toList(),
                )
            } finally {
                root.joinStopAll(
                    "test complete",
                )
                workerDispatcher.close()
                providerDispatcher.close()
                workerExecutor.shutdownNow()
                providerExecutor.shutdownNow()
            }
        }

    @Test
    fun newerPlaySelectionSupersedesPreparedOlderSelection(): Unit =
        runBlocking {
            val workerExecutor =
                Executors.newSingleThreadExecutor()
            val providerExecutor =
                Executors.newSingleThreadExecutor()
            val workerDispatcher =
                workerExecutor.asCoroutineDispatcher()
            val providerDispatcher =
                providerExecutor.asCoroutineDispatcher()
            val root =
                VexExecutionRoot(
                    dispatcher =
                        workerDispatcher,
                )
            val firstResolverEntered =
                CountDownLatch(
                    1,
                )
            val releaseFirstResolver =
                CountDownLatch(
                    1,
                )
            val committed =
                CopyOnWriteArrayList<String>()
            val execution =
                VexStreamTrackPlaybackExecution(
                    root = root,
                    providerDispatcher =
                        providerDispatcher,
                    resolver =
                        VexStreamTrackMediaItemResolver { state ->
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
                            MediaItem.Builder()
                                .setMediaId(
                                    state.track
                                        .trackRef
                                        .value,
                                )
                                .build()
                        },
                    commitPlay = { resolved ->
                        committed +=
                            resolved.items[
                                resolved.selectedIndex
                            ].mediaId
                        true
                    },
                    commitQueue = { _, _ ->
                        true
                    },
                )
            val first =
                track(
                    "track.first",
                )
            val second =
                track(
                    "track.second",
                )

            try {
                assertTrue(
                    execution.play(
                        VexStreamTrackPlayRequest(
                            selected = first,
                            contextTracks =
                                listOf(
                                    first,
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

                assertTrue(
                    execution.play(
                        VexStreamTrackPlayRequest(
                            selected = second,
                            contextTracks =
                                listOf(
                                    second,
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
                        committed.isEmpty()
                    ) {
                        yield()
                    }
                }

                assertEquals(
                    listOf(
                        "track.second",
                    ),
                    committed.toList(),
                )
            } finally {
                execution.shutdown()
                root.joinStopAll(
                    "test complete",
                )
                workerDispatcher.close()
                providerDispatcher.close()
                workerExecutor.shutdownNow()
                providerExecutor.shutdownNow()
            }
        }

    @Test
    fun queueRequestsRemainOrderedWhileResolutionRunsOffThread(): Unit =
        runBlocking {
            val workerExecutor =
                Executors.newSingleThreadExecutor()
            val providerExecutor =
                Executors.newSingleThreadExecutor()
            val workerDispatcher =
                workerExecutor.asCoroutineDispatcher()
            val providerDispatcher =
                providerExecutor.asCoroutineDispatcher()
            val root =
                VexExecutionRoot(
                    dispatcher =
                        workerDispatcher,
                )
            val committed =
                CopyOnWriteArrayList<String>()
            val execution =
                VexStreamTrackPlaybackExecution(
                    root = root,
                    providerDispatcher =
                        providerDispatcher,
                    resolver =
                        VexStreamTrackMediaItemResolver { state ->
                            MediaItem.Builder()
                                .setMediaId(
                                    state.track
                                        .trackRef
                                        .value,
                                )
                                .build()
                        },
                    commitPlay = {
                        true
                    },
                    commitQueue = { _, item ->
                        committed +=
                            item.mediaId
                        true
                    },
                )
            val first =
                track(
                    "track.queue.first",
                )
            val second =
                track(
                    "track.queue.second",
                )

            try {
                assertTrue(
                    execution.queue(
                        VexStreamTrackQueueRequest(
                            track = first,
                            placement =
                                VexStreamTrackQueuePlacement.End,
                        ),
                    ),
                )
                assertTrue(
                    execution.queue(
                        VexStreamTrackQueueRequest(
                            track = second,
                            placement =
                                VexStreamTrackQueuePlacement.End,
                        ),
                    ),
                )

                withTimeout(
                    2_000L,
                ) {
                    while (
                        committed.size <
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
                    committed.toList(),
                )
            } finally {
                execution.shutdown()
                root.joinStopAll(
                    "test complete",
                )
                workerDispatcher.close()
                providerDispatcher.close()
                workerExecutor.shutdownNow()
                providerExecutor.shutdownNow()
            }
        }

    @Test
    fun playTargetFollowsResolvedContextInsteadOfRenderedIndex() {
        val alpha = track("track.alpha")
        val beta = track("track.beta")
        val gamma = track("track.gamma")
        val request =
            VexStreamTrackPlayRequest(
                selected = beta,
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
                request = request,
                resolvedMediaIds =
                    listOf(
                        "track.beta",
                        "track.gamma",
                    ),
            ),
        )
        assertNull(
            vexStreamTrackPlayTargetIndex(
                request = request,
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
                    ref = "track.unavailable",
                    hubAvailable = false,
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
        assertTrue(
            vexStreamTrackPlayCommandsAvailable(
                changeMediaItemsAvailable = true,
                prepareAvailable = true,
                playAvailable = true,
                shuffleEnabled = true,
                setShuffleModeAvailable = true,
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
                mediaItemCount = 0,
                currentMediaItemIndex = 0,
                timelineAvailable = true,
                setMediaItemAvailable = true,
                changeMediaItemsAvailable = false,
                prepareAvailable = true,
                playAvailable = true,
            ),
        )
        assertNull(
            vexStreamTrackQueueTarget(
                placement =
                    VexStreamTrackQueuePlacement.Next,
                mediaItemCount = 0,
                currentMediaItemIndex = 0,
                timelineAvailable = true,
                setMediaItemAvailable = true,
                changeMediaItemsAvailable = true,
                prepareAvailable = false,
                playAvailable = true,
            ),
        )
    }

    @Test
    fun playNextNeedsTimelineAndInsertsAfterCurrentItem() {
        assertEquals(
            VexStreamTrackQueueTarget.Insert(
                index = 2,
            ),
            vexStreamTrackQueueTarget(
                placement =
                    VexStreamTrackQueuePlacement.Next,
                mediaItemCount = 4,
                currentMediaItemIndex = 1,
                timelineAvailable = true,
                setMediaItemAvailable = false,
                changeMediaItemsAvailable = true,
                prepareAvailable = false,
                playAvailable = false,
            ),
        )
        assertNull(
            vexStreamTrackQueueTarget(
                placement =
                    VexStreamTrackQueuePlacement.Next,
                mediaItemCount = null,
                currentMediaItemIndex = null,
                timelineAvailable = false,
                setMediaItemAvailable = true,
                changeMediaItemsAvailable = true,
                prepareAvailable = true,
                playAvailable = true,
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
                mediaItemCount = null,
                currentMediaItemIndex = null,
                timelineAvailable = false,
                setMediaItemAvailable = false,
                changeMediaItemsAvailable = true,
                prepareAvailable = false,
                playAvailable = false,
            ),
        )
        assertNull(
            vexStreamTrackQueueTarget(
                placement =
                    VexStreamTrackQueuePlacement.End,
                mediaItemCount = null,
                currentMediaItemIndex = null,
                timelineAvailable = false,
                setMediaItemAvailable = true,
                changeMediaItemsAvailable = false,
                prepareAvailable = true,
                playAvailable = true,
            ),
        )
    }

    private fun track(
        ref: String,
        hubAvailable: Boolean = true,
    ): VexStreamTrackState =
        VexStreamTrackState(
            track =
                VexStreamTrack(
                    trackRef =
                        VexStreamTrackRef(
                            ref,
                        ),
                    mediaSha256 =
                        VexStreamMediaSha256(
                            "a".repeat(64),
                        ),
                    mediaByteLength = 1L,
                    title = ref,
                    artist = "Vex",
                ),
            hubAvailable = hubAvailable,
        )
}

// [VXG RealForever]
