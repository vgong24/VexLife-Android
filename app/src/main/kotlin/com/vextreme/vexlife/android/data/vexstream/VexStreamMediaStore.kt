package com.vextreme.vexlife.android.data.vexstream

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest

enum class VexStreamMediaStoreRejection {
    LENGTH_MISMATCH,
    DIGEST_MISMATCH,
    EXISTING_COPY_INVALID,
    IO_FAILURE,
}

sealed interface VexStreamMediaStoreResult {
    data class Stored(
        val localCopy: VexStreamLocalCopyState.Downloaded,
    ) : VexStreamMediaStoreResult

    data class Rejected(
        val reason: VexStreamMediaStoreRejection,
    ) : VexStreamMediaStoreResult
}

/**
 * Verified media-byte owner for phone-local VexStream copies.
 *
 * Production should root this store under Android app-private storage. The class itself receives a
 * [File] so byte/integrity behavior remains ordinary JVM-testable and does not require broad Android
 * shared-storage permission.
 *
 * A copy becomes visible only after:
 * - exact expected byte count;
 * - exact expected SHA-256;
 * - output flush/sync;
 * - rename from a temporary file to the digest-addressed final file.
 *
 * The store never exposes its filesystem path as product identity. Higher layers receive an opaque
 * localMediaRef that can be resolved only by this owner.
 */
class VexStreamMediaStore(
    private val root: File,
) {
    fun store(
        expectedSha256: VexStreamMediaSha256,
        expectedByteLength: Long,
        input: InputStream,
    ): VexStreamMediaStoreResult {
        require(expectedByteLength > 0L) {
            "expectedByteLength must be positive"
        }

        return try {
            ensureRoot()

            val finalFile = finalFile(expectedSha256)
            if (finalFile.exists()) {
                if (
                    verifyFile(
                        file = finalFile,
                        expectedSha256 = expectedSha256,
                        expectedByteLength = expectedByteLength,
                    )
                ) {
                    return VexStreamMediaStoreResult.Stored(
                        downloadedState(
                            expectedSha256 = expectedSha256,
                            expectedByteLength = expectedByteLength,
                        ),
                    )
                }

                if (!finalFile.delete()) {
                    return VexStreamMediaStoreResult.Rejected(
                        VexStreamMediaStoreRejection.EXISTING_COPY_INVALID,
                    )
                }
            }

            val tempFile = File.createTempFile(
                ".vexstream-",
                ".part",
                root,
            )

            try {
                val digest = MessageDigest.getInstance("SHA-256")
                var byteCount = 0L

                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) {
                            break
                        }

                        byteCount += read
                        if (byteCount > expectedByteLength) {
                            return VexStreamMediaStoreResult.Rejected(
                                VexStreamMediaStoreRejection.LENGTH_MISMATCH,
                            )
                        }

                        digest.update(buffer, 0, read)
                        output.write(buffer, 0, read)
                    }

                    output.flush()
                    output.fd.sync()
                }

                if (byteCount != expectedByteLength) {
                    return VexStreamMediaStoreResult.Rejected(
                        VexStreamMediaStoreRejection.LENGTH_MISMATCH,
                    )
                }

                val actualDigest = digest.digest().toHex()
                if (actualDigest != expectedSha256.value) {
                    return VexStreamMediaStoreResult.Rejected(
                        VexStreamMediaStoreRejection.DIGEST_MISMATCH,
                    )
                }

                if (!tempFile.renameTo(finalFile)) {
                    return VexStreamMediaStoreResult.Rejected(
                        VexStreamMediaStoreRejection.IO_FAILURE,
                    )
                }

                VexStreamMediaStoreResult.Stored(
                    downloadedState(
                        expectedSha256 = expectedSha256,
                        expectedByteLength = expectedByteLength,
                    ),
                )
            } finally {
                if (tempFile.exists()) {
                    tempFile.delete()
                }
            }
        } catch (_: Exception) {
            VexStreamMediaStoreResult.Rejected(
                VexStreamMediaStoreRejection.IO_FAILURE,
            )
        }
    }

    fun verify(
        localCopy: VexStreamLocalCopyState.Downloaded,
    ): Boolean {
        val mediaRefDigest = digestFromLocalMediaRef(localCopy.localMediaRef)
            ?: return false

        if (mediaRefDigest != localCopy.mediaSha256) {
            return false
        }

        return try {
            verifyFile(
                file = finalFile(localCopy.mediaSha256),
                expectedSha256 = localCopy.mediaSha256,
                expectedByteLength = localCopy.byteLength,
            )
        } catch (_: Exception) {
            false
        }
    }

    fun open(
        localCopy: VexStreamLocalCopyState.Downloaded,
    ): InputStream {
        check(verify(localCopy)) {
            "VexStream local media copy is missing or failed verification"
        }

        return FileInputStream(
            finalFile(localCopy.mediaSha256),
        )
    }

    /**
     * Playback-only filesystem resolution after re-verifying the retained local copy.
     *
     * The path remains an implementation detail and is never stored in VexStream product state.
     */
    fun fileForPlayback(
        localCopy: VexStreamLocalCopyState.Downloaded,
    ): File? =
        if (verify(localCopy)) {
            finalFile(localCopy.mediaSha256)
        } else {
            null
        }

    fun remove(
        localCopy: VexStreamLocalCopyState.Downloaded,
    ): Boolean {
        val digest = digestFromLocalMediaRef(localCopy.localMediaRef)
            ?: return false

        if (digest != localCopy.mediaSha256) {
            return false
        }

        val file = finalFile(localCopy.mediaSha256)
        return !file.exists() || file.delete()
    }

    private fun ensureRoot() {
        if (root.exists()) {
            require(root.isDirectory) {
                "VexStream media root must be a directory"
            }
            return
        }

        check(root.mkdirs()) {
            "Unable to create VexStream media root"
        }
    }

    private fun verifyFile(
        file: File,
        expectedSha256: VexStreamMediaSha256,
        expectedByteLength: Long,
    ): Boolean {
        if (!file.isFile || file.length() != expectedByteLength) {
            return false
        }

        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) {
                    break
                }
                digest.update(buffer, 0, read)
            }
        }

        return digest.digest().toHex() == expectedSha256.value
    }

    private fun downloadedState(
        expectedSha256: VexStreamMediaSha256,
        expectedByteLength: Long,
    ): VexStreamLocalCopyState.Downloaded =
        VexStreamLocalCopyState.Downloaded(
            mediaSha256 = expectedSha256,
            byteLength = expectedByteLength,
            localMediaRef = localMediaRef(expectedSha256),
        )

    private fun finalFile(
        digest: VexStreamMediaSha256,
    ): File =
        File(root, digest.value + MEDIA_EXTENSION)

    private fun localMediaRef(
        digest: VexStreamMediaSha256,
    ): String =
        LOCAL_MEDIA_REF_PREFIX + digest.value

    private fun digestFromLocalMediaRef(
        ref: String,
    ): VexStreamMediaSha256? {
        if (!ref.startsWith(LOCAL_MEDIA_REF_PREFIX)) {
            return null
        }

        return runCatching {
            VexStreamMediaSha256(
                ref.removePrefix(LOCAL_MEDIA_REF_PREFIX),
            )
        }.getOrNull()
    }

    private fun ByteArray.toHex(): String =
        joinToString(separator = "") { byte ->
            "%02x".format(byte)
        }

    companion object {
        private const val MEDIA_EXTENSION = ".media"
        private const val LOCAL_MEDIA_REF_PREFIX = "vexstream.local.sha256."

        fun production(
            contextFilesDir: File,
        ): VexStreamMediaStore =
            VexStreamMediaStore(
                File(contextFilesDir, "vexstream/media"),
            )
    }
}

// [VXG RealForever]
