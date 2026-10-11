package com.vextreme.vexlife.android.data.vexstream

import android.content.Context
import java.io.File
import java.net.HttpURLConnection
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
) : VexStreamMusicRepository {

    private val operationMutex = Mutex()
    private val remoteTransport =
        VexStreamRemoteTransport(
            connectionStore,
        )
    private var selectedService: VexStreamSelectedService? = null

    private val mutableState =
        MutableStateFlow(
            initialState(),
        )

    override val state: StateFlow<VexStreamMusicState> =
        mutableState.asStateFlow()

    override suspend fun findNearbyVexStream(): VexStreamRepositoryResult =
        operationMutex.withLock {
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
        operationMutex.withLock {
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
        operationMutex.withLock {
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
        operationMutex.withLock {
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
        operationMutex.withLock {
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
    ): VexStreamRepositoryResult =
        operationMutex.withLock {
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
        operationMutex.withLock {
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
            }

            VexStreamRepositoryResult.Accepted(
                trackRef,
            )
        }

    private suspend fun refreshCatalogLocked(
        connection: VexStreamPairedConnection,
    ): VexStreamRepositoryResult =
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
            mutableState.value = mutableState.value
                .withHubCatalog(
                    catalog,
                )
                .copy(
                    connectionState =
                        VexStreamConnectionState.Paired(
                            displayName = connection.displayName,
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

    private fun initialState(): VexStreamMusicState {
        val offline =
            offlineIndex.load()
        val connection =
            connectionStore.load()

        return if (connection == null) {
            offline.copy(
                connectionState = VexStreamConnectionState.NotPaired,
                hubState = VexStreamHubState.NotConfigured,
            )
        } else {
            offline.copy(
                connectionState =
                    VexStreamConnectionState.Paired(
                        displayName = connection.displayName,
                    ),
                hubState = VexStreamHubState.Unreachable,
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

        fun production(
            context: Context,
        ): PairedVexStreamMusicRepository {
            val mediaStore =
                VexStreamMediaStore.production(
                    contextFilesDir = context.filesDir,
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
            )
        }
    }
}

// [VXG RealForever]
