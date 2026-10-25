package com.vextreme.vexlife.android.data.vexstream

/**
 * Capability classes the Android consumer needs from a future authorized VexStream Hub bridge.
 *
 * These are consumer intentions, not canonical Home/Safety capability-ref strings. The accepted
 * security owner must map/admit concrete capability refs before real authorization is claimed.
 */
enum class VexStreamHubCapability {
    CATALOG_READ,
    MEDIA_STREAM,
    MEDIA_COPY,
}

/**
 * Sanitized Hub catalog projection.
 *
 * The producer is expected to bind [catalogRevision] to the exact projected catalog bytes or an
 * equivalent stable revision identity. Android does not accept raw VexStream Track JSON because the
 * desktop object currently contains filesystem/source details that do not belong across the bridge.
 */
data class VexStreamHubCatalogProjection(
    val hubRef: String,
    val sourceVersion: String,
    val catalogRevision: String,
    val tracks: List<VexStreamHubTrackProjection>,
    val playlists: List<VexStreamHubPlaylistProjection> = emptyList(),
) {
    init {
        require(hubRef.isNotBlank()) {
            "hubRef must be non-blank"
        }
        require(sourceVersion.isNotBlank()) {
            "sourceVersion must be non-blank"
        }
        require(catalogRevision.isNotBlank()) {
            "catalogRevision must be non-blank"
        }

        val duplicateRefs = tracks
            .groupBy { it.trackRef }
            .filterValues { it.size > 1 }
            .keys

        require(duplicateRefs.isEmpty()) {
            "duplicate Hub track refs: " +
                duplicateRefs.joinToString { it.value }
        }
    }
}

data class VexStreamHubPlaylistProjection(
    val playlistRef: VexStreamPlaylistRef,
    val name: String,
    val trackRefs: List<VexStreamTrackRef>,
) {
    fun toPlaylist(): VexStreamPlaylist =
        VexStreamPlaylist(
            playlistRef = playlistRef,
            name = name,
            trackRefs = trackRefs,
        )
}

data class VexStreamHubTrackProjection(
    val trackRef: VexStreamTrackRef,
    val mediaSha256: VexStreamMediaSha256,
    val mediaByteLength: Long,
    val title: String,
    val artist: String,
    val artworkUrl: String? = null,
    val artworkRef: String? = null,
    val album: String = "",
    val genre: String = "",
    val year: String = "",
    val durationSeconds: Double = 0.0,
    val playbackStartSeconds: Double = 0.0,
    val playbackEndSeconds: Double? = null,
) {
    fun toTrack(): VexStreamTrack =
        VexStreamTrack(
            trackRef = trackRef,
            mediaSha256 = mediaSha256,
            mediaByteLength = mediaByteLength,
            title = title,
            artist = artist,
            artworkUrl = artworkUrl,
            artworkRef = artworkRef,
            album = album,
            genre = genre,
            year = year,
            durationSeconds = durationSeconds,
            playbackStartSeconds = playbackStartSeconds,
            playbackEndSeconds = playbackEndSeconds,
        )
}

/**
 * Merge a current Hub catalog into Android's retained state without confusing Hub addressing with
 * portable media identity.
 *
 * Verified local copies follow the media digest. If the desktop reorganizes a source and emits a
 * different path-derived Hub track ref for the same bytes, the phone keeps the verified local copy
 * attached to those bytes. Downloaded media no longer present in the current Hub catalog remains as
 * LOCAL_ONLY rather than disappearing from the phone.
 */
fun VexStreamMusicState.withHubCatalog(
    catalog: VexStreamHubCatalogProjection,
    rediscoveredLocalCopies:
        Map<
            VexStreamMediaSha256,
            VexStreamLocalCopyState.Downloaded,
        > = emptyMap(),
): VexStreamMusicState {
    val downloadedByDigest = tracks
        .mapNotNull { state ->
            val local = state.localCopy as? VexStreamLocalCopyState.Downloaded
                ?: return@mapNotNull null
            local.mediaSha256 to Pair(state.track, local)
        }
        .toMap()

    rediscoveredLocalCopies.forEach {
            digest,
            localCopy,
        ->
        require(
            localCopy.mediaSha256 == digest,
        ) {
            "rediscovered local copy digest key must match its mediaSha256"
        }
    }

    catalog.tracks.forEach { projection ->
        val retainedLocal =
            downloadedByDigest[
                projection.mediaSha256
            ]?.second
        val rediscoveredLocal =
            rediscoveredLocalCopies[
                projection.mediaSha256
            ]

        require(
            retainedLocal == null ||
                retainedLocal.byteLength ==
                projection.mediaByteLength,
        ) {
            "same mediaSha256 cannot be rebound to a different mediaByteLength"
        }
        require(
            rediscoveredLocal == null ||
                rediscoveredLocal.byteLength ==
                projection.mediaByteLength,
        ) {
            "rediscovered mediaSha256 cannot be rebound to a different mediaByteLength"
        }
    }

    val currentDigests = catalog.tracks
        .map { it.mediaSha256 }
        .toSet()

    val connectedTracks =
        catalog.tracks.map { projection ->
            val localCopy =
                downloadedByDigest[
                    projection.mediaSha256
                ]?.second
                    ?: rediscoveredLocalCopies[
                        projection.mediaSha256
                    ]

            VexStreamTrackState(
                track = projection.toTrack(),
                hubAvailable = true,
                localCopy =
                    localCopy
                        ?: VexStreamLocalCopyState.NotDownloaded,
            )
        }

    val localOnlyTracks = downloadedByDigest
        .filterKeys { it !in currentDigests }
        .values
        .map { (track, localCopy) ->
            VexStreamTrackState(
                track = track,
                hubAvailable = false,
                localCopy = localCopy,
            )
        }

    val availableTrackRefs =
        connectedTracks
            .map { it.track.trackRef }
            .toSet()

    val playlists =
        catalog.playlists.map { projection ->
            projection.copy(
                trackRefs =
                    projection.trackRefs.filter {
                        it in availableTrackRefs
                    },
            ).toPlaylist()
        }

    return VexStreamMusicState(
        hubState = VexStreamHubState.Ready(
            hubRef = catalog.hubRef,
            catalogRevision = catalog.catalogRevision,
        ),
        connectionState = connectionState,
        libraryLoadState = libraryLoadState,
        tracks = connectedTracks + localOnlyTracks,
        playlists = playlists,
    )
}

/**
 * Lose current Hub reachability without erasing verified offline media.
 */
fun VexStreamMusicState.withHubUnavailable(
    hubState: VexStreamHubState,
): VexStreamMusicState {
    require(hubState !is VexStreamHubState.Ready) {
        "withHubUnavailable requires a non-Ready Hub state"
    }

    return copy(
        hubState = hubState,
        tracks = tracks.map { track ->
            track.copy(hubAvailable = false)
        },
    )
}

// [VXG RealForever]
