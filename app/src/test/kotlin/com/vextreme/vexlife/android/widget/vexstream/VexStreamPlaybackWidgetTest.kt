package com.vextreme.vexlife.android.widget.vexstream

import android.content.Intent
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
    fun widgetArtworkDecodeTargetTracksDisplayDensityWithinBoundedMemory() {
        assertEquals(
            64,
            vexStreamPlaybackWidgetArtworkTargetPx(
                1f,
            ),
        )
        assertEquals(
            192,
            vexStreamPlaybackWidgetArtworkTargetPx(
                3f,
            ),
        )
        assertEquals(
            256,
            vexStreamPlaybackWidgetArtworkTargetPx(
                4f,
            ),
        )
        assertEquals(
            512,
            vexStreamPlaybackWidgetArtworkTargetPx(
                10f,
            ),
        )
    }

    @Test
    fun widgetArtworkSamplingStaysSharpAndBoundsExtremeAspectRatios() {
        assertEquals(
            8,
            vexStreamPlaybackWidgetArtworkSampleSize(
                sourceWidth = 2048,
                sourceHeight = 2048,
                targetPx = 256,
            ),
        )
        assertEquals(
            8,
            vexStreamPlaybackWidgetArtworkSampleSize(
                sourceWidth = 4096,
                sourceHeight = 256,
                targetPx = 256,
            ),
        )
        assertEquals(
            8,
            vexStreamPlaybackWidgetArtworkSampleSize(
                sourceWidth = 256,
                sourceHeight = 4096,
                targetPx = 256,
            ),
        )
        assertEquals(
            1,
            vexStreamPlaybackWidgetArtworkSampleSize(
                sourceWidth = 192,
                sourceHeight = 192,
                targetPx = 192,
            ),
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

}

// [VXG RealForever]
