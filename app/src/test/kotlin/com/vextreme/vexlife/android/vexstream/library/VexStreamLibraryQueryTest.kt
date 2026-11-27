package com.vextreme.vexlife.android.vexstream.library

import com.vextreme.vexlife.android.data.vexstream.VexStreamLocalCopyState
import com.vextreme.vexlife.android.data.vexstream.VexStreamMediaSha256
import com.vextreme.vexlife.android.data.vexstream.VexStreamMusicState
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrack
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackRef
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackState
import com.vextreme.vexlife.android.screens.shared.vexstream.toVexStreamLibraryProjection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class VexStreamLibraryQueryTest {
    private val alright =
        track(
            ref = "alright",
            title = "Alright",
            artist = "Kendrick Lamar",
            album = "To Pimp a Butterfly",
        )
    private val alrightLive =
        track(
            ref = "alright-live",
            title = "Alright (Live)",
            artist = "Kendrick Lamar",
        )
    private val notAlright =
        track(
            ref = "not-alright",
            title = "Not Alright",
            artist = "Someone",
        )
    private val humble =
        track(
            ref = "humble",
            title = "HUMBLE.",
            artist = "Kendrick Lamar",
            album = "DAMN.",
            downloaded = true,
            hubAvailable = false,
        )
    private val supergrassAlright =
        track(
            ref = "supergrass-alright",
            title = "Alright",
            artist = "Supergrass",
        )
    private val unavailable =
        track(
            ref = "unavailable",
            title = "Alright",
            artist = "Ghost",
            hubAvailable = false,
        )

    @Test
    fun exactTitleOutranksPrefixAndContains() {
        val result =
            state(
                notAlright,
                alrightLive,
                alright,
            ).queryVexStreamLibrary(
                " alright ",
            )

        assertEquals(
            "alright",
            result.query,
        )
        assertEquals(
            listOf(
                alright,
                alrightLive,
                notAlright,
            ),
            result.matches.map { it.track },
        )
        assertSame(
            alright,
            result.single,
        )
        assertFalse(
            result.ambiguous,
        )
    }

    @Test
    fun sameTitleFromTwoArtistsIsAmbiguousNotGuessed() {
        val result =
            state(
                alright,
                supergrassAlright,
            ).queryVexStreamLibrary(
                "Alright",
            )

        assertTrue(
            result.ambiguous,
        )
        assertNull(
            result.single,
        )
        assertEquals(
            setOf(
                alright,
                supergrassAlright,
            ),
            result.best.map { it.track }.toSet(),
        )
    }

    @Test
    fun titleOutranksOtherFieldsAtEqualStrength() {
        val titled =
            track(
                ref = "kendrick-interlude",
                title = "Kendrick Interlude",
                artist = "Various",
            )
        val result =
            state(
                humble,
                titled,
            ).queryVexStreamLibrary(
                "kendrick",
            )

        assertEquals(
            VexStreamLibraryMatchField.Title,
            result.best.single().field,
        )
        assertSame(
            titled,
            result.single,
        )
        assertEquals(
            VexStreamLibraryMatchField.Artist,
            result.matches.last().field,
        )
    }

    @Test
    fun artistRequestReturnsEveryArtistTrackAsOneRank() {
        val result =
            state(
                alright,
                humble,
                supergrassAlright,
            ).queryVexStreamLibrary(
                "kendrick lamar",
            )

        assertEquals(
            listOf(
                alright,
                humble,
            ),
            result.best.map { it.track },
        )
        assertTrue(
            result.best.all {
                it.field ==
                    VexStreamLibraryMatchField.Artist &&
                    it.strength ==
                    VexStreamLibraryMatchStrength.Exact
            },
        )
    }

    @Test
    fun downloadedScopeIsOnlyVerifiedLocalCopies() {
        val music =
            state(
                alright,
                humble,
                unavailable,
            )

        assertEquals(
            listOf(humble),
            music.tracksIn(
                VexStreamLibraryScope.Downloaded,
            ),
        )
        assertEquals(
            listOf(humble),
            music
                .queryVexStreamLibrary(
                    "kendrick",
                    scope =
                        VexStreamLibraryScope.Downloaded,
                )
                .matches
                .map { it.track },
        )
    }

    @Test
    fun unavailableTracksAndBlankQueriesNeverMatch() {
        val music =
            state(
                unavailable,
            )

        assertTrue(
            music.queryVexStreamLibrary(
                "Alright",
            ).isEmpty,
        )
        assertTrue(
            state(
                alright,
            ).queryVexStreamLibrary(
                "   ",
            ).isEmpty,
        )
    }

    @Test
    fun humanSearchMembershipEqualsQueryMembership() {
        val music =
            state(
                alright,
                alrightLive,
                notAlright,
                humble,
                supergrassAlright,
                unavailable,
            )

        listOf(
            "alright",
            "KENDRICK",
            "damn",
            "live",
            "zzz",
        ).forEach { query ->
            assertEquals(
                "membership for '$query'",
                music
                    .toVexStreamLibraryProjection(
                        searchQuery =
                            query,
                    )
                    .searchResults
                    .toSet(),
                music
                    .queryVexStreamLibrary(
                        query,
                    )
                    .matches
                    .map { it.track }
                    .toSet(),
            )
        }
    }

    private fun state(
        vararg tracks: VexStreamTrackState,
    ): VexStreamMusicState =
        VexStreamMusicState(
            tracks =
                tracks.toList(),
        )

    private fun track(
        ref: String,
        title: String,
        artist: String,
        album: String = "",
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
            "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
    }
}

// [VXG RealForever][VEXSTREAM][LIBRARY][QUERY][PROOF]
