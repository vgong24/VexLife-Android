package com.vextreme.vexlife.android.vexstream.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VexStreamPlayerSeekCurrentnessTest {
    @Test
    fun exactCurrentMediaAdmitsSeek() {
        assertEquals(
            60_000L,
            seekTarget(
                mediaId = "track.alpha",
                mediaItemIndex = 1,
                positionMs = 60_000L,
                currentMediaId = "track.alpha",
                currentMediaItemIndex = 1,
                seekAvailable = true,
            ),
        )
    }

    @Test
    fun staleMediaIdentityOrOccurrenceRejectsSeek() {
        assertNull(
            seekTarget(
                mediaId = "track.alpha",
                mediaItemIndex = 1,
                positionMs = 60_000L,
                currentMediaId = "track.beta",
                currentMediaItemIndex = 1,
                seekAvailable = true,
            ),
        )
        assertNull(
            seekTarget(
                mediaId = "track.alpha",
                mediaItemIndex = 1,
                positionMs = 60_000L,
                currentMediaId = "track.alpha",
                currentMediaItemIndex = 2,
                seekAvailable = true,
            ),
        )
    }

    @Test
    fun providerCapabilityRejectsSeek() {
        assertNull(
            seekTarget(
                mediaId = "track.alpha",
                mediaItemIndex = 1,
                positionMs = 60_000L,
                currentMediaId = "track.alpha",
                currentMediaItemIndex = 1,
                seekAvailable = false,
            ),
        )
    }
}

// [VXG RealForever][VEXSTREAM][PLAYER-PARTNER][SEEK-CURRENTNESS]
