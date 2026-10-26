package com.vextreme.vexlife.android.widget.vexstream

import com.vextreme.vexlife.android.MainActivity
import com.vextreme.vexlife.android.resolveVexLifeExternalScreen
import com.vextreme.vexlife.android.screens.shared.vexstream.VexStreamMusicScreen
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackControl
import com.vextreme.vexlife.android.vexstream.queue.VexStreamQueueControl
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
    fun widgetProviderRefreshPerformsOneBoundedConvergenceRead() =
        runTest {
            val observations =
                mutableListOf<String>()

            convergeVexStreamPlaybackWidgetProjection(
                refresh = {
                    observations +=
                        "refresh"
                },
                awaitProviderConvergence = {
                    observations +=
                        "provider-convergence"
                },
            )

            assertEquals(
                listOf(
                    "refresh",
                    "provider-convergence",
                    "refresh",
                ),
                observations,
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
            VEXSTREAM_WIDGET_PROVIDER_CONVERGENCE_MILLIS >
                0L,
        )
    }

}

// [VXG RealForever]
