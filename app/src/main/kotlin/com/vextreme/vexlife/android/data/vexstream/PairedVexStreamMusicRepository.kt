package com.vextreme.vexlife.android.data.vexstream

import android.content.Context
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject

/**
 * Repository-owned single-flight gate for artwork cache preparation.
 *
 * Rechecking local cache presence only after entering the gate prevents a session backfill, explicit
 * library reconciliation and playback preparation from independently fetching/writing the same
 * admitted artwork source at the same time. The gate is held for one source attempt only, not for an
 * entire library backfill, so playback-relevant artwork is not queued behind hundreds of tracks.
 */
internal class VexStreamArtworkSingleFlight {
    private val mutex = Mutex()

    suspend fun cacheIfMissing(
        isCached: () -> Boolean,
        fetch: suspend () -> Boolean,
    ): Boolean =
        mutex.withLock {
            if (isCached()) {
                true
            } else {
                fetch()
            }
        }
}

/**
 * Product-shaped same-Wi-Fi VexStream consumer.
 *
 * Discovery is human-mediated by Android where available. The selected service advertises its exact
 * TLS certificate digest, which is pinned before pairing data is sent. Desktop approval returns a
 * random bearer token that is stored through [VexStreamConnectionStore].
 *
 * This is intentionally a local VexStream pairing candidate. It does not claim canonical Home/Safety
 * membership, authentication, authorization, or capability-lease semantics.
 */
class PairedVexStreamMusicRepository(
    private val mediaStore: VexStreamMediaStore,
    private val offlineIndex: VexStreamOfflineIndex,
    private val connectionStore: VexStreamConnectionStore,
    private val nearbyDiscovery: VexStreamNearbyDiscovery,
    private val artworkStore: VexStreamArtworkStore? = null,
) : VexStreamMusicRepository {

    private val operationMutex = Mutex()
    private val restoreMutex = Mutex()
    private val artworkSingleFlight =
        VexStreamArtworkSingleFlight()
    private var artworkBackfillOffset = 0
    private var persistedStateRestored = false
    private val remoteTransport =
        VexStreamRemoteTransport(
            connectionStore,
        )
    private var selectedService: VexStreamSelectedService? = null

    private val mutableState =
        MutableStateFlow(
            initialConnectionState(),
        )

    override val state: StateFlow<VexStreamMusicState> =
        mutableState.asStateFlow()

    override suspend fun backfillOfflineArtwork() {
        val store =
            artworkStore
                ?: return

        withContext(
            Dispatchers.IO,
        ) {
            val candidates =
                downloadedTracks(
                    mutableState.value,
                )
                    .filter {
                        (
                            it.artworkUrl != null ||
                                it.artworkRef != null
                        ) &&
                            store.fileFor(
                                it,
                            ) == null
                    }
                    .distinctBy {
                        it.artworkUrl
                            ?: "embedded:" +
                                it.artworkRef
                    }

            if (candidates.isEmpty()) {
                artworkBackfillOffset =
                    0
                store.pruneTo(
                    artworkReferencedTracks(
                        mutableState.value,
                    ),
                )
                return@withContext
            }

            val startOffset =
                artworkBackfillOffset %
                    candidates.size
            var consecutiveFailures =
                0

            repeat(
                candidates.size,
            ) { attempt ->
                currentCoroutineContext()
                    .ensureActive()

                if (
                    mutableState.value
                        .hubState !is
                    VexStreamHubState.Ready
                ) {
                    return@withContext
                }

                val index =
                    (
                        startOffset +
                            attempt
                    ) %
                        candidates.size
                val track =
                    candidates[
                        index
                    ]

                artworkBackfillOffset =
                    (
                        index +
                            1
                    ) %
                        candidates.size

                if (
                    cacheArtworkBestEffort(
                        track,
                    )
                ) {
                    consecutiveFailures =
                        0
                } else {
                    consecutiveFailures +=
                        1
                    if (
                        consecutiveFailures >=
                        ARTWORK_BACKFILL_CONSECUTIVE_FAILURE_LIMIT
                    ) {
                        return@withContext
                    }
                }
            }

            currentCoroutineContext()
                .ensureActive()
            store.pruneTo(
                artworkReferencedTracks(
                    mutableState.value,
                ),
            )
        }
    }

    override suspend fun preparePlaybackArtwork(
        trackRef: VexStreamTrackRef,
        expectedMediaSha256: VexStreamMediaSha256,
    ) {
        restorePersistedState()

        val currentTrack =
            operationMutex.withLock {
                mutableState.value
                    .track(
                        trackRef,
                    )
                    ?.track
                    ?.takeIf {
                        it.mediaSha256 ==
                            expectedMediaSha256
                    }
            }
                ?: return

        withContext(
            Dispatchers.IO,
        ) {
            cacheArtworkBestEffort(
                currentTrack,
            )
        }
    }

    override suspend fun restorePersistedState() {
        restoreMutex.withLock {
            if (persistedStateRestored) {
                return@withLock
            }

            val restored =
                withContext(
                    Dispatchers.IO,
                ) {
                    offlineIndex.load()
                }
            val current =
                mutableState.value
            mutableState.value =
                restored.copy(
                    hubState = current.hubState,
                    connectionState =
                        current.connectionState,
                    libraryLoadState =
                        VexStreamLibraryLoadState.READY,
                )
            persistedStateRestored = true
        }
    }

    override suspend fun findNearbyVexStream(): VexStreamRepositoryResult =
        withOperation {
            mutableState.value = mutableState.value
                .withHubUnavailable(VexStreamHubState.NotConfigured)
                .copy(
                    connectionState = VexStreamConnectionState.Searching,
                )

            try {
                val selected =
                    nearbyDiscovery.selectService()
                val remembered =
                    connectionStore.load()

                if (
                    remembered != null &&
                    remembered.certificateSha256 == selected.certificateSha256
                ) {
                    val rerouted =
                        remembered.copy(
                            displayName = selected.displayName,
                            host = selected.host,
                            port = selected.port,
                        )
                    connectionStore.save(
                        rerouted,
                    )
                    selectedService = null
                    mutableState.value = mutableState.value.copy(
                        connectionState =
                            VexStreamConnectionState.Paired(
                                displayName = rerouted.displayName,
                            ),
                    )

                    return refreshCatalogLocked(
                        connection = rerouted,
                    )
                }

                val info =
                    readPairingInfo(
                        selected,
                    )

                selectedService =
                    selected.copy(
                        displayName = info.displayName,
                    )
                mutableState.value = mutableState.value.copy(
                    hubState = VexStreamHubState.PairingRequired,
                    connectionState =
                        VexStreamConnectionState.ConfirmCode(
                            displayName = info.displayName,
                            pairingCode = info.pairingCode,
                        ),
                )

                VexStreamRepositoryResult.Accepted()
            } catch (_: Exception) {
                selectedService = null
                mutableState.value = mutableState.value.copy(
                    hubState = VexStreamHubState.NotConfigured,
                    connectionState =
                        VexStreamConnectionState.Failed(
                            reasonCode = "DISCOVERY_OR_TLS_FAILED",
                        ),
                )

                VexStreamRepositoryResult.Rejected(
                    reason = VexStreamRepositoryRejection.DISCOVERY_UNAVAILABLE,
                )
            }
        }

    override suspend fun reconnectPairedVexStream(): VexStreamRepositoryResult =
        withOperation {
            val remembered =
                connectionStore.load()
                    ?: return VexStreamRepositoryResult.Rejected(
                        reason = VexStreamRepositoryRejection.HUB_NOT_READY,
                    )

            val rememberedRouteResult =
                refreshCatalogLocked(
                    connection = remembered,
                )
            if (
                rememberedRouteResult is
                    VexStreamRepositoryResult.Accepted
            ) {
                return rememberedRouteResult
            }

            // An authorization failure clears pairing inside refreshCatalogBlocking. Do not
            // rediscover and silently rebind a credential that the desktop has revoked.
            val stillPaired =
                connectionStore.load()
                    ?: return rememberedRouteResult

            // Reconnect means "reach this paired PC", not "LAN discovery". Nearby discovery is a
            // bounded recovery only after the remembered local/direct/relay routes all fail. It
            // can refresh a DHCP-changed LAN address, but only for the exact pinned certificate.
            val selected =
                withTimeoutOrNull(
                    RECONNECT_DISCOVERY_TIMEOUT_MS,
                ) {
                    runCatching {
                        nearbyDiscovery.selectService()
                    }.getOrNull()
                }
                    ?: return rememberedRouteResult

            if (
                selected.certificateSha256 !=
                stillPaired.certificateSha256
            ) {
                return rememberedRouteResult
            }

            val rerouted =
                stillPaired.copy(
                    displayName = selected.displayName,
                    host = selected.host,
                    port = selected.port,
                )
            connectionStore.save(
                rerouted,
            )
            selectedService = null
            mutableState.value = mutableState.value.copy(
                connectionState =
                    VexStreamConnectionState.Paired(
                        displayName = rerouted.displayName,
                    ),
            )

            refreshCatalogLocked(
                connection = rerouted,
            )
        }

    override suspend fun confirmPairingCode(): VexStreamRepositoryResult =
        withOperation {
            val selected =
                selectedService
                    ?: return VexStreamRepositoryResult.Rejected(
                        reason = VexStreamRepositoryRejection.PAIRING_NOT_READY,
                    )
            val confirmation =
                mutableState.value.connectionState as? VexStreamConnectionState.ConfirmCode
                    ?: return VexStreamRepositoryResult.Rejected(
                        reason = VexStreamRepositoryRejection.PAIRING_NOT_READY,
                    )

            try {
                val requestRef =
                    createPairingRequest(
                        selected = selected,
                    )

                mutableState.value = mutableState.value.copy(
                    connectionState =
                        VexStreamConnectionState.WaitingForDesktopApproval(
                            displayName = confirmation.displayName,
                        ),
                )

                val approval =
                    awaitPairingDecision(
                        selected = selected,
                        requestRef = requestRef,
                    )

                val connection =
                    VexStreamPairedConnection(
                        displayName = confirmation.displayName,
                        host = selected.host,
                        port = selected.port,
                        certificateSha256 = selected.certificateSha256,
                        token = approval.token,
                        remoteHomeRef = approval.remoteHomeRef,
                        remoteGatewayPublicKey =
                            approval.remoteGatewayPublicKey,
                        remoteDirectBaseUrls =
                            approval.remoteDirectBaseUrls,
                        remoteRelayBaseUrl =
                            approval.remoteRelayBaseUrl,
                    )
                connectionStore.save(connection)
                selectedService = null

                mutableState.value = mutableState.value.copy(
                    connectionState =
                        VexStreamConnectionState.Paired(
                            displayName = connection.displayName,
                        ),
                    hubState = VexStreamHubState.AuthorizationRequired,
                )

                refreshCatalogLocked(
                    connection = connection,
                )
            } catch (_: Exception) {
                mutableState.value = mutableState.value.copy(
                    hubState = VexStreamHubState.PairingRequired,
                    connectionState =
                        VexStreamConnectionState.Failed(
                            reasonCode = "PAIRING_FAILED",
                        ),
                )

                VexStreamRepositoryResult.Rejected(
                    reason = VexStreamRepositoryRejection.PAIRING_FAILED,
                )
            }
        }

    override suspend fun forgetPairedVexStream(): VexStreamRepositoryResult =
        withOperation {
            connectionStore.load()?.let { connection ->
                withContext(
                    Dispatchers.IO,
                ) {
                    runCatching {
                        val response =
                            authorizedConnection(
                                connection = connection,
                                path = "v1/pairing/revoke-self",
                                method = "POST",
                            )
                        response.responseCode
                        response.disconnect()
                    }
                }
            }

            connectionStore.clear()
            selectedService = null
            mutableState.value = mutableState.value
                .withHubUnavailable(
                    VexStreamHubState.NotConfigured,
                )
                .copy(
                    connectionState = VexStreamConnectionState.NotPaired,
                )

            VexStreamRepositoryResult.Accepted()
        }

    override suspend fun refreshCatalog(): VexStreamRepositoryResult =
        withOperation {
            val connection =
                connectionStore.load()
                    ?: return VexStreamRepositoryResult.Rejected(
                        reason = VexStreamRepositoryRejection.HUB_NOT_READY,
                    )

            refreshCatalogLocked(
                connection = connection,
            )
        }

    override suspend fun downloadForOffline(
        trackRef: VexStreamTrackRef,
        expectedMediaSha256: VexStreamMediaSha256?,
        expectedMediaByteLength: Long?,
    ): VexStreamRepositoryResult =
        withOperation {
            val connection =
                connectionStore.load()
                    ?: return VexStreamRepositoryResult.Rejected(
                        reason = VexStreamRepositoryRejection.HUB_NOT_READY,
                        trackRef = trackRef,
                    )

            val current = mutableState.value
            if (current.hubState !is VexStreamHubState.Ready) {
                return VexStreamRepositoryResult.Rejected(
                    reason = VexStreamRepositoryRejection.HUB_NOT_READY,
                    trackRef = trackRef,
                )
            }

            val target =
                current.track(trackRef)
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

            val rediscovered =
                withContext(
                    Dispatchers.IO,
                ) {
                    mediaStore
                        .rediscoverDeviceCopy(
                            expectedSha256 =
                                target.track.mediaSha256,
                            expectedByteLength =
                                target.track.mediaByteLength,
                        )
                }
            if (rediscovered != null) {
                updateLocalCopy(
                    trackRef = trackRef,
                    localCopy = rediscovered,
                )
                withContext(
                    Dispatchers.IO,
                ) {
                    offlineIndex.persist(
                        mutableState.value,
                    )
                }
                return VexStreamRepositoryResult.Rejected(
                    reason =
                        VexStreamRepositoryRejection
                            .ALREADY_DOWNLOADED,
                    trackRef = trackRef,
                )
            }

            updateLocalCopy(
                trackRef = trackRef,
                localCopy =
                    VexStreamLocalCopyState.Downloading(
                        bytesDownloaded = 0L,
                        totalBytes = target.track.mediaByteLength,
                    ),
            )

            downloadMedia(
                connection = connection,
                target = target,
                trackRef = trackRef,
            )
        }

    override suspend fun removeOfflineCopy(
        trackRef: VexStreamTrackRef,
    ): VexStreamRepositoryResult =
        withOperation {
            val target =
                mutableState.value.track(trackRef)
                    ?: return VexStreamRepositoryResult.Rejected(
                        reason = VexStreamRepositoryRejection.TRACK_UNKNOWN,
                        trackRef = trackRef,
                    )

            val local =
                target.localCopy as? VexStreamLocalCopyState.Downloaded
                    ?: return VexStreamRepositoryResult.Rejected(
                        reason = VexStreamRepositoryRejection.NOT_DOWNLOADED,
                        trackRef = trackRef,
                    )

            val removed =
                withContext(
                    Dispatchers.IO,
                ) {
                    mediaStore.remove(
                        local,
                    )
                }
            if (!removed) {
                return VexStreamRepositoryResult.Rejected(
                    reason = VexStreamRepositoryRejection.NOT_DOWNLOADED,
                    trackRef = trackRef,
                )
            }

            updateLocalCopy(
                trackRef,
                VexStreamLocalCopyState.NotDownloaded,
            )
            withContext(
                Dispatchers.IO,
            ) {
                offlineIndex.persist(
                    mutableState.value,
                )
                artworkStore
                    ?.pruneTo(
                        downloadedTracks(
                            mutableState.value,
                        ),
                    )
            }

            VexStreamRepositoryResult.Accepted(
                trackRef,
            )
        }

    private suspend fun refreshCatalogLocked(
        connection: VexStreamPairedConnection,
    ): VexStreamRepositoryResult {
        mutableState.value =
            mutableState.value.copy(
                libraryLoadState =
                    VexStreamLibraryLoadState.REFRESHING_CATALOG,
            )

        return try {
            withContext(
                Dispatchers.IO,
            ) {
                refreshCatalogBlocking(
                    connection =
                        upgradeRemotePairingIfAvailable(
                            connection,
                        ),
                )
            }
        } finally {
            mutableState.value =
                mutableState.value.copy(
                    libraryLoadState =
                        VexStreamLibraryLoadState.READY,
                )
        }
    }

    private fun upgradeRemotePairingIfAvailable(
        connection: VexStreamPairedConnection,
    ): VexStreamPairedConnection =
        runCatching {
            val request =
                authorizedConnection(
                    connection = connection,
                    path = "v1/pairing/remote-upgrade",
                    method = "POST",
                )
            request.doOutput = true
            request.setRequestProperty(
                "Content-Type",
                "application/json",
            )
            request.outputStream
                .bufferedWriter()
                .use {
                    it.write(
                        JSONObject()
                            .put(
                                "devicePublicKey",
                                connectionStore
                                    .deviceSigningPublicKey(),
                            )
                            .toString(),
                    )
                }

            if (
                request.responseCode !=
                HttpURLConnection.HTTP_OK
            ) {
                request.disconnect()
                return@runCatching connection
            }

            val response =
                JSONObject(
                    VexStreamPinnedTls.readText(
                        request,
                    ),
                )
            request.disconnect()

            val upgraded =
                connection.copy(
                    remoteHomeRef =
                        response.optString(
                            "remoteHomeRef",
                        ).takeIf {
                            it.isNotBlank()
                        },
                    remoteGatewayPublicKey =
                        response.optString(
                            "remoteGatewayPublicKey",
                        ).takeIf {
                            it.isNotBlank()
                        },
                    remoteDirectBaseUrls =
                        response.remoteDirectBaseUrls(),
                    remoteRelayBaseUrl =
                        response.optString(
                            "remoteRelayBaseUrl",
                        ).takeIf {
                            it.startsWith(
                                "https://",
                            )
                        },
                )
            connectionStore.save(
                upgraded,
            )
            upgraded
        }.getOrElse {
            // Remote route refresh is opportunistic while the already-paired desktop is directly
            // reachable. Failure must not make the proven same-LAN product path unusable.
            connection
        }

    private fun refreshCatalogBlocking(
        connection: VexStreamPairedConnection,
    ): VexStreamRepositoryResult {
        mutableState.value = mutableState.value.copy(
            connectionState =
                VexStreamConnectionState.Paired(
                    displayName = connection.displayName,
                ),
        )

        return try {
            val response =
                authorizedTextResponse(
                    connection = connection,
                    path = "v1/catalog",
                )
            val status =
                response.status

            if (status == HttpURLConnection.HTTP_UNAUTHORIZED) {
                connectionStore.clear()
                mutableState.value = mutableState.value
                    .withHubUnavailable(
                        VexStreamHubState.Revoked,
                    )
                    .copy(
                        connectionState =
                            VexStreamConnectionState.Failed(
                                reasonCode = "DESKTOP_ACCESS_REVOKED",
                            ),
                    )

                return VexStreamRepositoryResult.Rejected(
                    reason = VexStreamRepositoryRejection.HUB_NOT_READY,
                )
            }

            if (status != HttpURLConnection.HTTP_OK) {
                mutableState.value =
                    mutableState.value.withHubUnavailable(
                        VexStreamHubState.Unreachable,
                    )

                return VexStreamRepositoryResult.Rejected(
                    reason = VexStreamRepositoryRejection.HUB_NOT_READY,
                )
            }

            val body =
                response.body

            val catalog =
                parseCatalog(
                    body,
                )
            val offlineBeforeRefresh =
                offlineSnapshot(
                    mutableState.value,
                )
            val migrationChanged =
                migrateRetainedPrivateCopies(
                    catalog,
                )
            val retainedLocalDigests =
                mutableState.value
                    .tracks
                    .mapNotNull { state ->
                        (
                            state.localCopy as?
                                VexStreamLocalCopyState.Downloaded
                        )
                            ?.mediaSha256
                    }
                    .toSet()
            val rediscoveredLocalCopies =
                catalog.tracks
                    .filter { projection ->
                        projection.mediaSha256 !in
                            retainedLocalDigests
                    }
                    .mapNotNull { projection ->
                        mediaStore
                            .rediscoverDeviceCopy(
                                expectedSha256 =
                                    projection.mediaSha256,
                                expectedByteLength =
                                    projection.mediaByteLength,
                            )
                            ?.let { localCopy ->
                                projection.mediaSha256 to
                                    localCopy
                            }
                    }
                    .toMap()

            val nextState =
                mutableState.value
                    .withHubCatalog(
                        catalog = catalog,
                        rediscoveredLocalCopies =
                            rediscoveredLocalCopies,
                    )
                    .copy(
                        connectionState =
                            VexStreamConnectionState.Paired(
                                displayName = connection.displayName,
                            ),
                    )

            val offlineAfterRefresh =
                offlineSnapshot(
                    nextState,
                )
            if (
                migrationChanged ||
                rediscoveredLocalCopies
                    .isNotEmpty() ||
                offlineBeforeRefresh !=
                    offlineAfterRefresh
            ) {
                offlineIndex.persist(
                    nextState,
                )
            }

            // Publish current local-copy truth only after the matching restart index write has
            // completed. A collector must not observe HUB_AND_LOCAL in the gap before its private
            // recovery index is rebuilt.
            mutableState.value =
                nextState

            artworkStore
                ?.pruneTo(
                    artworkReferencedTracks(
                        nextState,
                    ),
                )

            VexStreamRepositoryResult.Accepted()
        } catch (_: Exception) {
            mutableState.value = mutableState.value
                .withHubUnavailable(
                    VexStreamHubState.Unreachable,
                )
                .copy(
                    connectionState =
                        VexStreamConnectionState.Paired(
                            displayName = connection.displayName,
                        ),
                )

            VexStreamRepositoryResult.Rejected(
                reason = VexStreamRepositoryRejection.HUB_NOT_READY,
            )
        }
    }

    private fun migrateRetainedPrivateCopies(
        catalog: VexStreamHubCatalogProjection,
    ): Boolean {
        var current =
            mutableState.value
        var changed =
            false

        catalog.tracks.forEach { projection ->
            val localCopy =
                current.tracks
                    .firstOrNull { state ->
                        state.track.mediaSha256 ==
                            projection.mediaSha256
                    }
                    ?.localCopy
                    as? VexStreamLocalCopyState.Downloaded
                    ?: return@forEach

            if (
                localCopy.byteLength !=
                projection.mediaByteLength
            ) {
                return@forEach
            }

            val migrated =
                mediaStore.migratePrivateCopy(
                    track = projection.toTrack(),
                    localCopy = localCopy,
                ) as? VexStreamMediaStoreResult.Stored
                    ?: return@forEach

            if (
                migrated.localCopy ==
                localCopy
            ) {
                return@forEach
            }

            current =
                current.copy(
                    tracks =
                        current.tracks.map { state ->
                            if (
                                state.track.mediaSha256 ==
                                projection.mediaSha256 &&
                                state.localCopy ==
                                localCopy
                            ) {
                                state.copy(
                                    localCopy =
                                        migrated.localCopy,
                                )
                            } else {
                                state
                            }
                        },
                )
            changed =
                true
        }

        if (changed) {
            mutableState.value =
                current
        }

        return changed
    }

    private suspend fun readPairingInfo(
        selected: VexStreamSelectedService,
    ): PairingInfo =
        withContext(
            Dispatchers.IO,
        ) {
            readPairingInfoBlocking(
                selected = selected,
            )
        }

    private fun readPairingInfoBlocking(
        selected: VexStreamSelectedService,
    ): PairingInfo {
        val connection =
            VexStreamPinnedTls.open(
                baseUrl = selected.baseUrl(),
                certificateSha256 = selected.certificateSha256,
                path = "v1/pairing/info",
            )

        check(
            connection.responseCode == HttpURLConnection.HTTP_OK,
        ) {
            "pairing info unavailable"
        }

        val body =
            connection.inputStream
                .bufferedReader()
                .use { it.readText() }
        connection.disconnect()

        val json =
            JSONObject(body)
        val fingerprint =
            json.getString(
                "certificateSha256",
            ).lowercase()

        check(
            fingerprint == selected.certificateSha256,
        ) {
            "selected VexStream certificate identity changed"
        }
        check(
            json.optBoolean(
                "pairingWindowOpen",
                false,
            ),
        ) {
            "desktop pairing window is closed"
        }

        return PairingInfo(
            displayName =
                json.getString(
                    "displayName",
                ),
            pairingCode =
                json.getString(
                    "pairingCode",
                ),
        )
    }

    private suspend fun createPairingRequest(
        selected: VexStreamSelectedService,
    ): String =
        withContext(
            Dispatchers.IO,
        ) {
            createPairingRequestBlocking(
                selected = selected,
            )
        }

    private fun createPairingRequestBlocking(
        selected: VexStreamSelectedService,
    ): String {
        val connection =
            VexStreamPinnedTls.open(
                baseUrl = selected.baseUrl(),
                certificateSha256 = selected.certificateSha256,
                path = "v1/pairing/request",
                method = "POST",
            )
        connection.doOutput = true
        connection.setRequestProperty(
            "Content-Type",
            "application/json",
        )

        val body =
            JSONObject()
                .put(
                    "deviceRef",
                    connectionStore.deviceRef(),
                )
                .put(
                    "displayName",
                    connectionStore.deviceDisplayName(),
                )
                .put(
                    "devicePublicKey",
                    connectionStore.deviceSigningPublicKey(),
                )
                .toString()
        connection.outputStream
            .bufferedWriter()
            .use {
                it.write(body)
            }

        check(
            connection.responseCode == HttpURLConnection.HTTP_OK,
        ) {
            "pairing request rejected"
        }

        val response =
            JSONObject(
                VexStreamPinnedTls.readText(
                    connection,
                ),
            )
        connection.disconnect()

        return response.getString(
            "requestRef",
        )
    }

    private fun JSONObject.remoteDirectBaseUrls(): List<String> {
        val routes =
            optJSONArray(
                "remoteDirectBaseUrls",
            ) ?: return emptyList()

        return buildList {
            for (
                index in
                    0 until
                        minOf(
                            routes.length(),
                            MAX_REMOTE_DIRECT_ROUTES,
                        )
            ) {
                routes.optString(
                    index,
                ).takeIf {
                    it.startsWith(
                        "https://",
                    ) &&
                        it.length <=
                        MAX_REMOTE_DIRECT_ROUTE_LENGTH
                }?.let(
                    ::add,
                )
            }
        }.distinct()
    }

    private data class PairingApproval(
        val token: String,
        val remoteHomeRef: String?,
        val remoteGatewayPublicKey: String?,
        val remoteDirectBaseUrls: List<String>,
        val remoteRelayBaseUrl: String?,
    )

    private suspend fun awaitPairingDecision(
        selected: VexStreamSelectedService,
        requestRef: String,
    ): PairingApproval =
        withTimeout(PAIRING_APPROVAL_TIMEOUT_MS) {
            while (true) {
                val response =
                    withContext(
                        Dispatchers.IO,
                    ) {
                        readPairingStatusBlocking(
                            selected = selected,
                            requestRef = requestRef,
                        )
                    }

                when (
                    response.getString(
                        "state",
                    )
                ) {
                    "APPROVED" ->
                        return@withTimeout PairingApproval(
                            token =
                                response.getString(
                                    "token",
                                ),
                            remoteHomeRef =
                                response.optString(
                                    "remoteHomeRef",
                                ).takeIf {
                                    it.isNotBlank()
                                },
                            remoteGatewayPublicKey =
                                response.optString(
                                    "remoteGatewayPublicKey",
                                ).takeIf {
                                    it.isNotBlank()
                                },
                            remoteDirectBaseUrls =
                                response.remoteDirectBaseUrls(),
                            remoteRelayBaseUrl =
                                response.optString(
                                    "remoteRelayBaseUrl",
                                ).takeIf {
                                    it.startsWith(
                                        "https://",
                                    )
                                },
                        )

                    "DENIED" ->
                        error(
                            "desktop denied pairing",
                        )
                }

                delay(
                    PAIRING_POLL_MS,
                )
            }

            error(
                "pairing approval unavailable",
            )
        }

    private fun readPairingStatusBlocking(
        selected: VexStreamSelectedService,
        requestRef: String,
    ): JSONObject {
        val connection =
            VexStreamPinnedTls.open(
                baseUrl = selected.baseUrl(),
                certificateSha256 = selected.certificateSha256,
                path =
                    "v1/pairing/status?requestRef=" +
                        encodeQueryComponent(
                            requestRef,
                        ),
            )

        check(
            connection.responseCode == HttpURLConnection.HTTP_OK,
        ) {
            "pairing request disappeared"
        }

        return try {
            JSONObject(
                VexStreamPinnedTls.readText(
                    connection,
                ),
            )
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun downloadMedia(
        connection: VexStreamPairedConnection,
        target: VexStreamTrackState,
        trackRef: VexStreamTrackRef,
    ): VexStreamRepositoryResult =
        withContext(
            Dispatchers.IO,
        ) {
            try {
                val media =
                    authorizedMediaStream(
                        connection = connection,
                        path =
                            "v1/media/" +
                                encodePathSegment(
                                    trackRef.value,
                                ),
                        expectedLength =
                            target.track.mediaByteLength,
                    )

                if (media.status != HttpURLConnection.HTTP_OK) {
                    media.input?.close()
                    updateLocalCopy(
                        trackRef,
                        VexStreamLocalCopyState.Failed(
                            reasonCode = "HTTP_${media.status}",
                            retryable = media.status >= 500,
                        ),
                    )

                    return@withContext VexStreamRepositoryResult.Rejected(
                        reason =
                            VexStreamRepositoryRejection
                                .MEDIA_NOT_AVAILABLE_FROM_HUB,
                        trackRef = trackRef,
                    )
                }

                val input =
                    requireNotNull(
                        media.input,
                    )
                val stored =
                    input.use {
                        mediaStore.store(
                            expectedSha256 =
                                target.track.mediaSha256,
                            expectedByteLength =
                                target.track.mediaByteLength,
                            input = it,
                            track =
                                target.track,
                        )
                    }

                when (stored) {
                    is VexStreamMediaStoreResult.Stored -> {
                        updateLocalCopy(
                            trackRef,
                            stored.localCopy,
                        )
                        if (
                            !offlineIndex.persist(
                                mutableState.value,
                            )
                        ) {
                            updateLocalCopy(
                                trackRef,
                                VexStreamLocalCopyState.Failed(
                                    reasonCode =
                                        "OFFLINE_INDEX_PERSISTENCE_FAILURE",
                                    retryable = true,
                                ),
                            )
                            return@withContext VexStreamRepositoryResult.Rejected(
                                reason =
                                    VexStreamRepositoryRejection
                                        .MEDIA_NOT_AVAILABLE_FROM_HUB,
                                trackRef = trackRef,
                            )
                        }

                        VexStreamRepositoryResult.Accepted(
                            trackRef,
                        )
                    }

                    is VexStreamMediaStoreResult.Rejected -> {
                        updateLocalCopy(
                            trackRef,
                            VexStreamLocalCopyState.Failed(
                                reasonCode =
                                    stored.reason.name,
                                retryable =
                                    stored.reason ==
                                        VexStreamMediaStoreRejection
                                            .IO_FAILURE,
                            ),
                        )

                        VexStreamRepositoryResult.Rejected(
                            reason =
                                VexStreamRepositoryRejection
                                    .MEDIA_NOT_AVAILABLE_FROM_HUB,
                            trackRef = trackRef,
                        )
                    }
                }
            } catch (_: Exception) {
                mutableState.value =
                    mutableState.value.withHubUnavailable(
                        VexStreamHubState.Unreachable,
                    )
                updateLocalCopy(
                    trackRef,
                    VexStreamLocalCopyState.Failed(
                        reasonCode = "TRANSPORT_FAILURE",
                        retryable = true,
                    ),
                )

                VexStreamRepositoryResult.Rejected(
                    reason =
                        VexStreamRepositoryRejection
                            .MEDIA_NOT_AVAILABLE_FROM_HUB,
                    trackRef = trackRef,
                )
            }
        }

    private data class AuthorizedTextResponse(
        val status: Int,
        val body: String,
    )

    private data class AuthorizedMediaStream(
        val status: Int,
        val input: java.io.InputStream?,
    )

    private fun authorizedTextResponse(
        connection: VexStreamPairedConnection,
        path: String,
    ): AuthorizedTextResponse {
        try {
            val direct =
                authorizedConnection(
                    connection = connection,
                    path = path,
                )
            val status =
                direct.responseCode
            val body =
                VexStreamPinnedTls.readText(
                    direct,
                )
            direct.disconnect()
            return AuthorizedTextResponse(
                status = status,
                body = body,
            )
        } catch (directFailure: Exception) {
            if (!connection.remoteReady) {
                throw directFailure
            }
        }

        val remote =
            remoteTransport.execute(
                connection = connection,
                method = "GET",
                path = path,
            )
        return AuthorizedTextResponse(
            status = remote.status,
            body =
                String(
                    remote.body,
                    Charsets.UTF_8,
                ),
        )
    }

    private fun authorizedMediaStream(
        connection: VexStreamPairedConnection,
        path: String,
        expectedLength: Long,
    ): AuthorizedMediaStream {
        try {
            val direct =
                authorizedConnection(
                    connection = connection,
                    path = path,
                )
            val status =
                direct.responseCode
            if (
                status ==
                HttpURLConnection.HTTP_OK
            ) {
                return AuthorizedMediaStream(
                    status = status,
                    input =
                        object :
                            java.io.FilterInputStream(
                                direct.inputStream,
                            ) {
                            override fun close() {
                                try {
                                    super.close()
                                } finally {
                                    direct.disconnect()
                                }
                            }
                        },
                )
            }
            direct.disconnect()
            return AuthorizedMediaStream(
                status = status,
                input = null,
            )
        } catch (directFailure: Exception) {
            if (!connection.remoteReady) {
                throw directFailure
            }
        }

        return AuthorizedMediaStream(
            status = HttpURLConnection.HTTP_OK,
            input =
                remoteTransport.openRangeStream(
                    connection = connection,
                    path = path,
                    expectedLength = expectedLength,
                ),
        )
    }

    private fun authorizedConnection(
        connection: VexStreamPairedConnection,
        path: String,
        method: String = "GET",
    ) =
        VexStreamPinnedTls.open(
            baseUrl = connection.endpointBaseUrl(),
            certificateSha256 = connection.certificateSha256,
            path = path,
            method = method,
            token = connection.token,
        )

    /**
     * Artwork is a best-effort companion to verified audio, never a download-integrity predicate.
     *
     * Only HTTPS sources admitted by [VexStreamTrack] are followed, redirects may not downgrade
     * transport, response bytes stay bounded, and any failure leaves the verified audio copy intact.
     */
    private suspend fun cacheArtworkBestEffort(
        track: VexStreamTrack,
    ): Boolean {
        val store =
            artworkStore
                ?: return false

        return artworkSingleFlight
            .cacheIfMissing(
                isCached = {
                    store.fileFor(
                        track,
                    ) != null
                },
                fetch = {
                    when {
                        track.artworkUrl != null ->
                            cacheExternalArtworkBestEffort(
                                track = track,
                                artworkUrl =
                                    track.artworkUrl,
                                store = store,
                            )

                        track.artworkRef != null ->
                            cacheEmbeddedArtworkBestEffort(
                                track = track,
                                artworkRef =
                                    track.artworkRef,
                                store = store,
                            )

                        else ->
                            false
                    }
                },
            )
    }

    private fun cacheExternalArtworkBestEffort(
        track: VexStreamTrack,
        artworkUrl: String,
        store: VexStreamArtworkStore,
    ): Boolean {
        return runCatching {
            var current =
                URL(
                    artworkUrl,
                )

            for (
                redirectCount in
                0..ARTWORK_MAX_REDIRECTS
            ) {
                if (
                    !current.protocol.equals(
                        "https",
                        ignoreCase = true,
                    )
                ) {
                    return@runCatching false
                }

                val connection =
                    current.openConnection()
                        as? HttpsURLConnection
                        ?: return@runCatching false
                try {
                    connection.instanceFollowRedirects =
                        false
                    connection.connectTimeout =
                        ARTWORK_CONNECT_TIMEOUT_MS
                    connection.readTimeout =
                        ARTWORK_READ_TIMEOUT_MS
                    connection.requestMethod =
                        "GET"
                    connection.setRequestProperty(
                        "Accept",
                        "image/*",
                    )

                    when (
                        val status =
                            connection.responseCode
                    ) {
                        HttpURLConnection.HTTP_MOVED_PERM,
                        HttpURLConnection.HTTP_MOVED_TEMP,
                        HttpURLConnection.HTTP_SEE_OTHER,
                        HTTP_TEMPORARY_REDIRECT,
                        HTTP_PERMANENT_REDIRECT -> {
                            if (
                                redirectCount ==
                                ARTWORK_MAX_REDIRECTS
                            ) {
                                return@runCatching false
                            }

                            val location =
                                connection
                                    .getHeaderField(
                                        "Location",
                                    )
                                    ?.trim()
                                    ?.takeIf {
                                        it.isNotBlank()
                                    }
                                    ?: return@runCatching false
                            current =
                                URL(
                                    current,
                                    location,
                                )
                            if (
                                !current.protocol.equals(
                                    "https",
                                    ignoreCase = true,
                                )
                            ) {
                                return@runCatching false
                            }
                            continue
                        }

                        HttpURLConnection.HTTP_OK ->
                            Unit

                        else ->
                            return@runCatching false
                    }

                    val contentType =
                        connection.contentType
                            ?.substringBefore(
                                ';',
                            )
                            ?.trim()
                            ?.lowercase()
                    if (
                        contentType == null ||
                        !contentType.startsWith(
                            "image/",
                        )
                    ) {
                        return@runCatching false
                    }

                    val advertisedLength =
                        connection.contentLengthLong
                    if (
                        advertisedLength == 0L ||
                        advertisedLength >
                        VexStreamArtworkStore
                            .MAX_ARTWORK_BYTES
                    ) {
                        return@runCatching false
                    }

                    return@runCatching connection
                        .inputStream
                        .use { input ->
                            store.store(
                                track = track,
                                input = input,
                            ) != null
                        }
                } finally {
                    connection.disconnect()
                }
            }

            false
        }.getOrDefault(
            false,
        )
    }

    private fun cacheEmbeddedArtworkBestEffort(
        track: VexStreamTrack,
        artworkRef: String,
        store: VexStreamArtworkStore,
    ): Boolean {
        val paired =
            connectionStore.load()
                ?: return false
        val encodedRef =
            URLEncoder.encode(
                artworkRef,
                Charsets.UTF_8.name(),
            ).replace(
                "+",
                "%20",
            )

        return runCatching {
            val connection =
                authorizedConnection(
                    connection = paired,
                    path =
                        "v1/artwork/" +
                            encodedRef,
                )
            try {
                connection.connectTimeout =
                    ARTWORK_CONNECT_TIMEOUT_MS
                connection.readTimeout =
                    ARTWORK_READ_TIMEOUT_MS
                connection.setRequestProperty(
                    "Accept",
                    "image/*",
                )

                if (
                    connection.responseCode !=
                    HttpURLConnection.HTTP_OK
                ) {
                    return@runCatching false
                }

                val contentType =
                    connection.contentType
                        ?.substringBefore(
                            ';',
                        )
                        ?.trim()
                        ?.lowercase()
                if (
                    contentType == null ||
                    !contentType.startsWith(
                        "image/",
                    )
                ) {
                    return@runCatching false
                }

                val advertisedLength =
                    connection.contentLengthLong
                if (
                    advertisedLength <= 0L ||
                    advertisedLength >
                    VexStreamArtworkStore
                        .MAX_ARTWORK_BYTES
                ) {
                    return@runCatching false
                }

                connection
                    .inputStream
                    .use { input ->
                        store.store(
                            track = track,
                            input = input,
                        ) != null
                    }
            } finally {
                connection.disconnect()
            }
        }.getOrDefault(
            false,
        )
    }

    private fun artworkReferencedTracks(
        state: VexStreamMusicState,
    ): List<VexStreamTrack> =
        state.tracks
            .map {
                it.track
            }
            .filter {
                it.artworkUrl != null ||
                    it.artworkRef != null
            }

    private fun downloadedTracks(
        state: VexStreamMusicState,
    ): List<VexStreamTrack> =
        state.tracks
            .mapNotNull { item ->
                if (
                    item.localCopy is
                    VexStreamLocalCopyState.Downloaded
                ) {
                    item.track
                } else {
                    null
                }
            }

    private fun offlineSnapshot(
        state: VexStreamMusicState,
    ): List<
        Pair<
            VexStreamTrack,
            VexStreamLocalCopyState.Downloaded,
        >
    > =
        state.tracks
            .mapNotNull { item ->
                val local =
                    item.localCopy as?
                        VexStreamLocalCopyState.Downloaded
                        ?: return@mapNotNull null
                item.track to local
            }
            .sortedBy {
                it.first.mediaSha256.value
            }

    private fun updateLocalCopy(
        trackRef: VexStreamTrackRef,
        localCopy: VexStreamLocalCopyState,
    ) {
        val current =
            mutableState.value
        mutableState.value =
            current.copy(
                tracks =
                    current.tracks.map { state ->
                        if (state.track.trackRef == trackRef) {
                            state.copy(
                                localCopy = localCopy,
                            )
                        } else {
                            state
                        }
                    },
            )
    }

    private fun parseCatalog(
        body: String,
    ): VexStreamHubCatalogProjection {
        val root =
            JSONObject(body)
        val tracksJson =
            root.getJSONArray(
                "tracks",
            )
        val tracks =
            buildList {
                repeat(
                    tracksJson.length(),
                ) { index ->
                    val track =
                        tracksJson.getJSONObject(
                            index,
                        )
                    add(
                        VexStreamHubTrackProjection(
                            trackRef =
                                VexStreamTrackRef(
                                    track.getString(
                                        "trackRef",
                                    ),
                                ),
                            mediaSha256 =
                                VexStreamMediaSha256(
                                    track.getString(
                                        "mediaSha256",
                                    ),
                                ),
                            mediaByteLength =
                                track.getLong(
                                    "mediaByteLength",
                                ),
                            title =
                                track.getString(
                                    "title",
                                ),
                            artist =
                                track.optString(
                                    "artist",
                                ),
                            artworkUrl =
                                track.optString(
                                    "artworkUrl",
                                )
                                    .trim()
                                    .takeIf {
                                        it.startsWith(
                                            "https://",
                                        )
                                    },
                            artworkRef =
                                track.optString(
                                    "artworkRef",
                                )
                                    .trim()
                                    .takeIf {
                                        it.isNotBlank() &&
                                            !it.contains('/') &&
                                            !it.contains('\\')
                                    },
                            album =
                                track.optString(
                                    "album",
                                ),
                            genre =
                                track.optString(
                                    "genre",
                                ),
                            year =
                                track.optString(
                                    "year",
                                ),
                            durationSeconds =
                                track.optDouble(
                                    "durationSeconds",
                                    0.0,
                                ),
                            playbackStartSeconds =
                                track.optDouble(
                                    "playbackStartSeconds",
                                    0.0,
                                ),
                            playbackEndSeconds =
                                if (
                                    track.has(
                                        "playbackEndSeconds",
                                    ) &&
                                    !track.isNull(
                                        "playbackEndSeconds",
                                    )
                                ) {
                                    track.getDouble(
                                        "playbackEndSeconds",
                                    )
                                } else {
                                    null
                                },
                        ),
                    )
                }
            }

        val playlistsJson =
            root.optJSONArray(
                "playlists",
            )
        val playlists =
            buildList {
                if (playlistsJson != null) {
                    repeat(
                        playlistsJson.length(),
                    ) { index ->
                        val playlist =
                            playlistsJson.getJSONObject(
                                index,
                            )
                        val refsJson =
                            playlist.getJSONArray(
                                "trackRefs",
                            )
                        val refs =
                            buildList {
                                repeat(
                                    refsJson.length(),
                                ) { refIndex ->
                                    add(
                                        VexStreamTrackRef(
                                            refsJson.getString(
                                                refIndex,
                                            ),
                                        ),
                                    )
                                }
                            }
                        add(
                            VexStreamHubPlaylistProjection(
                                playlistRef =
                                    VexStreamPlaylistRef(
                                        playlist.getString(
                                            "playlistRef",
                                        ),
                                    ),
                                name =
                                    playlist.getString(
                                        "name",
                                    ),
                                trackRefs = refs,
                            ),
                        )
                    }
                }
            }

        return VexStreamHubCatalogProjection(
            hubRef =
                root.getString(
                    "hubRef",
                ),
            sourceVersion =
                root.getString(
                    "sourceVersion",
                ),
            catalogRevision =
                root.getString(
                    "catalogRevision",
                ),
            tracks = tracks,
            playlists = playlists,
        )
    }

    private suspend inline fun <Result> withOperation(
        operation: () -> Result,
    ): Result {
        restorePersistedState()
        return operationMutex.withLock {
            operation()
        }
    }

    private fun initialConnectionState(): VexStreamMusicState {
        val connection =
            connectionStore.load()

        return if (connection == null) {
            VexStreamMusicState.Empty.copy(
                libraryLoadState =
                    VexStreamLibraryLoadState.RESTORING_LOCAL,
            )
        } else {
            VexStreamMusicState.Empty.copy(
                connectionState =
                    VexStreamConnectionState.Paired(
                        displayName = connection.displayName,
                    ),
                hubState = VexStreamHubState.Unreachable,
                libraryLoadState =
                    VexStreamLibraryLoadState.RESTORING_LOCAL,
            )
        }
    }

    private fun VexStreamSelectedService.baseUrl(): String {
        val normalizedHost =
            if (":" in host && !host.startsWith("[")) {
                "[$host]"
            } else {
                host
            }

        return "https://$normalizedHost:$port"
    }

    private fun encodePathSegment(
        value: String,
    ): String =
        URLEncoder.encode(
            value,
            Charsets.UTF_8.name(),
        ).replace(
            "+",
            "%20",
        )

    private fun encodeQueryComponent(
        value: String,
    ): String =
        URLEncoder.encode(
            value,
            Charsets.UTF_8.name(),
        )

    private data class PairingInfo(
        val displayName: String,
        val pairingCode: String,
    ) {
        init {
            require(displayName.isNotBlank())
            require(pairingCode.matches(Regex("^[0-9]{6}$")))
        }
    }

    companion object {
        private const val MAX_REMOTE_DIRECT_ROUTES = 8
        private const val MAX_REMOTE_DIRECT_ROUTE_LENGTH = 512
        private const val PAIRING_POLL_MS = 750L
        private const val PAIRING_APPROVAL_TIMEOUT_MS = 5 * 60 * 1000L
        private const val RECONNECT_DISCOVERY_TIMEOUT_MS = 8_000L
        private const val ARTWORK_BACKFILL_CONSECUTIVE_FAILURE_LIMIT = 3
        private const val ARTWORK_MAX_REDIRECTS = 3
        private const val ARTWORK_CONNECT_TIMEOUT_MS = 2_000
        private const val ARTWORK_READ_TIMEOUT_MS = 3_000
        private const val HTTP_TEMPORARY_REDIRECT = 307
        private const val HTTP_PERMANENT_REDIRECT = 308

        fun production(
            context: Context,
        ): PairedVexStreamMusicRepository {
            val mediaStore =
                VexStreamMediaStore.production(
                    context = context,
                )

            return PairedVexStreamMusicRepository(
                mediaStore = mediaStore,
                offlineIndex =
                    VexStreamOfflineIndex.production(
                        contextFilesDir = context.filesDir,
                        mediaStore = mediaStore,
                    ),
                connectionStore =
                    VexStreamConnectionStore(
                        context,
                    ),
                nearbyDiscovery =
                    VexStreamNearbyDiscovery(
                        context,
                    ),
                artworkStore =
                    VexStreamArtworkStore.production(
                        context.filesDir,
                    ),
            )
        }
    }
}

// [VXG RealForever]
