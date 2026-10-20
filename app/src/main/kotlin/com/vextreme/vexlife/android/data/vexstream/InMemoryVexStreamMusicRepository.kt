package com.vextreme.vexlife.android.data.vexstream

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * State-only VexStream repository for tests/Preview.
 *
 * It never opens a network connection and never writes media bytes. "Downloaded" means the fixture
 * state has been advanced to the same verified-local-copy shape a real media store must earn.
 */
class InMemoryVexStreamMusicRepository(
    initialState: VexStreamMusicState = VexStreamMusicState.Empty,
) : VexStreamMusicRepository {

    private val mutableState = MutableStateFlow(initialState)

    override val state: StateFlow<VexStreamMusicState> =
        mutableState.asStateFlow()

    fun publishCatalog(
        catalog: VexStreamHubCatalogProjection,
    ) {
        mutableState.value = mutableState.value.withHubCatalog(catalog)
    }

    fun publishHubUnavailable(
        hubState: VexStreamHubState,
    ) {
        mutableState.value = mutableState.value.withHubUnavailable(hubState)
    }

    override suspend fun findNearbyVexStream(): VexStreamRepositoryResult {
        mutableState.value = mutableState.value
            .withHubUnavailable(
                VexStreamHubState.PairingRequired,
            )
            .copy(
                connectionState =
                    VexStreamConnectionState.ConfirmCode(
                        displayName = "VexStream on Test PC",
                        pairingCode = "123456",
                    ),
            )

        return VexStreamRepositoryResult.Accepted()
    }

    override suspend fun reconnectPairedVexStream(): VexStreamRepositoryResult {
        val current = mutableState.value
        if (current.connectionState !is VexStreamConnectionState.Paired) {
            return VexStreamRepositoryResult.Rejected(
                reason = VexStreamRepositoryRejection.PAIRING_NOT_READY,
            )
        }

        mutableState.value = current.copy(
            hubState =
                VexStreamHubState.Ready(
                    hubRef = "hub.in-memory",
                    catalogRevision = "catalog.in-memory.reconnected",
                ),
            tracks = current.tracks.map { state ->
                state.copy(
                    hubAvailable = true,
                )
            },
        )
        return VexStreamRepositoryResult.Accepted()
    }

    override suspend fun confirmPairingCode(): VexStreamRepositoryResult {
        val current = mutableState.value
        if (current.connectionState !is VexStreamConnectionState.ConfirmCode) {
            return VexStreamRepositoryResult.Rejected(
                reason = VexStreamRepositoryRejection.PAIRING_NOT_READY,
            )
        }

        /*
         * Pairing makes the Hub reachable again. findNearbyVexStream() intentionally marked every
         * remote track unavailable while pairing; if we only flip hubState/connectionState here,
         * playableTracks() remains empty and the library cannot render PlayTrack after a successful
         * pairing. Keep the fixture's per-track availability coherent with the Ready Hub state.
         */
        mutableState.value = current.copy(
            hubState =
                VexStreamHubState.Ready(
                    hubRef = "hub.in-memory",
                    catalogRevision = "catalog.in-memory",
                ),
            connectionState =
                VexStreamConnectionState.Paired(
                    displayName = "VexStream on Test PC",
                ),
            tracks =
                current.tracks.map { state ->
                    state.copy(
                        hubAvailable = true,
                    )
                },
        )

        return VexStreamRepositoryResult.Accepted()
    }

    override suspend fun forgetPairedVexStream(): VexStreamRepositoryResult {
        mutableState.value = mutableState.value
            .withHubUnavailable(VexStreamHubState.NotConfigured)
            .copy(
                connectionState = VexStreamConnectionState.NotPaired,
            )
        return VexStreamRepositoryResult.Accepted()
    }

    override suspend fun refreshCatalog(): VexStreamRepositoryResult =
        if (mutableState.value.hubState is VexStreamHubState.Ready) {
            VexStreamRepositoryResult.Accepted()
        } else {
            VexStreamRepositoryResult.Rejected(
                reason = VexStreamRepositoryRejection.HUB_NOT_READY,
            )
        }

    override suspend fun downloadForOffline(
        trackRef: VexStreamTrackRef,
        expectedMediaSha256: VexStreamMediaSha256?,
        expectedMediaByteLength: Long?,
    ): VexStreamRepositoryResult {
        val current = mutableState.value
        if (current.hubState !is VexStreamHubState.Ready) {
            return VexStreamRepositoryResult.Rejected(
                reason = VexStreamRepositoryRejection.HUB_NOT_READY,
                trackRef = trackRef,
            )
        }

        val target = current.track(trackRef)
            ?: return VexStreamRepositoryResult.Rejected(
                reason = VexStreamRepositoryRejection.TRACK_UNKNOWN,
                trackRef = trackRef,
            )

        if (
            (
                expectedMediaSha256 != null &&
                    target.track.mediaSha256 != expectedMediaSha256
            ) ||
            (
                expectedMediaByteLength != null &&
                    target.track.mediaByteLength != expectedMediaByteLength
            )
        ) {
            return VexStreamRepositoryResult.Rejected(
                reason = VexStreamRepositoryRejection.MEDIA_NOT_AVAILABLE_FROM_HUB,
                trackRef = trackRef,
            )
        }

        if (!target.hubAvailable) {
            return VexStreamRepositoryResult.Rejected(
                reason = VexStreamRepositoryRejection.MEDIA_NOT_AVAILABLE_FROM_HUB,
                trackRef = trackRef,
            )
        }

        if (target.localCopy is VexStreamLocalCopyState.Downloaded) {
            return VexStreamRepositoryResult.Rejected(
                reason = VexStreamRepositoryRejection.ALREADY_DOWNLOADED,
                trackRef = trackRef,
            )
        }

        val downloaded = VexStreamLocalCopyState.Downloaded(
            mediaSha256 = target.track.mediaSha256,
            byteLength = target.track.mediaByteLength,
            localMediaRef =
                "local.fixture." + target.track.mediaSha256.value.take(16),
        )

        mutableState.value = current.copy(
            tracks = current.tracks.map { state ->
                if (state.track.trackRef == trackRef) {
                    state.copy(localCopy = downloaded)
                } else {
                    state
                }
            },
        )

        return VexStreamRepositoryResult.Accepted(trackRef)
    }

    override suspend fun removeOfflineCopy(
        trackRef: VexStreamTrackRef,
    ): VexStreamRepositoryResult {
        val current = mutableState.value
        val target = current.track(trackRef)
            ?: return VexStreamRepositoryResult.Rejected(
                reason = VexStreamRepositoryRejection.TRACK_UNKNOWN,
                trackRef = trackRef,
            )

        if (target.localCopy !is VexStreamLocalCopyState.Downloaded) {
            return VexStreamRepositoryResult.Rejected(
                reason = VexStreamRepositoryRejection.NOT_DOWNLOADED,
                trackRef = trackRef,
            )
        }

        mutableState.value = current.copy(
            tracks = current.tracks.map { state ->
                if (state.track.trackRef == trackRef) {
                    state.copy(
                        localCopy = VexStreamLocalCopyState.NotDownloaded,
                    )
                } else {
                    state
                }
            },
        )

        return VexStreamRepositoryResult.Accepted(trackRef)
    }
}

// [VXG RealForever]
