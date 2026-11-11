package com.vextreme.vexlife.android.widget.vexstream

import android.content.Intent
import com.vextreme.vexlife.android.MainActivity
import com.vextreme.vexlife.android.resolveVexLifeExternalScreen
import com.vextreme.vexlife.android.screens.shared.vexstream.VexStreamMusicScreen
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackControl
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackFamily
import com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerPartner
import com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerStore
import com.vextreme.vexlife.android.vexstream.queue.VexStreamQueueControl
import com.vextreme.vexlife.android.vexstream.queue.VexStreamQueueFamily
import com.vextreme.vexlife.android.widget.VexWidgetActionRef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Test

class VexStreamPlaybackWidgetTest {

    @Test
    fun widgetActionRefsResolveOnlyExistingPlaybackAndQueueControls() {
        assertEquals(
            VexStreamPlaybackWidgetCommand.Queue(
                VexStreamQueueControl.Previous,
            ),
            vexStreamPlaybackWidgetCommand(
                VexStreamPlaybackWidgetActions.Previous,
            ),
        )
        assertEquals(
            VexStreamPlaybackWidgetCommand.Playback(
                VexStreamPlaybackControl.PlayPause,
            ),
            vexStreamPlaybackWidgetCommand(
                VexStreamPlaybackWidgetActions.PlayPause,
            ),
        )
        assertEquals(
            VexStreamPlaybackWidgetCommand.Queue(
                VexStreamQueueControl.Next,
            ),
            vexStreamPlaybackWidgetCommand(
                VexStreamPlaybackWidgetActions.Next,
            ),
        )
        assertNull(
            vexStreamPlaybackWidgetCommand(
                VexWidgetActionRef(
                    "vexstream.widget.unknown",
                ),
            ),
        )
    }

    @Test
    fun coldWidgetWaitsForServiceOwnedPlayerStoreCurrentBeforeActivation() =
        runTest {
            val store =
                VexStreamPlayerStore()
            val waiting =
                async {
                    awaitVexStreamWidgetPlayerSnapshot(
                        store =
                            store,
                        timeoutMillis =
                            1_000L,
                    )
                }

            yield()
            assertFalse(
                waiting.isCompleted,
            )

            val connectionId =
                VexStreamPlayerPartner.Connection.Id(
                    value =
                        "widget-current",
                    occurrence =
                        1L,
                )
            val observedAt =
                1L
            val signals:
                List<
                    VexStreamPlayerPartner.Signal
                > =
                listOf(
                    VexStreamPlayerPartner.Signal.Playback(
                        hasMedia = true,
                        playRequested = true,
                        phase =
                            VexStreamPlayerPartner.PlaybackPhase.Ready,
                        playPauseAvailable = true,
                        restartAvailable = true,
                    ),
                    VexStreamPlayerPartner.Signal.QueueItems(
                        items =
                            listOf(
                                VexStreamPlayerPartner.Signal.QueueItems.Item(
                                    mediaId = "widget.track",
                                    title = "Widget track",
                                    artist = "Vex",
                                ),
                            ),
                    ),
                    VexStreamPlayerPartner.Signal.QueueNavigation(
                        currentIndex = 0,
                        previousAvailable = false,
                        nextAvailable = false,
                        changeMediaItemsAvailable = true,
                    ),
                    VexStreamPlayerPartner.Signal.Repeat(
                        enabled = false,
                        toggleAvailable = true,
                    ),
                    VexStreamPlayerPartner.Signal.CurrentMedia(
                        mediaId = "widget.track",
                        title = "Widget track",
                        artist = "Vex",
                        artworkUri = null,
                    ),
                )

            store.connectionOpened(
                connectionId =
                    connectionId,
                observations =
                    signals.map { signal ->
                        VexStreamPlayerPartner.Connection.Observation(
                            connectionId =
                                connectionId,
                            signal =
                                signal,
                            observedAtEpochMillis =
                                observedAt,
                        )
                    },
            )

            val current =
                requireNotNull(
                    waiting.await(),
                )

            assertEquals(
                "widget.track",
                current.currentMedia.mediaId,
            )
            assertEquals(
                listOf(
                    "widget.track",
                ),
                current.queue.items.mediaIds,
            )
        }

    @Test
    fun widgetPlaybackWaitsForExistingFamilyToCatchCurrentStoreTruth() =
        runTest {
            val store =
                VexStreamPlayerStore()
            val snapshot =
                widgetPlayerSnapshot(
                    mediaId =
                        "widget.playback.current",
                    playRequested =
                        true,
                )
            openWidgetPlayerStore(
                store =
                    store,
                snapshot =
                    snapshot,
                occurrence =
                    2L,
            )
            val family =
                VexStreamPlaybackFamily()

            val waiting =
                async {
                    awaitVexStreamWidgetPlaybackFamilyCurrent(
                        family =
                            family,
                        store =
                            store,
                        timeoutMillis =
                            1_000L,
                    )
                }

            yield()
            assertFalse(
                waiting.isCompleted,
            )

            family.observePlayer(
                snapshot.playback,
            )

            assertTrue(
                waiting.await(),
            )
        }

    @Test
    fun widgetQueueWaitsForExistingFamilyToCatchCurrentStoreTruth() =
        runTest {
            val store =
                VexStreamPlayerStore()
            val snapshot =
                widgetPlayerSnapshot(
                    mediaId =
                        "widget.queue.current",
                    playRequested =
                        false,
                )
            openWidgetPlayerStore(
                store =
                    store,
                snapshot =
                    snapshot,
                occurrence =
                    3L,
            )
            val family =
                VexStreamQueueFamily()

            val waiting =
                async {
                    awaitVexStreamWidgetQueueFamilyCurrent(
                        family =
                            family,
                        store =
                            store,
                        timeoutMillis =
                            1_000L,
                    )
                }

            yield()
            assertFalse(
                waiting.isCompleted,
            )

            family.observeStore(
                snapshot.queue,
            )

            assertTrue(
                waiting.await(),
            )
        }

    @Test
    fun widgetEntryReusesTheExistingMainActivityTask() {
        assertTrue(
            VEXSTREAM_WIDGET_ENTRY_ACTIVITY_FLAGS and
                Intent.FLAG_ACTIVITY_CLEAR_TOP !=
                0,
        )
        assertTrue(
            VEXSTREAM_WIDGET_ENTRY_ACTIVITY_FLAGS and
                Intent.FLAG_ACTIVITY_SINGLE_TOP !=
                0,
        )
    }

    @Test
    fun widgetContentEntryUsesTheRegisteredVexStreamScreen() {
        assertSame(
            VexStreamMusicScreen,
            resolveVexLifeExternalScreen(
                MainActivity
                    .ACTION_OPEN_VEXSTREAM_FULL_PLAYER,
            ),
        )
    }

    @Test
    fun widgetHeadlineKeepsArtistOnTheSameLineAsTitle() {
        assertEquals(
            "My Lovely Man — Red Hot Chili Peppers",
            vexStreamPlaybackWidgetHeadline(
                title =
                    "My Lovely Man",
                artist =
                    "Red Hot Chili Peppers",
            ),
        )
        assertEquals(
            "Instrumental",
            vexStreamPlaybackWidgetHeadline(
                title =
                    "Instrumental",
                artist = "",
            ),
        )
    }

    @Test
    fun widgetSurfaceStateIncludesArtworkAvailabilityInStructuralEquality() {
        val missing =
            VexStreamPlaybackWidgetSurfaceState(
                title = "Song",
                artist = "Artist",
                artworkUri =
                    "file:///artwork",
                artworkAvailable = false,
                previousEnabled = true,
                playPauseEnabled = true,
                showPause = true,
                nextEnabled = true,
            )
        val available =
            missing.copy(
                artworkAvailable =
                    true,
            )

        assertFalse(
            missing ==
                available,
        )
    }

    @Test
    fun failedWidgetPublicationGetsOneBoundedRetry() =
        runTest {
            val observations =
                mutableListOf<String>()
            var attempts =
                0

            retryVexStreamPlaybackWidgetPublication(
                publish = {
                    attempts +=
                        1
                    observations +=
                        "publish-$attempts"
                    attempts >
                        1
                },
                awaitRetry = {
                    observations +=
                        "retry-wait"
                },
            )

            assertEquals(
                listOf(
                    "publish-1",
                    "retry-wait",
                    "publish-2",
                ),
                observations,
            )
        }

    @Test
    fun convergedWidgetPublicationDoesNotRetry() =
        runTest {
            val observations =
                mutableListOf<String>()

            retryVexStreamPlaybackWidgetPublication(
                publish = {
                    observations +=
                        "publish"
                    true
                },
                awaitRetry = {
                    observations +=
                        "retry-wait"
                },
            )

            assertEquals(
                listOf(
                    "publish",
                ),
                observations,
            )
        }

    @Test
    fun widgetTransportControlsMeetMinimumTouchTarget() {
        assertTrue(
            VEXSTREAM_WIDGET_CONTROL_TOUCH_TARGET_DP >=
                56,
        )
        assertTrue(
            VEXSTREAM_WIDGET_CONTROL_SYMBOL_SP >=
                28,
        )
        assertTrue(
            VEXSTREAM_WIDGET_ARTWORK_SIZE_DP >=
                VEXSTREAM_WIDGET_CONTROL_TOUCH_TARGET_DP,
        )
        assertTrue(
            VEXSTREAM_WIDGET_PUBLICATION_RETRY_MILLIS >
                0L,
        )
    }

    private fun widgetPlayerSnapshot(
        mediaId:
            String,
        playRequested:
            Boolean,
    ):
        VexStreamPlayerStore.Snapshot.Available =
        VexStreamPlayerStore.Snapshot.Available(
            playback =
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
                ),
            queueItems =
                VexStreamPlayerPartner.Signal.QueueItems(
                    items =
                        listOf(
                            VexStreamPlayerPartner.Signal.QueueItems.Item(
                                mediaId =
                                    mediaId,
                                title =
                                    "Widget track",
                                artist =
                                    "Vex",
                            ),
                        ),
                ),
            queueNavigation =
                VexStreamPlayerPartner.Signal.QueueNavigation(
                    currentIndex =
                        0,
                    previousAvailable =
                        false,
                    nextAvailable =
                        false,
                    changeMediaItemsAvailable =
                        true,
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
                        mediaId,
                    title =
                        "Widget track",
                    artist =
                        "Vex",
                    artworkUri =
                        null,
                ),
        )

    private fun openWidgetPlayerStore(
        store:
            VexStreamPlayerStore,
        snapshot:
            VexStreamPlayerStore.Snapshot.Available,
        occurrence:
            Long,
    ) {
        val connectionId =
            VexStreamPlayerPartner.Connection.Id(
                value =
                    "widget-current-$occurrence",
                occurrence =
                    occurrence,
            )
        val signals:
            List<
                VexStreamPlayerPartner.Signal
            > =
            listOf(
                snapshot.playback,
                snapshot.queueItems,
                snapshot.queueNavigation,
                snapshot.repeat,
                snapshot.currentMedia,
            )

        store.connectionOpened(
            connectionId =
                connectionId,
            observations =
                signals.map { signal ->
                    VexStreamPlayerPartner.Connection.Observation(
                        connectionId =
                            connectionId,
                        signal =
                            signal,
                        observedAtEpochMillis =
                            occurrence,
                    )
                },
        )
    }


}

// [VXG RealForever]
