package com.vextreme.vexlife.android.vexstream.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

    @Test
    fun exactCurrentMediaAdmitsSeekRequest() {
        val request =
            VexStreamPlaybackSeekRequest(
                mediaId = "track.alpha",
                positionMs = 60_000L,
            )

        assertEquals(
            60_000L,
            vexStreamPlaybackSeekTarget(
                request = request,
                currentMediaId = "track.alpha",
                seekAvailable = true,
            ),
        )
    }

    @Test
    fun staleMediaIdentityRejectsSeekRequest() {
        val request =
            VexStreamPlaybackSeekRequest(
                mediaId = "track.alpha",
                positionMs = 60_000L,
            )

        assertNull(
            vexStreamPlaybackSeekTarget(
                request = request,
                currentMediaId = "track.beta",
                seekAvailable = true,
            ),
        )
    }

    @Test
    fun providerCommandUnavailabilityRejectsSeekRequest() {
        val request =
            VexStreamPlaybackSeekRequest(
                mediaId = "track.alpha",
                positionMs = 60_000L,
            )

        assertNull(
            vexStreamPlaybackSeekTarget(
                request = request,
                currentMediaId = "track.alpha",
                seekAvailable = false,
            ),
        )
    }
}

// [VXG RealForever]
