package com.vextreme.vexlife.android.data.vexstream

import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest

enum class VexStreamMediaStoreRejection {
    LENGTH_MISMATCH,
    DIGEST_MISMATCH,
    EXISTING_COPY_INVALID,
    PERMISSION_REQUIRED,
    SHARED_COPY_CONFLICT,
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
 * Downloads first cross the existing app-private integrity boundary so byte verification remains
 * ordinary JVM-testable and a failed shared-media publication never destroys the verified staging
 * copy. Production then promotes that exact verified file through [VexStreamDeviceMediaStore].
 *
 * A private staging copy becomes eligible for promotion only after:
 * - exact expected byte count;
 * - exact expected SHA-256;
 * - output flush/sync;
 * - rename from a temporary file to the digest-addressed private file.
 *
 * The store never exposes a filesystem path as product identity. Higher layers receive either the
 * legacy opaque private reference or a verified MediaStore content URI owned by the device store.
 */
class VexStreamMediaStore private constructor(
    private val root: File,
    private val deviceStore: VexStreamDeviceMediaStore?,
    @Suppress("UNUSED_PARAMETER")
    constructorMarker: Unit,
) {
    constructor(
        root: File,
    ) : this(
        root = root,
        deviceStore = null,
        constructorMarker = Unit,
    )

    internal constructor(
        root: File,
        deviceStore: VexStreamDeviceMediaStore,
    ) : this(
        root = root,
        deviceStore = deviceStore,
        constructorMarker = Unit,
    )
    fun store(
        expectedSha256: VexStreamMediaSha256,
        expectedByteLength: Long,
        input: InputStream,
        track: VexStreamTrack? = null,
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
                    return promoteOrPrivate(
                        track = track,
                        expectedSha256 = expectedSha256,
                        expectedByteLength = expectedByteLength,
                        finalFile = finalFile,
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

                promoteOrPrivate(
                    track = track,
                    expectedSha256 = expectedSha256,
                    expectedByteLength = expectedByteLength,
                    finalFile = finalFile,
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

    fun rediscoverDeviceCopy(
        expectedSha256: VexStreamMediaSha256,
        expectedByteLength: Long,
    ): VexStreamLocalCopyState.Downloaded? =
        deviceStore
            ?.rediscover(
                expectedSha256 = expectedSha256,
                expectedByteLength = expectedByteLength,
            )

    fun verify(
        localCopy: VexStreamLocalCopyState.Downloaded,
    ): Boolean {
        val mediaRefDigest =
            digestFromLocalMediaRef(
                localCopy.localMediaRef,
            )

        if (mediaRefDigest == null) {
            return deviceStore
                ?.verify(localCopy)
                ?: false
        }

        if (
            mediaRefDigest !=
            localCopy.mediaSha256
        ) {
            return false
        }

        return try {
            verifyFile(
                file =
                    finalFile(
                        localCopy.mediaSha256,
                    ),
                expectedSha256 =
                    localCopy.mediaSha256,
                expectedByteLength =
                    localCopy.byteLength,
            )
        } catch (_: Exception) {
            false
        }
    }

    fun open(
        localCopy: VexStreamLocalCopyState.Downloaded,
    ): InputStream {
        val privateDigest =
            digestFromLocalMediaRef(
                localCopy.localMediaRef,
            )

        if (privateDigest == null) {
            val sharedStore =
                requireNotNull(
                    deviceStore,
                ) {
                    "VexStream shared media store is unavailable"
                }
            check(
                sharedStore.verify(
                    localCopy,
                ),
            ) {
                "VexStream shared media copy is missing or failed verification"
            }
            return sharedStore.open(
                localCopy,
            )
        }

        check(verify(localCopy)) {
            "VexStream local media copy is missing or failed verification"
        }

        return FileInputStream(
            finalFile(
                localCopy.mediaSha256,
            ),
        )
    }

    /**
     * Playback-only filesystem resolution for legacy app-private copies.
     *
     * Durable MediaStore copies return null here and are resolved through [contentUriForPlayback].
     */
    fun fileForPlayback(
        localCopy: VexStreamLocalCopyState.Downloaded,
    ): File? {
        if (
            digestFromLocalMediaRef(
                localCopy.localMediaRef,
            ) == null
        ) {
            return null
        }

        return if (verify(localCopy)) {
            finalFile(
                localCopy.mediaSha256,
            )
        } else {
            null
        }
    }

    fun contentUriForPlayback(
        localCopy: VexStreamLocalCopyState.Downloaded,
    ): String? =
        deviceStore
            ?.playbackUri(
                localCopy,
            )

    fun remove(
        localCopy: VexStreamLocalCopyState.Downloaded,
    ): Boolean {
        val digest =
            digestFromLocalMediaRef(
                localCopy.localMediaRef,
            )

        if (digest == null) {
            return deviceStore
                ?.remove(localCopy)
                ?: false
        }

        if (
            digest !=
            localCopy.mediaSha256
        ) {
            return false
        }

        val file =
            finalFile(
                localCopy.mediaSha256,
            )
        return !file.exists() ||
            file.delete()
    }

    private fun promoteOrPrivate(
        track: VexStreamTrack?,
        expectedSha256: VexStreamMediaSha256,
        expectedByteLength: Long,
        finalFile: File,
    ): VexStreamMediaStoreResult {
        val privateCopy =
            downloadedState(
                expectedSha256 =
                    expectedSha256,
                expectedByteLength =
                    expectedByteLength,
            )
        val sharedStore =
            deviceStore

        if (
            track == null ||
            sharedStore == null
        ) {
            return VexStreamMediaStoreResult.Stored(
                privateCopy,
            )
        }

        require(
            track.mediaSha256 ==
                expectedSha256 &&
                track.mediaByteLength ==
                expectedByteLength,
        ) {
            "VexStream shared publication identity must match the verified private copy"
        }

        val sharedResult =
            FileInputStream(
                finalFile,
            ).use { input ->
                sharedStore.store(
                    track = track,
                    input = input,
                )
            }

        if (
            sharedResult is
            VexStreamMediaStoreResult.Stored
        ) {
            // Shared verified media is now the durable owner. App-private cleanup is best-effort;
            // failure to remove the redundant staging copy must not invalidate the published song.
            finalFile.delete()
        }

        return sharedResult
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
                File(
                    contextFilesDir,
                    "vexstream/media",
                ),
            )

        fun production(
            context: Context,
        ): VexStreamMediaStore =
            VexStreamMediaStore(
                root =
                    File(
                        context.filesDir,
                        "vexstream/media",
                    ),
                deviceStore =
                    VexStreamDeviceMediaStore(
                        context.applicationContext,
                    ),
            )
    }
}

// [VXG RealForever]
