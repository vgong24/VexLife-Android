package com.vextreme.vexlife.android.vexstream.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VexStreamPlaybackPendingTest {
    @Test
    fun requestPlayEmitsPendingSemanticStateBeforeProviderAcknowledgement() {
        val emitted =
            VexStreamPlaybackState.Media(
                playRequested =
                    false,
                phase =
                    VexStreamPlaybackPhase.Buffering,
                playPauseAvailable =
                    true,
            ).afterIntent(
                VexStreamPlaybackEffect.RequestPlay,
            )

        assertEquals(
            VexStreamPlaybackState.Media(
                playRequested =
                    true,
                requestPending =
                    true,
                phase =
                    VexStreamPlaybackPhase.Buffering,
                playPauseAvailable =
                    true,
            ),
            emitted,
        )
    }

    @Test
    fun staleProviderObservationCannotEraseNewerPlayIntent() {
        val current =
            VexStreamPlaybackState.Media(
                playRequested =
                    true,
                requestPending =
                    true,
                phase =
                    VexStreamPlaybackPhase.Buffering,
                playPauseAvailable =
                    true,
            )
        val staleProvider =
            VexStreamPlaybackState.Media(
                playRequested =
                    false,
                phase =
                    VexStreamPlaybackPhase.Ready,
                playPauseAvailable =
                    true,
            )

        val reconciled =
            reconcileVexStreamPlaybackObservation(
                observed =
                    staleProvider,
                current =
                    current,
            ) as
                VexStreamPlaybackState.Media

        assertTrue(
            reconciled.playRequested,
        )
        assertTrue(
            reconciled.requestPending,
        )
    }

    @Test
    fun matchingProviderObservationClearsPendingPlayIntent() {
        val current =
            VexStreamPlaybackState.Media(
                playRequested =
                    true,
                requestPending =
                    true,
                phase =
                    VexStreamPlaybackPhase.Buffering,
                playPauseAvailable =
                    true,
            )
        val provider =
            VexStreamPlaybackState.Media(
                playRequested =
                    true,
                phase =
                    VexStreamPlaybackPhase.Ready,
                playPauseAvailable =
                    true,
            )

        val reconciled =
            reconcileVexStreamPlaybackObservation(
                observed =
                    provider,
                current =
                    current,
            ) as
                VexStreamPlaybackState.Media

        assertTrue(
            reconciled.playRequested,
        )
        assertFalse(
            reconciled.requestPending,
        )
    }

    @Test
    fun newerPauseIntentCanReplaceStillPendingPlayIntent() {
        val emitted =
            VexStreamPlaybackState.Media(
                playRequested =
                    true,
                requestPending =
                    true,
                phase =
                    VexStreamPlaybackPhase.Buffering,
                playPauseAvailable =
                    true,
            ).afterIntent(
                VexStreamPlaybackEffect.RequestPause,
            ) as
                VexStreamPlaybackState.Media

        assertFalse(
            emitted.playRequested,
        )
        assertTrue(
            emitted.requestPending,
        )
    }

    @Test
    fun restartIntentStaysPendingUntilProviderLeavesEnded() {
        val current =
            VexStreamPlaybackState.Ended(
                restartAvailable =
                    true,
                restartPending =
                    true,
            )

        val stillEnded =
            reconcileVexStreamPlaybackObservation(
                observed =
                    VexStreamPlaybackState.Ended(
                        restartAvailable =
                            true,
                    ),
                current =
                    current,
            ) as
                VexStreamPlaybackState.Ended

        assertTrue(
            stillEnded.restartPending,
        )

        val leftEnded =
            reconcileVexStreamPlaybackObservation(
                observed =
                    VexStreamPlaybackState.Media(
                        playRequested =
                            true,
                        phase =
                            VexStreamPlaybackPhase.Ready,
                        playPauseAvailable =
                            true,
                    ),
                current =
                    current,
            ) as
                VexStreamPlaybackState.Media

        assertFalse(
            leftEnded.requestPending,
        )
    }
}

// [VXG RealForever][VEXSTREAM][PLAYBACK][PENDING][PROOF]
