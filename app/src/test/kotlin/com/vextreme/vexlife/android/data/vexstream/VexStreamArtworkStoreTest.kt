package com.vextreme.vexlife.android.data.vexstream

import java.io.ByteArrayInputStream
import java.nio.file.Files
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VexStreamArtworkStoreTest {

    @Test
    fun artworkPersistsByExactSourceUrl() {
        withStore { store ->
            val track =
                track(
                    digest =
                        "a".repeat(
                            64,
                        ),
                    artworkUrl =
                        "https://images.example.test/album-a.jpg",
                )
            val bytes =
                "offline artwork".toByteArray()

            val stored =
                store.store(
                    track = track,
                    input =
                        ByteArrayInputStream(
                            bytes,
                        ),
                )

            assertEquals(
                stored,
                store.fileFor(
                    track,
                ),
            )
            assertArrayEquals(
                bytes,
                requireNotNull(
                    store.fileFor(
                        track,
                    ),
                ).readBytes(),
            )

            assertNull(
                store.fileFor(
                    track.copy(
                        artworkUrl =
                            "https://images.example.test/album-b.jpg",
                    ),
                ),
            )
        }
    }

    @Test
    fun persistedArtworkCanBeResolvedDirectlyFromItsAdmittedSourceUrl() {
        withStore { store ->
            val artworkUrl =
                "https://images.example.test/widget-cover.jpg"
            val track =
                track(
                    digest =
                        "d".repeat(
                            64,
                        ),
                    artworkUrl =
                        artworkUrl,
                )
            val stored =
                requireNotNull(
                    store.store(
                        track,
                        ByteArrayInputStream(
                            byteArrayOf(
                                7,
                                8,
                                9,
                            ),
                        ),
                    ),
                )

            assertEquals(
                stored,
                store.fileForArtworkUrl(
                    artworkUrl,
                ),
            )
        }
    }

    @Test
    fun sameArtworkSourceIsSharedAcrossDifferentDownloadedMedia() {
        withStore { store ->
            val artworkUrl =
                "https://images.example.test/shared-album.jpg"
            val first =
                track(
                    digest =
                        "a".repeat(
                            64,
                        ),
                    artworkUrl =
                        artworkUrl,
                )
            val second =
                track(
                    digest =
                        "b".repeat(
                            64,
                        ),
                    artworkUrl =
                        artworkUrl,
                )
            val firstFile =
                requireNotNull(
                    store.store(
                        first,
                        ByteArrayInputStream(
                            byteArrayOf(
                                1,
                                2,
                                3,
                            ),
                        ),
                    ),
                )

            assertEquals(
                firstFile,
                store.fileFor(
                    second,
                ),
            )
            assertEquals(
                firstFile,
                store.store(
                    second,
                    ByteArrayInputStream(
                        byteArrayOf(
                            9,
                            9,
                            9,
                        ),
                    ),
                ),
            )
            assertArrayEquals(
                byteArrayOf(
                    1,
                    2,
                    3,
                ),
                firstFile.readBytes(),
            )
        }
    }

    @Test
    fun oversizedArtworkFailsClosedWithoutAUsablePartialFile() {
        val root =
            Files.createTempDirectory(
                "vexstream-artwork-bounded-test",
            ).toFile()
        try {
            val store =
                VexStreamArtworkStore(
                    root = root,
                    maxBytes = 8L,
                )
            val track =
                track(
                    digest =
                        "a".repeat(
                            64,
                        ),
                    artworkUrl =
                        "https://images.example.test/large.jpg",
                )

            assertNull(
                store.store(
                    track = track,
                    input =
                        ByteArrayInputStream(
                            ByteArray(
                                9,
                            ) {
                                1
                            },
                        ),
                ),
            )
            assertNull(
                store.fileFor(
                    track,
                ),
            )
            assertTrue(
                root.listFiles()
                    .orEmpty()
                    .none {
                        it.name.endsWith(
                            ".tmp",
                        )
                    },
            )
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun pruneKeepsSharedReferencedArtworkAndRemovesUnreferencedArtwork() {
        withStore { store ->
            val sharedUrl =
                "https://images.example.test/shared.jpg"
            val keptFirst =
                track(
                    digest =
                        "a".repeat(
                            64,
                        ),
                    artworkUrl =
                        sharedUrl,
                )
            val keptSecond =
                track(
                    digest =
                        "b".repeat(
                            64,
                        ),
                    artworkUrl =
                        sharedUrl,
                )
            val removed =
                track(
                    digest =
                        "c".repeat(
                            64,
                        ),
                    artworkUrl =
                        "https://images.example.test/removed.jpg",
                )

            assertTrue(
                store.store(
                    keptFirst,
                    ByteArrayInputStream(
                        byteArrayOf(
                            1,
                            2,
                            3,
                        ),
                    ),
                ) != null,
            )
            assertTrue(
                store.store(
                    removed,
                    ByteArrayInputStream(
                        byteArrayOf(
                            4,
                            5,
                            6,
                        ),
                    ),
                ) != null,
            )

            assertTrue(
                store.pruneTo(
                    listOf(
                        keptSecond,
                    ),
                ),
            )
            assertTrue(
                store.fileFor(
                    keptFirst,
                ) != null,
            )
            assertTrue(
                store.fileFor(
                    keptSecond,
                ) != null,
            )
            assertNull(
                store.fileFor(
                    removed,
                ),
            )
        }
    }


    @Test
    fun embeddedArtworkPersistsByOpaqueRefAndResolvesFromOwnedFileUri() {
        withStore { store ->
            val track =
                track(
                    digest =
                        "e".repeat(
                            64,
                        ),
                    artworkUrl = null,
                    artworkRef =
                        "track.embedded.cover",
                )
            val bytes =
                byteArrayOf(
                    11,
                    12,
                    13,
                )

            val stored =
                requireNotNull(
                    store.store(
                        track = track,
                        input =
                            ByteArrayInputStream(
                                bytes,
                            ),
                    ),
                )

            assertEquals(
                stored,
                store.fileFor(
                    track,
                ),
            )
            assertEquals(
                stored,
                store.fileForArtworkRef(
                    "track.embedded.cover",
                ),
            )
            assertEquals(
                stored,
                store.fileForPlaybackArtworkUri(
                    stored.toURI()
                        .toString(),
                ),
            )
            assertArrayEquals(
                bytes,
                stored.readBytes(),
            )

            val outside =
                Files.createTempFile(
                    "vexstream-artwork-outside",
                    ".artwork",
                ).toFile()
            try {
                outside.writeBytes(
                    byteArrayOf(
                        1,
                    ),
                )
                assertNull(
                    store.fileForPlaybackArtworkUri(
                        outside.toURI()
                            .toString(),
                    ),
                )
            } finally {
                outside.delete()
            }
        }
    }

    @Test
    fun externalAndEmbeddedArtworkUseIndependentCacheKeys() {
        withStore { store ->
            val external =
                track(
                    digest =
                        "f".repeat(
                            64,
                        ),
                    artworkUrl =
                        "https://images.example.test/same-label.jpg",
                )
            val embedded =
                track(
                    digest =
                        "f".repeat(
                            64,
                        ),
                    artworkUrl = null,
                    artworkRef =
                        "https://images.example.test/same-label.jpg",
                )

            val externalFile =
                requireNotNull(
                    store.store(
                        external,
                        ByteArrayInputStream(
                            byteArrayOf(
                                1,
                            ),
                        ),
                    ),
                )
            val embeddedFile =
                requireNotNull(
                    store.store(
                        embedded,
                        ByteArrayInputStream(
                            byteArrayOf(
                                2,
                            ),
                        ),
                    ),
                )

            assertNotEquals(
                externalFile,
                embeddedFile,
            )
        }
    }

    private fun withStore(
        block: (
            VexStreamArtworkStore,
        ) -> Unit,
    ) {
        val root =
            Files.createTempDirectory(
                "vexstream-artwork-store-test",
            ).toFile()
        try {
            block(
                VexStreamArtworkStore(
                    root,
                ),
            )
        } finally {
            root.deleteRecursively()
        }
    }

    private fun track(
        digest: String,
        artworkUrl: String? = null,
        artworkRef: String? = null,
    ): VexStreamTrack =
        VexStreamTrack(
            trackRef =
                VexStreamTrackRef(
                    "track.artwork." +
                        digest.take(
                            8,
                        ),
                ),
            mediaSha256 =
                VexStreamMediaSha256(
                    digest,
                ),
            mediaByteLength =
                4_096L,
            title =
                "Artwork",
            artist =
                "Vex",
            artworkUrl =
                artworkUrl,
            artworkRef =
                artworkRef,
        )
}

// [VXG RealForever]
