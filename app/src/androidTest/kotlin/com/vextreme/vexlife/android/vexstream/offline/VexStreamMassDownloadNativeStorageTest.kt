package com.vextreme.vexlife.android.vexstream.offline

import androidx.test.platform.app.InstrumentationRegistry
import com.vextreme.vexlife.android.data.vexstream.LoopbackVexStreamMusicRepository
import com.vextreme.vexlife.android.data.vexstream.VexStreamLocalCopyState
import com.vextreme.vexlife.android.data.vexstream.VexStreamMediaSha256
import com.vextreme.vexlife.android.data.vexstream.VexStreamMediaStore
import com.vextreme.vexlife.android.data.vexstream.VexStreamOfflineIndex
import com.vextreme.vexlife.android.data.vexstream.VexStreamRepositoryResult
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackRef
import java.io.File
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.net.URLDecoder
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Native Android proof for the MassDownload storage boundary.
 *
 * This test deliberately does not claim Desktop pairing/network EndToEnd. It isolates the question
 * that remained after the C02 physical-device Journey and the established single-track
 * Desktop<->Android lived proof:
 *
 * can one real Android repository execute Download missing across several exact media targets,
 * persist verified files/index truth, restore that truth, and remove every test-owned byte again?
 *
 * The proof root lives under an isolated app-private subdirectory and is deleted in finally even when
 * an assertion fails. Ordinary VexLife pairing/download state is never used or cleared.
 */
class VexStreamMassDownloadNativeStorageTest {
    @Test
    fun downloadMissingWritesRestoresAndCleansOnlyTestOwnedMedia() =
        runBlocking {
            val context =
                InstrumentationRegistry
                    .getInstrumentation()
                    .targetContext
            val proofRoot =
                File(
                    context.filesDir,
                    "test-proof/vexstream-massdownload-native/" +
                        System.nanoTime(),
                )
            val fixtures =
                listOf(
                    Fixture(
                        ref =
                            VexStreamTrackRef(
                                "mass-download.native.one",
                            ),
                        bytes =
                            "native mass download one".encodeToByteArray(),
                    ),
                    Fixture(
                        ref =
                            VexStreamTrackRef(
                                "mass-download.native.two",
                            ),
                        bytes =
                            "native mass download two".encodeToByteArray(),
                    ),
                    Fixture(
                        ref =
                            VexStreamTrackRef(
                                "mass-download.native.three",
                            ),
                        bytes =
                            "native mass download three".encodeToByteArray(),
                    ),
                )

            FixtureServer(
                fixtures = fixtures,
            ).use { server ->
                try {
                    val mediaStore =
                        VexStreamMediaStore.production(
                            contextFilesDir = proofRoot,
                        )
                    val offlineIndex =
                        VexStreamOfflineIndex.production(
                            contextFilesDir = proofRoot,
                            mediaStore = mediaStore,
                        )
                    val repository =
                        LoopbackVexStreamMusicRepository(
                            mediaStore = mediaStore,
                            offlineIndex = offlineIndex,
                            baseUrl = server.baseUrl,
                        )

                    assertEquals(
                        VexStreamRepositoryResult.Accepted(),
                        repository.refreshCatalog(),
                    )

                    val first =
                        fixtures.first()
                    assertEquals(
                        VexStreamRepositoryResult.Accepted(
                            first.ref,
                        ),
                        repository.downloadForOffline(
                            trackRef = first.ref,
                            expectedMediaSha256 =
                                first.sha256,
                            expectedMediaByteLength =
                                first.bytes.size.toLong(),
                        ),
                    )
                    assertDownloadedBytes(
                        repository = repository,
                        fixture = first,
                    )

                    server.expandCatalog()

                    assertEquals(
                        VexStreamRepositoryResult.Accepted(),
                        repository.refreshCatalog(),
                    )

                    val before =
                        vexStreamOfflineCollectionPlan(
                            repository.state.value.tracks,
                        )
                    assertEquals(
                        3,
                        before.totalCount,
                    )
                    assertEquals(
                        1,
                        before.onDeviceCount,
                    )
                    assertEquals(
                        fixtures
                            .drop(1)
                            .map(Fixture::ref),
                        before.targets.map {
                            it.trackRef
                        },
                    )

                    val request =
                        requireNotNull(
                            before.requestOrNull(),
                        )
                    VexStreamOfflineCopyConnection(
                        repository = repository,
                        scope = this,
                    ).request(
                        request,
                    ).join()

                    val after =
                        vexStreamOfflineCollectionPlan(
                            repository.state.value.tracks,
                        )
                    assertTrue(
                        after.fullyOnDevice,
                    )
                    assertEquals(
                        3,
                        after.onDeviceCount,
                    )
                    assertEquals(
                        0,
                        after.downloadableMissingCount,
                    )

                    fixtures.forEach { fixture ->
                        assertDownloadedBytes(
                            repository = repository,
                            fixture = fixture,
                        )
                        assertEquals(
                            1,
                            server.mediaRequestCount(
                                fixture.ref,
                            ),
                        )
                    }

                    val restoredMediaStore =
                        VexStreamMediaStore.production(
                            contextFilesDir = proofRoot,
                        )
                    val restored =
                        LoopbackVexStreamMusicRepository(
                            mediaStore =
                                restoredMediaStore,
                            offlineIndex =
                                VexStreamOfflineIndex.production(
                                    contextFilesDir = proofRoot,
                                    mediaStore = restoredMediaStore,
                                ),
                            baseUrl = server.baseUrl,
                        )

                    assertEquals(
                        3,
                        restored.state.value.tracks.size,
                    )
                    fixtures.forEach { fixture ->
                        val state =
                            requireNotNull(
                                restored.state.value.track(
                                    fixture.ref,
                                ),
                            )
                        assertTrue(
                            state.localCopy is
                                VexStreamLocalCopyState.Downloaded,
                        )
                        assertDownloadedBytes(
                            repository = restored,
                            fixture = fixture,
                        )
                    }

                    fixtures.forEach { fixture ->
                        assertEquals(
                            VexStreamRepositoryResult.Accepted(
                                fixture.ref,
                            ),
                            restored.removeOfflineCopy(
                                fixture.ref,
                            ),
                        )
                    }

                    assertTrue(
                        restored.state.value.tracks.all { state ->
                            state.localCopy ===
                                VexStreamLocalCopyState.NotDownloaded
                        },
                    )

                    val afterRemovalStore =
                        VexStreamMediaStore.production(
                            contextFilesDir = proofRoot,
                        )
                    val afterRemoval =
                        LoopbackVexStreamMusicRepository(
                            mediaStore = afterRemovalStore,
                            offlineIndex =
                                VexStreamOfflineIndex.production(
                                    contextFilesDir = proofRoot,
                                    mediaStore = afterRemovalStore,
                                ),
                            baseUrl = server.baseUrl,
                        )
                    assertTrue(
                        afterRemoval.state.value.tracks.isEmpty(),
                    )

                    val mediaDirectory =
                        File(
                            proofRoot,
                            "vexstream/media",
                        )
                    assertFalse(
                        mediaDirectory
                            .listFiles()
                            .orEmpty()
                            .any {
                                it.isFile
                            },
                    )
                } finally {
                    proofRoot.deleteRecursively()
                }
            }

            assertFalse(
                "MassDownload native proof must not leave test-owned media/index files on the device",
                proofRoot.exists(),
            )
        }

    private fun assertDownloadedBytes(
        repository: LoopbackVexStreamMusicRepository,
        fixture: Fixture,
    ) {
        val state =
            requireNotNull(
                repository.state.value.track(
                    fixture.ref,
                ),
            )
        val local =
            state.localCopy as?
                VexStreamLocalCopyState.Downloaded
        assertNotNull(
            "expected downloaded state for ${fixture.ref.value}",
            local,
        )
        repository
            .openLocalCopy(
                fixture.ref,
            )
            ?.use { input ->
                assertEquals(
                    fixture.bytes.toList(),
                    input.readBytes().toList(),
                )
            }
            ?: error(
                "verified local media missing for ${fixture.ref.value}",
            )
    }

    private data class Fixture(
        val ref: VexStreamTrackRef,
        val bytes: ByteArray,
    ) {
        val sha256: VexStreamMediaSha256 =
            VexStreamMediaSha256(
                MessageDigest
                    .getInstance(
                        "SHA-256",
                    )
                    .digest(
                        bytes,
                    )
                    .joinToString(
                        separator = "",
                    ) { byte ->
                        "%02x".format(
                            byte,
                        )
                    },
            )
    }

    private class FixtureServer(
        private val fixtures: List<Fixture>,
    ) : AutoCloseable {
        private val server =
            ServerSocket(
                0,
                8,
                InetAddress.getByName(
                    "127.0.0.1",
                ),
            )
        private val requests =
            ConcurrentHashMap<
                VexStreamTrackRef,
                AtomicInteger
            >()

        @Volatile
        private var expanded = false

        private val worker =
            thread(
                start = true,
                isDaemon = true,
                name =
                    "vexstream-massdownload-native-fixture",
            ) {
                serve()
            }

        val baseUrl: String =
            "http://127.0.0.1:${server.localPort}"

        fun expandCatalog() {
            expanded = true
        }

        fun mediaRequestCount(
            ref: VexStreamTrackRef,
        ): Int =
            requests[ref]
                ?.get()
                ?: 0

        private fun serve() {
            while (
                !server.isClosed
            ) {
                try {
                    server
                        .accept()
                        .use(
                            ::respond,
                        )
                } catch (
                    failure: SocketException
                ) {
                    if (
                        !server.isClosed
                    ) {
                        throw failure
                    }
                }
            }
        }

        private fun respond(
            socket: Socket,
        ) {
            val reader =
                socket
                    .getInputStream()
                    .bufferedReader()
            val requestLine =
                reader.readLine()
                    ?: return

            while (
                true
            ) {
                val line =
                    reader.readLine()
                        ?: break
                if (
                    line.isEmpty()
                ) {
                    break
                }
            }

            val path =
                requestLine
                    .split(
                        " ",
                    )
                    .getOrNull(
                        1,
                    )
                    ?: "/"

            when {
                path ==
                    "/v1/catalog" ->
                    writeResponse(
                        socket = socket,
                        status =
                            "200 OK",
                        contentType =
                            "application/json",
                        body =
                            catalogJson()
                                .encodeToByteArray(),
                    )

                path.startsWith(
                    "/v1/media/",
                ) -> {
                    val ref =
                        VexStreamTrackRef(
                            URLDecoder.decode(
                                path.removePrefix(
                                    "/v1/media/",
                                ),
                                Charsets.UTF_8.name(),
                            ),
                        )
                    val fixture =
                        fixtures.firstOrNull {
                            it.ref == ref
                        }
                    if (
                        fixture == null
                    ) {
                        writeResponse(
                            socket = socket,
                            status =
                                "404 Not Found",
                            contentType =
                                "text/plain",
                            body =
                                "missing"
                                    .encodeToByteArray(),
                        )
                    } else {
                        requests
                            .computeIfAbsent(
                                ref,
                            ) {
                                AtomicInteger()
                            }
                            .incrementAndGet()
                        writeResponse(
                            socket = socket,
                            status =
                                "200 OK",
                            contentType =
                                "audio/mpeg",
                            body =
                                fixture.bytes,
                        )
                    }
                }

                else ->
                    writeResponse(
                        socket = socket,
                        status =
                            "404 Not Found",
                        contentType =
                            "text/plain",
                        body =
                            "missing"
                                .encodeToByteArray(),
                    )
            }
        }

        private fun catalogJson(): String {
            val visible =
                if (
                    expanded
                ) {
                    fixtures
                } else {
                    fixtures.take(
                        1,
                    )
                }

            return JSONObject()
                .put(
                    "hubRef",
                    "hub.mass-download.native-proof",
                )
                .put(
                    "sourceVersion",
                    "mass-download-native-proof",
                )
                .put(
                    "catalogRevision",
                    if (
                        expanded
                    ) {
                        "catalog.mass-download.native.2"
                    } else {
                        "catalog.mass-download.native.1"
                    },
                )
                .put(
                    "tracks",
                    JSONArray().apply {
                        visible.forEachIndexed {
                                index,
                                fixture,
                            ->
                            put(
                                JSONObject()
                                    .put(
                                        "trackRef",
                                        fixture.ref.value,
                                    )
                                    .put(
                                        "mediaSha256",
                                        fixture.sha256.value,
                                    )
                                    .put(
                                        "mediaByteLength",
                                        fixture.bytes.size,
                                    )
                                    .put(
                                        "title",
                                        "Native MassDownload ${index + 1}",
                                    )
                                    .put(
                                        "artist",
                                        "Vex",
                                    )
                                    .put(
                                        "album",
                                        "Native MassDownload",
                                    ),
                            )
                        }
                    },
                )
                .put(
                    "playlists",
                    JSONArray(),
                )
                .toString()
        }

        private fun writeResponse(
            socket: Socket,
            status: String,
            contentType: String,
            body: ByteArray,
        ) {
            socket
                .getOutputStream()
                .apply {
                    write(
                        buildString {
                            append(
                                "HTTP/1.1 ",
                            )
                            append(
                                status,
                            )
                            append(
                                "\r\n",
                            )
                            append(
                                "Content-Type: ",
                            )
                            append(
                                contentType,
                            )
                            append(
                                "\r\n",
                            )
                            append(
                                "Content-Length: ",
                            )
                            append(
                                body.size,
                            )
                            append(
                                "\r\n",
                            )
                            append(
                                "Connection: close\r\n\r\n",
                            )
                        }.toByteArray(
                            Charsets.US_ASCII,
                        ),
                    )
                    write(
                        body,
                    )
                    flush()
                }
        }

        override fun close() {
            server.close()
            worker.join(
                2_000,
            )
        }
    }
}

// [VXG RealForever]
