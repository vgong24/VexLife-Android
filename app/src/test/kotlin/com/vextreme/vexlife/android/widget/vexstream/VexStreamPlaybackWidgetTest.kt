package com.vextreme.vexlife.android.widget.vexstream

import com.vextreme.vexlife.android.MainActivity
import com.vextreme.vexlife.android.resolveVexLifeExternalScreen
import com.vextreme.vexlife.android.screens.shared.vexstream.VexStreamMusicScreen
import androidx.media3.common.Player
import com.vextreme.vexlife.android.vexstream.playback.VexStreamCurrentPlaybackObservation
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackControl
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackReadState
import com.vextreme.vexlife.android.vexstream.playback.vexStreamPlaybackBehavior
import com.vextreme.vexlife.android.vexstream.playback.vexStreamPlaybackControls
import com.vextreme.vexlife.android.vexstream.playback.vexStreamPlaybackState
import com.vextreme.vexlife.android.vexstream.queue.VexStreamQueueControl
import com.vextreme.vexlife.android.vexstream.queue.vexStreamQueueBehavior
import com.vextreme.vexlife.android.vexstream.queue.vexStreamQueueControls
import com.vextreme.vexlife.android.vexstream.queue.vexStreamQueueState
import com.vextreme.vexlife.android.widget.VexWidgetActionRef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import kotlinx.coroutines.test.runTest
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
    fun widgetProjectionMapsOneProviderObservationWithoutAnotherRead() {
        val providerRead =
            vexStreamPlaybackWidgetProviderRead(
                fallbackTitle = "VexStream",
                observation =
                    VexStreamCurrentPlaybackObservation(
                        readState =
                            VexStreamPlaybackReadState(
                                currentMediaId =
                                    "track.little-bit",
                                currentTitle =
                                    "Little Bit",
                                currentArtist =
                                    "The Main Squeeze",
                                currentArtworkUri =
                                    "file:///cover.jpg",
                                currentIndex = 1,
                            ),
                        playbackProjection =
                            vexStreamPlaybackBehavior()
                                .vexStreamPlaybackControls()
                                .projection(
                                    vexStreamPlaybackState(
                                        mediaItemCount = 2,
                                        playWhenReady = true,
                                        playbackState =
                                            Player.STATE_READY,
                                        playPauseAvailable = true,
                                        restartAvailable = true,
                                    ),
                                ),
                        queueProjection =
                            vexStreamQueueBehavior()
                                .vexStreamQueueControls()
                                .projection(
                                    vexStreamQueueState(
                                        mediaItemCount = 2,
                                        previousAvailable = true,
                                        nextAvailable = false,
                                        shuffleAvailable = true,
                                        clearAvailable = true,
                                    ),
                                ),
                    ),
            )

        assertEquals(
            "Little Bit",
            providerRead.title,
        )
        assertEquals(
            "The Main Squeeze",
            providerRead.artist,
        )
        assertEquals(
            "file:///cover.jpg",
            providerRead.artworkUri,
        )
        assertTrue(
            providerRead.previousEnabled,
        )
        assertTrue(
            providerRead.playPauseEnabled,
        )
        assertTrue(
            providerRead.showPause,
        )
        assertFalse(
            providerRead.nextEnabled,
        )
    }

    @Test
    fun blankProviderTitleUsesOnlyTheSuppliedSurfaceFallback() {
        val observation =
            VexStreamCurrentPlaybackObservation(
                readState =
                    VexStreamPlaybackReadState(),
                playbackProjection =
                    vexStreamPlaybackBehavior()
                        .vexStreamPlaybackControls()
                        .projection(
                            vexStreamPlaybackState(
                                mediaItemCount = 0,
                                playWhenReady = false,
                                playbackState =
                                    Player.STATE_IDLE,
                                playPauseAvailable = false,
                                restartAvailable = false,
                            ),
                        ),
                queueProjection =
                    vexStreamQueueBehavior()
                        .vexStreamQueueControls()
                        .projection(
                            vexStreamQueueState(
                                mediaItemCount = 0,
                                previousAvailable = false,
                                nextAvailable = false,
                                shuffleAvailable = false,
                                clearAvailable = false,
                            ),
                        ),
            )

        assertEquals(
            "VexStream",
            vexStreamPlaybackWidgetProviderRead(
                fallbackTitle = "VexStream",
                observation = observation,
            ).title,
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
    fun retainedProviderTruthSurvivesFailedRenderBootstrap() {
        val retained =
            VexStreamPlaybackWidgetProviderRead(
                title = "Current song",
                artist = "Current artist",
                artworkUri = "file:///current-artwork",
                previousEnabled = true,
                playPauseEnabled = true,
                showPause = true,
                nextEnabled = true,
            )

        assertSame(
            retained,
            selectVexStreamPlaybackWidgetProviderRead(
                retained = retained,
                bootstrap = null,
            ),
        )
        assertSame(
            retained,
            selectVexStreamPlaybackWidgetProviderRead(
                retained = retained,
                bootstrap =
                    retained.copy(
                        title = "Stale bootstrap",
                    ),
            ),
        )
        assertNull(
            selectVexStreamPlaybackWidgetProviderRead(
                retained = null,
                bootstrap = null,
            ),
        )
    }

    @Test
    fun desiredRevisionAdvancesOnlyWhenProviderTruthChanges() {
        val first =
            VexStreamPlaybackWidgetProviderRead(
                title = "Song one",
                artist = "Artist",
                artworkUri = null,
                previousEnabled = false,
                playPauseEnabled = true,
                showPause = false,
                nextEnabled = true,
            )
        val second =
            first.copy(
                title = "Song two",
            )

        assertEquals(
            1L,
            nextVexStreamPlaybackWidgetDesiredRevision(
                current = null,
                currentRevision = 0L,
                next = first,
            ),
        )
        assertEquals(
            1L,
            nextVexStreamPlaybackWidgetDesiredRevision(
                current = first,
                currentRevision = 1L,
                next = first,
            ),
        )
        assertEquals(
            2L,
            nextVexStreamPlaybackWidgetDesiredRevision(
                current = first,
                currentRevision = 1L,
                next = second,
            ),
        )
    }

    @Test
    fun renderedCurrentnessRequiresExactDesiredRevision() {
        assertTrue(
            VexStreamPlaybackWidgetCurrentness(
                desiredRevision = 7L,
                renderedRevision = 7L,
            ).isConverged,
        )
        assertFalse(
            VexStreamPlaybackWidgetCurrentness(
                desiredRevision = 7L,
                renderedRevision = 6L,
            ).isConverged,
        )
        assertFalse(
            VexStreamPlaybackWidgetCurrentness(
                desiredRevision = 0L,
                renderedRevision = 0L,
            ).isConverged,
        )
    }

    @Test
    fun widgetTransportControlsMeetMinimumTouchTarget() {
        assertTrue(
            VEXSTREAM_WIDGET_CONTROL_TOUCH_TARGET_DP >=
                48,
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

}

// [VXG RealForever]
