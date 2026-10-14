package com.vextreme.vexlife.android.behavior.example.playback

import com.vextreme.vexlife.android.behavior.vexBehaviorControl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * Proof for the inverse read:
 *
 * current State -> available Acts -> resolved Controls.
 */
class PlaybackControlProjectionTest {
    private val blueprint =
        playbackBehaviorBlueprint()

    private val controls =
        PlaybackControls()

    @Test
    fun playingProjectsRewindPauseAndFastForward() {
        val projection =
            controls.project(
                blueprint = blueprint,
                current = Playing,
            )

        assertSame(
            Rewind,
            projection.rewind,
        )
        assertSame(
            Pause,
            projection.playPause,
        )
        assertSame(
            FastForward,
            projection.fastForward,
        )
    }

    @Test
    fun fastForwardingProjectsRewindPlayAndNoFastForward() {
        val projection =
            controls.project(
                blueprint = blueprint,
                current = FastForwarding,
            )

        assertSame(
            Rewind,
            projection.rewind,
        )
        assertSame(
            Play,
            projection.playPause,
        )
        assertNull(
            projection.fastForward,
        )
    }

    @Test
    fun playPausePriorityIsControlPolicyNotBehaviorPolicy() {
        val preferPlay =
            vexBehaviorControl<PlaybackAct>(
                "example.prefer-play",
                Play,
                Pause,
            )
        val preferPause =
            vexBehaviorControl<PlaybackAct>(
                "example.prefer-pause",
                Pause,
                Play,
            )

        /*
         * FastForwarding currently admits BOTH Play and Pause. The Behavior graph exposes both;
         * the Control decides which meaning one shared affordance reflects.
         */
        assertEquals(
            setOf(
                Play,
                Pause,
                Rewind,
            ),
            blueprint
                .availableActs(
                    FastForwarding,
                )
                .toSet(),
        )

        assertSame(
            Play,
            preferPlay.resolve(
                blueprint = blueprint,
                current = FastForwarding,
            )?.act,
        )
        assertSame(
            Pause,
            preferPause.resolve(
                blueprint = blueprint,
                current = FastForwarding,
            )?.act,
        )
    }

    @Test
    fun pausedProjectsRewindPlayAndFastForward() {
        val projection =
            controls.project(
                blueprint = blueprint,
                current = Paused,
            )

        assertSame(
            Rewind,
            projection.rewind,
        )
        assertSame(
            Play,
            projection.playPause,
        )
        assertSame(
            FastForward,
            projection.fastForward,
        )
    }
}

// [VXG RealForever]
