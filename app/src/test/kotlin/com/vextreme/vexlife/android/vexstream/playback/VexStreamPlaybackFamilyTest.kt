package com.vextreme.vexlife.android.vexstream.playback

import com.vextreme.vexlife.android.execution.VexExecution
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class VexStreamPlaybackFamilyTest {
    @Test
    fun partnerConnectionPublishesAdmittedFamilyCurrentBeforeEffectRealization() {
        val family =
            VexStreamPlaybackFamily(
                VexStreamPlaybackState.Media(
                    playRequested = false,
                    phase =
                        VexStreamPlaybackPhase.Ready,
                    playPauseAvailable = true,
                ),
            )
        val sequence =
            mutableListOf<String>()
        var admittedState:
            VexStreamPlaybackState? =
            null
        val connection =
            PartnerVexStreamPlaybackConnection(
                family =
                    family,
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

        connection.activate(
            VexStreamPlaybackControl.PlayPause,
        )

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
