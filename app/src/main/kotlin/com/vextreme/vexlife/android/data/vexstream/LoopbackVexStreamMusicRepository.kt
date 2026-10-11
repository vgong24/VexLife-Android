package com.vextreme.vexlife.android.data.vexstream

import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * First lived-device VexStream transport.
 *
 * The matching desktop producer stays bound to desktop loopback. During the initial physical-device
 * proof, adb reverse maps the phone's localhost port to that desktop loopback listener.
 *
 * This class owns transport/parsing only. It does not claim Home pairing, authentication,
 * authorization, or final product reachability.
 */
class LoopbackVexStreamMusicRepository(
    private val mediaStore: VexStreamMediaStore,
    private val offlineIndex: VexStreamOfflineIndex,
    private val baseUrl: String = DEFAULT_BASE_URL,
) : VexStreamMusicRepository {

    private val operationMutex = Mutex()
    private val mutableState = MutableStateFlow(
        offlineIndex.load(),
    )

    override val state: StateFlow<VexStreamMusicState> =
        mutableState.asStateFlow()

    override suspend fun findNearbyVexStream(): VexStreamRepositoryResult =
        VexStreamRepositoryResult.Rejected(
            reason = VexStreamRepositoryRejection.DISCOVERY_UNAVAILABLE,
        )

    override suspend fun confirmPairingCode(): VexStreamRepositoryResult =
        VexStreamRepositoryResult.Rejected(
            reason = VexStreamRepositoryRejection.PAIRING_NOT_READY,
        )

    override suspend fun forgetPairedVexStream(): VexStreamRepositoryResult {
        mutableState.value = mutableState.value
            .withHubUnavailable(VexStreamHubState.NotConfigured)
            .copy(
                connectionState = VexStreamConnectionState.NotPaired,
            )
        return VexStreamRepositoryResult.Accepted()
    }

    override suspend fun refreshCatalog(): VexStreamRepositoryResult =
        operationMutex.withLock {
            withContext(Dispatchers.IO) {
                try {
                    val response = request("v1/catalog")
                    if (response.responseCode != HttpURLConnection.HTTP_OK) {
                        response.disconnect()
                        mutableState.value = mutableState.value.withHubUnavailable(
                            VexStreamHubState.Unreachable,
                        )
                        return@withContext VexStreamRepositoryResult.Rejected(
                            reason = VexStreamRepositoryRejection.HUB_NOT_READY,
                        )
                    }

                    val body = response.inputStream.bufferedReader().use { it.readText() }
                    response.disconnect()

                    val catalog = parseCatalog(body)
                    mutableState.value = mutableState.value.withHubCatalog(catalog)
                    VexStreamRepositoryResult.Accepted()
                } catch (_: Exception) {
                    mutableState.value = mutableState.value.withHubUnavailable(
                        VexStreamHubState.Unreachable,
                    )
                    VexStreamRepositoryResult.Rejected(
                        reason = VexStreamRepositoryRejection.HUB_NOT_READY,
                    )
                }
            }
        }

    override suspend fun downloadForOffline(
        trackRef: VexStreamTrackRef,
    ): VexStreamRepositoryResult =
        operationMutex.withLock {
            withContext(Dispatchers.IO) {
                val current = mutableState.value
                if (current.hubState !is VexStreamHubState.Ready) {
                    return@withContext VexStreamRepositoryResult.Rejected(
                        reason = VexStreamRepositoryRejection.HUB_NOT_READY,
                        trackRef = trackRef,
                    )
                }

                val target = current.track(trackRef)
                    ?: return@withContext VexStreamRepositoryResult.Rejected(
                        reason = VexStreamRepositoryRejection.TRACK_UNKNOWN,
                        trackRef = trackRef,
                    )

                if (!target.hubAvailable) {
                    return@withContext VexStreamRepositoryResult.Rejected(
                        reason = VexStreamRepositoryRejection.MEDIA_NOT_AVAILABLE_FROM_HUB,
                        trackRef = trackRef,
                    )
                }

                if (target.localCopy is VexStreamLocalCopyState.Downloaded) {
                    return@withContext VexStreamRepositoryResult.Rejected(
                        reason = VexStreamRepositoryRejection.ALREADY_DOWNLOADED,
                        trackRef = trackRef,
                    )
                }

                updateLocalCopy(
                    trackRef = trackRef,
                    localCopy = VexStreamLocalCopyState.Downloading(
                        bytesDownloaded = 0L,
                        totalBytes = target.track.mediaByteLength,
                    ),
                )

                try {
                    val response = request(
                        path = "v1/media/" + encodePathSegment(trackRef.value),
                    )
                    if (response.responseCode != HttpURLConnection.HTTP_OK) {
                        val status = response.responseCode
                        response.disconnect()
                        updateLocalCopy(
                            trackRef,
                            VexStreamLocalCopyState.Failed(
                                reasonCode = "HTTP_" + status,
                                retryable = status >= 500,
                            ),
                        )
                        return@withContext VexStreamRepositoryResult.Rejected(
                            reason = VexStreamRepositoryRejection.MEDIA_NOT_AVAILABLE_FROM_HUB,
                            trackRef = trackRef,
                        )
                    }

                    val stored = response.inputStream.use { input ->
                        mediaStore.store(
                            expectedSha256 = target.track.mediaSha256,
                            expectedByteLength = target.track.mediaByteLength,
                            input = input,
                        )
                    }
                    response.disconnect()

                    when (stored) {
                        is VexStreamMediaStoreResult.Stored -> {
                            updateLocalCopy(trackRef, stored.localCopy)

                            if (!offlineIndex.persist(mutableState.value)) {
                                updateLocalCopy(
                                    trackRef,
                                    VexStreamLocalCopyState.Failed(
                                        reasonCode = "OFFLINE_INDEX_PERSISTENCE_FAILURE",
                                        retryable = true,
                                    ),
                                )
                                return@withContext VexStreamRepositoryResult.Rejected(
                                    reason = VexStreamRepositoryRejection.MEDIA_NOT_AVAILABLE_FROM_HUB,
                                    trackRef = trackRef,
                                )
                            }

                            VexStreamRepositoryResult.Accepted(trackRef)
                        }

                        is VexStreamMediaStoreResult.Rejected -> {
                            updateLocalCopy(
                                trackRef,
                                VexStreamLocalCopyState.Failed(
                                    reasonCode = stored.reason.name,
                                    retryable = stored.reason == VexStreamMediaStoreRejection.IO_FAILURE,
                                ),
                            )
                            VexStreamRepositoryResult.Rejected(
                                reason = VexStreamRepositoryRejection.MEDIA_NOT_AVAILABLE_FROM_HUB,
                                trackRef = trackRef,
                            )
                        }
                    }
                } catch (_: Exception) {
                    updateLocalCopy(
                        trackRef,
                        VexStreamLocalCopyState.Failed(
                            reasonCode = "TRANSPORT_FAILURE",
                            retryable = true,
                        ),
                    )
                    VexStreamRepositoryResult.Rejected(
                        reason = VexStreamRepositoryRejection.MEDIA_NOT_AVAILABLE_FROM_HUB,
                        trackRef = trackRef,
                    )
                }
            }
        }

    override suspend fun removeOfflineCopy(
        trackRef: VexStreamTrackRef,
    ): VexStreamRepositoryResult =
        operationMutex.withLock {
            withContext(Dispatchers.IO) {
                val target = mutableState.value.track(trackRef)
                    ?: return@withContext VexStreamRepositoryResult.Rejected(
                        reason = VexStreamRepositoryRejection.TRACK_UNKNOWN,
                        trackRef = trackRef,
                    )

                val local = target.localCopy as? VexStreamLocalCopyState.Downloaded
                    ?: return@withContext VexStreamRepositoryResult.Rejected(
                        reason = VexStreamRepositoryRejection.NOT_DOWNLOADED,
                        trackRef = trackRef,
                    )

                if (!mediaStore.remove(local)) {
                    return@withContext VexStreamRepositoryResult.Rejected(
                        reason = VexStreamRepositoryRejection.NOT_DOWNLOADED,
                        trackRef = trackRef,
                    )
                }

                updateLocalCopy(
                    trackRef,
                    VexStreamLocalCopyState.NotDownloaded,
                )
                offlineIndex.persist(mutableState.value)
                VexStreamRepositoryResult.Accepted(trackRef)
            }
        }

    fun openLocalCopy(
        trackRef: VexStreamTrackRef,
    ) =
        (mutableState.value.track(trackRef)?.localCopy as? VexStreamLocalCopyState.Downloaded)
            ?.let(mediaStore::open)

    private fun request(
        path: String,
    ): HttpURLConnection =
        (URL(baseUrl.trimEnd('/') + "/" + path.trimStart('/')).openConnection() as HttpURLConnection)
            .apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                useCaches = false
                setRequestProperty("Accept", "application/json, audio/mpeg")
            }

    private fun updateLocalCopy(
        trackRef: VexStreamTrackRef,
        localCopy: VexStreamLocalCopyState,
    ) {
        val current = mutableState.value
        mutableState.value = current.copy(
            tracks = current.tracks.map { state ->
                if (state.track.trackRef == trackRef) {
                    state.copy(localCopy = localCopy)
                } else {
                    state
                }
            },
        )
    }

    private fun parseCatalog(
        body: String,
    ): VexStreamHubCatalogProjection {
        val root = JSONObject(body)
        val tracksJson = root.getJSONArray("tracks")
        val tracks = buildList {
            repeat(tracksJson.length()) { index ->
                val track = tracksJson.getJSONObject(index)
                add(
                    VexStreamHubTrackProjection(
                        trackRef = VexStreamTrackRef(track.getString("trackRef")),
                        mediaSha256 = VexStreamMediaSha256(track.getString("mediaSha256")),
                        mediaByteLength = track.getLong("mediaByteLength"),
                        title = track.getString("title"),
                        artist = track.optString("artist"),
                        album = track.optString("album"),
                        genre = track.optString("genre"),
                        year = track.optString("year"),
                        durationSeconds = track.optDouble("durationSeconds", 0.0),
                        playbackStartSeconds = track.optDouble("playbackStartSeconds", 0.0),
                        playbackEndSeconds =
                            if (track.has("playbackEndSeconds") && !track.isNull("playbackEndSeconds")) {
                                track.getDouble("playbackEndSeconds")
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
            hubRef = root.getString("hubRef"),
            sourceVersion = root.getString("sourceVersion"),
            catalogRevision = root.getString("catalogRevision"),
            tracks = tracks,
            playlists = playlists,
        )
    }

    private fun encodePathSegment(value: String): String =
        java.net.URLEncoder.encode(value, Charsets.UTF_8.name())
            .replace("+", "%20")

    companion object {
        const val DEFAULT_BASE_URL = "http://127.0.0.1:18765"
        private const val CONNECT_TIMEOUT_MS = 2_000
        private const val READ_TIMEOUT_MS = 30_000

        fun production(contextFilesDir: File): LoopbackVexStreamMusicRepository {
            val mediaStore =
                VexStreamMediaStore.production(
                    contextFilesDir = contextFilesDir,
                )

            return LoopbackVexStreamMusicRepository(
                mediaStore = mediaStore,
                offlineIndex = VexStreamOfflineIndex.production(
                    contextFilesDir = contextFilesDir,
                    mediaStore = mediaStore,
                ),
            )
        }
    }
}

// [VXG RealForever]
