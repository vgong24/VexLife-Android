package com.vextreme.vexlife.android.behavior.example.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * Proof for the inverse read:
 *
 * current State -> available Acts -> complete finite Control vocabulary -> immutable Projection.
 */
class PlaybackControlProjectionTest {
    private val behavior =
        playbackBehaviorBlueprint()

    private val controls =
        behavior.playbackControls()

    @Test
    fun normalPlayingProjectsRewindPauseAndFastForward() {
        val current =
            PlaybackState.Playing(
                speed =
                    PlaybackSpeed.Normal,
            )
        val projection =
            controls.projection(
                current,
            )

        assertEquals(
            current,
            projection.state,
        )
        assertSame(
            PlaybackAct.Rewind,
            projection[
                PlaybackControl.Rewind
            ].act,
        )
        assertSame(
            PlaybackAct.Pause,
            projection[
                PlaybackControl.PlayPause
            ].act,
        )
        assertSame(
            PlaybackAct.FastForward,
            projection[
                PlaybackControl.FastForward
            ].act,
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
            ].act,
        )
        assertSame(
            PlaybackAct.Play,
            projection[
                PlaybackControl.PlayPause
            ].act,
        )
        assertNull(
            projection[
                PlaybackControl.FastForward
            ].act,
        )
        assertEquals(
            false,
            projection[
                PlaybackControl.FastForward
            ].available,
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
            ].act,
        )
        assertSame(
            PlaybackAct.Play,
            projection[
                PlaybackControl.PlayPause
            ].act,
        )
        assertSame(
            PlaybackAct.FastForward,
            projection[
                PlaybackControl.FastForward
            ].act,
        )
    }

    @Test
    fun oneProjectionCanFeedSeveralUiConsumersWithoutRecomputingBehavior() {
        val projection =
            controls.projection(
                PlaybackState.Playing(
                    speed =
                        PlaybackSpeed.Normal,
                ),
            )

        /*
         * These stand in for two independent presentation consumers such as Mini Player and Context
         * Dock. Both read the same immutable semantic result; neither owns State/Act inference.
         */
        val miniPlayerMeaning =
            projection[
                PlaybackControl.PlayPause
            ].act
        val contextDockMeaning =
            projection[
                PlaybackControl.PlayPause
            ].act

        assertSame(
            PlaybackAct.Pause,
            miniPlayerMeaning,
        )
        assertSame(
            miniPlayerMeaning,
            contextDockMeaning,
        )
    }
}

// [VXG RealForever]
