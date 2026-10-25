package com.vextreme.vexlife.android.widget.vexstream

import com.vextreme.vexlife.android.MainActivity
import com.vextreme.vexlife.android.resolveVexLifeExternalScreen
import com.vextreme.vexlife.android.screens.shared.vexstream.VexStreamMusicScreen
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackControl
import com.vextreme.vexlife.android.vexstream.queue.VexStreamQueueControl
import com.vextreme.vexlife.android.widget.VexWidgetActionRef
import org.junit.Assert.assertEquals
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
