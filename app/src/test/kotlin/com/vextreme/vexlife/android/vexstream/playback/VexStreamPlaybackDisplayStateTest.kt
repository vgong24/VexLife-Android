package com.vextreme.vexlife.android.vexstream.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class VexStreamPlaybackDisplayStateTest {
    private val behavior =
        vexStreamPlaybackBehavior()

    private val controls =
        behavior
            .vexStreamPlaybackControls()

    private val playback =
        VexStreamPlaybackReadState(
            currentMediaId =
                "media.test",
            currentTitle =
                "Test Track",
        )

    @Test
    fun missingProjectionIsUnknown() {
        assertEquals(
            VexStreamPlaybackDisplayState
                .Unknown(
                    playback =
                        playback,
                ),
            (null as VexStreamPlaybackProjection?)
                .toVexStreamPlaybackDisplayState(
                    playback =
                        playback,
                ),
        )
    }

    @Test
    fun noMediaProjectionIsInactive() {
        val projection =
            controls.projection(
                VexStreamPlaybackState
                    .NoMedia,
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
            controls.projection(
                VexStreamPlaybackState
                    .Media(
                        playRequested = true,
                        requestPending = true,
                        phase =
                            VexStreamPlaybackPhase
                                .Buffering,
                        playPauseAvailable =
                            true,
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
            controls.projection(
                VexStreamPlaybackState
                    .Ended(
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
            controls.projection(
                VexStreamPlaybackState
                    .Media(
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

    @Test
    fun endedProjectionWithoutPendingRestartRemainsActiveDisplay() {
        val projection =
            controls.projection(
                VexStreamPlaybackState
                    .Ended(
                        restartAvailable = true,
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

// [VXG RealForever]
