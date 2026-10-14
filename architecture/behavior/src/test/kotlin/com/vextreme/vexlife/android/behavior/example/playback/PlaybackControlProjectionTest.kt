package com.vextreme.vexlife.android.behavior.example.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * Proof for the inverse read:
 *
 * current State -> available Acts -> complete finite Control vocabulary.
 */
class PlaybackControlProjectionTest {
    private val behavior =
        playbackBehaviorBlueprint()

    private val controls =
        behavior.playbackControls()

    @Test
    fun playingProjectsRewindPauseAndFastForward() {
        val projection =
            controls.projection(
                PlaybackState.Playing,
            )

        assertSame(
            PlaybackAct.Rewind,
            projection[
                PlaybackControl.Rewind
            ],
        )
        assertSame(
            PlaybackAct.Pause,
            projection[
                PlaybackControl.PlayPause
            ],
        )
        assertSame(
            PlaybackAct.FastForward,
            projection[
                PlaybackControl.FastForward
            ],
        )
    }

    @Test
    fun fastForwardingProjectsRewindPlayAndNoFastForward() {
        val projection =
            controls.projection(
                PlaybackState.FastForwarding,
            )

        assertSame(
            PlaybackAct.Rewind,
            projection[
                PlaybackControl.Rewind
            ],
        )
        assertSame(
            PlaybackAct.Play,
            projection[
                PlaybackControl.PlayPause
            ],
        )
        assertNull(
            projection[
                PlaybackControl.FastForward
            ],
        )
    }

    private enum class PriorityControl {
        PlayPause,
    }

    @Test
    fun playPausePriorityIsControlPolicyNotBehaviorPolicy() {
        val preferPlay =
            behavior.controls<PriorityControl> {
                control(
                    PriorityControl.PlayPause,
                    PlaybackAct.Play,
                    PlaybackAct.Pause,
                )
            }
        val preferPause =
            behavior.controls<PriorityControl> {
                control(
                    PriorityControl.PlayPause,
                    PlaybackAct.Pause,
                    PlaybackAct.Play,
                )
            }

        /*
         * FastForwarding currently admits BOTH Play and Pause. The Behavior graph exposes both;
         * the Control blueprint decides which meaning one shared affordance reflects.
         */
        assertEquals(
            setOf(
                PlaybackAct.Play,
                PlaybackAct.Pause,
                PlaybackAct.Rewind,
            ),
            behavior
                .availableActs(
                    PlaybackState.FastForwarding,
                )
                .toSet(),
        )

        assertSame(
            PlaybackAct.Play,
            preferPlay.resolve(
                current =
                    PlaybackState.FastForwarding,
                control =
                    PriorityControl.PlayPause,
            )?.act,
        )
        assertSame(
            PlaybackAct.Pause,
            preferPause.resolve(
                current =
                    PlaybackState.FastForwarding,
                control =
                    PriorityControl.PlayPause,
            )?.act,
        )
    }

    @Test
    fun pausedProjectsRewindPlayAndFastForward() {
        val projection =
            controls.projection(
                PlaybackState.Paused,
            )

        assertSame(
            PlaybackAct.Rewind,
            projection[
                PlaybackControl.Rewind
            ],
        )
        assertSame(
            PlaybackAct.Play,
            projection[
                PlaybackControl.PlayPause
            ],
        )
        assertSame(
            PlaybackAct.FastForward,
            projection[
                PlaybackControl.FastForward
            ],
        )
    }
}

// [VXG RealForever]
