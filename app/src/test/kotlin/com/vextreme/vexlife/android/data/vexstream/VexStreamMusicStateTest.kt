package com.vextreme.vexlife.android.data.vexstream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VexStreamMusicStateTest {

    @Test
    fun availabilityKeepsHubAndOfflineTruthSeparate() {
        val track = track(ref = "hub.track.one", digest = DIGEST_A)

        assertEquals(
            VexStreamTrackAvailability.HUB_ONLY,
            VexStreamTrackState(
                track = track,
                hubAvailable = true,
            ).availability,
        )

        val local = VexStreamLocalCopyState.Downloaded(
            mediaSha256 = track.mediaSha256,
            byteLength = track.mediaByteLength,
            localMediaRef = "local.fixture.a",
        )

        assertEquals(
            VexStreamTrackAvailability.HUB_AND_LOCAL,
            VexStreamTrackState(
                track = track,
                hubAvailable = true,
                localCopy = local,
            ).availability,
        )

        assertEquals(
            VexStreamTrackAvailability.LOCAL_ONLY,
            VexStreamTrackState(
                track = track,
                hubAvailable = false,
                localCopy = local,
            ).availability,
        )
    }

    @Test
    fun catalogRefMayChangeWithoutLosingVerifiedCopyForSameMediaBytes() {
        val oldTrack = track(
            ref = "hub.path-derived.old",
            digest = DIGEST_A,
        )
        val local = VexStreamLocalCopyState.Downloaded(
            mediaSha256 = oldTrack.mediaSha256,
            byteLength = oldTrack.mediaByteLength,
            localMediaRef = "local.fixture.a",
        )

        val prior = VexStreamMusicState(
            hubState = VexStreamHubState.Unreachable,
            tracks = listOf(
                VexStreamTrackState(
                    track = oldTrack,
                    hubAvailable = false,
                    localCopy = local,
                ),
            ),
        )

        val next = prior.withHubCatalog(
            catalog(
                VexStreamHubTrackProjection(
                    trackRef = VexStreamTrackRef("hub.path-derived.new"),
                    mediaSha256 = VexStreamMediaSha256(DIGEST_A),
                    mediaByteLength = 4096,
                    title = "Same Bytes",
                    artist = "Vex",
                    durationSeconds = 180.0,
                ),
            ),
        )

        assertEquals(1, next.tracks.size)
        assertEquals(
            "hub.path-derived.new",
            next.tracks.single().track.trackRef.value,
        )
        assertEquals(
            VexStreamTrackAvailability.HUB_AND_LOCAL,
            next.tracks.single().availability,
        )
        assertEquals(local, next.tracks.single().localCopy)
    }

    @Test(expected = IllegalArgumentException::class)
    fun sameDigestCannotBeReboundToDifferentByteLength() {
        val localTrack = track(
            ref = "hub.path-derived.old",
            digest = DIGEST_A,
        )

        val prior = VexStreamMusicState(
            tracks = listOf(
                VexStreamTrackState(
                    track = localTrack,
                    hubAvailable = false,
                    localCopy = VexStreamLocalCopyState.Downloaded(
                        mediaSha256 = localTrack.mediaSha256,
                        byteLength = localTrack.mediaByteLength,
                        localMediaRef = "local.fixture.a",
                    ),
                ),
            ),
        )

        prior.withHubCatalog(
            catalog(
                VexStreamHubTrackProjection(
                    trackRef = VexStreamTrackRef("hub.path-derived.new"),
                    mediaSha256 = VexStreamMediaSha256(DIGEST_A),
                    mediaByteLength = localTrack.mediaByteLength + 1L,
                    title = "Contradictory Bytes",
                    artist = "Vex",
                ),
            ),
        )
    }

    @Test
    fun downloadedTrackMissingFromNewHubCatalogRemainsLocalOnly() {
        val localTrack = track(
            ref = "hub.old.only",
            digest = DIGEST_A,
        )
        val prior = VexStreamMusicState(
            tracks = listOf(
                VexStreamTrackState(
                    track = localTrack,
                    hubAvailable = false,
                    localCopy = VexStreamLocalCopyState.Downloaded(
                        mediaSha256 = localTrack.mediaSha256,
                        byteLength = localTrack.mediaByteLength,
                        localMediaRef = "local.fixture.a",
                    ),
                ),
            ),
        )

        val next = prior.withHubCatalog(
            catalog(
                VexStreamHubTrackProjection(
                    trackRef = VexStreamTrackRef("hub.other"),
                    mediaSha256 = VexStreamMediaSha256(DIGEST_B),
                    mediaByteLength = 8192,
                    title = "Other Track",
                    artist = "Vex",
                ),
            ),
        )

        assertEquals(2, next.tracks.size)
        assertTrue(
            next.tracks.any {
                it.track.mediaSha256.value == DIGEST_A &&
                    it.availability == VexStreamTrackAvailability.LOCAL_ONLY
            },
        )
    }

    @Test
    fun catalogMergePreservesRememberedPairingLifecycle() {
        val prior = VexStreamMusicState(
            connectionState = VexStreamConnectionState.Paired(
                displayName = "VexStream on Home PC",
            ),
        )

        val next = prior.withHubCatalog(
            catalog(
                VexStreamHubTrackProjection(
                    trackRef = VexStreamTrackRef("hub.track.connected"),
                    mediaSha256 = VexStreamMediaSha256(DIGEST_A),
                    mediaByteLength = 4096,
                    title = "Connected Track",
                    artist = "Vex",
                ),
            ),
        )

        assertEquals(
            VexStreamConnectionState.Paired(
                displayName = "VexStream on Home PC",
            ),
            next.connectionState,
        )
        assertTrue(next.hubState is VexStreamHubState.Ready)
    }

    @Test
    fun losingHubReachabilityDoesNotErasePairingOrVerifiedLocalMedia() {
        val track = track(
            ref = "hub.track.local",
            digest = DIGEST_A,
        )
        val downloaded = VexStreamLocalCopyState.Downloaded(
            mediaSha256 = track.mediaSha256,
            byteLength = track.mediaByteLength,
            localMediaRef = "local.fixture.a",
        )
        val prior = VexStreamMusicState(
            hubState = VexStreamHubState.Ready(
                hubRef = "hub.fixture",
                catalogRevision = "catalog.1",
            ),
            connectionState = VexStreamConnectionState.Paired(
                displayName = "VexStream on Home PC",
            ),
            tracks = listOf(
                VexStreamTrackState(
                    track = track,
                    hubAvailable = true,
                    localCopy = downloaded,
                ),
            ),
        )

        val offline = prior.withHubUnavailable(
            VexStreamHubState.Unreachable,
        )

        assertEquals(prior.connectionState, offline.connectionState)
        assertEquals(
            VexStreamTrackAvailability.LOCAL_ONLY,
            offline.tracks.single().availability,
        )
        assertEquals(downloaded, offline.tracks.single().localCopy)
    }

    @Test
    fun desktopPlaylistProjectionKeepsOrderAndDropsUnavailableTrackRefs() {
        val next = VexStreamMusicState.Empty.withHubCatalog(
            VexStreamHubCatalogProjection(
                hubRef = "hub.fixture",
                sourceVersion = "3.0.0",
                catalogRevision = "catalog.playlists.1",
                tracks = listOf(
                    VexStreamHubTrackProjection(
                        trackRef = VexStreamTrackRef("hub.track.a"),
                        mediaSha256 = VexStreamMediaSha256(DIGEST_A),
                        mediaByteLength = 4096,
                        title = "A",
                        artist = "Vex",
                    ),
                    VexStreamHubTrackProjection(
                        trackRef = VexStreamTrackRef("hub.track.b"),
                        mediaSha256 = VexStreamMediaSha256(DIGEST_B),
                        mediaByteLength = 8192,
                        title = "B",
                        artist = "Vex",
                    ),
                ),
                playlists = listOf(
                    VexStreamHubPlaylistProjection(
                        playlistRef = VexStreamPlaylistRef("playlist.road"),
                        name = "Road Trip",
                        trackRefs = listOf(
                            VexStreamTrackRef("hub.track.b"),
                            VexStreamTrackRef("missing.track"),
                            VexStreamTrackRef("hub.track.a"),
                        ),
                    ),
                ),
            ),
        )

        assertEquals(1, next.playlists.size)
        assertEquals(
            listOf("hub.track.b", "hub.track.a"),
            next.playlists.single().trackRefs.map { it.value },
        )

        val offline =
            next.withHubUnavailable(
                VexStreamHubState.Unreachable,
            )
        assertEquals(
            next.playlists,
            offline.playlists,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun mediaDigestMustBeExactSha256() {
        VexStreamMediaSha256("not-a-digest")
    }

    private fun track(
        ref: String,
        digest: String,
    ): VexStreamTrack =
        VexStreamTrack(
            trackRef = VexStreamTrackRef(ref),
            mediaSha256 = VexStreamMediaSha256(digest),
            mediaByteLength = 4096,
            title = "Track",
            artist = "Artist",
            durationSeconds = 180.0,
        )

    private fun catalog(
        vararg tracks: VexStreamHubTrackProjection,
    ): VexStreamHubCatalogProjection =
        VexStreamHubCatalogProjection(
            hubRef = "hub.fixture",
            sourceVersion = "2.2.0",
            catalogRevision = "catalog.fixture.1",
            tracks = tracks.toList(),
        )

    private companion object {
        const val DIGEST_A =
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        const val DIGEST_B =
            "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
    }
}

// [VXG RealForever]
