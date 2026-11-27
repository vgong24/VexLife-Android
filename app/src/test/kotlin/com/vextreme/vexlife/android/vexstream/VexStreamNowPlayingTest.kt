package com.vextreme.vexlife.android.vexstream

import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackPhase
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackState
import com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerPartner
import com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Now Playing glance = Player Store identity/metadata + Playback Family intent. It disappears when
 * there is no media rather than pretending something is loaded.
 */
class VexStreamNowPlayingTest {
    @Test
    fun unavailableStoreOrNoMediaHasNoGlance() {
        assertNull(
            vexStreamNowPlaying(
                VexStreamPlayerStore.Snapshot.Unavailable,
                media(playRequested = true),
            ),
        )
        assertNull(
            vexStreamNowPlaying(
                snapshot(hasMedia = false),
                media(playRequested = true),
            ),
        )
        assertNull(
            vexStreamNowPlaying(
                snapshot(),
                VexStreamPlaybackState.NoMedia,
            ),
        )
    }

    @Test
    fun metadataComesFromThePlayerStoreAndIntentFromTheFamily() {
        val glance =
            requireNotNull(
                vexStreamNowPlaying(
                    snapshot(title = "Alright", artist = "Kendrick Lamar"),
                    media(playRequested = true),
                ),
            )

        assertEquals("Alright", glance.title)
        assertEquals("Kendrick Lamar", glance.artist)
        assertTrue(glance.playRequested)
        assertFalse(glance.transitioning)
        assertFalse(glance.ended)
    }

    @Test
    fun pendingFamilyIntentShowsAsTransitioning() {
        val glance =
            requireNotNull(
                vexStreamNowPlaying(
                    snapshot(),
                    media(playRequested = false, requestPending = true),
                ),
            )

        assertFalse(glance.playRequested)
        assertTrue(glance.transitioning)
    }

    @Test
    fun endedMediaIsNotPlayingAndOffersRestartOnlyWhenAvailable() {
        val glance =
            requireNotNull(
                vexStreamNowPlaying(
                    snapshot(),
                    VexStreamPlaybackState.Ended(restartAvailable = false),
                ),
            )

        assertTrue(glance.ended)
        assertFalse(glance.playRequested)
        assertFalse(glance.playPauseAvailable)
    }

    private fun media(
        playRequested: Boolean,
        requestPending: Boolean = false,
    ) =
        VexStreamPlaybackState.Media(
            playRequested = playRequested,
            requestPending = requestPending,
            phase = VexStreamPlaybackPhase.Ready,
            playPauseAvailable = true,
        )

    private fun snapshot(
        hasMedia: Boolean = true,
        title: String = "Track",
        artist: String = "Artist",
    ) =
        VexStreamPlayerStore.Snapshot.Available(
            playback =
                VexStreamPlayerPartner.Signal.Playback(
                    hasMedia = hasMedia,
                    playRequested = true,
                    phase = VexStreamPlayerPartner.PlaybackPhase.Ready,
                    playPauseAvailable = true,
                    restartAvailable = true,
                ),
            queueItems =
                VexStreamPlayerPartner.Signal.QueueItems(
                    items = emptyList(),
                ),
            queueNavigation =
                VexStreamPlayerPartner.Signal.QueueNavigation(
                    currentIndex = 0,
                    previousAvailable = false,
                    nextAvailable = false,
                    changeMediaItemsAvailable = true,
                ),
            repeat =
                VexStreamPlayerPartner.Signal.Repeat(
                    enabled = false,
                    toggleAvailable = true,
                ),
            currentMedia =
                VexStreamPlayerPartner.Signal.CurrentMedia(
                    mediaId = "media.1",
                    title = title,
                    artist = artist,
                    artworkUri = null,
                ),
        )
}

// [VXG RealForever][VEXSTREAM][NOW-PLAYING][GLANCE][PROOF]
