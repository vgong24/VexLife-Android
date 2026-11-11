package com.vextreme.vexlife.android.vexstream.playback

import com.vextreme.vexlife.android.execution.VexExecution
import com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerPartner
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class VexStreamPlaybackFamilyTest {
    @Test
    fun noMediaRejectsPlayPause() {
        val family =
            VexStreamPlaybackFamily()

        assertSame(
            VexStreamPlaybackState.NoMedia,
            family.currentState,
        )
        assertSame(
            null,
            family.activatePlayback(
                VexStreamPlaybackControl.PlayPause,
            ),
        )
    }

    @Test
    fun playPauseAdmitsPendingIntentBeforeProviderAcknowledgement() {
        val family =
            VexStreamPlaybackFamily(
                initialState =
                    VexStreamPlaybackState.Media(
                        playRequested =
                            false,
                        phase =
                            VexStreamPlaybackPhase.Buffering,
                        playPauseAvailable =
                            true,
                    ),
            )

        val accepted =
            requireNotNull(
                family.activatePlayback(
                    VexStreamPlaybackControl.PlayPause,
                ),
            )
        val current =
            family.currentState as
                VexStreamPlaybackState.Media
        val begin =
            requireNotNull(
                accepted.realizationBegin,
            )

        assertTrue(
            current.playRequested,
        )
        assertTrue(
            current.requestPending,
        )
        assertSame(
            VexStreamPlaybackEffect.RequestPlay,
            begin.effect,
        )
        assertSame(
            VexExecution.Approach.Latest,
            begin.approach,
        )
    }

    @Test
    fun matchingProviderTruthClearsPendingIntent() {
        val family =
            VexStreamPlaybackFamily(
                initialState =
                    VexStreamPlaybackState.Media(
                        playRequested =
                            false,
                        phase =
                            VexStreamPlaybackPhase.Ready,
                        playPauseAvailable =
                            true,
                    ),
            )
        family.activatePlayback(
            VexStreamPlaybackControl.PlayPause,
        )

        val after =
            family.observePlayer(
                playback(
                    playRequested =
                        true,
                ),
            ) as
                VexStreamPlaybackState.Media

        assertTrue(
            after.playRequested,
        )
        assertFalse(
            after.requestPending,
        )
    }

    @Test
    fun staleProviderTruthCannotEraseNewerPendingIntent() {
        val family =
            VexStreamPlaybackFamily(
                initialState =
                    VexStreamPlaybackState.Media(
                        playRequested =
                            false,
                        phase =
                            VexStreamPlaybackPhase.Ready,
                        playPauseAvailable =
                            true,
                    ),
            )
        family.activatePlayback(
            VexStreamPlaybackControl.PlayPause,
        )

        val after =
            family.observePlayer(
                playback(
                    playRequested =
                        false,
                ),
            ) as
                VexStreamPlaybackState.Media

        assertTrue(
            after.playRequested,
        )
        assertTrue(
            after.requestPending,
        )
    }

    @Test
    fun olderFailedAcceptanceCannotRollbackNewerAbaIntent() {
        val provider =
            playback(
                playRequested =
                    false,
            )
        val family =
            VexStreamPlaybackFamily()
        family.observePlayer(
            provider,
        )

        val firstPlay =
            requireNotNull(
                family.activatePlayback(
                    VexStreamPlaybackControl.PlayPause,
                ),
            )
        requireNotNull(
            family.activatePlayback(
                VexStreamPlaybackControl.PlayPause,
            ),
        )
        val newestPlay =
            requireNotNull(
                family.activatePlayback(
                    VexStreamPlaybackControl.PlayPause,
                ),
            )

        family.settlePlaybackRealizationFailure(
            accepted =
                firstPlay,
            playback =
                provider,
        )

        val stillPending =
            family.currentState as
                VexStreamPlaybackState.Media
        assertTrue(
            stillPending.playRequested,
        )
        assertTrue(
            stillPending.requestPending,
        )

        family.settlePlaybackRealizationFailure(
            accepted =
                newestPlay,
            playback =
                provider,
        )

        val rolledBack =
            family.currentState as
                VexStreamPlaybackState.Media
        assertFalse(
            rolledBack.playRequested,
        )
        assertFalse(
            rolledBack.requestPending,
        )
    }

    @Test
    fun endedPlaybackRequiresRestartCapability() {
        val blocked =
            VexStreamPlaybackFamily(
                initialState =
                    VexStreamPlaybackState.Ended(
                        restartAvailable =
                            false,
                    ),
            )
        assertSame(
            null,
            blocked.activatePlayback(
                VexStreamPlaybackControl.PlayPause,
            ),
        )

        val available =
            VexStreamPlaybackFamily(
                initialState =
                    VexStreamPlaybackState.Ended(
                        restartAvailable =
                            true,
                    ),
            )
        val accepted =
            requireNotNull(
                available.activatePlayback(
                    VexStreamPlaybackControl.PlayPause,
                ),
            )
        val current =
            available.currentState as
                VexStreamPlaybackState.Ended

        assertTrue(
            current.restartPending,
        )
        assertSame(
            VexStreamPlaybackEffect.RestartCurrent,
            requireNotNull(
                accepted.realizationBegin,
            ).effect,
        )
    }

    private fun playback(
        playRequested:
            Boolean,
    ):
        VexStreamPlayerPartner.Signal.Playback =
        VexStreamPlayerPartner.Signal.Playback(
            hasMedia =
                true,
            playRequested =
                playRequested,
            phase =
                VexStreamPlayerPartner.PlaybackPhase.Ready,
            playPauseAvailable =
                true,
            restartAvailable =
                true,
        )
}

// [VXG RealForever][VEXSTREAM][PLAYBACK][DIRECT-FAMILY][PROOF]
