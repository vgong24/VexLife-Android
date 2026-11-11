package com.vextreme.vexlife.android.vexstream.playback

import com.vextreme.vexlife.android.elements.ElementIcon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VexStreamPlaybackPresentationTest {
    private fun projection(
        state:
            VexStreamPlaybackState,
    ):
        VexStreamPlaybackProjection =
        VexStreamPlaybackFamily.project(
            state,
        )

    @Test
    fun normalPlayingPresentsPause() {
        val presentation =
            projection(
                VexStreamPlaybackState.Media(
                    playRequested = true,
                    phase =
                        VexStreamPlaybackPhase.Ready,
                    playPauseAvailable = true,
                ),
            ).present(
                VexStreamPlaybackControl.PlayPause,
            )

        assertEquals(
            ElementIcon.Pause,
            presentation.icon,
        )
        assertTrue(
            presentation.enabled,
        )
    }

    @Test
    fun pausedMediaPresentsPlay() {
        val presentation =
            projection(
                VexStreamPlaybackState.Media(
                    playRequested = false,
                    phase =
                        VexStreamPlaybackPhase.Ready,
                    playPauseAvailable = true,
                ),
            ).present(
                VexStreamPlaybackControl.PlayPause,
            )

        assertEquals(
            ElementIcon.Play,
            presentation.icon,
        )
        assertTrue(
            presentation.enabled,
        )
    }

    @Test
    fun unavailableControlPresentsNeutralDisabledAffordance() {
        val presentation =
            projection(
                VexStreamPlaybackState.NoMedia,
            ).present(
                VexStreamPlaybackControl.PlayPause,
            )

        assertEquals(
            ElementIcon.PlayPause,
            presentation.icon,
        )
        assertFalse(
            presentation.enabled,
        )
    }
}

// [VXG RealForever][VEXSTREAM][PLAYBACK][DIRECT-FAMILY]
