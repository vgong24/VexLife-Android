package com.vextreme.vexlife.android.vexstream.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VexStreamPlaybackPendingTest {
    @Test
    fun requestPlayAdmitsPendingSemanticStateBeforeProviderAcknowledgement() {
        val emitted =
            VexStreamPlaybackState.Media(
                playRequested = false,
                phase =
                    VexStreamPlaybackPhase.Buffering,
                playPauseAvailable = true,
            ).afterIntent(
                VexStreamPlaybackEffect.RequestPlay,
            )

        assertEquals(
            VexStreamPlaybackState.Media(
                playRequested = true,
                requestPending = true,
                phase =
                    VexStreamPlaybackPhase.Buffering,
                playPauseAvailable = true,
            ),
            emitted,
        )
    }

    @Test
    fun staleProviderObservationCannotEraseNewerFamilyIntent() {
        val current =
            VexStreamPlaybackState.Media(
                playRequested = true,
                requestPending = true,
                phase =
                    VexStreamPlaybackPhase.Buffering,
                playPauseAvailable = true,
            )
        val staleProvider =
            VexStreamPlaybackState.Media(
                playRequested = false,
                phase =
                    VexStreamPlaybackPhase.Buffering,
                playPauseAvailable = true,
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
    fun matchingProviderObservationClearsPendingFamilyIntent() {
        val current =
            VexStreamPlaybackState.Media(
                playRequested = true,
                requestPending = true,
                phase =
                    VexStreamPlaybackPhase.Buffering,
                playPauseAvailable = true,
            )
        val provider =
            VexStreamPlaybackState.Media(
                playRequested = true,
                phase =
                    VexStreamPlaybackPhase.Ready,
                playPauseAvailable = true,
            )

        val reconciled =
            reconcileVexStreamPlaybackObservation(
                observed =
                    provider,
                current =
                    current,
            ) as
                VexStreamPlaybackState.Media

        assertFalse(
            reconciled.requestPending,
        )
        assertTrue(
            reconciled.playRequested,
        )
    }

    @Test
    fun newerPauseIntentCanReplacePendingPlayIntentInSharedFamilyState() {
        val pendingPause =
            VexStreamPlaybackState.Media(
                playRequested = true,
                requestPending = true,
                phase =
                    VexStreamPlaybackPhase.Buffering,
                playPauseAvailable = true,
            ).afterIntent(
                VexStreamPlaybackEffect.RequestPause,
            ) as
                VexStreamPlaybackState.Media

        assertFalse(
            pendingPause.playRequested,
        )
        assertTrue(
            pendingPause.requestPending,
        )
    }

    @Test
    fun restartIntentStaysPendingUntilProviderLeavesEnded() {
        val current =
            VexStreamPlaybackState.Ended(
                restartAvailable = true,
                restartPending = true,
            )
        val provider =
            VexStreamPlaybackState.Ended(
                restartAvailable = true,
            )

        val stillEnded =
            reconcileVexStreamPlaybackObservation(
                observed =
                    provider,
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
                        playRequested = true,
                        phase =
                            VexStreamPlaybackPhase.Buffering,
                        playPauseAvailable = true,
                    ),
                current =
                    current,
            ) as
                VexStreamPlaybackState.Media

        assertFalse(
            leftEnded.requestPending,
        )
    }

    @Test
    fun admittedPlayImmediatelyChangesTheSharedFamilyProjection() {
        val family =
            VexStreamPlaybackFamily(
                VexStreamPlaybackState.Media(
                    playRequested = false,
                    phase =
                        VexStreamPlaybackPhase.Buffering,
                    playPauseAvailable = true,
                ),
            )

        family.activate(
            VexStreamPlaybackControl.PlayPause,
        )

        val projection =
            family
                .currentPlaybackProjection

        assertEquals(
            ElementIconExpected.Pause,
            if (
                (
                    projection.state as
                        VexStreamPlaybackState.Media
                ).playRequested
            ) {
                ElementIconExpected.Pause
            } else {
                ElementIconExpected.Play
            },
        )
    }

    private enum class ElementIconExpected {
        Play,
        Pause,
    }
}

// [VXG RealForever][VEXSTREAM][PLAYBACK][DIRECT-FAMILY]
