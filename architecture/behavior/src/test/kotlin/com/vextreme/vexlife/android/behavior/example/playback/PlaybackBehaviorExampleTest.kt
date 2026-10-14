package com.vextreme.vexlife.android.behavior.example.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Proof for the worked Playback causal graph.
 */
class PlaybackBehaviorExampleTest {
    private val blueprint =
        playbackBehaviorBlueprint()

    @Test
    fun fastForwardThenPlayReturnsPlayingToNormalSpeedInsteadOfPausing() {
        val fastForward =
            blueprint.resolve(
                current =
                    PlaybackState.Playing(
                        speed =
                            PlaybackSpeed.Normal,
                    ),
                act =
                    PlaybackAct.FastForward,
            )

        assertEquals(
            PlaybackEffect.SetSpeed(
                PlaybackSpeed.FastForward,
            ),
            fastForward?.effect,
        )
        assertEquals(
            PlaybackState.Playing::class,
            fastForward?.destination,
        )

        /*
         * The Effect requests the travel. Once runtime truth is observed as Playing(FastForward),
         * the next Act resolves from THAT current State rather than hidden history.
         */
        val play =
            blueprint.resolve(
                current =
                    PlaybackState.Playing(
                        speed =
                            PlaybackSpeed.FastForward,
                    ),
                act =
                    PlaybackAct.Play,
            )

        assertEquals(
            PlaybackEffect.SetSpeed(
                PlaybackSpeed.Normal,
            ),
            play?.effect,
        )
        assertEquals(
            PlaybackState.Playing::class,
            play?.destination,
        )
    }

    @Test
    fun normalPlayingDoesNotExposeASecondPlayAct() {
        assertNull(
            blueprint.resolve(
                current =
                    PlaybackState.Playing(
                        speed =
                            PlaybackSpeed.Normal,
                    ),
                act =
                    PlaybackAct.Play,
            ),
        )
    }

    @Test
    fun currentStateFlavorDefinesWhichActsHaveMeaning() {
        val fastForwardActs =
            blueprint.availableActs(
                PlaybackState.Playing(
                    speed =
                        PlaybackSpeed.FastForward,
                ),
            ).toSet()

        assertEquals(
            setOf(
                PlaybackAct.Play,
                PlaybackAct.Pause,
                PlaybackAct.Rewind,
            ),
            fastForwardActs,
        )
    }

    @Test
    fun pausedPlayRequestsNormalSpeedPlaying() {
        val play =
            blueprint.resolve(
                current =
                    PlaybackState.Paused,
                act =
                    PlaybackAct.Play,
            )

        assertEquals(
            PlaybackEffect.SetSpeed(
                PlaybackSpeed.Normal,
            ),
            play?.effect,
        )
        assertEquals(
            PlaybackState.Playing::class,
            play?.destination,
        )
    }

    @Test
    fun graphDescriptionKeepsEffectSeparateFromDestinationState() {
        val description =
            blueprint.describe()

        assertTrue(
            description.contains(
                "Playing --Pause / PausePlayback--> Paused",
            ),
        )
        assertTrue(
            description.contains(
                "Playing --FastForward / SetSpeed(speed=FastForward)--> Playing",
            ),
        )
    }
}

// [VXG RealForever]
