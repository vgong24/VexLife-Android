package com.vextreme.vexlife.android.widget.vexstream

import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackPhase
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackState
import com.vextreme.vexlife.android.vexstream.queue.VexStreamQueueState
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
                playbackState =
                    familyState,
                queueState =
                    queueState(),

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
    fun queueFamilyOriginOwnsPreviousAndNextAvailability() {
        val output =
            vexStreamPlaybackWidgetRelayOutput(
                player =
                    playerInput(),
                playbackState =
                    VexStreamPlaybackState.Media(
                        playRequested = false,
                        phase =
                            VexStreamPlaybackPhase.Ready,
                        playPauseAvailable = true,
                    ),
                queueState =
                    queueState(
                        previousAvailable =
                            true,
                        nextAvailable =
                            false,
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
                playbackState =
                    VexStreamPlaybackState.NoMedia,
                queueState =
                    VexStreamQueueState.Empty,
            )

        assertSame(
            VexStreamPlaybackState.NoMedia,
            output.playback.state,
        )
        assertFalse(
            output.playback.playPauseAvailable,
        )
    }

    private fun queueState(
        previousAvailable:
            Boolean =
            true,
        nextAvailable:
            Boolean =
            false,
    ):
        VexStreamQueueState =
        VexStreamQueueState.NonEmpty(
            previousAvailable =
                previousAvailable,
            nextAvailable =
                nextAvailable,
            shuffleAvailable =
                false,
            clearAvailable =
                true,
        )

    private fun playerInput():
        VexStreamPlaybackWidgetPlayerInput =
        VexStreamPlaybackWidgetPlayerInput(
            mediaId = "track-1",
            title = "Track 1",
            artist = "Artist",
            artworkUri = "file:///artwork",
        )
}

// [VXG RealForever][VEXSTREAM][WIDGET][RELAY][TEST]
