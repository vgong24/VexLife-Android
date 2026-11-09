package com.vextreme.vexlife.android.vexstream.playback

import com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerPartner
import com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VexStreamPlaybackExperienceRelayTest {
    @Test
    fun familyOnlyAdmissionRetainsTheExactBoundedPlayerReadCurrent() {
        val family =
            VexStreamPlaybackFamily(
                VexStreamPlaybackState.Media(
                    playRequested = false,
                    phase =
                        VexStreamPlaybackPhase.Ready,
                    playPauseAvailable = true,
                ),
            )
        val relay =
            VexStreamPlaybackExperienceRelay(
                playerStore =
                    VexStreamPlayerStore(),
                playbackFamily =
                    family,
            )
        val playerCurrent =
            playerCurrent(
                queueSize =
                    500,
            )

        relay.admitProviderRoundTrip(
            playerCurrent =
                playerCurrent,
            familyState =
                family.currentState,
        )

        family.activate(
            VexStreamPlaybackControl.PlayPause,
        )
        relay.admitFamilyCurrent(
            family.currentState,
        )

        val current =
            relay.currentOutput
        assertEquals(
            "track-1",
            current.readState
                .currentMediaId,
        )
        assertEquals(
            500,
            current.readState
                .queueSize,
        )
        assertEquals(
            37,
            current.readState
                .currentIndex,
        )
        val playback =
            current.playbackProjection
                .state as
                VexStreamPlaybackState.Media
        assertTrue(
            playback.playRequested,
        )
        assertTrue(
            playback.requestPending,
        )
    }

    private fun playerCurrent(
        queueSize:
            Int,
    ):
        VexStreamPlayerPartner.Current.Available {
        val items =
            (0 until queueSize)
                .map { index ->
                    VexStreamPlayerPartner.Signal.QueueItems.Item(
                        mediaId =
                            "track-$index",
                        title =
                            "Track $index",
                        artist =
                            "Artist",
                    )
                }

        return VexStreamPlayerPartner.Current.Available(
            playback =
                VexStreamPlayerPartner.Signal.Playback(
                    hasMedia = true,
                    playRequested = false,
                    phase =
                        VexStreamPlayerPartner.PlaybackPhase.Ready,
                    playPauseAvailable = true,
                    restartAvailable = true,
                ),
            queueItems =
                VexStreamPlayerPartner.Signal.QueueItems(
                    items,
                ),
            queueNavigation =
                VexStreamPlayerPartner.Signal.QueueNavigation(
                    currentIndex = 37,
                    previousAvailable = true,
                    nextAvailable = true,
                    shuffleAvailable = true,
                    clearAvailable = true,
                ),
            repeat =
                VexStreamPlayerPartner.Signal.Repeat(
                    enabled = false,
                    toggleAvailable = true,
                ),
            currentMedia =
                VexStreamPlayerPartner.Signal.CurrentMedia(
                    mediaId = "track-1",
                    title = "Track 1",
                    artist = "Artist",
                    artworkUri = "file:///artwork",
                ),
        )
    }
}

// [VXG RealForever][VEXSTREAM][PLAYBACK][EXPERIENCE-RELAY][TEST]
