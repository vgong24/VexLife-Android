package com.vextreme.vexlife.android.vexstream.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VexStreamPlaybackPendingTest {
    @Test
    fun requestPlayEmitsPendingSemanticStateBeforeProviderAcknowledgement() {
        val current =
            VexStreamPlaybackState.Media(
                playRequested = false,
                phase =
                    VexStreamPlaybackPhase.Buffering,
                playPauseAvailable = true,
            )

        val pending =
            VexStreamPlaybackPending()
                .after(
                    VexStreamPlaybackEffect.RequestPlay,
                )

        val emitted =
            current.afterIntent(
                VexStreamPlaybackEffect.RequestPlay,
            )

        assertEquals(
            true,
            pending.playRequested,
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
    fun staleProviderObservationCannotEraseNewerPlayIntent() {
        val pending =
            VexStreamPlaybackPending(
                playRequested = true,
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
                observed = staleProvider,
                pending = pending,
            )
        val state =
            reconciled.state as
                VexStreamPlaybackState.Media

        assertEquals(
            true,
            state.playRequested,
        )
        assertTrue(
            state.requestPending,
        )
        assertEquals(
            true,
            reconciled.pending.playRequested,
        )
    }

    @Test
    fun matchingProviderObservationClearsPendingPlayIntent() {
        val pending =
            VexStreamPlaybackPending(
                playRequested = true,
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
                observed = provider,
                pending = pending,
            )
        val state =
            reconciled.state as
                VexStreamPlaybackState.Media

        assertFalse(
            state.requestPending,
        )
        assertEquals(
            null,
            reconciled.pending.playRequested,
        )
    }

    @Test
    fun newerPauseIntentCanReplaceStillPendingPlayIntent() {
        val pendingPlay =
            VexStreamPlaybackPending(
                playRequested = true,
            )
        val pendingPause =
            pendingPlay.after(
                VexStreamPlaybackEffect.RequestPause,
            )

        assertEquals(
            false,
            pendingPause.playRequested,
        )

        val emitted =
            VexStreamPlaybackState.Media(
                playRequested = true,
                requestPending = true,
                phase =
                    VexStreamPlaybackPhase.Buffering,
                playPauseAvailable = true,
            ).afterIntent(
                VexStreamPlaybackEffect.RequestPause,
            ) as VexStreamPlaybackState.Media

        assertEquals(
            false,
            emitted.playRequested,
        )
        assertTrue(
            emitted.requestPending,
        )
    }

    @Test
    fun restartIntentStaysPendingUntilProviderLeavesEnded() {
        val pending =
            VexStreamPlaybackPending()
                .after(
                    VexStreamPlaybackEffect.RestartCurrent,
                )

        val emitted =
            VexStreamPlaybackState.Ended(
                restartAvailable = true,
            ).afterIntent(
                VexStreamPlaybackEffect.RestartCurrent,
            )

        assertEquals(
            VexStreamPlaybackState.Ended(
                restartAvailable = true,
                restartPending = true,
            ),
            emitted,
        )

        val stillEnded =
            reconcileVexStreamPlaybackObservation(
                observed =
                    VexStreamPlaybackState.Ended(
                        restartAvailable = true,
                    ),
                pending = pending,
            )
        val state =
            stillEnded.state as
                VexStreamPlaybackState.Ended

        assertTrue(
            state.restartPending,
        )
        assertTrue(
            stillEnded.pending.restartPending,
        )
    }
    @Test
    fun synchronousPlayIntentImmediatelyFlipsPlayPauseControlToPause() {
        val behavior =
            vexStreamPlaybackBehavior()
        val controls =
            behavior.vexStreamPlaybackControls()

        val pendingState =
            VexStreamPlaybackState.Media(
                playRequested = false,
                phase =
                    VexStreamPlaybackPhase.Buffering,
                playPauseAvailable = true,
            ).afterIntent(
                VexStreamPlaybackEffect.RequestPlay,
            )

        val projection =
            controls.projection(
                pendingState,
            )

        assertEquals(
            VexStreamPlaybackAct.Pause,
            projection[
                VexStreamPlaybackControl.PlayPause
            ].act,
        )
    }

}

// [VXG RealForever]
