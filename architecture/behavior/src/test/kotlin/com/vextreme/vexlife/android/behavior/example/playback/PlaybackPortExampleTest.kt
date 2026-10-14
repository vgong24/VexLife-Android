package com.vextreme.vexlife.android.behavior.example.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class PlaybackPortExampleTest {
    @Test
    fun portFlowPublishesCurrentProjectionAndAcceptsControl() {
        val port =
            PlaybackPortExample(
                initialState =
                    PlaybackState.Playing(
                        speed =
                            PlaybackSpeed.Normal,
                    ),
            )

        assertEquals(
            port.projections.value,
            port.currentProjection,
        )
        assertSame(
            PlaybackAct.Pause,
            port.currentProjection[
                PlaybackControl.PlayPause
            ].act,
        )

        port.activate(
            PlaybackControl.PlayPause,
        )

        assertEquals(
            PlaybackState.Paused,
            port.currentProjection.state,
        )
        assertEquals(
            listOf(
                PlaybackEffect.PausePlayback,
            ),
            port.realizedEffects,
        )
    }

    @Test
    fun activationReResolvesControlFromLatestStateInsteadOfRenderedAct() {
        val port =
            PlaybackPortExample(
                initialState =
                    PlaybackState.Playing(
                        speed =
                            PlaybackSpeed.Normal,
                    ),
            )

        val renderedAct =
            port.currentProjection[
                PlaybackControl.PlayPause
            ].act

        assertSame(
            PlaybackAct.Pause,
            renderedAct,
        )

        port.observe(
            PlaybackState.Playing(
                speed =
                    PlaybackSpeed.FastForward,
            ),
        )

        port.activate(
            PlaybackControl.PlayPause,
        )

        assertEquals(
            PlaybackState.Playing(
                speed =
                    PlaybackSpeed.Normal,
            ),
            port.currentProjection.state,
        )
        assertEquals(
            PlaybackEffect.SetSpeed(
                PlaybackSpeed.Normal,
            ),
            port.realizedEffects.single(),
        )
    }
}

// [VXG RealForever]
