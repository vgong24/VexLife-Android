package com.vextreme.vexlife.android.data.vexstream

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.ByteArrayInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VexStreamDeviceMediaStoreInstrumentedTest {
    @Test
    fun verifiedMp3PublishesToMusicVexStreamAndIsIdempotent() {
        assumeTrue(
            Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.Q,
        )

        val context =
            InstrumentationRegistry
                .getInstrumentation()
                .targetContext
        val bytes =
            "durable vexstream device music"
                .encodeToByteArray()
        val track =
            track(
                bytes = bytes,
                suffix = "verified",
            )
        val store =
            VexStreamDeviceMediaStore(
                context,
            )

        val first =
            store.store(
                track = track,
                input =
                    ByteArrayInputStream(
                        bytes,
                    ),
            ) as VexStreamMediaStoreResult.Stored

        try {
            assertTrue(
                first.localCopy.localMediaRef
                    .startsWith(
                        "content://",
                    ),
            )
            assertTrue(
                store.verify(
                    first.localCopy,
                ),
            )
            store.open(
                first.localCopy,
            ).use { input ->
                assertArrayEquals(
                    bytes,
                    input.readBytes(),
                )
            }

            context.contentResolver
                .query(
                    android.net.Uri.parse(
                        first.localCopy.localMediaRef,
                    ),
                    arrayOf(
                        MediaStore.Audio.Media.DISPLAY_NAME,
                        MediaStore.Audio.Media.RELATIVE_PATH,
                        MediaStore.Audio.Media.MIME_TYPE,
                        MediaStore.Audio.Media.IS_PENDING,
                    ),
                    null,
                    null,
                    null,
                )
                ?.use { cursor ->
                    assertTrue(
                        cursor.moveToFirst(),
                    )
                    assertEquals(
                        track.mediaSha256.value +
                            ".mp3",
                        cursor.getString(
                            cursor.getColumnIndexOrThrow(
                                MediaStore.Audio.Media.DISPLAY_NAME,
                            ),
                        ),
                    )
                    assertEquals(
                        "Music/VexStream/",
                        cursor.getString(
                            cursor.getColumnIndexOrThrow(
                                MediaStore.Audio.Media.RELATIVE_PATH,
                            ),
                        ),
                    )
                    assertEquals(
                        "audio/mpeg",
                        cursor.getString(
                            cursor.getColumnIndexOrThrow(
                                MediaStore.Audio.Media.MIME_TYPE,
                            ),
                        ),
                    )
                    assertEquals(
                        0,
                        cursor.getInt(
                            cursor.getColumnIndexOrThrow(
                                MediaStore.Audio.Media.IS_PENDING,
                            ),
                        ),
                    )
                }
                ?: error(
                    "published MediaStore row was not readable",
                )

            val second =
                store.store(
                    track = track,
                    input =
                        ByteArrayInputStream(
                            "unused repeat input"
                                .encodeToByteArray(),
                        ),
                ) as VexStreamMediaStoreResult.Stored

            assertEquals(
                first.localCopy,
                second.localCopy,
            )
        } finally {
            assertTrue(
                store.remove(
                    first.localCopy,
                ),
            )
        }
    }

    @Test
    fun digestMismatchNeverLeavesPublishedSharedAudio() {
        assumeTrue(
            Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.Q,
        )

        val context =
            InstrumentationRegistry
                .getInstrumentation()
                .targetContext
        val expected =
            "expected vexstream bytes"
                .encodeToByteArray()
        val actual =
            "different vexstream bytes"
                .encodeToByteArray()
        val track =
            track(
                bytes = expected,
                suffix = "mismatch",
            )
        val store =
            VexStreamDeviceMediaStore(
                context,
            )

        assertEquals(
            VexStreamMediaStoreResult.Rejected(
                VexStreamMediaStoreRejection.DIGEST_MISMATCH,
            ),
            store.store(
                track = track,
                input =
                    ByteArrayInputStream(
                        actual,
                    ),
            ),
        )

        val collection =
            MediaStore.Audio.Media
                .getContentUri(
                    MediaStore.VOLUME_EXTERNAL_PRIMARY,
                )
        context.contentResolver
            .query(
                collection,
                arrayOf(
                    MediaStore.Audio.Media._ID,
                ),
                MediaStore.Audio.Media.DISPLAY_NAME +
                    " = ? AND " +
                    MediaStore.Audio.Media.RELATIVE_PATH +
                    " = ?",
                arrayOf(
                    track.mediaSha256.value +
                        ".mp3",
                    "Music/VexStream/",
                ),
                null,
            )
            ?.use { cursor ->
                assertEquals(
                    0,
                    cursor.count,
                )
            }
            ?: error(
                "MediaStore collection query failed",
            )
    }

    @Test
    fun matchingBytesAtAnUnownedContentUriAreRejected() {
        assumeTrue(
            Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.Q,
        )

        val context =
            InstrumentationRegistry
                .getInstrumentation()
                .targetContext
        val bytes =
            "same bytes wrong media location"
                .encodeToByteArray()
        val track =
            track(
                bytes = bytes,
                suffix = "unowned",
            )
        val collection =
            MediaStore.Audio.Media
                .getContentUri(
                    MediaStore.VOLUME_EXTERNAL_PRIMARY,
                )
        val values =
            ContentValues().apply {
                put(
                    MediaStore.Audio.Media.DISPLAY_NAME,
                    track.mediaSha256.value +
                        ".mp3",
                )
                put(
                    MediaStore.Audio.Media.RELATIVE_PATH,
                    "Music/NotVexStream/",
                )
                put(
                    MediaStore.Audio.Media.MIME_TYPE,
                    "audio/mpeg",
                )
                put(
                    MediaStore.Audio.Media.IS_PENDING,
                    1,
                )
            }
        val uri =
            requireNotNull(
                context.contentResolver.insert(
                    collection,
                    values,
                ),
            )

        try {
            val descriptor =
                requireNotNull(
                    context.contentResolver
                        .openFileDescriptor(
                            uri,
                            "w",
                        ),
                )
            descriptor.use {
                FileOutputStream(
                    it.fileDescriptor,
                ).use { output ->
                    output.write(
                        bytes,
                    )
                    output.flush()
                    output.fd.sync()
                }
            }

            context.contentResolver.update(
                uri,
                ContentValues().apply {
                    put(
                        MediaStore.Audio.Media.IS_PENDING,
                        0,
                    )
                },
                null,
                null,
            )

            val localCopy =
                VexStreamLocalCopyState.Downloaded(
                    mediaSha256 =
                        track.mediaSha256,
                    byteLength =
                        track.mediaByteLength,
                    localMediaRef =
                        uri.toString(),
                )
            val store =
                VexStreamDeviceMediaStore(
                    context,
                )

            assertFalse(
                store.verify(
                    localCopy,
                ),
            )
            assertFalse(
                store.remove(
                    localCopy,
                ),
            )
        } finally {
            context.contentResolver.delete(
                uri,
                null,
                null,
            )
        }
    }

    @Test
    fun sameBytesAtDeterministicPathWithWrongMimeAreAConflict() {
        assumeTrue(
            Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.Q,
        )

        val context =
            InstrumentationRegistry
                .getInstrumentation()
                .targetContext
        val bytes =
            "same bytes wrong mime"
                .encodeToByteArray()
        val track =
            track(
                bytes = bytes,
                suffix = "wrong-mime",
            )
        val collection =
            MediaStore.Audio.Media
                .getContentUri(
                    MediaStore.VOLUME_EXTERNAL_PRIMARY,
                )
        val uri =
            requireNotNull(
                context.contentResolver.insert(
                    collection,
                    ContentValues().apply {
                        put(
                            MediaStore.Audio.Media.DISPLAY_NAME,
                            track.mediaSha256.value +
                                ".mp3",
                        )
                        put(
                            MediaStore.Audio.Media.RELATIVE_PATH,
                            "Music/VexStream/",
                        )
                        put(
                            MediaStore.Audio.Media.MIME_TYPE,
                            "audio/mpeg",
                        )
                        put(
                            MediaStore.Audio.Media.IS_PENDING,
                            1,
                        )
                    },
                ),
            )

        try {
            val descriptor =
                requireNotNull(
                    context.contentResolver
                        .openFileDescriptor(
                            uri,
                            "w",
                        ),
                )
            descriptor.use {
                FileOutputStream(
                    it.fileDescriptor,
                ).use { output ->
                    output.write(
                        bytes,
                    )
                    output.flush()
                    output.fd.sync()
                }
            }

            assertEquals(
                1,
                context.contentResolver.update(
                    uri,
                    ContentValues().apply {
                        put(
                            MediaStore.Audio.Media.IS_PENDING,
                            0,
                        )
                        put(
                            MediaStore.Audio.Media.MIME_TYPE,
                            "audio/flac",
                        )
                    },
                    null,
                    null,
                ),
            )

            context.contentResolver
                .query(
                    uri,
                    arrayOf(
                        MediaStore.Audio.Media.MIME_TYPE,
                    ),
                    null,
                    null,
                    null,
                )
                ?.use { cursor ->
                    assertTrue(
                        cursor.moveToFirst(),
                    )
                    assertEquals(
                        "audio/flac",
                        cursor.getString(
                            cursor.getColumnIndexOrThrow(
                                MediaStore.Audio.Media.MIME_TYPE,
                            ),
                        ),
                    )
                }
                ?: error(
                    "wrong-MIME conflict row was not readable",
                )

            assertEquals(
                VexStreamMediaStoreResult.Rejected(
                    VexStreamMediaStoreRejection
                        .SHARED_COPY_CONFLICT,
                ),
                VexStreamDeviceMediaStore(
                    context,
                ).store(
                    track = track,
                    input =
                        ByteArrayInputStream(
                            "unused input after conflict"
                                .encodeToByteArray(),
                        ),
                ),
            )

            context.contentResolver
                .query(
                    uri,
                    arrayOf(
                        MediaStore.Audio.Media._ID,
                    ),
                    null,
                    null,
                    null,
                )
                ?.use { cursor ->
                    assertTrue(
                        "conflicting row must remain untouched",
                        cursor.moveToFirst(),
                    )
                }
                ?: error(
                    "conflicting row disappeared",
                )
        } finally {
            context.contentResolver.delete(
                uri,
                null,
                null,
            )
        }
    }

    @Test
    fun legacyPermissionDenialFailsBeforeSharedWrite() {
        assumeTrue(
            Build.VERSION.SDK_INT <=
                Build.VERSION_CODES.P,
        )

        val instrumentation =
            InstrumentationRegistry
                .getInstrumentation()
        val context =
            instrumentation.targetContext

        revokeLegacyStoragePermission(
            instrumentation = instrumentation,
            packageName = context.packageName,
        )

        val bytes =
            "legacy denied device storage"
                .encodeToByteArray()
        val track =
            track(
                bytes = bytes,
                suffix = "legacy-denied",
            )

        assertEquals(
            VexStreamMediaStoreResult.Rejected(
                VexStreamMediaStoreRejection
                    .PERMISSION_REQUIRED,
            ),
            VexStreamDeviceMediaStore(
                context,
            ).store(
                track = track,
                input =
                    ByteArrayInputStream(
                        bytes,
                    ),
            ),
        )
    }

    @Test
    @Suppress("DEPRECATION")
    fun legacyGrantedPermissionPublishesExactMusicPath() {
        assumeTrue(
            Build.VERSION.SDK_INT <=
                Build.VERSION_CODES.P,
        )

        val instrumentation =
            InstrumentationRegistry
                .getInstrumentation()
        val context =
            instrumentation.targetContext

        grantLegacyStoragePermission(
            instrumentation = instrumentation,
            packageName = context.packageName,
        )
        assertEquals(
            PackageManager.PERMISSION_GRANTED,
            context.checkSelfPermission(
                Manifest.permission
                    .WRITE_EXTERNAL_STORAGE,
            ),
        )

        val bytes =
            "legacy durable vexstream music"
                .encodeToByteArray()
        val track =
            track(
                bytes = bytes,
                suffix = "legacy-granted",
            )
        val store =
            VexStreamDeviceMediaStore(
                context,
            )
        val result =
            store.store(
                track = track,
                input =
                    ByteArrayInputStream(
                        bytes,
                    ),
            ) as VexStreamMediaStoreResult.Stored

        try {
            assertTrue(
                store.verify(
                    result.localCopy,
                ),
            )

            context.contentResolver
                .query(
                    android.net.Uri.parse(
                        result.localCopy.localMediaRef,
                    ),
                    arrayOf(
                        MediaStore.Audio.Media.DATA,
                        MediaStore.Audio.Media.DISPLAY_NAME,
                        MediaStore.Audio.Media.MIME_TYPE,
                    ),
                    null,
                    null,
                    null,
                )
                ?.use { cursor ->
                    assertTrue(
                        cursor.moveToFirst(),
                    )
                    val path =
                        cursor.getString(
                            cursor.getColumnIndexOrThrow(
                                MediaStore.Audio.Media.DATA,
                            ),
                        )
                    assertTrue(
                        path.endsWith(
                            "/Music/VexStream/" +
                                track.mediaSha256.value +
                                ".mp3",
                        ),
                    )
                    assertEquals(
                        "audio/mpeg",
                        cursor.getString(
                            cursor.getColumnIndexOrThrow(
                                MediaStore.Audio.Media.MIME_TYPE,
                            ),
                        ),
                    )
                }
                ?: error(
                    "legacy MediaStore row was not readable",
                )
        } finally {
            assertTrue(
                store.remove(
                    result.localCopy,
                ),
            )
        }
    }

    private fun grantLegacyStoragePermission(
        instrumentation: android.app.Instrumentation,
        packageName: String,
    ) {
        listOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
        ).forEach { permission ->
            instrumentation.uiAutomation
                .executeShellCommand(
                    "pm grant $packageName $permission",
                )
                .close()
        }
    }

    private fun revokeLegacyStoragePermission(
        instrumentation: android.app.Instrumentation,
        packageName: String,
    ) {
        instrumentation.uiAutomation
            .executeShellCommand(
                "pm revoke $packageName " +
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
            )
            .close()
    }

    private fun track(
        bytes: ByteArray,
        suffix: String,
    ): VexStreamTrack =
        VexStreamTrack(
            trackRef =
                VexStreamTrackRef(
                    "device-storage.$suffix",
                ),
            mediaSha256 =
                bytes.sha256(),
            mediaByteLength =
                bytes.size.toLong(),
            title =
                "Device Storage $suffix",
            artist = "Vex",
            album = "VexStream Mobile",
        )

    private fun ByteArray.sha256(): VexStreamMediaSha256 =
        VexStreamMediaSha256(
            MessageDigest
                .getInstance(
                    "SHA-256",
                )
                .digest(
                    this,
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

// [VXG RealForever]
