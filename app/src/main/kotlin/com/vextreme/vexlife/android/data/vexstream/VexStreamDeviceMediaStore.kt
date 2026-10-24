package com.vextreme.vexlife.android.data.vexstream

import android.Manifest
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest

/**
 * Android-native durable owner for VexStream audio that belongs in the device music library.
 *
 * Product identity remains [VexStreamMediaSha256] + exact byte length. MediaStore row IDs and
 * content URIs are storage addresses only.
 *
 * Android 10+ writes through a pending MediaStore row and publishes only after the stored bytes are
 * re-opened and verified. Android 7-9 uses the same MediaStore audio collection but requires the
 * legacy storage permission and an exact public Music/VexStream path.
 */
internal data class VexStreamSharedCopySnapshot(
    val localMediaRef: String,
    val mediaSha256: VexStreamMediaSha256,
    val byteLength: Long,
    val pending: Boolean,
    val verificationReceipt:
        VexStreamLocalCopyVerificationReceipt,
)

internal data class VexStreamSharedCopyVerificationCandidate(
    val localCopy:
        VexStreamLocalCopyState.Downloaded,
    val snapshot:
        VexStreamSharedCopySnapshot,
)

internal data class VexStreamSharedCopyReconciliation(
    val reusableCopies:
        Map<
            String,
            VexStreamLocalCopyState.Downloaded
        >,
    val verificationCandidates:
        List<
            VexStreamSharedCopyVerificationCandidate
        >,
)

/**
 * Pure relationship calculation between persisted offline-index truth and one current MediaStore
 * snapshot.
 *
 * Stable receipts are reusable immediately. Missing/stale receipts become bounded byte-verification
 * candidates. Rows that are missing, pending, wrong-digest or wrong-length are rejected before any
 * byte verification is attempted.
 */
internal fun reconcilePersistedSharedCopies(
    localCopies:
        List<
            VexStreamLocalCopyState.Downloaded
        >,
    snapshots:
        Map<
            String,
            VexStreamSharedCopySnapshot
        >,
): VexStreamSharedCopyReconciliation {
    val reusable =
        linkedMapOf<
            String,
            VexStreamLocalCopyState.Downloaded
        >()
    val verificationCandidates =
        mutableListOf<
            VexStreamSharedCopyVerificationCandidate
        >()

    localCopies.forEach { localCopy ->
        val snapshot =
            snapshots[
                localCopy.localMediaRef
            ] ?: return@forEach

        if (
            snapshot.mediaSha256 !=
                localCopy.mediaSha256 ||
            snapshot.byteLength !=
                localCopy.byteLength ||
            snapshot.pending
        ) {
            return@forEach
        }

        val recordedReceipt =
            localCopy.verificationReceipt

        if (
            recordedReceipt != null &&
            recordedReceipt ==
                snapshot.verificationReceipt
        ) {
            reusable[
                localCopy.localMediaRef
            ] =
                localCopy.copy(
                    verificationReceipt =
                        snapshot.verificationReceipt,
                )
        } else {
            verificationCandidates +=
                VexStreamSharedCopyVerificationCandidate(
                    localCopy = localCopy,
                    snapshot = snapshot,
                )
        }
    }

    return VexStreamSharedCopyReconciliation(
        reusableCopies = reusable,
        verificationCandidates =
            verificationCandidates,
    )
}

internal class VexStreamDeviceMediaStore(
    context: Context,
) {
    private val appContext =
        context.applicationContext
    private val contentResolver =
        appContext.contentResolver

    fun store(
        track: VexStreamTrack,
        input: InputStream,
    ): VexStreamMediaStoreResult {
        if (
            Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return VexStreamMediaStoreResult.Rejected(
                VexStreamMediaStoreRejection.PERMISSION_REQUIRED,
            )
        }

        return try {
            val existing =
                existingRow(
                    track.mediaSha256,
                )

            if (existing != null) {
                if (
                    !isOwnedUri(
                        uri = existing.uri,
                        digest = track.mediaSha256,
                    )
                ) {
                    return VexStreamMediaStoreResult.Rejected(
                        VexStreamMediaStoreRejection.SHARED_COPY_CONFLICT,
                    )
                }

                if (existing.pending) {
                    if (contentResolver.delete(existing.uri, null, null) <= 0) {
                        return VexStreamMediaStoreResult.Rejected(
                            VexStreamMediaStoreRejection.IO_FAILURE,
                        )
                    }
                } else {
                    return if (
                        verifyUri(
                            uri = existing.uri,
                            expectedSha256 = track.mediaSha256,
                            expectedByteLength = track.mediaByteLength,
                        ) == null
                    ) {
                        VexStreamMediaStoreResult.Stored(
                            downloadedState(
                                track = track,
                                uri = existing.uri,
                            ),
                        )
                    } else {
                        VexStreamMediaStoreResult.Rejected(
                            VexStreamMediaStoreRejection.SHARED_COPY_CONFLICT,
                        )
                    }
                }
            }

            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                val parent =
                    legacyFile(track.mediaSha256)
                        .parentFile
                if (
                    parent != null &&
                    !parent.isDirectory &&
                    !parent.mkdirs()
                ) {
                    return VexStreamMediaStoreResult.Rejected(
                        VexStreamMediaStoreRejection.IO_FAILURE,
                    )
                }
            }

            val uri =
                contentResolver.insert(
                    audioCollection(),
                    contentValues(track),
                )
                    ?: return VexStreamMediaStoreResult.Rejected(
                        VexStreamMediaStoreRejection.IO_FAILURE,
                    )

            val writeFailure =
                writeAndVerify(
                    uri = uri,
                    expectedSha256 = track.mediaSha256,
                    expectedByteLength = track.mediaByteLength,
                    input = input,
                )

            if (writeFailure != null) {
                contentResolver.delete(uri, null, null)
                return VexStreamMediaStoreResult.Rejected(
                    writeFailure,
                )
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val publishValues =
                    ContentValues().apply {
                        put(
                            MediaStore.Audio.Media.IS_PENDING,
                            0,
                        )
                    }
                if (
                    contentResolver.update(
                        uri,
                        publishValues,
                        null,
                        null,
                    ) != 1
                ) {
                    contentResolver.delete(uri, null, null)
                    return VexStreamMediaStoreResult.Rejected(
                        VexStreamMediaStoreRejection.IO_FAILURE,
                    )
                }
            }

            VexStreamMediaStoreResult.Stored(
                downloadedState(
                    track = track,
                    uri = uri,
                ),
            )
        } catch (_: Exception) {
            VexStreamMediaStoreResult.Rejected(
                VexStreamMediaStoreRejection.IO_FAILURE,
            )
        }
    }

    fun rediscover(
        expectedSha256: VexStreamMediaSha256,
        expectedByteLength: Long,
    ): VexStreamLocalCopyState.Downloaded? {
        if (
            Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.READ_EXTERNAL_STORAGE,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }

        return try {
            val existing =
                existingRow(
                    expectedSha256,
                )
                    ?: return null

            if (
                existing.pending ||
                !isOwnedUri(
                    uri = existing.uri,
                    digest = expectedSha256,
                ) ||
                verifyUri(
                    uri = existing.uri,
                    expectedSha256 = expectedSha256,
                    expectedByteLength = expectedByteLength,
                ) != null
            ) {
                return null
            }

            downloadedState(
                expectedSha256 = expectedSha256,
                expectedByteLength = expectedByteLength,
                uri = existing.uri,
            )
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Restores a persisted shared-library set with one MediaStore collection query on API 30+.
     *
     * The returned map is keyed by the exact persisted content URI. Unchanged receipts require no
     * media-byte reads; only rows whose stored receipt no longer matches fall back to exact SHA-256.
     */
    fun restorePersisted(
        localCopies:
            List<
                VexStreamLocalCopyState.Downloaded
            >,
    ): Map<
        String,
        VexStreamLocalCopyState.Downloaded
    > {
        if (
            localCopies.isEmpty()
        ) {
            return emptyMap()
        }

        if (
            Build.VERSION.SDK_INT <
            Build.VERSION_CODES.R
        ) {
            return localCopies
                .mapNotNull { localCopy ->
                    restorePersisted(
                        localCopy,
                    )?.let { restored ->
                        localCopy.localMediaRef to
                            restored
                    }
                }
                .toMap()
        }

        val snapshots =
            currentOwnedSnapshots()
        if (
            snapshots.isEmpty()
        ) {
            return emptyMap()
        }

        val reconciliation =
            reconcilePersistedSharedCopies(
                localCopies = localCopies,
                snapshots = snapshots,
            )

        return buildMap {
            putAll(
                reconciliation.reusableCopies,
            )

            reconciliation
                .verificationCandidates
                .forEach { candidate ->
                    val localCopy =
                        candidate.localCopy
                    val snapshot =
                        candidate.snapshot
                    val uri =
                        Uri.parse(
                            snapshot.localMediaRef,
                        )

                    if (
                        verifyUri(
                            uri = uri,
                            expectedSha256 =
                                localCopy.mediaSha256,
                            expectedByteLength =
                                localCopy.byteLength,
                        ) == null
                    ) {
                        put(
                            localCopy.localMediaRef,
                            localCopy.copy(
                                verificationReceipt =
                                    snapshot
                                        .verificationReceipt,
                            ),
                        )
                    }
                }
        }
    }

    /**
     * Restores one copy recorded by the app-private offline index.
     *
     * API 30+ shared MediaStore copies reuse a prior cryptographic proof while the exact owned row,
     * byte length, MediaStore version and row generation remain unchanged. A legacy v1 index has no
     * prior MediaStore receipt, so its bytes are verified once before the v2 index may persist the
     * first receipt. Any later generation/version change likewise falls back to full SHA-256
     * verification before a refreshed receipt is admitted.
     *
     * Android 7-10 retain the earlier full-verification behavior.
     */
    fun restorePersisted(
        localCopy: VexStreamLocalCopyState.Downloaded,
    ): VexStreamLocalCopyState.Downloaded? {
        val uri =
            localCopy.contentUri()
                ?: return null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val currentReceipt =
                verificationReceipt(
                    uri = uri,
                    digest = localCopy.mediaSha256,
                    expectedByteLength = localCopy.byteLength,
                )
                    ?: return null

            val recordedReceipt =
                localCopy.verificationReceipt
            if (
                recordedReceipt != null &&
                recordedReceipt == currentReceipt
            ) {
                return localCopy.copy(
                    verificationReceipt = currentReceipt,
                )
            }
        }

        if (
            !isOwnedUri(
                uri = uri,
                digest = localCopy.mediaSha256,
            ) ||
            verifyUri(
                uri = uri,
                expectedSha256 = localCopy.mediaSha256,
                expectedByteLength = localCopy.byteLength,
            ) != null
        ) {
            return null
        }

        return downloadedState(
            expectedSha256 = localCopy.mediaSha256,
            expectedByteLength = localCopy.byteLength,
            uri = uri,
        )
    }

    fun verify(
        localCopy: VexStreamLocalCopyState.Downloaded,
    ): Boolean {
        val uri =
            localCopy.contentUri()
                ?: return false

        val recordedReceipt =
            localCopy.verificationReceipt
        if (
            recordedReceipt != null &&
            verificationReceipt(
                uri = uri,
                digest = localCopy.mediaSha256,
                expectedByteLength = localCopy.byteLength,
            ) == recordedReceipt
        ) {
            return true
        }

        if (
            !isOwnedUri(
                uri = uri,
                digest = localCopy.mediaSha256,
            )
        ) {
            return false
        }

        return verifyUri(
            uri = uri,
            expectedSha256 = localCopy.mediaSha256,
            expectedByteLength = localCopy.byteLength,
        ) == null
    }

    fun open(
        localCopy: VexStreamLocalCopyState.Downloaded,
    ): InputStream {
        val uri =
            localCopy.contentUri()
                ?: error("VexStream shared media reference is not a content URI")

        check(
            verify(localCopy),
        ) {
            "VexStream shared media copy is missing or failed verification"
        }

        return requireNotNull(
            contentResolver.openInputStream(uri),
        ) {
            "VexStream shared media is unavailable"
        }
    }

    fun playbackUri(
        localCopy: VexStreamLocalCopyState.Downloaded,
    ): String? =
        if (verify(localCopy)) {
            localCopy.localMediaRef
        } else {
            null
        }

    fun remove(
        localCopy: VexStreamLocalCopyState.Downloaded,
    ): Boolean {
        val uri =
            localCopy.contentUri()
                ?: return false

        return try {
            if (
                !isOwnedUri(
                    uri = uri,
                    digest = localCopy.mediaSha256,
                )
            ) {
                false
            } else {
                contentResolver.delete(
                    uri,
                    null,
                    null,
                ) > 0
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun writeAndVerify(
        uri: Uri,
        expectedSha256: VexStreamMediaSha256,
        expectedByteLength: Long,
        input: InputStream,
    ): VexStreamMediaStoreRejection? {
        val digest =
            MessageDigest.getInstance(
                "SHA-256",
            )
        var byteCount = 0L

        val descriptor =
            contentResolver.openFileDescriptor(
                uri,
                "w",
            )
                ?: return VexStreamMediaStoreRejection.IO_FAILURE

        descriptor.use { parcelFileDescriptor ->
            FileOutputStream(
                parcelFileDescriptor.fileDescriptor,
            ).use { output ->
                val buffer =
                    ByteArray(
                        DEFAULT_BUFFER_SIZE,
                    )

                while (true) {
                    val read =
                        input.read(
                            buffer,
                        )
                    if (read < 0) {
                        break
                    }

                    byteCount +=
                        read.toLong()
                    if (
                        byteCount >
                        expectedByteLength
                    ) {
                        return VexStreamMediaStoreRejection.LENGTH_MISMATCH
                    }

                    digest.update(
                        buffer,
                        0,
                        read,
                    )
                    output.write(
                        buffer,
                        0,
                        read,
                    )
                }

                output.flush()
                output.fd.sync()
            }
        }

        if (
            byteCount !=
            expectedByteLength
        ) {
            return VexStreamMediaStoreRejection.LENGTH_MISMATCH
        }

        if (
            digest.digest().toHex() !=
            expectedSha256.value
        ) {
            return VexStreamMediaStoreRejection.DIGEST_MISMATCH
        }

        return verifyUri(
            uri = uri,
            expectedSha256 = expectedSha256,
            expectedByteLength = expectedByteLength,
        )
    }

    private fun verifyUri(
        uri: Uri,
        expectedSha256: VexStreamMediaSha256,
        expectedByteLength: Long,
    ): VexStreamMediaStoreRejection? =
        try {
            val input =
                contentResolver.openInputStream(uri)
                    ?: return VexStreamMediaStoreRejection.IO_FAILURE
            val digest =
                MessageDigest.getInstance(
                    "SHA-256",
                )
            var byteCount = 0L

            input.use {
                val buffer =
                    ByteArray(
                        DEFAULT_BUFFER_SIZE,
                    )
                while (true) {
                    val read =
                        it.read(buffer)
                    if (read < 0) {
                        break
                    }
                    byteCount +=
                        read.toLong()
                    if (
                        byteCount >
                        expectedByteLength
                    ) {
                        return VexStreamMediaStoreRejection.LENGTH_MISMATCH
                    }
                    digest.update(
                        buffer,
                        0,
                        read,
                    )
                }
            }

            when {
                byteCount != expectedByteLength ->
                    VexStreamMediaStoreRejection.LENGTH_MISMATCH

                digest.digest().toHex() != expectedSha256.value ->
                    VexStreamMediaStoreRejection.DIGEST_MISMATCH

                else ->
                    null
            }
        } catch (_: Exception) {
            VexStreamMediaStoreRejection.IO_FAILURE
        }

    private fun currentOwnedSnapshots():
        Map<
            String,
            VexStreamSharedCopySnapshot
        > {
        if (
            Build.VERSION.SDK_INT <
            Build.VERSION_CODES.R
        ) {
            return emptyMap()
        }

        val projection =
            arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.RELATIVE_PATH,
                MediaStore.Audio.Media.MIME_TYPE,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.GENERATION_MODIFIED,
                MediaStore.Audio.Media.IS_PENDING,
            )
        val mediaStoreVersion =
            try {
                MediaStore.getVersion(
                    appContext,
                    MediaStore.VOLUME_EXTERNAL_PRIMARY,
                )
            } catch (_: Exception) {
                return emptyMap()
            }

        return try {
            contentResolver.query(
                audioCollection(),
                projection,
                MediaStore.Audio.Media.RELATIVE_PATH +
                    " = ?",
                arrayOf(
                    RELATIVE_PATH,
                ),
                null,
            )?.use { cursor ->
                buildMap {
                    while (
                        cursor.moveToNext()
                    ) {
                        val name =
                            cursor.getString(
                                cursor.getColumnIndexOrThrow(
                                    MediaStore.Audio.Media.DISPLAY_NAME,
                                ),
                            )
                        val digestText =
                            name
                                .takeIf {
                                    it.endsWith(
                                        MEDIA_EXTENSION,
                                    )
                                }
                                ?.removeSuffix(
                                    MEDIA_EXTENSION,
                                )
                                ?: continue
                        val digest =
                            runCatching {
                                VexStreamMediaSha256(
                                    digestText,
                                )
                            }.getOrNull()
                                ?: continue
                        val relativePath =
                            cursor.getString(
                                cursor.getColumnIndexOrThrow(
                                    MediaStore.Audio.Media.RELATIVE_PATH,
                                ),
                            )
                        val mime =
                            cursor.getString(
                                cursor.getColumnIndexOrThrow(
                                    MediaStore.Audio.Media.MIME_TYPE,
                                ),
                            )
                        if (
                            relativePath !=
                                RELATIVE_PATH ||
                            mime != MIME_TYPE
                        ) {
                            continue
                        }

                        val id =
                            cursor.getLong(
                                cursor.getColumnIndexOrThrow(
                                    MediaStore.Audio.Media._ID,
                                ),
                            )
                        val uri =
                            ContentUris
                                .withAppendedId(
                                    audioCollection(),
                                    id,
                                )
                        val byteLength =
                            cursor.getLong(
                                cursor.getColumnIndexOrThrow(
                                    MediaStore.Audio.Media.SIZE,
                                ),
                            )
                        val generation =
                            cursor.getLong(
                                cursor.getColumnIndexOrThrow(
                                    MediaStore.Audio.Media.GENERATION_MODIFIED,
                                ),
                            )
                        val pending =
                            cursor.getInt(
                                cursor.getColumnIndexOrThrow(
                                    MediaStore.Audio.Media.IS_PENDING,
                                ),
                            ) != 0

                        val localMediaRef =
                            uri.toString()
                        put(
                            localMediaRef,
                            VexStreamSharedCopySnapshot(
                                localMediaRef =
                                    localMediaRef,
                                mediaSha256 = digest,
                                byteLength =
                                    byteLength,
                                pending = pending,
                                verificationReceipt =
                                    VexStreamLocalCopyVerificationReceipt(
                                        mediaStoreVersion =
                                            mediaStoreVersion,
                                        generationModified =
                                            generation,
                                    ),
                            ),
                        )
                    }
                }
            } ?: emptyMap()
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun verificationReceipt(
        uri: Uri,
        digest: VexStreamMediaSha256,
        expectedByteLength: Long,
    ): VexStreamLocalCopyVerificationReceipt? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return null
        }

        val projection =
            arrayOf(
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.RELATIVE_PATH,
                MediaStore.Audio.Media.MIME_TYPE,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.GENERATION_MODIFIED,
            )

        return try {
            contentResolver.query(
                uri,
                projection,
                null,
                null,
                null,
            )?.use { cursor ->
                if (!cursor.moveToFirst()) {
                    null
                } else {
                    val name =
                        cursor.getString(
                            cursor.getColumnIndexOrThrow(
                                MediaStore.Audio.Media.DISPLAY_NAME,
                            ),
                        )
                    val relativePath =
                        cursor.getString(
                            cursor.getColumnIndexOrThrow(
                                MediaStore.Audio.Media.RELATIVE_PATH,
                            ),
                        )
                    val mime =
                        cursor.getString(
                            cursor.getColumnIndexOrThrow(
                                MediaStore.Audio.Media.MIME_TYPE,
                            ),
                        )
                    val size =
                        cursor.getLong(
                            cursor.getColumnIndexOrThrow(
                                MediaStore.Audio.Media.SIZE,
                            ),
                        )
                    val generation =
                        cursor.getLong(
                            cursor.getColumnIndexOrThrow(
                                MediaStore.Audio.Media.GENERATION_MODIFIED,
                            ),
                        )

                    if (
                        name != displayName(digest) ||
                        relativePath != RELATIVE_PATH ||
                        mime != MIME_TYPE ||
                        size != expectedByteLength
                    ) {
                        null
                    } else {
                        VexStreamLocalCopyVerificationReceipt(
                            mediaStoreVersion =
                                MediaStore.getVersion(
                                    appContext,
                                    MediaStore.VOLUME_EXTERNAL_PRIMARY,
                                ),
                            generationModified = generation,
                        )
                    }
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun isOwnedUri(
        uri: Uri,
        digest: VexStreamMediaSha256,
    ): Boolean {
        val projection =
            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.Q
            ) {
                arrayOf(
                    MediaStore.Audio.Media.DISPLAY_NAME,
                    MediaStore.Audio.Media.RELATIVE_PATH,
                    MediaStore.Audio.Media.MIME_TYPE,
                )
            } else {
                arrayOf(
                    MediaStore.Audio.Media.DISPLAY_NAME,
                    MediaStore.Audio.Media.DATA,
                    MediaStore.Audio.Media.MIME_TYPE,
                )
            }

        return try {
            contentResolver.query(
                uri,
                projection,
                null,
                null,
                null,
            )?.use { cursor ->
                if (!cursor.moveToFirst()) {
                    false
                } else {
                    val name =
                        cursor.getString(
                            cursor.getColumnIndexOrThrow(
                                MediaStore.Audio.Media.DISPLAY_NAME,
                            ),
                        )
                    val mime =
                        cursor.getString(
                            cursor.getColumnIndexOrThrow(
                                MediaStore.Audio.Media.MIME_TYPE,
                            ),
                        )
                    val locationMatches =
                        if (
                            Build.VERSION.SDK_INT >=
                            Build.VERSION_CODES.Q
                        ) {
                            cursor.getString(
                                cursor.getColumnIndexOrThrow(
                                    MediaStore.Audio.Media.RELATIVE_PATH,
                                ),
                            ) == RELATIVE_PATH
                        } else {
                            cursor.getString(
                                cursor.getColumnIndexOrThrow(
                                    MediaStore.Audio.Media.DATA,
                                ),
                            ) ==
                                legacyFile(
                                    digest,
                                ).absolutePath
                        }

                    name ==
                        displayName(
                            digest,
                        ) &&
                        mime == MIME_TYPE &&
                        locationMatches
                }
            } ?: false
        } catch (_: Exception) {
            false
        }
    }

    private fun existingRow(
        digest: VexStreamMediaSha256,
    ): ExistingRow? {
        val projection =
            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.Q
            ) {
                arrayOf(
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.IS_PENDING,
                )
            } else {
                arrayOf(
                    MediaStore.Audio.Media._ID,
                )
            }

        val selection: String
        val arguments: Array<String>
        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.Q
        ) {
            selection =
                MediaStore.Audio.Media.DISPLAY_NAME +
                    " = ? AND " +
                    MediaStore.Audio.Media.RELATIVE_PATH +
                    " = ?"
            arguments =
                arrayOf(
                    displayName(digest),
                    RELATIVE_PATH,
                )
        } else {
            selection =
                MediaStore.Audio.Media.DATA +
                    " = ?"
            arguments =
                arrayOf(
                    legacyFile(digest)
                        .absolutePath,
                )
        }

        contentResolver.query(
            audioCollection(),
            projection,
            selection,
            arguments,
            null,
        )?.use { cursor ->
            if (!cursor.moveToFirst()) {
                return null
            }

            val id =
                cursor.getLong(
                    cursor.getColumnIndexOrThrow(
                        MediaStore.Audio.Media._ID,
                    ),
                )
            val pending =
                Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.Q &&
                    cursor.getInt(
                        cursor.getColumnIndexOrThrow(
                            MediaStore.Audio.Media.IS_PENDING,
                        ),
                    ) != 0

            return ExistingRow(
                uri =
                    ContentUris.withAppendedId(
                        audioCollection(),
                        id,
                    ),
                pending = pending,
            )
        }

        return null
    }

    private fun contentValues(
        track: VexStreamTrack,
    ): ContentValues =
        ContentValues().apply {
            put(
                MediaStore.Audio.Media.DISPLAY_NAME,
                displayName(track.mediaSha256),
            )
            put(
                MediaStore.Audio.Media.MIME_TYPE,
                MIME_TYPE,
            )
            put(
                MediaStore.Audio.Media.TITLE,
                sanitizeMetadata(track.title),
            )
            put(
                MediaStore.Audio.Media.ARTIST,
                sanitizeMetadata(track.artist),
            )
            put(
                MediaStore.Audio.Media.ALBUM,
                sanitizeMetadata(track.album),
            )
            put(
                MediaStore.Audio.Media.IS_MUSIC,
                1,
            )

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.Q
            ) {
                put(
                    MediaStore.Audio.Media.RELATIVE_PATH,
                    RELATIVE_PATH,
                )
                put(
                    MediaStore.Audio.Media.IS_PENDING,
                    1,
                )
            } else {
                @Suppress("DEPRECATION")
                put(
                    MediaStore.Audio.Media.DATA,
                    legacyFile(
                        track.mediaSha256,
                    ).absolutePath,
                )
            }
        }

    private fun audioCollection(): Uri =
        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.Q
        ) {
            MediaStore.Audio.Media.getContentUri(
                MediaStore.VOLUME_EXTERNAL_PRIMARY,
            )
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

    @Suppress("DEPRECATION")
    private fun legacyFile(
        digest: VexStreamMediaSha256,
    ): File =
        File(
            File(
                Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_MUSIC,
                ),
                DIRECTORY_NAME,
            ),
            displayName(digest),
        )

    private fun downloadedState(
        track: VexStreamTrack,
        uri: Uri,
    ): VexStreamLocalCopyState.Downloaded =
        downloadedState(
            expectedSha256 = track.mediaSha256,
            expectedByteLength = track.mediaByteLength,
            uri = uri,
        )

    private fun downloadedState(
        expectedSha256: VexStreamMediaSha256,
        expectedByteLength: Long,
        uri: Uri,
    ): VexStreamLocalCopyState.Downloaded =
        VexStreamLocalCopyState.Downloaded(
            mediaSha256 = expectedSha256,
            byteLength = expectedByteLength,
            localMediaRef = uri.toString(),
            verificationReceipt =
                verificationReceipt(
                    uri = uri,
                    digest = expectedSha256,
                    expectedByteLength = expectedByteLength,
                ),
        )

    private fun VexStreamLocalCopyState.Downloaded.contentUri(): Uri? =
        runCatching {
            Uri.parse(
                localMediaRef,
            )
        }.getOrNull()
            ?.takeIf {
                it.scheme ==
                    ContentResolver.SCHEME_CONTENT
            }

    private fun displayName(
        digest: VexStreamMediaSha256,
    ): String =
        digest.value +
            MEDIA_EXTENSION

    private fun sanitizeMetadata(
        value: String,
    ): String =
        value
            .filterNot {
                it.isISOControl()
            }
            .trim()
            .take(
                MAX_METADATA_LENGTH,
            )

    private fun ByteArray.toHex(): String =
        joinToString(
            separator = "",
        ) { byte ->
            "%02x".format(
                byte,
            )
        }

    private data class ExistingRow(
        val uri: Uri,
        val pending: Boolean,
    )

    companion object {
        private const val DIRECTORY_NAME =
            "VexStream"
        private const val RELATIVE_PATH =
            "Music/VexStream/"
        private const val MEDIA_EXTENSION =
            ".mp3"
        private const val MIME_TYPE =
            "audio/mpeg"
        private const val MAX_METADATA_LENGTH =
            512
    }
}

// [VXG RealForever]
