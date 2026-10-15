package com.vextreme.vexlife.android.vexstream.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class VexStreamPlaybackDisplayStateTest {
    private val behavior =
        vexStreamPlaybackBehavior()

    private val controls =
        behavior
            .vexStreamPlaybackControls()

    @Test
    fun missingProjectionIsUnknown() {
        assertSame(
            VexStreamPlaybackDisplayState
                .Unknown,
            (null as VexStreamPlaybackProjection?)
                .toVexStreamPlaybackDisplayState(),
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
                    projection,
                ),
            projection
                .toVexStreamPlaybackDisplayState(),
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
                    projection,
                ),
            projection
                .toVexStreamPlaybackDisplayState(),
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
                    projection,
                ),
            projection
                .toVexStreamPlaybackDisplayState(),
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
                    projection,
                ),
            projection
                .toVexStreamPlaybackDisplayState(),
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
                    projection,
                ),
            projection
                .toVexStreamPlaybackDisplayState(),
        )
    }
}

// [VXG RealForever]
