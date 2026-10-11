package com.vextreme.vexlife.android.screens.shared.vexstream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import kotlin.random.Random

class VexStreamPlaybackUiStateTest {

    @Test
    fun progressProjectionClampsToThePlayableTimeline() {
        assertEquals(
            0.25f,
            VexStreamPlaybackUiState(
                currentPositionMs = 30_000L,
                durationMs = 120_000L,
            ).progressFraction,
            0.0001f,
        )

        assertEquals(
            1.0f,
            VexStreamPlaybackUiState(
                currentPositionMs = 150_000L,
                durationMs = 120_000L,
            ).progressFraction,
            0.0001f,
        )

        assertEquals(
            0.0f,
            VexStreamPlaybackUiState(
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
    fun oneShotShuffleKeepsHistoryAndCurrentFixedWhileChangingFutureOrder() {
        val natural =
            (0 until 7).toList()
        val shuffled =
            vexStreamQueueShuffleTargetOrder(
                itemCount = natural.size,
                currentIndex = 2,
                random = Random(7),
            )

        assertEquals(
            natural.take(3),
            shuffled.take(3),
        )
        assertEquals(
            natural.toSet(),
            shuffled.toSet(),
        )
        assertNotEquals(
            natural.drop(3),
            shuffled.drop(3),
        )
    }

    @Test
    fun oneShotShuffleCanReorderTheWholeQueueBeforePlaybackStarts() {
        val natural =
            (0 until 5).toList()
        val shuffled =
            vexStreamQueueShuffleTargetOrder(
                itemCount = natural.size,
                currentIndex = -1,
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
