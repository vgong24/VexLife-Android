package com.vextreme.vexlife.android.vexstream.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class VexStreamPlaybackSeekTest {
    @Test
    fun seekPositionClampsToPlayableDuration() {
        assertEquals(
            60_000L,
            vexStreamPlaybackSeekPositionMs(
                fraction = 0.5f,
                durationMs = 120_000L,
            ),
        )
        assertEquals(
            120_000L,
            vexStreamPlaybackSeekPositionMs(
                fraction = 2f,
                durationMs = 120_000L,
            ),
        )
        assertEquals(
            0L,
            vexStreamPlaybackSeekPositionMs(
                fraction = -1f,
                durationMs = 120_000L,
            ),
        )
        assertEquals(
            0L,
            vexStreamPlaybackSeekPositionMs(
                fraction = 0.5f,
                durationMs = 0L,
            ),
        )
    }
}

// [VXG RealForever][VEXSTREAM][PLAYBACK][PRESENTATION-SEEK]
