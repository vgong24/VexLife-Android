package com.vextreme.vexlife.android.vexstream.collection

import com.vextreme.vexlife.android.data.vexstream.VexStreamMediaSha256
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrack
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackRef
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackState
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerPartner
import com.vextreme.vexlife.android.vexstream.track.VexStreamTrackPlaybackResolver
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VexStreamCollectionPlaybackTest {
    @Test
    fun projectedAvailabilityNeedsAtLeastOneCurrentlyAvailableTrack() {
        assertFalse(
            vexStreamCollectionSourceProjectedAvailable(
                emptyList(),
            ),
        )
        assertFalse(
            vexStreamCollectionSourceProjectedAvailable(
                listOf(
                    track(
                        ref =
                            "track.offline",
                        hubAvailable =
                            false,
                    ),
                ),
            ),
        )
        assertTrue(
            vexStreamCollectionSourceProjectedAvailable(
                listOf(
                    track(
                        ref =
                            "track.offline",
                        hubAvailable =
                            false,
                    ),
                    track(
                        ref =
                            "track.hub",
                    ),
                ),
            ),
        )
    }

    @Test
    fun collectionExecutionPreparesCurrentArtworkThenHandsVexMediaToPlayerRealization() =
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
                VexStreamCollectionPlayback(
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
                        requestConnection { resolved ->
                            events +=
                                "realize:" +
                                    resolved.items
                                        .joinToString(
                                            separator =
                                                ",",
                                        ) {
                                            it.mediaId
                                        }
                        },
                )

            try {
                execution.play(
                    VexStreamCollectionPlaybackRequest(
                        tracks =
                            listOf(
                                track(
                                    "track.alpha",
                                ),
                                track(
                                    "track.beta",
                                ),
                            ),
                        order =
                            VexStreamCollectionPlaybackOrder
                                .Natural,
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

                assertEquals(
                    1,
                    events.count {
                        it.startsWith(
                            "prepare:",
                        )
                    },
                )
                assertTrue(
                    events.indexOf(
                        "prepare:track.alpha",
                    ) <
                        events.indexOf(
                            "realize:track.alpha,track.beta",
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
    fun newerCollectionPlaySupersedesOlderArtworkPreparation() =
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
            val firstPreparationEntered =
                CountDownLatch(
                    1,
                )
            val releaseFirstPreparation =
                CountDownLatch(
                    1,
                )
            val realized =
                CopyOnWriteArrayList<String>()
            val execution =
                VexStreamCollectionPlayback(
                    root =
                        root,
                    resolver =
                        object :
                            VexStreamTrackPlaybackResolver {
                            override fun media(
                                state:
                                    VexStreamTrackState,
                            ):
                                VexStreamPlayerPartner.Media =
                                media(
                                    state,
                                )

                            override suspend fun prepareArtwork(
                                state:
                                    VexStreamTrackState,
                            ) {
                                if (
                                    state.track
                                        .trackRef
                                        .value ==
                                    "track.first"
                                ) {
                                    firstPreparationEntered
                                        .countDown()
                                    check(
                                        releaseFirstPreparation.await(
                                            2,
                                            TimeUnit.SECONDS,
                                        ),
                                    )
                                }
                            }
                        },
                    connection =
                        requestConnection { resolved ->
                            realized +=
                                resolved.items
                                    .first()
                                    .mediaId
                        },
                )

            try {
                execution.play(
                    VexStreamCollectionPlaybackRequest(
                        tracks =
                            listOf(
                                track(
                                    "track.first",
                                ),
                            ),
                        order =
                            VexStreamCollectionPlaybackOrder
                                .Natural,
                    ),
                )
                assertTrue(
                    firstPreparationEntered.await(
                        2,
                        TimeUnit.SECONDS,
                    ),
                )

                execution.play(
                    VexStreamCollectionPlaybackRequest(
                        tracks =
                            listOf(
                                track(
                                    "track.second",
                                ),
                            ),
                        order =
                            VexStreamCollectionPlaybackOrder
                                .Natural,
                    ),
                )
                releaseFirstPreparation
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
    fun playAllPreservesResolvedCollectionOrder() {
        assertEquals(
            listOf(
                0,
                1,
                2,
                3,
            ),
            vexStreamCollectionPlaybackTargetOrder(
                itemCount =
                    4,
                order =
                    VexStreamCollectionPlaybackOrder
                        .Natural,
            ),
        )
    }

    @Test
    fun shuffleAllFormsOneCompleteChangedPermutationBeforePlaybackStarts() {
        val natural =
            (0 until 5)
                .toList()
        val shuffled =
            vexStreamCollectionPlaybackTargetOrder(
                itemCount =
                    natural.size,
                order =
                    VexStreamCollectionPlaybackOrder
                        .Shuffled,
                random =
                    Random(
                        11,
                    ),
            )

        assertEquals(
            natural.toSet(),
            shuffled.toSet(),
        )
        assertNotEquals(
            natural,
            shuffled,
        )
    }

    private fun requestConnection(
        onRealize:
            suspend (
                ResolvedCollectionPlayback,
            ) -> Unit =
            {},
    ):
        VexStreamPlayerPartner.Connection.Request =
        object :
            VexStreamPlayerPartner.Connection.Request {
            override val id =
                VexStreamPlayerPartner.Connection.Id(
                    value =
                        "collection-test-controller",
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
                onRealize(
                    ResolvedCollectionPlayback(
                        orderedStates =
                            emptyList(),
                        items =
                            items,
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
            ) =
                true

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

// [VXG RealForever][VEXSTREAM][COLLECTION][PLAYER-PARTNER]
