package com.vextreme.vexlife.android.data.vexstream

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InMemoryVexStreamMusicRepositoryTest {
    @Test
    fun successfulPairingRestoresFixtureHubAvailability() = runTest {
        val track =
            VexStreamTrack(
                trackRef = VexStreamTrackRef("fixture.pairing"),
                mediaSha256 =
                    VexStreamMediaSha256(
                        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                    ),
                mediaByteLength = 1_024L,
                title = "Pairing Fixture",
                artist = "Vex",
            )
        val repository =
            InMemoryVexStreamMusicRepository(
                initialState =
                    VexStreamMusicState(
                        tracks =
                            listOf(
                                VexStreamTrackState(
                                    track = track,
                                    hubAvailable = true,
                                ),
                            ),
                    ),
            )

        repository.findNearbyVexStream()
        assertTrue(
            repository.state.value.tracks.none {
                it.hubAvailable
            },
        )

        repository.confirmPairingCode()

        assertTrue(
            repository.state.value.hubState is
                VexStreamHubState.Ready,
        )
        assertTrue(
            repository.state.value.tracks.all {
                it.hubAvailable
            },
        )
        assertEquals(
            listOf(track.trackRef),
            repository.state.value
                .playableTracks()
                .map { it.track.trackRef },
        )
    }


    @Test
    fun offlineDownloadSurvivesHubLoss() = runTest {
        val repository = InMemoryVexStreamMusicRepository()
        val trackRef = VexStreamTrackRef("hub.track.one")

        repository.publishCatalog(
            catalog(
                VexStreamHubTrackProjection(
                    trackRef = trackRef,
                    mediaSha256 = VexStreamMediaSha256(DIGEST),
                    mediaByteLength = 1234,
                    title = "On The Go",
                    artist = "Vex",
                ),
            ),
        )

        assertTrue(
            repository.downloadForOffline(trackRef) is
                VexStreamRepositoryResult.Accepted,
        )

        repository.publishHubUnavailable(
            VexStreamHubState.Unreachable,
        )

        val track = repository.state.value.track(trackRef)
            ?: error("track missing")

        assertEquals(
            VexStreamTrackAvailability.LOCAL_ONLY,
            track.availability,
        )
        assertTrue(
            track.localCopy is VexStreamLocalCopyState.Downloaded,
        )
    }

    @Test
    fun downloadFailsClosedWhenHubIsNotReady() = runTest {
        val trackRef = VexStreamTrackRef("hub.track.one")
        val repository = InMemoryVexStreamMusicRepository(
            VexStreamMusicState(
                hubState = VexStreamHubState.AuthorizationRequired,
                tracks = listOf(
                    VexStreamTrackState(
                        track = VexStreamTrack(
                            trackRef = trackRef,
                            mediaSha256 = VexStreamMediaSha256(DIGEST),
                            mediaByteLength = 1234,
                            title = "Held Track",
                            artist = "Vex",
                        ),
                        hubAvailable = false,
                    ),
                ),
            ),
        )

        assertEquals(
            VexStreamRepositoryResult.Rejected(
                reason = VexStreamRepositoryRejection.HUB_NOT_READY,
                trackRef = trackRef,
            ),
            repository.downloadForOffline(trackRef),
        )
    }

    @Test
    fun reconnectRestoresPairedHubReachabilityWithoutDiscoverySemantics() = runTest {
        val trackRef =
            VexStreamTrackRef(
                "hub.track.reconnect",
            )
        val repository =
            InMemoryVexStreamMusicRepository(
                VexStreamMusicState(
                    hubState =
                        VexStreamHubState.Unreachable,
                    connectionState =
                        VexStreamConnectionState.Paired(
                            displayName = "VexStream on Test PC",
                        ),
                    tracks =
                        listOf(
                            VexStreamTrackState(
                                track =
                                    VexStreamTrack(
                                        trackRef = trackRef,
                                        mediaSha256 =
                                            VexStreamMediaSha256(
                                                DIGEST,
                                            ),
                                        mediaByteLength = 1_234,
                                        title = "Reconnect Track",
                                        artist = "Vex",
                                    ),
                                hubAvailable = false,
                            ),
                        ),
                ),
            )

        assertTrue(
            repository.reconnectPairedVexStream() is
                VexStreamRepositoryResult.Accepted,
        )
        assertTrue(
            repository.state.value.connectionState is
                VexStreamConnectionState.Paired,
        )
        assertTrue(
            repository.state.value.hubState is
                VexStreamHubState.Ready,
        )
        assertEquals(
            VexStreamTrackAvailability.HUB_ONLY,
            repository.state.value.track(trackRef)?.availability,
        )
    }

    @Test
    fun removeDownloadReturnsTrackToHubOnlyWhenHubRemainsAvailable() = runTest {
        val repository = InMemoryVexStreamMusicRepository()
        val trackRef = VexStreamTrackRef("hub.track.one")

        repository.publishCatalog(
            catalog(
                VexStreamHubTrackProjection(
                    trackRef = trackRef,
                    mediaSha256 = VexStreamMediaSha256(DIGEST),
                    mediaByteLength = 1234,
                    title = "On The Go",
                    artist = "Vex",
                ),
            ),
        )

        repository.downloadForOffline(trackRef)
        assertTrue(
            repository.removeOfflineCopy(trackRef) is
                VexStreamRepositoryResult.Accepted,
        )

        assertEquals(
            VexStreamTrackAvailability.HUB_ONLY,
            repository.state.value.track(trackRef)?.availability,
        )
    }

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
        const val DIGEST =
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    }
}

// [VXG RealForever]
