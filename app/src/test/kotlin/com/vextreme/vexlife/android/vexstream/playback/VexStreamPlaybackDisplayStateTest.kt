package com.vextreme.vexlife.android.vexstream.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class VexStreamPlaybackDisplayStateTest {
    private val playback =
        VexStreamPlaybackReadState(
            currentMediaId =
                "media.test",
            currentTitle =
                "Test Track",
        )

    private fun projection(
        state:
            VexStreamPlaybackState,
    ):
        VexStreamPlaybackProjection =
        VexStreamPlaybackFamily.project(
            state,
        )

    @Test
    fun missingProjectionIsUnknown() {
        assertEquals(
            VexStreamPlaybackDisplayState
                .Unknown(
                    playback =
                        playback,
                ),
            (null as
                VexStreamPlaybackProjection?)
                .toVexStreamPlaybackDisplayState(
                    playback =
                        playback,
                ),
        )
    }

    @Test
    fun noMediaProjectionIsInactive() {
        val projection =
            projection(
                VexStreamPlaybackState.NoMedia,
            )

        assertEquals(
            VexStreamPlaybackDisplayState
                .Inactive(
                    projection =
                        projection,
                    playback =
                        playback,
                ),
            projection
                .toVexStreamPlaybackDisplayState(
                    playback =
                        playback,
                ),
        )
    }

    @Test
    fun pendingMediaProjectionIsTransitioning() {
        val projection =
            projection(
                VexStreamPlaybackState.Media(
                    playRequested = true,
                    requestPending = true,
                    phase =
                        VexStreamPlaybackPhase
                            .Buffering,
                    playPauseAvailable = true,
                ),
            )

        assertEquals(
            VexStreamPlaybackDisplayState
                .Transitioning(
                    projection =
                        projection,
                    playback =
                        playback,
                ),
            projection
                .toVexStreamPlaybackDisplayState(
                    playback =
                        playback,
                ),
        )
    }

    @Test
    fun pendingRestartProjectionIsTransitioning() {
        val projection =
            projection(
                VexStreamPlaybackState.Ended(
                    restartAvailable = true,
                    restartPending = true,
                ),
            )

        assertEquals(
            VexStreamPlaybackDisplayState
                .Transitioning(
                    projection =
                        projection,
                    playback =
                        playback,
                ),
            projection
                .toVexStreamPlaybackDisplayState(
                    playback =
                        playback,
                ),
        )
    }

    @Test
    fun ordinaryMediaProjectionIsActive() {
        val projection =
            projection(
                VexStreamPlaybackState.Media(
                    playRequested = false,
                    phase =
                        VexStreamPlaybackPhase
                            .Ready,
                    playPauseAvailable =
                        true,
                ),
            )

        assertEquals(
            VexStreamPlaybackDisplayState
                .Active(
                    projection =
                        projection,
                    playback =
                        playback,
                ),
            projection
                .toVexStreamPlaybackDisplayState(
                    playback =
                        playback,
                ),
        )
    }
}

// [VXG RealForever][VEXSTREAM][PLAYBACK][DIRECT-FAMILY]
