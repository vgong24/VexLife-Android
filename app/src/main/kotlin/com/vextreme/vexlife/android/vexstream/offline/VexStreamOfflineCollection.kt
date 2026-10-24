package com.vextreme.vexlife.android.vexstream.offline

import com.vextreme.vexlife.android.data.vexstream.VexStreamLocalCopyState
import com.vextreme.vexlife.android.data.vexstream.VexStreamMediaSha256
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackRef
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackState

/**
 * Exact media target captured when the listener asks to download the currently missing part of a
 * rendered collection.
 *
 * trackRef addresses the current Hub object. SHA-256 + byte length bind the request to the exact
 * media bytes the human saw when they acted. Execution re-reads repository truth before every
 * target, so a stale rendered collection cannot silently download different bytes under the same
 * current address.
 */
internal data class VexStreamOfflineCopyTarget(
    val trackRef: VexStreamTrackRef,
    val mediaSha256: VexStreamMediaSha256,
    val mediaByteLength: Long,
) {
    init {
        require(mediaByteLength > 0L) {
            "offline copy target byte length must be positive"
        }
    }

    fun isCurrentlyDownloadable(
        current: VexStreamTrackState?,
    ): Boolean {
        if (
            current == null ||
            current.track.trackRef != trackRef ||
            current.track.mediaSha256 != mediaSha256 ||
            current.track.mediaByteLength != mediaByteLength ||
            !current.hubAvailable
        ) {
            return false
        }

        return when (val local = current.localCopy) {
            VexStreamLocalCopyState.NotDownloaded ->
                true

            is VexStreamLocalCopyState.Failed ->
                local.retryable

            is VexStreamLocalCopyState.Downloading,
            is VexStreamLocalCopyState.Downloaded ->
                false
        }
    }
}

/**
 * One-shot request formed from one current collection projection.
 *
 * This is deliberately not a durable "keep this collection synced" contract. Collection membership
 * may evolve after the request is formed; every target therefore carries exact current media
 * identity and is revalidated immediately before execution.
 */
internal data class VexStreamOfflineCollectionRequest(
    val targets: List<VexStreamOfflineCopyTarget>,
) {
    init {
        require(
            targets.map(VexStreamOfflineCopyTarget::trackRef)
                .distinct()
                .size == targets.size,
        ) {
            "offline collection request track refs must be unique"
        }
    }
}

/**
 * One-shot request to remove the current verified local copies for a rendered/library track set.
 *
 * Removal carries only current track addresses because the repository remains the authority for
 * whether a local copy still exists when each target is reached. It never mutates catalog
 * membership.
 */
internal data class VexStreamOfflineCollectionRemovalRequest(
    val trackRefs: List<VexStreamTrackRef>,
) {
    init {
        require(trackRefs.isNotEmpty()) {
            "offline collection removal request must contain at least one track"
        }
        require(trackRefs.distinct().size == trackRefs.size) {
            "offline collection removal request track refs must be unique"
        }
    }
}

/**
 * Read-only relationship between one rendered collection and current phone-local copy truth.
 *
 * The repository remains the owner of each track's actual local-copy state. This plan answers only:
 *
 * - how much of this collection is already on the phone;
 * - which copies are already in flight;
 * - which exact missing media can be requested now;
 * - which missing items are currently blocked.
 *
 * No additional collection state owner is created.
 */
internal data class VexStreamOfflineCollectionPlan(
    val totalCount: Int,
    val onDeviceCount: Int,
    val downloadingCount: Int,
    val blockedMissingCount: Int,
    val targets: List<VexStreamOfflineCopyTarget>,
) {
    init {
        require(totalCount >= 0)
        require(onDeviceCount >= 0)
        require(downloadingCount >= 0)
        require(blockedMissingCount >= 0)
        require(
            totalCount ==
                onDeviceCount +
                downloadingCount +
                blockedMissingCount +
                targets.size,
        ) {
            "offline collection plan categories must partition the collection"
        }
    }

    val downloadableMissingCount: Int
        get() = targets.size

    val missingCount: Int
        get() = totalCount - onDeviceCount

    val fullyOnDevice: Boolean
        get() =
            totalCount > 0 &&
                onDeviceCount == totalCount

    val hasDownloadableMissing: Boolean
        get() = targets.isNotEmpty()

    fun requestOrNull(): VexStreamOfflineCollectionRequest? =
        targets
            .takeIf(List<*>::isNotEmpty)
            ?.let(::VexStreamOfflineCollectionRequest)
}

internal fun vexStreamOfflineCollectionPlan(
    tracks: List<VexStreamTrackState>,
): VexStreamOfflineCollectionPlan {
    var onDeviceCount = 0
    var downloadingCount = 0
    var blockedMissingCount = 0
    val targets =
        mutableListOf<VexStreamOfflineCopyTarget>()

    tracks.forEach { state ->
        when (val local = state.localCopy) {
            is VexStreamLocalCopyState.Downloaded -> {
                onDeviceCount += 1
            }

            is VexStreamLocalCopyState.Downloading -> {
                downloadingCount += 1
            }

            VexStreamLocalCopyState.NotDownloaded -> {
                if (state.hubAvailable) {
                    targets += state.toOfflineCopyTarget()
                } else {
                    blockedMissingCount += 1
                }
            }

            is VexStreamLocalCopyState.Failed -> {
                if (
                    local.retryable &&
                    state.hubAvailable
                ) {
                    targets += state.toOfflineCopyTarget()
                } else {
                    blockedMissingCount += 1
                }
            }
        }
    }

    return VexStreamOfflineCollectionPlan(
        totalCount = tracks.size,
        onDeviceCount = onDeviceCount,
        downloadingCount = downloadingCount,
        blockedMissingCount = blockedMissingCount,
        targets = targets,
    )
}

internal fun vexStreamOfflineCollectionRemovalRequest(
    tracks: List<VexStreamTrackState>,
): VexStreamOfflineCollectionRemovalRequest? =
    tracks
        .mapNotNull { state ->
            if (
                state.localCopy is
                VexStreamLocalCopyState.Downloaded
            ) {
                state.track.trackRef
            } else {
                null
            }
        }
        .takeIf(List<*>::isNotEmpty)
        ?.let(::VexStreamOfflineCollectionRemovalRequest)

private fun VexStreamTrackState.toOfflineCopyTarget():
    VexStreamOfflineCopyTarget =
    VexStreamOfflineCopyTarget(
        trackRef = track.trackRef,
        mediaSha256 = track.mediaSha256,
        mediaByteLength = track.mediaByteLength,
    )

// [VXG RealForever]
