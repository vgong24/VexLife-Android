package com.vextreme.vexlife.android.vexstream.collection

import androidx.media3.common.MediaItem
import com.vextreme.vexlife.android.data.vexstream.VexStreamMediaSha256
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrack
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackRef
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackState
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import com.vextreme.vexlife.android.vexstream.track.VexStreamTrackMediaItemResolver
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
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
                        ref = "track.offline",
                        hubAvailable = false,
                    ),
                ),
            ),
        )
        assertTrue(
            vexStreamCollectionSourceProjectedAvailable(
                listOf(
                    track(
                        ref = "track.offline",
                        hubAvailable = false,
                    ),
                    track(
                        ref = "track.hub",
                    ),
                ),
            ),
        )
    }

    @Test
    fun playbackAdmissionRequiresEveryProviderCommandActuallyUsed() {
        assertTrue(
            vexStreamCollectionPlaybackCommandsAvailable(
                changeMediaItemsAvailable = true,
                prepareAvailable = true,
                playAvailable = true,
                shuffleEnabled = false,
                setShuffleModeAvailable = false,
            ),
        )
        assertFalse(
            vexStreamCollectionPlaybackCommandsAvailable(
                changeMediaItemsAvailable = false,
                prepareAvailable = true,
                playAvailable = true,
                shuffleEnabled = false,
                setShuffleModeAvailable = true,
            ),
        )
        assertFalse(
            vexStreamCollectionPlaybackCommandsAvailable(
                changeMediaItemsAvailable = true,
                prepareAvailable = true,
                playAvailable = true,
                shuffleEnabled = true,
                setShuffleModeAvailable = false,
            ),
        )
        assertTrue(
            vexStreamCollectionPlaybackCommandsAvailable(
                changeMediaItemsAvailable = true,
                prepareAvailable = true,
                playAvailable = true,
                shuffleEnabled = true,
                setShuffleModeAvailable = true,
            ),
        )
    }

    @Test
    fun collectionExecutionPreparesOnlyCurrentOrderedArtworkOffProviderThread(): Unit =
        runBlocking {
            val workerExecutor =
                Executors.newSingleThreadExecutor { runnable ->
                    Thread(
                        runnable,
                        "vex-collection-worker",
                    )
                }
            val providerExecutor =
                Executors.newSingleThreadExecutor { runnable ->
                    Thread(
                        runnable,
                        "vex-collection-provider",
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
            val events =
                CopyOnWriteArrayList<String>()
            val execution =
                VexStreamCollectionPlaybackExecution(
                    root = root,
                    providerDispatcher =
                        providerDispatcher,
                    resolver =
                        object :
                            VexStreamTrackMediaItemResolver {
                            override fun mediaItem(
                                state: VexStreamTrackState,
                            ): MediaItem {
                                events +=
                                    "resolve:" +
                                        state.track
                                            .trackRef
                                            .value +
                                        ":" +
                                        Thread.currentThread()
                                            .name
                                return MediaItem.Builder()
                                    .setMediaId(
                                        state.track
                                            .trackRef
                                            .value,
                                    )
                                    .build()
                            }

                            override suspend fun prepareArtwork(
                                state: VexStreamTrackState,
                            ) {
                                events +=
                                    "prepare:" +
                                        state.track
                                            .trackRef
                                            .value +
                                        ":" +
                                        Thread.currentThread()
                                            .name
                            }
                        },
                    commitPlay = { resolved ->
                        events +=
                            "commit:" +
                                resolved.items
                                    .joinToString(
                                        separator = ",",
                                    ) {
                                        it.mediaId
                                    } +
                                ":" +
                                Thread.currentThread()
                                    .name
                        true
                    },
                )

            try {
                assertTrue(
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
                                VexStreamCollectionPlaybackOrder.Natural,
                        ),
                    ),
                )

                withTimeout(
                    2_000L,
                ) {
                    while (
                        events.none {
                            it.startsWith(
                                "commit:",
                            )
                        }
                    ) {
                        yield()
                    }
                }

                val preparation =
                    events.filter {
                        it.startsWith(
                            "prepare:",
                        )
                    }
                assertEquals(
                    1,
                    preparation.size,
                )
                assertTrue(
                    preparation.single()
                        .startsWith(
                            "prepare:track.alpha:vex-collection-worker",
                        ),
                )
                assertTrue(
                    events.last {
                        it.startsWith(
                            "commit:",
                        )
                    }.startsWith(
                        "commit:track.alpha,track.beta:vex-collection-provider",
                    ),
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
    fun playAllPreservesResolvedCollectionOrder() {
        assertEquals(
            listOf(
                0,
                1,
                2,
                3,
            ),
            vexStreamCollectionPlaybackTargetOrder(
                itemCount = 4,
                order =
                    VexStreamCollectionPlaybackOrder.Natural,
            ),
        )
    }

    @Test
    fun shuffleAllFormsOneCompleteChangedPermutationBeforePlaybackStarts() {
        val natural =
            (0 until 5).toList()
        val shuffled =
            vexStreamCollectionPlaybackTargetOrder(
                itemCount = natural.size,
                order =
                    VexStreamCollectionPlaybackOrder.Shuffled,
                random = Random(11),
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
