package com.vextreme.vexlife.android.widget.vexstream

import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackPhase
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class VexStreamPlaybackWidgetRelayTest {
    @Test
    fun relayOutputPreservesTheExactFamilyCurrentInsteadOfReinterpretingPlayerSignal() {
        val familyState =
            VexStreamPlaybackState.Media(
                playRequested = true,
                requestPending = true,
                phase =
                    VexStreamPlaybackPhase.Ready,
                playPauseAvailable = true,
            )

        val output =
            vexStreamPlaybackWidgetRelayOutput(
                player =
                    playerInput(),
                familyState =
                    familyState,
            )

        assertSame(
            familyState,
            output.playback.state,
        )
        assertTrue(
            (
                output.playback.state as
                    VexStreamPlaybackState.Media
            ).requestPending,
        )
    }

    @Test
    fun playerOriginContributesOnlyTheBoundedWidgetFacts() {
        val output =
            vexStreamPlaybackWidgetRelayOutput(
                player =
                    playerInput(),
                familyState =
                    VexStreamPlaybackState.Media(
                        playRequested = false,
                        phase =
                            VexStreamPlaybackPhase.Ready,
                        playPauseAvailable = true,
                    ),
            )

        assertEquals(
            "track-1",
            output.mediaId,
        )
        assertEquals(
            "Track 1",
            output.title,
        )
        assertTrue(
            output.previousEnabled,
        )
        assertFalse(
            output.nextEnabled,
        )
    }

    @Test
    fun familyNoMediaRemainsAuthoritativeEvenIfPlayerInputStillHasMedia() {
        val output =
            vexStreamPlaybackWidgetRelayOutput(
                player =
                    playerInput(),
                familyState =
                    VexStreamPlaybackState.NoMedia,
            )

        assertSame(
            VexStreamPlaybackState.NoMedia,
            output.playback.state,
        )
        assertFalse(
            output.playback.playPauseAvailable,
        )
    }

    private fun playerInput():
        VexStreamPlaybackWidgetPlayerInput =
        VexStreamPlaybackWidgetPlayerInput(
            mediaId = "track-1",
            title = "Track 1",
            artist = "Artist",
            artworkUri = "file:///artwork",
            previousAvailable = true,
            nextAvailable = false,
        )
}

// [VXG RealForever][VEXSTREAM][WIDGET][RELAY][TEST]
