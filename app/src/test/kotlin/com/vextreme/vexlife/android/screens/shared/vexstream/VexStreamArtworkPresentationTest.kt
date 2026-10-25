package com.vextreme.vexlife.android.screens.shared.vexstream

import com.vextreme.vexlife.android.data.vexstream.VexStreamArtworkStore
import com.vextreme.vexlife.android.data.vexstream.VexStreamLocalCopyState
import com.vextreme.vexlife.android.data.vexstream.VexStreamMediaSha256
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrack
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackRef
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackState
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VexStreamArtworkPresentationTest {

    @Test
    fun downloadedTrackPrefersDurableLocalArtworkOverRemoteSource() {
        val filesDir =
            Files.createTempDirectory(
                "vexstream-artwork-presentation-test",
            ).toFile()
        try {
            val state =
                downloadedState()
            val stored =
                requireNotNull(
                    VexStreamArtworkStore
                        .production(
                            filesDir,
                        )
                        .store(
                            track =
                                state.track,
                            input =
                                ByteArrayInputStream(
                                    byteArrayOf(
                                        1,
                                        2,
                                        3,
                                    ),
                                ),
                        ),
                )

            val model =
                state.vexStreamArtworkModel(
                    filesDir,
                )

            assertTrue(
                model is File,
            )
            assertEquals(
                stored,
                model,
            )
        } finally {
            filesDir.deleteRecursively()
        }
    }

    @Test
    fun remoteArtworkRemainsFallbackWhenNoDurableCompanionExists() {
        val filesDir =
            Files.createTempDirectory(
                "vexstream-artwork-fallback-test",
            ).toFile()
        try {
            val state =
                downloadedState()

            assertEquals(
                state.track.artworkUrl,
                state.vexStreamArtworkModel(
                    filesDir,
                ),
            )
        } finally {
            filesDir.deleteRecursively()
        }
    }

    private fun downloadedState():
        VexStreamTrackState {
        val digest =
            VexStreamMediaSha256(
                "b".repeat(
                    64,
                ),
            )
        val track =
            VexStreamTrack(
                trackRef =
                    VexStreamTrackRef(
                        "track.artwork.presentation",
                    ),
                mediaSha256 =
                    digest,
                mediaByteLength =
                    4_096L,
                title =
                    "Artwork",
                artist =
                    "Vex",
                artworkUrl =
                    "https://images.example.test/artwork.jpg",
            )

        return VexStreamTrackState(
            track =
                track,
            hubAvailable =
                false,
            localCopy =
                VexStreamLocalCopyState.Downloaded(
                    mediaSha256 =
                        digest,
                    byteLength =
                        track.mediaByteLength,
                    localMediaRef =
                        "content://media/external_primary/audio/media/42",
                ),
        )
    }
}

// [VXG RealForever]
