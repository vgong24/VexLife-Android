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
    fun fastForwardThenPlayReturnsToNormalPlayingInsteadOfTogglingToPause() {
        val fastForward =
            blueprint.resolve(
                current =
                    PlaybackState.Playing,
                act =
                    PlaybackAct.FastForward,
            )

        assertSame(
            PlaybackEffect.StartFastForward,
            fastForward?.effect,
        )
        assertEquals(
            PlaybackState.FastForwarding::class,
            fastForward?.destination,
        )

        /*
         * Runtime/provider realization is intentionally outside the blueprint. Once its observed
         * truth is FastForwarding, the next Act is resolved from THAT current State.
         */
        val play =
            blueprint.resolve(
                current =
                    PlaybackState.FastForwarding,
                act =
                    PlaybackAct.Play,
            )

        assertSame(
            PlaybackEffect.PlayAtNormalSpeed,
            play?.effect,
        )
        assertEquals(
            PlaybackState.Playing::class,
            play?.destination,
        )
    }

    @Test
    fun playMeansNormalPlaybackFromFastForwardingRegardlessOfHowThatStateWasReached() {
        val fromPaused =
            blueprint.resolve(
                current =
                    PlaybackState.Paused,
                act =
                    PlaybackAct.FastForward,
            )

        assertEquals(
            PlaybackState.FastForwarding::class,
            fromPaused?.destination,
        )

        val play =
            blueprint.resolve(
                current =
                    PlaybackState.FastForwarding,
                act =
                    PlaybackAct.Play,
            )

        assertEquals(
            PlaybackState.Playing::class,
            play?.destination,
        )
        assertSame(
            PlaybackEffect.PlayAtNormalSpeed,
            play?.effect,
        )
    }

    @Test
    fun currentStateDefinesWhichActsHaveMeaning() {
        assertNull(
            blueprint.resolve(
                current =
                    PlaybackState.Playing,
                act =
                    PlaybackAct.Play,
            ),
        )
        assertNull(
            blueprint.resolve(
                current =
                    PlaybackState.Paused,
                act =
                    PlaybackAct.Pause,
            ),
        )

        val fastForwardActs =
            blueprint.outgoing(
                PlaybackState.FastForwarding::class,
            ).map {
                it.act
            }.toSet()

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
    fun graphDescriptionReadsFromEachCurrentStateOutward() {
        val description =
            blueprint.describe()

        assertTrue(
            description.contains(
                "FastForwarding --Play / PlayAtNormalSpeed--> Playing",
            ),
        )
        assertTrue(
            description.contains(
                "Playing --FastForward / StartFastForward--> FastForwarding",
            ),
        )
    }
}

// [VXG RealForever]
