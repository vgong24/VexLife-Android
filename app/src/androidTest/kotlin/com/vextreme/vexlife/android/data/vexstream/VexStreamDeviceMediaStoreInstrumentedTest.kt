package com.vextreme.vexlife.android.data.vexstream

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
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
    fun api30SharedCopyCarriesReusableVerificationReceipt() {
        assumeTrue(
            Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.R,
        )

        val context =
            InstrumentationRegistry
                .getInstrumentation()
                .targetContext
        val bytes =
            "receipt-backed vexstream device music"
                .encodeToByteArray()
        val track =
            track(
                bytes = bytes,
                suffix = "receipt",
            )
        val store =
            VexStreamDeviceMediaStore(
                context,
            )
        val stored =
            store.store(
                track = track,
                input =
                    ByteArrayInputStream(
                        bytes,
                    ),
            ) as VexStreamMediaStoreResult.Stored

        try {
            val receipt =
                requireNotNull(
                    stored.localCopy
                        .verificationReceipt,
                )
            assertTrue(
                receipt.mediaStoreVersion
                    .isNotBlank(),
            )
            assertTrue(
                receipt.generationModified >= 0L,
            )

            val adopted =
                requireNotNull(
                    store.restorePersisted(
                        stored.localCopy.copy(
                            verificationReceipt = null,
                        ),
                    ),
                )
            assertEquals(
                stored.localCopy.localMediaRef,
                adopted.localMediaRef,
            )
            assertEquals(
                stored.localCopy.mediaSha256,
                adopted.mediaSha256,
            )
            assertTrue(
                adopted.verificationReceipt !=
                    null,
            )

            assertEquals(
                stored.localCopy.localMediaRef,
                store.playbackUri(
                    adopted,
                ),
            )
        } finally {
            assertTrue(
                store.remove(
                    stored.localCopy,
                ),
            )
        }
    }

    @Test
    fun missingReceiptCannotAdoptCorruptSameLengthBytes() {
        assumeTrue(
            Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.R,
        )

        val context =
            InstrumentationRegistry
                .getInstrumentation()
                .targetContext
        val bytes =
            "legacy receipt bootstrap bytes"
                .encodeToByteArray()
        val track =
            track(
                bytes = bytes,
                suffix = "missing-receipt-corrupt",
            )
        val store =
            VexStreamDeviceMediaStore(
                context,
            )
        val stored =
            store.store(
                track = track,
                input =
                    ByteArrayInputStream(
                        bytes,
                    ),
            ) as VexStreamMediaStoreResult.Stored
        val uri =
            android.net.Uri.parse(
                stored.localCopy.localMediaRef,
            )

        try {
            requireNotNull(
                context.contentResolver
                    .openFileDescriptor(
                        uri,
                        "w",
                    ),
            ).use { descriptor ->
                FileOutputStream(
                    descriptor.fileDescriptor,
                ).use { output ->
                    output.write(
                        ByteArray(
                            bytes.size,
                        ) {
                            0
                        },
                    )
                    output.flush()
                    output.fd.sync()
                }
            }

            assertEquals(
                null,
                store.restorePersisted(
                    stored.localCopy.copy(
                        verificationReceipt = null,
                    ),
                ),
            )
        } finally {
            context.contentResolver
                .delete(
                    uri,
                    null,
                    null,
                )
        }
    }

    @Test
    fun staleReceiptFallsBackToExactBytesBeforeReadmission() {
        assumeTrue(
            Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.R,
        )

        val context =
            InstrumentationRegistry
                .getInstrumentation()
                .targetContext
        val bytes =
            "stale receipt exact bytes"
                .encodeToByteArray()
        val track =
            track(
                bytes = bytes,
                suffix = "stale-receipt",
            )
        val store =
            VexStreamDeviceMediaStore(
                context,
            )
        val stored =
            store.store(
                track = track,
                input =
                    ByteArrayInputStream(
                        bytes,
                    ),
            ) as VexStreamMediaStoreResult.Stored
        val uri =
            android.net.Uri.parse(
                stored.localCopy.localMediaRef,
            )

        try {
            advanceVerificationGeneration(
                context = context,
                uri = uri,
            )

            val refreshed =
                requireNotNull(
                    store.restorePersisted(
                        stored.localCopy,
                    ),
                )
            assertTrue(
                refreshed.verificationReceipt !=
                    stored.localCopy
                        .verificationReceipt,
            )

            val corrupt =
                ByteArray(
                    bytes.size,
                ) {
                    0
                }
            requireNotNull(
                context.contentResolver
                    .openFileDescriptor(
                        uri,
                        "w",
                    ),
            ).use { descriptor ->
                FileOutputStream(
                    descriptor.fileDescriptor,
                ).use { output ->
                    output.write(
                        corrupt,
                    )
                    output.flush()
                    output.fd.sync()
                }
            }
            advanceVerificationGeneration(
                context = context,
                uri = uri,
            )

            assertEquals(
                null,
                store.restorePersisted(
                    refreshed,
                ),
            )
        } finally {
            context.contentResolver
                .delete(
                    uri,
                    null,
                    null,
                )
        }
    }

    @Test
    fun migratedPrivateCopyPublishesDurablyWithoutRedownload() {
        val instrumentation =
            InstrumentationRegistry
                .getInstrumentation()
        val context =
            instrumentation.targetContext

        if (
            Build.VERSION.SDK_INT <=
            Build.VERSION_CODES.P
        ) {
            grantLegacyStoragePermission(
                instrumentation = instrumentation,
                packageName = context.packageName,
            )
        }

        val bytes =
            "migrate verified private vexstream music"
                .encodeToByteArray()
        val track =
            track(
                bytes = bytes,
                suffix = "migration",
            )
        val privateRoot =
            File(
                context.cacheDir,
                "vexstream-private-migration-proof/" +
                    System.nanoTime(),
            )
        val privateStore =
            VexStreamMediaStore(
                privateRoot,
            )
        val privateCopy =
            (
                privateStore.store(
                    expectedSha256 =
                        track.mediaSha256,
                    expectedByteLength =
                        track.mediaByteLength,
                    input =
                        ByteArrayInputStream(
                            bytes,
                        ),
                ) as VexStreamMediaStoreResult.Stored
            ).localCopy

        val migrationStore =
            VexStreamMediaStore(
                root = privateRoot,
                deviceStore =
                    VexStreamDeviceMediaStore(
                        context,
                    ),
            )

        try {
            assertEquals(
                VexStreamMediaStoreResult.Rejected(
                    VexStreamMediaStoreRejection
                        .EXISTING_COPY_INVALID,
                ),
                migrationStore.migratePrivateCopy(
                    track =
                        track.copy(
                            mediaByteLength =
                                track.mediaByteLength + 1L,
                        ),
                    localCopy =
                        privateCopy,
                ),
            )
            assertTrue(
                privateStore.verify(
                    privateCopy,
                ),
            )

            val migrated =
                migrationStore.migratePrivateCopy(
                    track = track,
                    localCopy = privateCopy,
                ) as VexStreamMediaStoreResult.Stored

            assertTrue(
                migrated.localCopy.localMediaRef
                    .startsWith(
                        "content://",
                    ),
            )
            assertTrue(
                migrationStore.verify(
                    migrated.localCopy,
                ),
            )
            assertFalse(
                privateStore.verify(
                    privateCopy,
                ),
            )
            migrationStore.open(
                migrated.localCopy,
            ).use { input ->
                assertArrayEquals(
                    bytes,
                    input.readBytes(),
                )
            }

            assertTrue(
                migrationStore.remove(
                    migrated.localCopy,
                ),
            )
        } finally {
            privateRoot.deleteRecursively()
        }
    }

    @Test
    fun rediscoveredVerifiedSharedCopyUsesFreshContentUriLookup() {
        val instrumentation =
            InstrumentationRegistry
                .getInstrumentation()
        val context =
            instrumentation.targetContext

        if (
            Build.VERSION.SDK_INT <=
            Build.VERSION_CODES.P
        ) {
            grantLegacyStoragePermission(
                instrumentation = instrumentation,
                packageName = context.packageName,
            )
        }

        val bytes =
            "rediscover durable vexstream device music"
                .encodeToByteArray()
        val track =
            track(
                bytes = bytes,
                suffix = "rediscovery",
            )
        val firstStore =
            VexStreamDeviceMediaStore(
                context,
            )
        val stored =
            firstStore.store(
                track = track,
                input =
                    ByteArrayInputStream(
                        bytes,
                    ),
            ) as VexStreamMediaStoreResult.Stored

        try {
            assertEquals(
                null,
                VexStreamDeviceMediaStore(
                    context,
                ).rediscover(
                    expectedSha256 =
                        track.mediaSha256,
                    expectedByteLength =
                        track.mediaByteLength + 1L,
                ),
            )

            val rediscovered =
                requireNotNull(
                    VexStreamDeviceMediaStore(
                        context,
                    ).rediscover(
                        expectedSha256 =
                            track.mediaSha256,
                        expectedByteLength =
                            track.mediaByteLength,
                    ),
                )

            assertEquals(
                stored.localCopy,
                rediscovered,
            )
            assertTrue(
                VexStreamDeviceMediaStore(
                    context,
                ).verify(
                    rediscovered,
                ),
            )
        } finally {
            assertTrue(
                firstStore.remove(
                    stored.localCopy,
                ),
            )
        }
    }

    @Test
    fun sharedCopySurvivesPrivateStagingLossAndFreshStoreInstance() {
        assumeTrue(
            Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.Q,
        )

        val context =
            InstrumentationRegistry
                .getInstrumentation()
                .targetContext
        val bytes =
            "durable shared copy after private staging loss"
                .encodeToByteArray()
        val track =
            track(
                bytes = bytes,
                suffix = "fresh-store",
            )
        val privateRoot =
            File(
                context.cacheDir,
                "vexstream-device-store-proof/" +
                    System.nanoTime(),
            )
        val firstStore =
            VexStreamMediaStore(
                root = privateRoot,
                deviceStore =
                    VexStreamDeviceMediaStore(
                        context,
                    ),
            )
        val result =
            firstStore.store(
                expectedSha256 =
                    track.mediaSha256,
                expectedByteLength =
                    track.mediaByteLength,
                input =
                    ByteArrayInputStream(
                        bytes,
                    ),
                track = track,
            ) as VexStreamMediaStoreResult.Stored

        try {
            assertTrue(
                result.localCopy.localMediaRef
                    .startsWith(
                        "content://",
                    ),
            )

            privateRoot.deleteRecursively()

            val freshStore =
                VexStreamMediaStore(
                    root = privateRoot,
                    deviceStore =
                        VexStreamDeviceMediaStore(
                            context,
                        ),
                )

            assertTrue(
                freshStore.verify(
                    result.localCopy,
                ),
            )
            freshStore.open(
                result.localCopy,
            ).use { input ->
                assertArrayEquals(
                    bytes,
                    input.readBytes(),
                )
            }
            assertEquals(
                result.localCopy.localMediaRef,
                freshStore.contentUriForPlayback(
                    result.localCopy,
                ),
            )
        } finally {
            val cleanupStore =
                VexStreamMediaStore(
                    root = privateRoot,
                    deviceStore =
                        VexStreamDeviceMediaStore(
                            context,
                        ),
                )
            assertTrue(
                cleanupStore.remove(
                    result.localCopy,
                ),
            )
            privateRoot.deleteRecursively()
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
            "different vexstream byte"
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
    fun wrongMimeRowRespectsProviderNormalizedDeterministicIdentity() {
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
        val expectedDisplayName =
            track.mediaSha256.value +
                ".mp3"
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
                            expectedDisplayName,
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

            val providerIdentity =
                context.contentResolver
                    .query(
                        uri,
                        arrayOf(
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
                        Pair(
                            cursor.getString(
                                cursor.getColumnIndexOrThrow(
                                    MediaStore.Audio.Media.DISPLAY_NAME,
                                ),
                            ),
                            cursor.getString(
                                cursor.getColumnIndexOrThrow(
                                    MediaStore.Audio.Media.MIME_TYPE,
                                ),
                            ),
                        )
                    }
                    ?: error(
                        "wrong-MIME row was not readable",
                    )

            assertEquals(
                "audio/flac",
                providerIdentity.second,
            )

            val store =
                VexStreamDeviceMediaStore(
                    context,
                )
            val storeResult =
                store.store(
                    track = track,
                    input =
                        ByteArrayInputStream(
                            bytes,
                        ),
                )

            if (
                providerIdentity.first ==
                expectedDisplayName
            ) {
                assertEquals(
                    VexStreamMediaStoreResult.Rejected(
                        VexStreamMediaStoreRejection
                            .SHARED_COPY_CONFLICT,
                    ),
                    storeResult,
                )
            } else {
                val stored =
                    storeResult
                        as? VexStreamMediaStoreResult.Stored
                        ?: error(
                            "provider-renamed wrong-MIME row must not block the deterministic MP3",
                        )
                assertTrue(
                    store.verify(
                        stored.localCopy,
                    ),
                )
                assertTrue(
                    store.remove(
                        stored.localCopy,
                    ),
                )
            }

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
                        "provider-created row must remain untouched",
                        cursor.moveToFirst(),
                    )
                }
                ?: error(
                    "provider-created row disappeared",
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
    fun legacyDeniedMigrationPreservesVerifiedPrivateCopy() {
        assumeTrue(
            Build.VERSION.SDK_INT <=
                Build.VERSION_CODES.P,
        )

        val context =
            InstrumentationRegistry
                .getInstrumentation()
                .targetContext
        assumeTrue(
            "legacy migration denial proof requires the fresh disposable target to begin with WRITE_EXTERNAL_STORAGE denied",
            context.checkSelfPermission(
                Manifest.permission
                    .WRITE_EXTERNAL_STORAGE,
            ) != PackageManager.PERMISSION_GRANTED,
        )

        val bytes =
            "legacy denied migration private copy"
                .encodeToByteArray()
        val track =
            track(
                bytes = bytes,
                suffix = "legacy-migration-denied",
            )
        val privateRoot =
            File(
                context.cacheDir,
                "vexstream-private-migration-denied/" +
                    System.nanoTime(),
            )
        val privateStore =
            VexStreamMediaStore(
                privateRoot,
            )
        val privateCopy =
            (
                privateStore.store(
                    expectedSha256 =
                        track.mediaSha256,
                    expectedByteLength =
                        track.mediaByteLength,
                    input =
                        ByteArrayInputStream(
                            bytes,
                        ),
                ) as VexStreamMediaStoreResult.Stored
            ).localCopy

        try {
            val migrationStore =
                VexStreamMediaStore(
                    root = privateRoot,
                    deviceStore =
                        VexStreamDeviceMediaStore(
                            context,
                        ),
                )

            assertEquals(
                VexStreamMediaStoreResult.Rejected(
                    VexStreamMediaStoreRejection
                        .PERMISSION_REQUIRED,
                ),
                migrationStore.migratePrivateCopy(
                    track = track,
                    localCopy = privateCopy,
                ),
            )
            assertTrue(
                privateStore.verify(
                    privateCopy,
                ),
            )
        } finally {
            privateRoot.deleteRecursively()
        }
    }

    @Test
    fun legacyDeniedPermissionFailsBeforeSharedWrite() {
        assumeTrue(
            Build.VERSION.SDK_INT <=
                Build.VERSION_CODES.P,
        )

        val context =
            InstrumentationRegistry
                .getInstrumentation()
                .targetContext
        assumeTrue(
            "legacy denial proof requires the fresh disposable target to begin with WRITE_EXTERNAL_STORAGE denied",
            context.checkSelfPermission(
                Manifest.permission
                    .WRITE_EXTERNAL_STORAGE,
            ) != PackageManager.PERMISSION_GRANTED,
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

    private fun advanceVerificationGeneration(
        context: android.content.Context,
        uri: android.net.Uri,
    ) {
        val before =
            generationModified(
                context = context,
                uri = uri,
            )

        assertEquals(
            1,
            context.contentResolver
                .update(
                    uri,
                    ContentValues().apply {
                        put(
                            MediaStore.Audio.Media.IS_PENDING,
                            1,
                        )
                    },
                    null,
                    null,
                ),
        )
        assertEquals(
            1,
            context.contentResolver
                .update(
                    uri,
                    ContentValues().apply {
                        put(
                            MediaStore.Audio.Media.IS_PENDING,
                            0,
                        )
                    },
                    null,
                    null,
                ),
        )

        assertTrue(
            generationModified(
                context = context,
                uri = uri,
            ) != before,
        )
    }

    private fun generationModified(
        context: android.content.Context,
        uri: android.net.Uri,
    ): Long =
        requireNotNull(
            context.contentResolver
                .query(
                    uri,
                    arrayOf(
                        MediaStore.Audio.Media.GENERATION_MODIFIED,
                    ),
                    null,
                    null,
                    null,
                ),
        ).use { cursor ->
            check(
                cursor.moveToFirst(),
            ) {
                "VexStream receipt test row disappeared"
            }
            cursor.getLong(
                cursor.getColumnIndexOrThrow(
                    MediaStore.Audio.Media.GENERATION_MODIFIED,
                ),
            )
        }

    /*
     * Runtime-permission revocation kills the application UID on legacy Android.
     * The class orders the naturally denied case before the granted case, and
     * native legacy proof runs on a disposable API-28 emulator. Do not revoke
     * these permissions from inside the instrumentation process.
     */
    private fun grantLegacyStoragePermission(
        instrumentation: android.app.Instrumentation,
        packageName: String,
    ) {
        listOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
        ).forEach { permission ->
            executeShellCommandAndWait(
                instrumentation = instrumentation,
                command =
                    "pm grant $packageName $permission",
            )
        }
    }

    private fun executeShellCommandAndWait(
        instrumentation: android.app.Instrumentation,
        command: String,
    ): String =
        ParcelFileDescriptor.AutoCloseInputStream(
            instrumentation.uiAutomation
                .executeShellCommand(
                    command,
                ),
        ).use { input ->
            input.readBytes()
                .decodeToString()
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
