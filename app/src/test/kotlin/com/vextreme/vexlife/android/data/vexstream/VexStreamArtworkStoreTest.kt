package com.vextreme.vexlife.android.data.vexstream

import java.io.ByteArrayInputStream
import java.nio.file.Files
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VexStreamArtworkStoreTest {

    @Test
    fun artworkPersistsByMediaAndExactSourceUrl() {
        withStore { store ->
            val track =
                track(
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
    fun removingMediaArtworkCleansEveryArtworkRevisionForThatMedia() {
        withStore { store ->
            val first =
                track(
                    artworkUrl =
                        "https://images.example.test/first.jpg",
                )
            val second =
                first.copy(
                    artworkUrl =
                        "https://images.example.test/second.jpg",
                )

            assertTrue(
                store.store(
                    first,
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
                    second,
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
                store.remove(
                    first.mediaSha256,
                ),
            )
            assertNull(
                store.fileFor(
                    first,
                ),
            )
            assertNull(
                store.fileFor(
                    second,
                ),
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
        artworkUrl: String,
    ): VexStreamTrack =
        VexStreamTrack(
            trackRef =
                VexStreamTrackRef(
                    "track.artwork",
                ),
            mediaSha256 =
                VexStreamMediaSha256(
                    "a".repeat(
                        64,
                    ),
                ),
            mediaByteLength =
                4_096L,
            title =
                "Artwork",
            artist =
                "Vex",
            artworkUrl =
                artworkUrl,
        )
}

// [VXG RealForever]
