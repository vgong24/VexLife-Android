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
