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
    fun normalPlayingProjectsRewindPauseAndFastForward() {
        val projection =
            controls.projection(
                PlaybackState.Playing(
                    speed =
                        PlaybackSpeed.Normal,
                ),
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
    fun fastForwardPlayingProjectsRewindPlayAndNoFastForward() {
        val projection =
            controls.projection(
                PlaybackState.Playing(
                    speed =
                        PlaybackSpeed.FastForward,
                ),
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

        val fastForwarding =
            PlaybackState.Playing(
                speed =
                    PlaybackSpeed.FastForward,
            )

        assertEquals(
            setOf(
                PlaybackAct.Play,
                PlaybackAct.Pause,
                PlaybackAct.Rewind,
            ),
            behavior
                .availableActs(
                    fastForwarding,
                )
                .toSet(),
        )

        assertSame(
            PlaybackAct.Play,
            preferPlay.resolve(
                current =
                    fastForwarding,
                control =
                    PriorityControl.PlayPause,
            )?.act,
        )
        assertSame(
            PlaybackAct.Pause,
            preferPause.resolve(
                current =
                    fastForwarding,
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
