package com.vextreme.vexlife.android.vexstream.playback

import com.vextreme.vexlife.android.elements.ElementIcon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VexStreamPlaybackPresentationTest {
    private val behavior =
        vexStreamPlaybackBehavior()

    private val controls =
        behavior.vexStreamPlaybackControls()

    @Test
    fun normalPlayingPresentsPause() {
        val presentation =
            controls
                .projection(
                    VexStreamPlaybackState.Media(
                        playRequested = true,
                        phase =
                            VexStreamPlaybackPhase.Ready,
                        playPauseAvailable = true,
                    ),
                )
                .present(
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
            controls
                .projection(
                    VexStreamPlaybackState.Media(
                        playRequested = false,
                        phase =
                            VexStreamPlaybackPhase.Ready,
                        playPauseAvailable = true,
                    ),
                )
                .present(
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
            controls
                .projection(
                    VexStreamPlaybackState.NoMedia,
                )
                .present(
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

// [VXG RealForever]
