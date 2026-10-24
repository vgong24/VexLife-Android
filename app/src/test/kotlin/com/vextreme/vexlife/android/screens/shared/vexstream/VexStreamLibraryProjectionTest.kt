package com.vextreme.vexlife.android.screens.shared.vexstream

import com.vextreme.vexlife.android.data.vexstream.VexStreamLocalCopyState
import com.vextreme.vexlife.android.data.vexstream.VexStreamMediaSha256
import com.vextreme.vexlife.android.data.vexstream.VexStreamMusicState
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrack
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackAvailability
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackRef
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackState
import com.vextreme.vexlife.android.vexstream.collection.VexStreamCollectionPlaybackOrder
import com.vextreme.vexlife.android.vexstream.track.VexStreamTrackPlayRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
    fun durationFilterExcludesUnknownAndComposesWithSearchAndCategories() {
        val shortAmbient =
            track(
                ref = "short",
                title = "Short Ambient",
                artist = "Vex",
                genre = "Ambient",
                durationSeconds = 8 * 60.0,
                downloaded = true,
            )
        val mediumAmbient =
            track(
                ref = "medium",
                title = "Medium Ambient",
                artist = "Other",
                genre = "Ambient",
                durationSeconds = 25 * 60.0,
            )
        val longRock =
            track(
                ref = "long",
                title = "Long Rock Mix",
                artist = "Vex",
                genre = "Rock",
                durationSeconds = 45 * 60.0,
            )
        val unknownAmbient =
            track(
                ref = "unknown",
                title = "Unknown Duration",
                artist = "Vex",
                genre = "Ambient",
                durationSeconds = 0.0,
            )
        val state =
            VexStreamMusicState(
                tracks =
                    listOf(
                        shortAmbient,
                        mediumAmbient,
                        longRock,
                        unknownAmbient,
                    ),
            )
        val organization =
            VexStreamLibraryOrganization(
                durationComparison =
                    VexStreamDurationComparison.LESS_THAN,
                durationThresholdMinutes = 30,
                artists = setOf("vex"),
                genres = setOf("ambient"),
                availability =
                    setOf(
                        VexStreamTrackAvailability.HUB_AND_LOCAL,
                    ),
            )

        val projection =
            state.toVexStreamLibraryProjection(
                searchQuery = "short",
                organization = organization,
            )

        assertEquals(
            listOf(shortAmbient),
            projection.playableTracks,
        )
        assertEquals(
            listOf(shortAmbient),
            projection.searchResults,
        )
        assertEquals(
            emptyList<VexStreamTrackState>(),
            state
                .toVexStreamLibraryProjection(
                    searchQuery = "unknown",
                    organization =
                        organization.copy(
                            availability = emptySet(),
                        ),
                )
                .searchResults,
        )

        val longProjection =
            state.toVexStreamLibraryProjection(
                searchQuery = "",
                organization =
                    VexStreamLibraryOrganization(
                        durationComparison =
                            VexStreamDurationComparison.GREATER_THAN,
                        durationThresholdMinutes = 30,
                    ),
            )

        assertEquals(
            listOf(longRock),
            longProjection.playableTracks,
        )
        assertEquals(
            listOf(
                shortAmbient,
                mediumAmbient,
                longRock,
                unknownAmbient,
            ),
            state.playableTracks(),
        )
    }

    @Test
    fun filteredProjectionIsExactPlaybackMembershipForRowsPlayAllAndShuffleAll() {
        val short =
            track(
                ref = "short-playback",
                title = "Short",
                artist = "Vex",
                genre = "Pop",
                durationSeconds = 8 * 60.0,
            )
        val long =
            track(
                ref = "long-playback",
                title = "Long Mix",
                artist = "Vex",
                genre = "Mix",
                durationSeconds = 90 * 60.0,
            )
        val projection =
            VexStreamMusicState(
                tracks =
                    listOf(
                        short,
                        long,
                    ),
            ).toVexStreamLibraryProjection(
                searchQuery = "",
                organization =
                    VexStreamLibraryOrganization(
                        durationComparison =
                            VexStreamDurationComparison.LESS_THAN,
                        durationThresholdMinutes = 10,
                    ),
            )

        assertEquals(
            listOf(short),
            projection.playableTracks,
        )

        val rowRequest =
            VexStreamTrackPlayRequest(
                selected = short,
                contextTracks = projection.playableTracks,
            )
        val playAllRequest =
            vexStreamLibraryPlaybackRequest(
                tracks = projection.playableTracks,
                order =
                    VexStreamCollectionPlaybackOrder.Natural,
            )
        val shuffleAllRequest =
            vexStreamLibraryPlaybackRequest(
                tracks = projection.playableTracks,
                order =
                    VexStreamCollectionPlaybackOrder.Shuffled,
            )

        assertEquals(
            listOf(short),
            rowRequest.contextTracks,
        )
        assertEquals(
            listOf(short),
            playAllRequest.tracks,
        )
        assertEquals(
            listOf(short),
            shuffleAllRequest.tracks,
        )
    }

    @Test
    fun sortChangesTraversalOrderWithoutChangingMembership() {
        val unknown =
            track(
                ref = "unknown",
                title = "Zeta",
                artist = "Beta Artist",
                durationSeconds = 0.0,
            )
        val medium =
            track(
                ref = "medium",
                title = "Alpha",
                artist = "Zeta Artist",
                durationSeconds = 5 * 60.0,
            )
        val short =
            track(
                ref = "short",
                title = "Beta",
                artist = "Alpha Artist",
                durationSeconds = 2 * 60.0,
            )
        val state =
            VexStreamMusicState(
                tracks =
                    listOf(
                        unknown,
                        medium,
                        short,
                    ),
            )

        val byDuration =
            state.toVexStreamLibraryProjection(
                searchQuery = "",
                organization =
                    VexStreamLibraryOrganization(
                        sort = VexStreamLibrarySort.DURATION,
                    ),
            )
        val byArtist =
            state.toVexStreamLibraryProjection(
                searchQuery = "",
                organization =
                    VexStreamLibraryOrganization(
                        sort = VexStreamLibrarySort.ARTIST,
                    ),
            )

        assertEquals(
            listOf(
                short,
                medium,
                unknown,
            ),
            byDuration.playableTracks,
        )
        assertEquals(
            listOf(
                short,
                unknown,
                medium,
            ),
            byArtist.playableTracks,
        )
        assertEquals(
            byDuration.playableTracks.toSet(),
            byArtist.playableTracks.toSet(),
        )
    }

    @Test
    fun organizationOptionsComeOnlyFromCurrentPlayableTruth() {
        val hub =
            track(
                ref = "hub",
                title = "Hub",
                artist = "Vex",
                genre = "Ambient",
            )
        val local =
            track(
                ref = "local",
                title = "Local",
                artist = "Other",
                genre = "Rock",
                hubAvailable = false,
                downloaded = true,
            )
        val unavailable =
            track(
                ref = "hidden",
                title = "Hidden",
                artist = "Hidden Artist",
                genre = "Hidden Genre",
                hubAvailable = false,
            )

        val options =
            VexStreamMusicState(
                tracks =
                    listOf(
                        hub,
                        local,
                        unavailable,
                    ),
            ).toVexStreamLibraryOrganizationOptions()

        assertEquals(
            listOf(
                "Other",
                "Vex",
            ),
            options.artists,
        )
        assertEquals(
            listOf(
                "Ambient",
                "Rock",
            ),
            options.genres,
        )
        assertEquals(
            listOf(
                VexStreamTrackAvailability.HUB_ONLY,
                VexStreamTrackAvailability.LOCAL_ONLY,
            ),
            options.availability,
        )
    }

    @Test
    fun clearFiltersPreservesSelectedSortAndThreshold() {
        val organization =
            VexStreamLibraryOrganization(
                sort = VexStreamLibrarySort.DURATION,
                durationComparison =
                    VexStreamDurationComparison.GREATER_THAN,
                durationThresholdMinutes = 120,
                artists = setOf("Vex"),
                genres = setOf("Ambient"),
                availability =
                    setOf(
                        VexStreamTrackAvailability.HUB_ONLY,
                    ),
            )

        assertTrue(
            organization.hasActiveFilters,
        )

        val cleared =
            organization.clearFilters()

        assertFalse(
            cleared.hasActiveFilters,
        )
        assertEquals(
            VexStreamLibrarySort.DURATION,
            cleared.sort,
        )
        assertEquals(
            120,
            cleared.durationThresholdMinutes,
        )
    }

    @Test
    fun songAndDownloadPresentationOrderingCanUseTheSameSortChoice() {
        val zeta =
            track(
                ref = "zeta",
                title = "Zeta",
                artist = "Vex",
                downloaded = true,
                durationSeconds = 9 * 60.0,
            )
        val alpha =
            track(
                ref = "alpha",
                title = "alpha",
                artist = "Vex",
                durationSeconds = 2 * 60.0,
            )
        val beta =
            track(
                ref = "beta",
                title = "Beta",
                artist = "Vex",
                downloaded = true,
                durationSeconds = 4 * 60.0,
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
            tracks.vexStreamSortedDownloads(
                VexStreamLibrarySort.DURATION,
            ),
        )
    }

    private fun track(
        ref: String,
        title: String,
        artist: String,
        album: String = "",
        genre: String = "",
        year: String = "",
        durationSeconds: Double = 0.0,
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
                durationSeconds = durationSeconds,
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
