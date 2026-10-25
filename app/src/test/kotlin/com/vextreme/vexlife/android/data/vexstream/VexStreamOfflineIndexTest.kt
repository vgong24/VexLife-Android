package com.vextreme.vexlife.android.data.vexstream

import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VexStreamOfflineIndexTest {

    @Test
    fun verifiedDownloadRestoresAsLocalOnlyAfterRestart() {
        withEnvironment { mediaStore, index ->
            val bytes = "persistent VexStream media".toByteArray()
            val digest = bytes.sha256()
            val stored = mediaStore.store(
                expectedSha256 = digest,
                expectedByteLength = bytes.size.toLong(),
                input = ByteArrayInputStream(bytes),
            ) as VexStreamMediaStoreResult.Stored

            val state = VexStreamMusicState(
                hubState = VexStreamHubState.Ready(
                    hubRef = "hub.fixture",
                    catalogRevision = "catalog.1",
                ),
                tracks = listOf(
                    VexStreamTrackState(
                        track = VexStreamTrack(
                            trackRef = VexStreamTrackRef("hub.track.one"),
                            mediaSha256 = digest,
                            mediaByteLength = bytes.size.toLong(),
                            title = "Take It With You",
                            artist = "Vex",
                        ),
                        hubAvailable = true,
                        localCopy = stored.localCopy,
                    ),
                ),
            )

            assertTrue(index.persist(state))

            val restored = index.load()
            assertEquals(VexStreamHubState.NotConfigured, restored.hubState)
            assertEquals(1, restored.tracks.size)
            assertEquals(
                VexStreamTrackAvailability.LOCAL_ONLY,
                restored.tracks.single().availability,
            )
            assertEquals(
                stored.localCopy,
                restored.tracks.single().localCopy,
            )
        }
    }

    @Test
    fun artworkSourceRestoresWithDownloadedTrackMetadata() {
        withEnvironment { mediaStore, index ->
            val bytes =
                "offline media with artwork"
                    .toByteArray()
            val digest =
                bytes.sha256()
            val stored =
                mediaStore.store(
                    expectedSha256 =
                        digest,
                    expectedByteLength =
                        bytes.size.toLong(),
                    input =
                        ByteArrayInputStream(
                            bytes,
                        ),
                ) as VexStreamMediaStoreResult.Stored
            val artworkUrl =
                "https://images.example.test/offline-cover.jpg"
            val state =
                VexStreamMusicState(
                    tracks =
                        listOf(
                            VexStreamTrackState(
                                track =
                                    VexStreamTrack(
                                        trackRef =
                                            VexStreamTrackRef(
                                                "hub.track.artwork",
                                            ),
                                        mediaSha256 =
                                            digest,
                                        mediaByteLength =
                                            bytes.size.toLong(),
                                        title =
                                            "Offline Artwork",
                                        artist =
                                            "Vex",
                                        artworkUrl =
                                            artworkUrl,
                                    ),
                                hubAvailable =
                                    true,
                                localCopy =
                                    stored.localCopy,
                            ),
                        ),
                )

            assertTrue(
                index.persist(
                    state,
                ),
            )

            val restored =
                index.load()
                    .tracks
                    .single()

            assertEquals(
                artworkUrl,
                restored.track
                    .artworkUrl,
            )
            assertEquals(
                VexStreamTrackAvailability.LOCAL_ONLY,
                restored.availability,
            )
        }
    }

    @Test
    fun indexNeverRestoresMissingOrCorruptMediaAsDownloaded() {
        withEnvironment { mediaStore, index ->
            val bytes = "media".toByteArray()
            val digest = bytes.sha256()
            val stored = mediaStore.store(
                expectedSha256 = digest,
                expectedByteLength = bytes.size.toLong(),
                input = ByteArrayInputStream(bytes),
            ) as VexStreamMediaStoreResult.Stored

            val state = VexStreamMusicState(
                tracks = listOf(
                    VexStreamTrackState(
                        track = VexStreamTrack(
                            trackRef = VexStreamTrackRef("hub.track.one"),
                            mediaSha256 = digest,
                            mediaByteLength = bytes.size.toLong(),
                            title = "Missing Later",
                            artist = "Vex",
                        ),
                        hubAvailable = false,
                        localCopy = stored.localCopy,
                    ),
                ),
            )
            assertTrue(index.persist(state))
            assertTrue(mediaStore.remove(stored.localCopy))

            assertTrue(index.load().tracks.isEmpty())
        }
    }

    @Test
    fun legacyV1IndexStillRestoresPreviouslyVerifiedPrivateCopy() {
        val root =
            Files.createTempDirectory(
                "vexstream-offline-index-v1-test",
            ).toFile()
        try {
            val mediaStore =
                VexStreamMediaStore(
                    root.resolve(
                        "media",
                    ),
                )
            val indexFile =
                root.resolve(
                    "offline-index.properties",
                )
            val index =
                VexStreamOfflineIndex(
                    indexFile = indexFile,
                    mediaStore = mediaStore,
                )
            val bytes =
                "legacy offline index media"
                    .toByteArray()
            val digest =
                bytes.sha256()
            val stored =
                mediaStore.store(
                    expectedSha256 = digest,
                    expectedByteLength =
                        bytes.size.toLong(),
                    input =
                        ByteArrayInputStream(
                            bytes,
                        ),
                ) as VexStreamMediaStoreResult.Stored
            val state =
                VexStreamMusicState(
                    tracks =
                        listOf(
                            VexStreamTrackState(
                                track =
                                    VexStreamTrack(
                                        trackRef =
                                            VexStreamTrackRef(
                                                "hub.track.legacy",
                                            ),
                                        mediaSha256 =
                                            digest,
                                        mediaByteLength =
                                            bytes.size.toLong(),
                                        title =
                                            "Legacy Index",
                                        artist = "Vex",
                                    ),
                                hubAvailable = false,
                                localCopy =
                                    stored.localCopy,
                            ),
                        ),
                )

            assertTrue(
                index.persist(
                    state,
                ),
            )
            indexFile.writeText(
                indexFile.readText()
                    .replace(
                        "vexstream.android.offline-index/v3",
                        "vexstream.android.offline-index/v1",
                    ),
            )

            val restored =
                index.load()

            assertEquals(
                1,
                restored.tracks.size,
            )
            assertEquals(
                VexStreamTrackAvailability.LOCAL_ONLY,
                restored.tracks
                    .single()
                    .availability,
            )
            assertTrue(
                indexFile
                    .readText()
                    .contains(
                        "vexstream.android.offline-index/v3",
                    ),
            )
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun legacyV1IndexIsNotRewrittenWhenRestoreIsPartial() {
        val root =
            Files.createTempDirectory(
                "vexstream-offline-index-v1-partial-test",
            ).toFile()
        try {
            val mediaStore =
                VexStreamMediaStore(
                    root.resolve(
                        "media",
                    ),
                )
            val indexFile =
                root.resolve(
                    "offline-index.properties",
                )
            val index =
                VexStreamOfflineIndex(
                    indexFile = indexFile,
                    mediaStore = mediaStore,
                )
            val keptBytes =
                "legacy kept media"
                    .toByteArray()
            val missingBytes =
                "legacy missing media"
                    .toByteArray()
            val keptDigest =
                keptBytes.sha256()
            val missingDigest =
                missingBytes.sha256()
            val kept =
                mediaStore.store(
                    expectedSha256 =
                        keptDigest,
                    expectedByteLength =
                        keptBytes.size.toLong(),
                    input =
                        ByteArrayInputStream(
                            keptBytes,
                        ),
                ) as VexStreamMediaStoreResult.Stored
            val missing =
                mediaStore.store(
                    expectedSha256 =
                        missingDigest,
                    expectedByteLength =
                        missingBytes.size.toLong(),
                    input =
                        ByteArrayInputStream(
                            missingBytes,
                        ),
                ) as VexStreamMediaStoreResult.Stored
            val state =
                VexStreamMusicState(
                    tracks =
                        listOf(
                            VexStreamTrackState(
                                track =
                                    VexStreamTrack(
                                        trackRef =
                                            VexStreamTrackRef(
                                                "hub.track.kept",
                                            ),
                                        mediaSha256 =
                                            keptDigest,
                                        mediaByteLength =
                                            keptBytes.size.toLong(),
                                        title = "Kept",
                                        artist = "Vex",
                                    ),
                                hubAvailable = false,
                                localCopy =
                                    kept.localCopy,
                            ),
                            VexStreamTrackState(
                                track =
                                    VexStreamTrack(
                                        trackRef =
                                            VexStreamTrackRef(
                                                "hub.track.missing",
                                            ),
                                        mediaSha256 =
                                            missingDigest,
                                        mediaByteLength =
                                            missingBytes.size.toLong(),
                                        title = "Missing",
                                        artist = "Vex",
                                    ),
                                hubAvailable = false,
                                localCopy =
                                    missing.localCopy,
                            ),
                        ),
                )

            assertTrue(
                index.persist(
                    state,
                ),
            )
            indexFile.writeText(
                indexFile.readText()
                    .replace(
                        "vexstream.android.offline-index/v3",
                        "vexstream.android.offline-index/v1",
                    ),
            )
            assertTrue(
                mediaStore.remove(
                    missing.localCopy,
                ),
            )

            val restored =
                index.load()

            assertEquals(
                1,
                restored.tracks.size,
            )
            assertEquals(
                keptDigest,
                restored.tracks
                    .single()
                    .track
                    .mediaSha256,
            )
            assertTrue(
                indexFile
                    .readText()
                    .contains(
                        "vexstream.android.offline-index/v1",
                    ),
            )
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun verificationReceiptRejectsInvalidGeneration() {
        var failed = false
        try {
            VexStreamLocalCopyVerificationReceipt(
                mediaStoreVersion = "version",
                generationModified = -1L,
            )
        } catch (_: IllegalArgumentException) {
            failed = true
        }
        assertTrue(failed)
    }

    private fun withEnvironment(
        block: (
            VexStreamMediaStore,
            VexStreamOfflineIndex,
        ) -> Unit,
    ) {
        val root = Files.createTempDirectory("vexstream-offline-index-test").toFile()
        try {
            val mediaStore = VexStreamMediaStore(
                root.resolve("media"),
            )
            val index = VexStreamOfflineIndex(
                indexFile = root.resolve("offline-index.properties"),
                mediaStore = mediaStore,
            )
            block(mediaStore, index)
        } finally {
            root.deleteRecursively()
        }
    }

    private fun ByteArray.sha256(): VexStreamMediaSha256 =
        VexStreamMediaSha256(
            MessageDigest.getInstance("SHA-256")
                .digest(this)
                .joinToString(separator = "") { byte ->
                    "%02x".format(byte)
                },
        )
}

// [VXG RealForever]
