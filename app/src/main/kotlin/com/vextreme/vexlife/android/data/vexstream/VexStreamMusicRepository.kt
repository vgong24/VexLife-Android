package com.vextreme.vexlife.android.data.vexstream

import kotlinx.coroutines.flow.StateFlow

/**
 * Android consumer boundary for VexStream catalog + offline-copy state.
 *
 * Playback is deliberately not owned here. A later Media3 owner will choose between Hub streaming
 * and verified local media while this repository answers "what music is currently available here?"
 */
interface VexStreamMusicRepository {
    val state: StateFlow<VexStreamMusicState>

    /**
     * Restores durable local-copy truth after process creation.
     *
     * Production restoration can perform disk/media integrity work and therefore belongs behind an
     * owned asynchronous execution boundary rather than repository construction on the UI thread.
     * State-only fixture repositories have nothing to restore and inherit this no-op.
     */
    suspend fun restorePersistedState() = Unit

    suspend fun findNearbyVexStream(): VexStreamRepositoryResult

    /**
     * Reach the already-paired VexStream computer through the best currently usable route.
     *
     * This is deliberately different from nearby discovery: reconnecting preserves paired identity
     * while transport selection remains repository-owned.
     */
    suspend fun reconnectPairedVexStream(): VexStreamRepositoryResult

    suspend fun confirmPairingCode(): VexStreamRepositoryResult

    suspend fun forgetPairedVexStream(): VexStreamRepositoryResult

    suspend fun refreshCatalog(): VexStreamRepositoryResult

    /**
     * Requests one exact current Hub media object for verified phone-local storage.
     *
     * [trackRef] is the current Hub address. When [expectedMediaSha256] and/or
     * [expectedMediaByteLength] are supplied, the repository must compare those preconditions
     * against the track it re-reads at its serialized operation boundary before admitting the copy.
     *
     * This keeps a rendered request from silently following a reused/moved track address to
     * different bytes between UI/request formation and repository execution.
     */
    suspend fun downloadForOffline(
        trackRef: VexStreamTrackRef,
        expectedMediaSha256: VexStreamMediaSha256? = null,
        expectedMediaByteLength: Long? = null,
    ): VexStreamRepositoryResult

    suspend fun removeOfflineCopy(
        trackRef: VexStreamTrackRef,
    ): VexStreamRepositoryResult
}

enum class VexStreamRepositoryRejection {
    HUB_NOT_READY,
    TRACK_UNKNOWN,
    MEDIA_NOT_AVAILABLE_FROM_HUB,
    ALREADY_DOWNLOADED,
    NOT_DOWNLOADED,
    DISCOVERY_UNAVAILABLE,
    PAIRING_NOT_READY,
    PAIRING_FAILED,
}

/**
 * Repository result is intentionally small until the app's operation/attempt/result foundation is
 * accepted. It does not pretend that a state-only test double executed network or filesystem work.
 */
sealed interface VexStreamRepositoryResult {
    data class Accepted(
        val trackRef: VexStreamTrackRef? = null,
    ) : VexStreamRepositoryResult

    data class Rejected(
        val reason: VexStreamRepositoryRejection,
        val trackRef: VexStreamTrackRef? = null,
    ) : VexStreamRepositoryResult
}

// [VXG RealForever]
