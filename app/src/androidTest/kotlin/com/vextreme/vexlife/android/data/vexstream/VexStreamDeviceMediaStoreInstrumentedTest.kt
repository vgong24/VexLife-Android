package com.vextreme.vexlife.android.data.vexstream

import android.os.Build
import android.provider.MediaStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
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
