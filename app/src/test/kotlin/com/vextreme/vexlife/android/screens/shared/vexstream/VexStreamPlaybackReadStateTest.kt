package com.vextreme.vexlife.android.screens.shared.vexstream

import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackReadState
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
    fun queueReorderCrossesOneAdjacentItemAfterHalfItsHeight() {
        assertEquals(
            2,
            vexStreamQueueAdjacentMoveTarget(
                currentIndex = 1,
                dragOffsetPx = 51f,
                itemHeightPx = 100,
                lastIndex = 4,
            ),
        )
        assertEquals(
            0,
            vexStreamQueueAdjacentMoveTarget(
                currentIndex = 1,
                dragOffsetPx = -51f,
                itemHeightPx = 100,
                lastIndex = 4,
            ),
        )
        assertEquals(
            null,
            vexStreamQueueAdjacentMoveTarget(
                currentIndex = 1,
                dragOffsetPx = 49f,
                itemHeightPx = 100,
                lastIndex = 4,
            ),
        )
        assertEquals(
            null,
            vexStreamQueueAdjacentMoveTarget(
                currentIndex = 0,
                dragOffsetPx = -80f,
                itemHeightPx = 100,
                lastIndex = 4,
            ),
        )
        assertEquals(
            null,
            vexStreamQueueAdjacentMoveTarget(
                currentIndex = 4,
                dragOffsetPx = 80f,
                itemHeightPx = 100,
                lastIndex = 4,
            ),
        )
    }

    @Test
    fun seekProjectionClampsToPlayableDuration() {
        assertEquals(
            60_000L,
            vexStreamSeekPositionMs(
                fraction = 0.5f,
                durationMs = 120_000L,
            ),
        )
        assertEquals(
            120_000L,
            vexStreamSeekPositionMs(
                fraction = 2f,
                durationMs = 120_000L,
            ),
        )
        assertEquals(
            0L,
            vexStreamSeekPositionMs(
                fraction = -1f,
                durationMs = 120_000L,
            ),
        )
        assertEquals(
            0L,
            vexStreamSeekPositionMs(
                fraction = 0.5f,
                durationMs = 0L,
            ),
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
