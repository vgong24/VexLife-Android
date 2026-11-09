package com.vextreme.vexlife.android.vexstream.playback

import com.vextreme.vexlife.android.execution.VexExecution
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
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
    fun partnerConnectionPublishesAdmittedFamilyCurrentBeforeEffectRealization() =
        runBlocking {
        val family =
            VexStreamPlaybackFamily(
                VexStreamPlaybackState.Media(
                    playRequested = false,
                    phase =
                        VexStreamPlaybackPhase.Ready,
                    playPauseAvailable = true,
                ),
            )
        val admission =
            VexStreamPlaybackAdmission(
                family,
            )
        val root =
            VexExecutionRoot(
                dispatcher =
                    Dispatchers.Unconfined,
            )
        val sequence =
            mutableListOf<String>()
        var admittedState:
            VexStreamPlaybackState? =
            null
        val connection =
            PartnerVexStreamPlaybackConnection(
                admission =
                    admission,
                executionRoot =
                    root,
                effectConnection =
                    VexStreamPlaybackEffectConnection {
                        sequence +=
                            "effect"
                        assertEquals(
                            family.currentState,
                            admittedState,
                        )
                    },
                onFamilyCurrent = { state ->
                    sequence +=
                        "state"
                    admittedState =
                        state
                },
            )

        val job =
            requireNotNull(
                connection.activate(
                    VexStreamPlaybackControl.PlayPause,
                ),
            )
        job.join()

        assertEquals(
            listOf(
                "state",
                "effect",
            ),
            sequence,
        )
        assertEquals(
            family.currentState,
            admittedState,
        )
        root.joinStopAll(
            "test complete",
        )
    }

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
            val admission =
                VexStreamPlaybackAdmission(
                    family,
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
                    admission =
                        admission,
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
    fun playbackAdmissionSerializesPostAdmissionCallbacksAcrossOrigins() {
        val family =
            VexStreamPlaybackFamily(
                VexStreamPlaybackState.Media(
                    playRequested = false,
                    phase =
                        VexStreamPlaybackPhase.Ready,
                    playPauseAvailable = true,
                ),
            )
        val admission =
            VexStreamPlaybackAdmission(
                family,
            )
        val firstCallbackEntered =
            java.util.concurrent.CountDownLatch(
                1,
            )
        val releaseFirstCallback =
            java.util.concurrent.CountDownLatch(
                1,
            )
        val secondStarted =
            java.util.concurrent.CountDownLatch(
                1,
            )
        val secondAdmitted =
            java.util.concurrent.CountDownLatch(
                1,
            )
        val executor =
            java.util.concurrent.Executors
                .newFixedThreadPool(
                    2,
                )

        try {
            val first =
                executor.submit {
                    admission.activate(
                        control =
                            VexStreamPlaybackControl.PlayPause,
                        onAdmitted = {
                            firstCallbackEntered
                                .countDown()
                            releaseFirstCallback
                                .await(
                                    5,
                                    java.util.concurrent.TimeUnit.SECONDS,
                                )
                        },
                    )
                }

            assertTrue(
                firstCallbackEntered.await(
                    5,
                    java.util.concurrent.TimeUnit.SECONDS,
                ),
            )

            val second =
                executor.submit {
                    secondStarted
                        .countDown()
                    admission.observeExact(
                        state =
                            VexStreamPlaybackState.NoMedia,
                        onAdmitted = {
                            secondAdmitted
                                .countDown()
                        },
                    )
                }

            assertTrue(
                secondStarted.await(
                    5,
                    java.util.concurrent.TimeUnit.SECONDS,
                ),
            )
            assertFalse(
                secondAdmitted.await(
                    100,
                    java.util.concurrent.TimeUnit.MILLISECONDS,
                ),
            )

            releaseFirstCallback
                .countDown()

            first.get(
                5,
                java.util.concurrent.TimeUnit.SECONDS,
            )
            second.get(
                5,
                java.util.concurrent.TimeUnit.SECONDS,
            )

            assertTrue(
                secondAdmitted.await(
                    5,
                    java.util.concurrent.TimeUnit.SECONDS,
                ),
            )
            assertEquals(
                VexStreamPlaybackState.NoMedia,
                family.currentState,
            )
        } finally {
            releaseFirstCallback
                .countDown()
            executor.shutdownNow()
        }
    }

    @Test
    fun playPauseResolvesAgainstExactCurrentStateAndAdmitsPendingIntent() {
        val family =
            VexStreamPlaybackFamily(
                VexStreamPlaybackState.Media(
                    playRequested = false,
                    phase =
                        VexStreamPlaybackPhase
                            .Buffering,
                    playPauseAvailable = true,
                ),
            )

        val play =
            family.activate(
                VexStreamPlaybackControl
                    .PlayPause,
            )

        assertSame(
            VexStreamPlaybackEffect
                .RequestPlay,
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
                    VexStreamPlaybackPhase
                        .Buffering,
                playPauseAvailable = true,
            ),
            family.currentState,
        )

        val pause =
            family.activate(
                VexStreamPlaybackControl
                    .PlayPause,
            )

        assertSame(
            VexStreamPlaybackEffect
                .RequestPause,
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
                VexStreamPlaybackControl
                    .PlayPause,
            ),
        )
        assertEquals(
            null,
            family.activate(
                VexStreamPlaybackControl
                    .PlayPause,
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
                VexStreamPlaybackControl
                    .PlayPause,
            ),
        )
        val restart =
            available
                .activate(
                    VexStreamPlaybackControl
                        .PlayPause,
                )

        assertSame(
            VexStreamPlaybackEffect
                .RestartCurrent,
            restart
                ?.effect,
        )
        assertEquals(
            VexExecution.Approach.Latest,
            restart
                ?.approach,
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
                VexStreamPlaybackControl
                    .PlayPause,
            ),
        )
    }
}

// [VXG RealForever][VEXSTREAM][PLAYBACK][DIRECT-FAMILY]
