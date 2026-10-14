package com.vextreme.vexlife.android.behavior.example.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class PlaybackBehaviorConnectionExampleTest {
    @Test
    fun observableConnectionPublishesCurrentProjectionAndAcceptsControl() {
        val connection =
            PlaybackBehaviorConnectionExample(
                initialState =
                    PlaybackState.Playing(
                        speed =
                            PlaybackSpeed.Normal,
                    ),
            )

        assertEquals(
            connection.projections.value,
            connection.currentProjection,
        )
        assertSame(
            PlaybackAct.Pause,
            connection.currentProjection[
                PlaybackControl.PlayPause
            ].act,
        )

        connection.activate(
            PlaybackControl.PlayPause,
        )

        assertEquals(
            PlaybackState.Paused,
            connection.currentProjection.state,
        )
        assertEquals(
            listOf(
                PlaybackEffect.PausePlayback,
            ),
            connection.realizedEffects,
        )
    }

    @Test
    fun activationReResolvesControlFromLatestStateInsteadOfRenderedAct() {
        val connection =
            PlaybackBehaviorConnectionExample(
                initialState =
                    PlaybackState.Playing(
                        speed =
                            PlaybackSpeed.Normal,
                    ),
            )

        val renderedAct =
            connection.currentProjection[
                PlaybackControl.PlayPause
            ].act

        assertSame(
            PlaybackAct.Pause,
            renderedAct,
        )

        connection.observe(
            PlaybackState.Playing(
                speed =
                    PlaybackSpeed.FastForward,
            ),
        )

        connection.activate(
            PlaybackControl.PlayPause,
        )

        assertEquals(
            PlaybackState.Playing(
                speed =
                    PlaybackSpeed.Normal,
            ),
            connection.currentProjection.state,
        )
        assertEquals(
            PlaybackEffect.SetSpeed(
                PlaybackSpeed.Normal,
            ),
            connection.realizedEffects.single(),
        )
    }
}

// [VXG RealForever]
