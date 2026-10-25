package com.vextreme.vexlife.android.data.vexstream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VexStreamHubArtworkContractTest {

    @Test
    fun embeddedArtworkRefProjectsAsOpaqueTrackMetadata() {
        val projection =
            VexStreamHubTrackProjection(
                trackRef =
                    VexStreamTrackRef(
                        "track.embedded",
                    ),
                mediaSha256 =
                    VexStreamMediaSha256(
                        "a".repeat(
                            64,
                        ),
                    ),
                mediaByteLength =
                    4_096L,
                title =
                    "Embedded",
                artist =
                    "Vex",
                artworkRef =
                    "track.embedded",
            )

        val track =
            projection.toTrack()

        assertEquals(
            "track.embedded",
            track.artworkRef,
        )
        assertNull(
            track.artworkUrl,
        )
    }

    @Test
    fun embeddedArtworkRefSurvivesHubCatalogMerge() {
        val projection =
            VexStreamHubTrackProjection(
                trackRef =
                    VexStreamTrackRef(
                        "track.merge",
                    ),
                mediaSha256 =
                    VexStreamMediaSha256(
                        "b".repeat(
                            64,
                        ),
                    ),
                mediaByteLength =
                    8_192L,
                title =
                    "Merge",
                artist =
                    "Vex",
                artworkRef =
                    "track.merge",
            )

        val state =
            VexStreamMusicState.Empty
                .withHubCatalog(
                    VexStreamHubCatalogProjection(
                        hubRef =
                            "vexstream.desktop.local",
                        sourceVersion =
                            "4.0.0",
                        catalogRevision =
                            "catalog.artwork",
                        tracks =
                            listOf(
                                projection,
                            ),
                    ),
                )

        assertEquals(
            "track.merge",
            state.tracks
                .single()
                .track
                .artworkRef,
        )
    }

    @Test
    fun artworkRefRejectsPathTraversalShape() {
        var failed =
            false
        try {
            VexStreamTrack(
                trackRef =
                    VexStreamTrackRef(
                        "track.bad-artwork",
                    ),
                mediaSha256 =
                    VexStreamMediaSha256(
                        "c".repeat(
                            64,
                        ),
                    ),
                mediaByteLength =
                    1L,
                title =
                    "Bad artwork",
                artist =
                    "Vex",
                artworkRef =
                    "../private",
            )
        } catch (_: IllegalArgumentException) {
            failed =
                true
        }

        assertTrue(
            failed,
        )
    }
}

// [VXG RealForever]
