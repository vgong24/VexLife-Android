package com.vextreme.vexlife.android.vexstream.playback

import com.vextreme.vexlife.android.execution.VexExecution
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerPartner
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class VexStreamPlaybackFamilyTest {
    @Test
    fun acceptedLatestActUsesVexExecutionToCancelTheOlderPlaybackRealization() =
        runBlocking {
            val root =
                VexExecutionRoot()
            val family =
                VexStreamPlaybackFamily(
                    VexStreamPlaybackState.Media(
                        playRequested = false,
                        phase =
                            VexStreamPlaybackPhase.Ready,
                        playPauseAvailable = true,
                    ),
                )
            val firstStarted =
                CompletableDeferred<Unit>()
            val firstCancelled =
                CompletableDeferred<Unit>()
            val secondEffect =
                CompletableDeferred<
                    VexStreamPlaybackEffect
                >()
            var realization =
                0
            val connection =
                PartnerVexStreamPlaybackConnection(
                    family =
                        family,
                    executionRoot =
                        root,
                    effectConnection =
                        VexStreamPlaybackEffectConnection { effect ->
                            realization +=
                                1
                            if (
                                realization ==
                                1
                            ) {
                                firstStarted.complete(
                                    Unit,
                                )
                                try {
                                    awaitCancellation()
                                } finally {
                                    firstCancelled.complete(
                                        Unit,
                                    )
                                }
                            } else {
                                secondEffect.complete(
                                    effect,
                                )
                            }
                        },
                )

            requireNotNull(
                connection.activate(
                    VexStreamPlaybackControl.PlayPause,
                ),
            )
            withTimeout(
                5_000L,
            ) {
                firstStarted.await()
            }

            val secondJob =
                requireNotNull(
                    connection.activate(
                        VexStreamPlaybackControl.PlayPause,
                    ),
                )

            withTimeout(
                5_000L,
            ) {
                firstCancelled.await()
            }
            assertSame(
                VexStreamPlaybackEffect.RequestPause,
                withTimeout(
                    5_000L,
                ) {
                    secondEffect.await()
                },
            )
            secondJob.join()
            root.joinStopAll(
                "test complete",
            )
        }

    @Test
    fun providerObservationCannotEraseANewerPendingPlayPause() {
        val family =
            VexStreamPlaybackFamily(
                VexStreamPlaybackState.Media(
                    playRequested = false,
                    phase =
                        VexStreamPlaybackPhase.Ready,
                    playPauseAvailable = true,
                ),
            )

        family.activatePlayback(
            VexStreamPlaybackControl.PlayPause,
        )

        val stale =
            family.observePlayer(
                playbackSignal(
                    playRequested = false,
                ),
            ) as
                VexStreamPlaybackState.Media

        assertTrue(
            stale.playRequested,
        )
        assertTrue(
            stale.requestPending,
        )

        val acknowledged =
            family.observePlayer(
                playbackSignal(
                    playRequested = true,
                ),
            ) as
                VexStreamPlaybackState.Media

        assertTrue(
            acknowledged.playRequested,
        )
        assertFalse(
            acknowledged.requestPending,
        )
    }

    @Test
    fun playPauseResolvesAgainstExactCurrentStateAndAdmitsPendingIntent() {
        val family =
            VexStreamPlaybackFamily(
                VexStreamPlaybackState.Media(
                    playRequested = false,
                    phase =
                        VexStreamPlaybackPhase.Buffering,
                    playPauseAvailable = true,
                ),
            )

        val play =
            family.activate(
                VexStreamPlaybackControl.PlayPause,
            )

        assertSame(
            VexStreamPlaybackEffect.RequestPlay,
            play?.effect,
        )
        assertEquals(
            VexExecution.Approach.Latest,
            play?.approach,
        )
        assertEquals(
            VexStreamPlaybackState.Media(
                playRequested = true,
                requestPending = true,
                phase =
                    VexStreamPlaybackPhase.Buffering,
                playPauseAvailable = true,
            ),
            family.currentState,
        )

        val pause =
            family.activate(
                VexStreamPlaybackControl.PlayPause,
            )

        assertSame(
            VexStreamPlaybackEffect.RequestPause,
            pause?.effect,
        )
        assertEquals(
            VexExecution.Approach.Latest,
            pause?.approach,
        )
        assertEquals(
            play?.relationshipId,
            pause?.relationshipId,
        )
        assertEquals(
            false,
            (
                family.currentState as
                    VexStreamPlaybackState.Media
            ).playRequested,
        )
    }

    @Test
    fun noMediaCannotActivatePlayPause() {
        val family =
            VexStreamPlaybackFamily()

        assertFalse(
            family.isAvailable(
                VexStreamPlaybackControl.PlayPause,
            ),
        )
        assertEquals(
            null,
            family.activate(
                VexStreamPlaybackControl.PlayPause,
            ),
        )
    }

    @Test
    fun endedPlaybackRestartsOnlyWhenProviderAllowsIt() {
        val available =
            VexStreamPlaybackFamily(
                VexStreamPlaybackState.Ended(
                    restartAvailable = true,
                ),
            )

        assertTrue(
            available.isAvailable(
                VexStreamPlaybackControl.PlayPause,
            ),
        )
        val restart =
            available.activate(
                VexStreamPlaybackControl.PlayPause,
            )

        assertSame(
            VexStreamPlaybackEffect.RestartCurrent,
            restart?.effect,
        )
        assertEquals(
            VexExecution.Approach.Latest,
            restart?.approach,
        )
        assertEquals(
            VexStreamPlaybackState.Ended(
                restartAvailable = true,
                restartPending = true,
            ),
            available.currentState,
        )

        val blocked =
            VexStreamPlaybackFamily(
                VexStreamPlaybackState.Ended(
                    restartAvailable = false,
                ),
            )

        assertFalse(
            blocked.isAvailable(
                VexStreamPlaybackControl.PlayPause,
            ),
        )
    }

    private fun playbackSignal(
        playRequested:
            Boolean,
    ):
        VexStreamPlayerPartner.Signal.Playback =
        VexStreamPlayerPartner.Signal.Playback(
            hasMedia = true,
            playRequested =
                playRequested,
            phase =
                VexStreamPlayerPartner.PlaybackPhase.Ready,
            playPauseAvailable = true,
            restartAvailable = true,
        )
}

// [VXG RealForever][VEXSTREAM][PLAYBACK][DIRECT-FAMILY]
