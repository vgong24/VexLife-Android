package com.vextreme.vexlife.android.screens.shared.vexstream

import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackReadState
import com.vextreme.vexlife.android.vexstream.queue.VexStreamQueueControl
import org.junit.Assert.assertEquals
import org.junit.Test

class VexStreamPlaybackReadStateTest {

    @Test
    fun progressProjectionClampsToThePlayableTimeline() {
        assertEquals(
            0.25f,
            VexStreamPlaybackReadState(
                currentPositionMs = 30_000L,
                durationMs = 120_000L,
            ).progressFraction,
            0.0001f,
        )

        assertEquals(
            1.0f,
            VexStreamPlaybackReadState(
                currentPositionMs = 150_000L,
                durationMs = 120_000L,
            ).progressFraction,
            0.0001f,
        )

        assertEquals(
            0.0f,
            VexStreamPlaybackReadState(
                currentPositionMs = 30_000L,
                durationMs = 0L,
            ).progressFraction,
            0.0001f,
        )
    }

    @Test
    fun fullPlayerSwipeMapsOnlyBroadHorizontalGesturesToQueueControls() {
        assertEquals(
            VexStreamQueueControl.Previous,
            vexStreamFullPlayerSwipeControl(
                dragDistancePx = 120f,
                thresholdPx = 72f,
            ),
        )
        assertEquals(
            VexStreamQueueControl.Next,
            vexStreamFullPlayerSwipeControl(
                dragDistancePx = -120f,
                thresholdPx = 72f,
            ),
        )
        assertEquals(
            null,
            vexStreamFullPlayerSwipeControl(
                dragDistancePx = 40f,
                thresholdPx = 72f,
            ),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun fullPlayerSwipeRejectsNonPositiveThreshold() {
        vexStreamFullPlayerSwipeControl(
            dragDistancePx = 10f,
            thresholdPx = 0f,
        )
    }

    @Test
    fun playbackTimeFormatsCompactPhoneDurations() {
        assertEquals(
            "0:00",
            formatVexStreamPlaybackTime(0L),
        )
        assertEquals(
            "1:05",
            formatVexStreamPlaybackTime(65_000L),
        )
        assertEquals(
            "1:01:01",
            formatVexStreamPlaybackTime(3_661_000L),
        )
    }
}

// [VXG RealForever]
