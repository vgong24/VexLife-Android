package com.vextreme.vexlife.android.screens.shared.vexstream

import com.vextreme.vexlife.android.data.vexstream.VexStreamMediaSha256
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrack
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackRef
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackReadState
import org.junit.Assert.assertEquals
import org.junit.Test

class VexStreamPlaybackReadStateTest {

    @Test
    fun progressProjectionClampsToThePlayableTimeline() {
        assertEquals(
            0.25f,
            VexStreamPlaybackReadState(
                currentPositionMs = 30_000L,
                durationMs = 120_000L,
            ).progressFraction,
            0.0001f,
        )

        assertEquals(
            1.0f,
            VexStreamPlaybackReadState(
                currentPositionMs = 150_000L,
                durationMs = 120_000L,
            ).progressFraction,
            0.0001f,
        )

        assertEquals(
            0.0f,
            VexStreamPlaybackReadState(
                currentPositionMs = 30_000L,
                durationMs = 0L,
            ).progressFraction,
            0.0001f,
        )
    }

    @Test
    fun mediaMetadataCarriesOptionalArtworkUriWithoutChangingTrackIdentity() {
        val metadata =
            vexStreamMediaMetadata(
                VexStreamTrack(
                    trackRef =
                        VexStreamTrackRef(
                            "track.artwork",
                        ),
                    mediaSha256 =
                        VexStreamMediaSha256(
                            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                        ),
                    mediaByteLength = 4_096,
                    title = "Artwork Track",
                    artist = "Vex",
                    artworkUrl =
                        "https://images.example.test/artwork.jpg",
                ),
            )

        assertEquals(
            "Artwork Track",
            metadata.title?.toString(),
        )
        assertEquals(
            "Vex",
            metadata.artist?.toString(),
        )
        assertEquals(
            "https://images.example.test/artwork.jpg",
            metadata.artworkUri?.toString(),
        )
    }

    @Test
    fun mediaMetadataLeavesArtworkAbsentWhenTrackHasNone() {
        val metadata =
            vexStreamMediaMetadata(
                VexStreamTrack(
                    trackRef =
                        VexStreamTrackRef(
                            "track.no-artwork",
                        ),
                    mediaSha256 =
                        VexStreamMediaSha256(
                            "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
                        ),
                    mediaByteLength = 4_096,
                    title = "No Artwork",
                    artist = "Vex",
                ),
            )

        assertEquals(
            null,
            metadata.artworkUri,
        )
    }

    @Test
    fun playbackTimeFormatsCompactPhoneDurations() {
        assertEquals(
            "0:00",
            formatVexStreamPlaybackTime(0L),
        )
        assertEquals(
            "1:05",
            formatVexStreamPlaybackTime(65_000L),
        )
        assertEquals(
            "1:01:01",
            formatVexStreamPlaybackTime(3_661_000L),
        )
    }
}

// [VXG RealForever]
