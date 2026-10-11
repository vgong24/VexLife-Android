package com.vextreme.vexlife.android.data.vexstream

private val SHA256_PATTERN = Regex("^[0-9a-f]{64}$")

/**
 * Current Hub-catalog address for a track.
 *
 * This is intentionally opaque and is NOT the portable media identity. The current VexStream
 * desktop source derives its local track ID from the desktop path, so a moved/reorganized source
 * may legitimately receive another Hub track ref while retaining the same media bytes.
 */
@JvmInline
value class VexStreamTrackRef(
    val value: String,
) {
    init {
        require(value.isNotBlank()) {
            "VexStreamTrackRef must be non-blank"
        }
    }

    override fun toString(): String = value
}

/**
 * Portable identity of the exact media bytes admitted for stream/download.
 */
@JvmInline
value class VexStreamMediaSha256(
    val value: String,
) {
    init {
        require(SHA256_PATTERN.matches(value)) {
            "VexStreamMediaSha256 must be a lowercase 64-character SHA-256"
        }
    }

    override fun toString(): String = value
}

/**
 * Android's current projection of the Hub relationship.
 *
 * Pairing/authentication/authorization remain owned by the accepted Home/Safety contracts. These
 * states are a product projection only; they do not mint or validate security evidence themselves.
 */
sealed interface VexStreamHubState {
    data object NotConfigured : VexStreamHubState
    data object Unreachable : VexStreamHubState
    data object PairingRequired : VexStreamHubState
    data object AuthenticationRequired : VexStreamHubState
    data object AuthorizationRequired : VexStreamHubState
    data object Revoked : VexStreamHubState

    data class Ready(
        val hubRef: String,
        val catalogRevision: String,
    ) : VexStreamHubState {
        init {
            require(hubRef.isNotBlank()) {
                "hubRef must be non-blank"
            }
            require(catalogRevision.isNotBlank()) {
                "catalogRevision must be non-blank"
            }
        }
    }
}

/**
 * Human-visible local VexStream connection lifecycle.
 *
 * This is a local product pairing candidate only. It is deliberately separate from accepted
 * Home/Safety membership, authentication, authorization, and capability-lease semantics.
 */
sealed interface VexStreamConnectionState {
    data object NotPaired : VexStreamConnectionState
    data object Searching : VexStreamConnectionState

    data class ConfirmCode(
        val displayName: String,
        val pairingCode: String,
    ) : VexStreamConnectionState {
        init {
            require(displayName.isNotBlank())
            require(pairingCode.matches(Regex("^[0-9]{6}$")))
        }
    }

    data class WaitingForDesktopApproval(
        val displayName: String,
    ) : VexStreamConnectionState {
        init {
            require(displayName.isNotBlank())
        }
    }

    data class Paired(
        val displayName: String,
    ) : VexStreamConnectionState {
        init {
            require(displayName.isNotBlank())
        }
    }

    data class Failed(
        val reasonCode: String,
    ) : VexStreamConnectionState {
        init {
            require(reasonCode.isNotBlank())
        }
    }
}

/**
 * Local copy state belongs to the phone, not to the desktop Hub.
 *
 * [Downloaded] means the local bytes were verified against the expected digest/length by the
 * owning media-store implementation. It does not mean the Hub is currently reachable.
 */
sealed interface VexStreamLocalCopyState {
    data object NotDownloaded : VexStreamLocalCopyState

    data class Downloading(
        val bytesDownloaded: Long,
        val totalBytes: Long,
    ) : VexStreamLocalCopyState {
        init {
            require(bytesDownloaded >= 0L) {
                "bytesDownloaded must be non-negative"
            }
            require(totalBytes > 0L) {
                "totalBytes must be positive"
            }
            require(bytesDownloaded <= totalBytes) {
                "bytesDownloaded cannot exceed totalBytes"
            }
        }
    }

    data class Downloaded(
        val mediaSha256: VexStreamMediaSha256,
        val byteLength: Long,
        val localMediaRef: String,
    ) : VexStreamLocalCopyState {
        init {
            require(byteLength > 0L) {
                "byteLength must be positive"
            }
            require(localMediaRef.isNotBlank()) {
                "localMediaRef must be non-blank"
            }
        }
    }

    data class Failed(
        val reasonCode: String,
        val retryable: Boolean,
    ) : VexStreamLocalCopyState {
        init {
            require(reasonCode.isNotBlank()) {
                "reasonCode must be non-blank"
            }
        }
    }
}

/**
 * Sanitized music projection consumed by Android.
 *
 * It intentionally contains no desktop filesystem path, source-folder path, listener address,
 * credential, or raw private endpoint.
 */
data class VexStreamTrack(
    val trackRef: VexStreamTrackRef,
    val mediaSha256: VexStreamMediaSha256,
    val mediaByteLength: Long,
    val title: String,
    val artist: String,
    val album: String = "",
    val genre: String = "",
    val year: String = "",
    val durationSeconds: Double = 0.0,
    val playbackStartSeconds: Double = 0.0,
    val playbackEndSeconds: Double? = null,
) {
    init {
        require(mediaByteLength > 0L) {
            "mediaByteLength must be positive"
        }
        require(title.isNotBlank()) {
            "title must be non-blank"
        }
        require(durationSeconds >= 0.0) {
            "durationSeconds must be non-negative"
        }
        require(playbackStartSeconds >= 0.0) {
            "playbackStartSeconds must be non-negative"
        }
        require(playbackEndSeconds == null || playbackEndSeconds > playbackStartSeconds) {
            "playbackEndSeconds must be after playbackStartSeconds"
        }
        require(
            playbackEndSeconds == null ||
                durationSeconds == 0.0 ||
                playbackEndSeconds <= durationSeconds,
        ) {
            "playbackEndSeconds cannot exceed known durationSeconds"
        }
    }
}

enum class VexStreamTrackAvailability {
    HUB_ONLY,
    HUB_AND_LOCAL,
    LOCAL_ONLY,
    UNAVAILABLE,
}

data class VexStreamTrackState(
    val track: VexStreamTrack,
    val hubAvailable: Boolean,
    val localCopy: VexStreamLocalCopyState = VexStreamLocalCopyState.NotDownloaded,
) {
    val availability: VexStreamTrackAvailability
        get() {
            val local = localCopy is VexStreamLocalCopyState.Downloaded
            return when {
                hubAvailable && local -> VexStreamTrackAvailability.HUB_AND_LOCAL
                hubAvailable -> VexStreamTrackAvailability.HUB_ONLY
                local -> VexStreamTrackAvailability.LOCAL_ONLY
                else -> VexStreamTrackAvailability.UNAVAILABLE
            }
        }
}

data class VexStreamMusicState(
    val hubState: VexStreamHubState = VexStreamHubState.NotConfigured,
    val connectionState: VexStreamConnectionState = VexStreamConnectionState.NotPaired,
    val tracks: List<VexStreamTrackState> = emptyList(),
) {
    init {
        val duplicateTrackRefs = tracks
            .groupBy { it.track.trackRef }
            .filterValues { it.size > 1 }
            .keys

        require(duplicateTrackRefs.isEmpty()) {
            "duplicate VexStream track refs: " +
                duplicateTrackRefs.joinToString { it.value }
        }
    }

    fun track(trackRef: VexStreamTrackRef): VexStreamTrackState? =
        tracks.firstOrNull { it.track.trackRef == trackRef }

    fun playableTracks(): List<VexStreamTrackState> =
        tracks.filter {
            it.availability != VexStreamTrackAvailability.UNAVAILABLE
        }

    companion object {
        val Empty = VexStreamMusicState()
    }
}

// [VXG RealForever]
