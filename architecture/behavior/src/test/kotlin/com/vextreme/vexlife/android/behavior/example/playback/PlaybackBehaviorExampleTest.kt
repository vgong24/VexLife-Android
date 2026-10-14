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
                current = Playing,
                act = FastForward,
            )

        assertSame(
            StartFastForward,
            fastForward?.effect,
        )
        assertEquals(
            FastForwarding::class,
            fastForward?.destination,
        )

        /*
         * Runtime/provider realization is intentionally outside the blueprint. Once its observed
         * truth is FastForwarding, the next Act is resolved from THAT current State.
         */
        val play =
            blueprint.resolve(
                current = FastForwarding,
                act = Play,
            )

        assertSame(
            PlayAtNormalSpeed,
            play?.effect,
        )
        assertEquals(
            Playing::class,
            play?.destination,
        )
    }

    @Test
    fun playMeansNormalPlaybackFromFastForwardingRegardlessOfHowThatStateWasReached() {
        val fromPaused =
            blueprint.resolve(
                current = Paused,
                act = FastForward,
            )

        assertEquals(
            FastForwarding::class,
            fromPaused?.destination,
        )

        val play =
            blueprint.resolve(
                current = FastForwarding,
                act = Play,
            )

        assertEquals(
            Playing::class,
            play?.destination,
        )
        assertSame(
            PlayAtNormalSpeed,
            play?.effect,
        )
    }

    @Test
    fun currentStateDefinesWhichActsHaveMeaning() {
        assertNull(
            blueprint.resolve(
                current = Playing,
                act = Play,
            ),
        )
        assertNull(
            blueprint.resolve(
                current = Paused,
                act = Pause,
            ),
        )

        val fastForwardActs =
            blueprint.outgoing(
                FastForwarding::class,
            ).map {
                it.act
            }.toSet()

        assertEquals(
            setOf(
                Play,
                Pause,
                Rewind,
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
