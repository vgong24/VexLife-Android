package com.vextreme.vexlife.android.widget.vexstream

import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackPhase
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackState
import com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerPartner
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class VexStreamPlaybackWidgetRelayTest {
    @Test
    fun pendingFamilyIntentSurvivesAStalePlayerStatement() {
        val output =
            vexStreamPlaybackWidgetRelayOutput(
                player =
                    playerInput(
                        playRequested =
                            false,
                    ),
                familyState =
                    VexStreamPlaybackState.Media(
                        playRequested = true,
                        requestPending = true,
                        phase =
                            VexStreamPlaybackPhase.Ready,
                        playPauseAvailable = true,
                    ),
            )

        val state =
            output.playback.state as
                VexStreamPlaybackState.Media

        assertTrue(
            state.playRequested,
        )
        assertTrue(
            state.requestPending,
        )
        assertTrue(
            output.previousEnabled,
        )
        assertFalse(
            output.nextEnabled,
        )
    }

    @Test
    fun matchingPlayerAcknowledgementClearsPendingInTheRelayOutput() {
        val output =
            vexStreamPlaybackWidgetRelayOutput(
                player =
                    playerInput(
                        playRequested =
                            true,
                    ),
                familyState =
                    VexStreamPlaybackState.Media(
                        playRequested = true,
                        requestPending = true,
                        phase =
                            VexStreamPlaybackPhase.Buffering,
                        playPauseAvailable = true,
                    ),
            )

        val state =
            output.playback.state as
                VexStreamPlaybackState.Media

        assertTrue(
            state.playRequested,
        )
        assertFalse(
            state.requestPending,
        )
    }

    @Test
    fun unavailablePlayerSealsTheWidgetRelationshipAsNoMedia() {
        val output =
            vexStreamPlaybackWidgetRelayOutput(
                player =
                    VexStreamPlaybackWidgetPlayerInput
                        .Unavailable,
                familyState =
                    VexStreamPlaybackState.Media(
                        playRequested = true,
                        requestPending = true,
                        phase =
                            VexStreamPlaybackPhase.Ready,
                        playPauseAvailable = true,
                    ),
            )

        assertSame(
            VexStreamPlaybackState.NoMedia,
            output.playback.state,
        )
        assertFalse(
            output.playback.playPauseAvailable,
        )
    }

    private fun playerInput(
        playRequested:
            Boolean,
    ):
        VexStreamPlaybackWidgetPlayerInput =
        VexStreamPlaybackWidgetPlayerInput(
            playback =
                VexStreamPlayerPartner.Signal.Playback(
                    hasMedia = true,
                    playRequested =
                        playRequested,
                    phase =
                        VexStreamPlayerPartner.PlaybackPhase
                            .Ready,
                    playPauseAvailable = true,
                    restartAvailable = true,
                ),
            mediaId = "track-1",
            title = "Track 1",
            artist = "Artist",
            artworkUri = "file:///artwork",
            previousAvailable = true,
            nextAvailable = false,
        )
}

// [VXG RealForever][VEXSTREAM][WIDGET][RELAY][TEST]
