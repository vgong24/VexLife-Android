package com.vextreme.vexlife.android.vexstream.playback

import androidx.media3.common.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class VexStreamPlaybackBehaviorTest {
    private val behavior =
        vexStreamPlaybackBehavior()

    private val controls =
        behavior.vexStreamPlaybackControls()

    @Test
    fun bufferingWithPlayRequestedProjectsPause() {
        val state =
            vexStreamPlaybackState(
                mediaItemCount = 1,
                playWhenReady = true,
                playbackState =
                    Player.STATE_BUFFERING,
                playPauseAvailable = true,
                restartAvailable = true,
            )

        assertEquals(
            VexStreamPlaybackState.Media(
                playRequested = true,
                phase =
                    VexStreamPlaybackPhase.Buffering,
                playPauseAvailable = true,
            ),
            state,
        )

        val projection =
            controls.projection(
                state,
            )

        assertSame(
            VexStreamPlaybackAct.Pause,
            projection[
                VexStreamPlaybackControl.PlayPause
            ].act,
        )
    }

    @Test
    fun bufferingWithoutPlayRequestedProjectsPlay() {
        val state =
            vexStreamPlaybackState(
                mediaItemCount = 1,
                playWhenReady = false,
                playbackState =
                    Player.STATE_BUFFERING,
                playPauseAvailable = true,
                restartAvailable = true,
            )

        val projection =
            controls.projection(
                state,
            )

        assertSame(
            VexStreamPlaybackAct.Play,
            projection[
                VexStreamPlaybackControl.PlayPause
            ].act,
        )
    }

    @Test
    fun noMediaMakesPlayPauseUnavailable() {
        val state =
            vexStreamPlaybackState(
                mediaItemCount = 0,
                playWhenReady = false,
                playbackState =
                    Player.STATE_IDLE,
                playPauseAvailable = true,
                restartAvailable = true,
            )

        assertSame(
            VexStreamPlaybackState.NoMedia,
            state,
        )

        assertFalse(
            controls.projection(
                state,
            )[
                VexStreamPlaybackControl.PlayPause
            ].available,
        )
    }

    @Test
    fun endedMediaProjectsPlayOnlyWhenRestartIsAvailable() {
        val restartable =
            vexStreamPlaybackState(
                mediaItemCount = 1,
                playWhenReady = true,
                playbackState =
                    Player.STATE_ENDED,
                playPauseAvailable = true,
                restartAvailable = true,
            )

        val blocked =
            vexStreamPlaybackState(
                mediaItemCount = 1,
                playWhenReady = true,
                playbackState =
                    Player.STATE_ENDED,
                playPauseAvailable = true,
                restartAvailable = false,
            )

        assertSame(
            VexStreamPlaybackAct.Play,
            controls.projection(
                restartable,
            )[
                VexStreamPlaybackControl.PlayPause
            ].act,
        )

        val restart =
            behavior.resolve(
                current = restartable,
                act =
                    VexStreamPlaybackAct.Play,
            )

        assertSame(
            VexStreamPlaybackEffect.RestartCurrent,
            restart?.effect,
        )
        assertEquals(
            VexStreamPlaybackState.Ended::class,
            restart?.admittedStateKind,
        )
        assertFalse(
            controls.projection(
                blocked,
            )[
                VexStreamPlaybackControl.PlayPause
            ].available,
        )
    }

    @Test
    fun commandAvailabilityNarrowsActsWithoutChangingMediaStateKind() {
        val state =
            VexStreamPlaybackState.Media(
                playRequested = false,
                phase =
                    VexStreamPlaybackPhase.Ready,
                playPauseAvailable = false,
            )

        val projection =
            controls.projection(
                state,
            )

        assertFalse(
            projection[
                VexStreamPlaybackControl.PlayPause
            ].available,
        )

        assertTrue(
            behavior.outgoing(
                VexStreamPlaybackState.Media::class,
            ).isNotEmpty(),
        )
    }
}

// [VXG RealForever]
