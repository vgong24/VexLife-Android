package com.vextreme.vexlife.android.screens.shared.vexstream

import com.vextreme.vexlife.android.data.vexstream.VexStreamLocalCopyState
import com.vextreme.vexlife.android.data.vexstream.VexStreamMediaSha256
import com.vextreme.vexlife.android.data.vexstream.VexStreamMusicState
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrack
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackRef
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackState
import org.junit.Assert.assertEquals
import org.junit.Test

class VexStreamLibraryProjectionTest {

    @Test
    fun projectionUsesPlayableTruthAndSearchesHumanMetadata() {
        val alpha =
            track(
                ref = "alpha",
                title = "Alpha",
                artist = "Vex",
                album = "Home",
                genre = "Ambient",
                year = "2026",
            )
        val downloaded =
            track(
                ref = "downloaded",
                title = "Beta",
                artist = "Vex",
                hubAvailable = false,
                downloaded = true,
            )
        val unavailable =
            track(
                ref = "unavailable",
                title = "Hidden",
                artist = "Other",
                hubAvailable = false,
            )
        val state =
            VexStreamMusicState(
                tracks =
                    listOf(
                        alpha,
                        downloaded,
                        unavailable,
                    ),
            )

        val projection =
            state.toVexStreamLibraryProjection(
                searchQuery = " ambient ",
            )

        assertEquals(
            listOf(
                alpha,
                downloaded,
            ),
            projection.playableTracks,
        )
        assertEquals(
            listOf(alpha),
            projection.searchResults,
        )
        assertEquals(
            listOf(alpha),
            state
                .toVexStreamLibraryProjection(
                    searchQuery = "2026",
                )
                .searchResults,
        )
    }

    @Test
    fun songAndDownloadPresentationOrderingStayIndependent() {
        val zeta =
            track(
                ref = "zeta",
                title = "Zeta",
                artist = "Vex",
                downloaded = true,
            )
        val alpha =
            track(
                ref = "alpha",
                title = "alpha",
                artist = "Vex",
            )
        val beta =
            track(
                ref = "beta",
                title = "Beta",
                artist = "Vex",
                downloaded = true,
            )
        val tracks =
            listOf(
                zeta,
                alpha,
                beta,
            )

        assertEquals(
            listOf(
                alpha,
                beta,
                zeta,
            ),
            tracks.vexStreamSortedSongs(),
        )
        assertEquals(
            listOf(
                beta,
                zeta,
            ),
            tracks.vexStreamSortedDownloads(),
        )
    }

    private fun track(
        ref: String,
        title: String,
        artist: String,
        album: String = "",
        genre: String = "",
        year: String = "",
        hubAvailable: Boolean = true,
        downloaded: Boolean = false,
    ): VexStreamTrackState {
        val track =
            VexStreamTrack(
                trackRef =
                    VexStreamTrackRef(ref),
                mediaSha256 =
                    VexStreamMediaSha256(DIGEST),
                mediaByteLength = 1_234,
                title = title,
                artist = artist,
                album = album,
                genre = genre,
                year = year,
            )

        return VexStreamTrackState(
            track = track,
            hubAvailable = hubAvailable,
            localCopy =
                if (downloaded) {
                    VexStreamLocalCopyState.Downloaded(
                        mediaSha256 =
                            track.mediaSha256,
                        byteLength =
                            track.mediaByteLength,
                        localMediaRef =
                            "local.$ref",
                    )
                } else {
                    VexStreamLocalCopyState.NotDownloaded
                },
        )
    }

    private companion object {
        const val DIGEST =
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    }
}

// [VXG RealForever]
