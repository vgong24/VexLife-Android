package com.vextreme.vexlife.android.data.vexstream

import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VexStreamMediaStoreTest {

    @Test
    fun exactBytesBecomeVerifiedLocalCopy() {
        withStore { store ->
            val bytes = "take VexStream on the go".toByteArray()
            val digest = bytes.sha256()

            val result = store.store(
                expectedSha256 = digest,
                expectedByteLength = bytes.size.toLong(),
                input = ByteArrayInputStream(bytes),
            )

            val stored = result as VexStreamMediaStoreResult.Stored
            assertTrue(store.verify(stored.localCopy))

            store.open(stored.localCopy).use { input ->
                assertEquals(
                    bytes.toList(),
                    input.readBytes().toList(),
                )
            }
        }
    }

    @Test
    fun wrongDigestNeverBecomesDownloaded() {
        withStore { store ->
            val bytes = "actual bytes".toByteArray()
            val wrongDigest = "different bytes".toByteArray().sha256()

            assertEquals(
                VexStreamMediaStoreResult.Rejected(
                    VexStreamMediaStoreRejection.DIGEST_MISMATCH,
                ),
                store.store(
                    expectedSha256 = wrongDigest,
                    expectedByteLength = bytes.size.toLong(),
                    input = ByteArrayInputStream(bytes),
                ),
            )
        }
    }

    @Test
    fun wrongLengthNeverBecomesDownloaded() {
        withStore { store ->
            val bytes = "actual bytes".toByteArray()

            assertEquals(
                VexStreamMediaStoreResult.Rejected(
                    VexStreamMediaStoreRejection.LENGTH_MISMATCH,
                ),
                store.store(
                    expectedSha256 = bytes.sha256(),
                    expectedByteLength = bytes.size.toLong() + 1L,
                    input = ByteArrayInputStream(bytes),
                ),
            )
        }
    }

    @Test
    fun corruptExistingCopyIsReplacedByFreshVerifiedBytes() {
        withStore { store ->
            val wanted = "fresh verified bytes".toByteArray()
            val digest = wanted.sha256()

            val first = store.store(
                expectedSha256 = digest,
                expectedByteLength = wanted.size.toLong(),
                input = ByteArrayInputStream(wanted),
            ) as VexStreamMediaStoreResult.Stored

            val playbackFile = store.fileForPlayback(first.localCopy)
                ?: error("verified copy missing")
            playbackFile.writeBytes(
                ByteArray(wanted.size) { 0 },
            )
            assertFalse(store.verify(first.localCopy))

            val repaired = store.store(
                expectedSha256 = digest,
                expectedByteLength = wanted.size.toLong(),
                input = ByteArrayInputStream(wanted),
            ) as VexStreamMediaStoreResult.Stored

            assertEquals(first.localCopy, repaired.localCopy)
            assertTrue(store.verify(repaired.localCopy))
        }
    }

    @Test
    fun sameVerifiedBytesAreIdempotentAndRemovable() {
        withStore { store ->
            val bytes = "same bytes".toByteArray()
            val digest = bytes.sha256()

            val first = store.store(
                expectedSha256 = digest,
                expectedByteLength = bytes.size.toLong(),
                input = ByteArrayInputStream(bytes),
            ) as VexStreamMediaStoreResult.Stored

            val second = store.store(
                expectedSha256 = digest,
                expectedByteLength = bytes.size.toLong(),
                input = ByteArrayInputStream("ignored".toByteArray()),
            )

            assertEquals(first, second)
            assertTrue(store.remove(first.localCopy))
            assertFalse(store.verify(first.localCopy))
        }
    }

    private fun withStore(
        block: (VexStreamMediaStore) -> Unit,
    ) {
        val root = Files.createTempDirectory("vexstream-media-store-test").toFile()
        try {
            block(VexStreamMediaStore(root))
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
