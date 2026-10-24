package com.vextreme.vexlife.android.data.vexstream

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties

/**
 * App-private durable index for verified phone-local VexStream media.
 *
 * The media bytes remain owned by [VexStreamMediaStore]. This file only preserves enough sanitized
 * track metadata to rediscover verified local copies after process death while the desktop Hub may
 * be unavailable.
 */
class VexStreamOfflineIndex(
    private val indexFile: File,
    private val mediaStore: VexStreamMediaStore,
) {
    fun load(): VexStreamMusicState {
        if (!indexFile.isFile) {
            return VexStreamMusicState.Empty
        }

        return runCatching {
            val properties = Properties()
            FileInputStream(indexFile).use { input ->
                properties.load(input)
            }

            val storedSchemaVersion =
                properties.getProperty(
                    "schemaVersion",
                )
            check(
                storedSchemaVersion == SCHEMA_VERSION ||
                    storedSchemaVersion == LEGACY_SCHEMA_VERSION
            ) {
                "unsupported VexStream offline index"
            }
            var migrationNeeded =
                storedSchemaVersion !=
                    SCHEMA_VERSION

            val count = properties.getProperty("count")
                ?.toIntOrNull()
                ?.coerceAtLeast(0)
                ?: 0

            val tracks = buildList {
                repeat(count) { index ->
                    val prefix = "entry.$index."
                    val digest = VexStreamMediaSha256(
                        properties.require(prefix + "mediaSha256"),
                    )
                    val byteLength = properties.require(prefix + "mediaByteLength")
                        .toLong()
                    val receiptVersion =
                        properties
                            .getProperty(
                                prefix +
                                    "verificationMediaStoreVersion",
                            )
                            ?.takeIf {
                                it.isNotBlank()
                            }
                    val receiptGeneration =
                        properties
                            .getProperty(
                                prefix +
                                    "verificationGenerationModified",
                            )
                            ?.toLongOrNull()
                    val localCopy =
                        VexStreamLocalCopyState.Downloaded(
                            mediaSha256 = digest,
                            byteLength = byteLength,
                            localMediaRef =
                                properties.require(
                                    prefix +
                                        "localMediaRef",
                                ),
                            verificationReceipt =
                                if (
                                    receiptVersion != null &&
                                    receiptGeneration != null
                                ) {
                                    VexStreamLocalCopyVerificationReceipt(
                                        mediaStoreVersion =
                                            receiptVersion,
                                        generationModified =
                                            receiptGeneration,
                                    )
                                } else {
                                    null
                                },
                        )

                    val restoredCopy =
                        mediaStore
                            .restorePersistedCopy(
                                localCopy,
                            )
                            ?: return@repeat
                    if (
                        restoredCopy !=
                        localCopy
                    ) {
                        migrationNeeded = true
                    }

                    val playbackEnd = properties.getProperty(prefix + "playbackEndSeconds")
                        ?.takeIf { it.isNotBlank() }
                        ?.toDouble()

                    val track = VexStreamTrack(
                        trackRef = VexStreamTrackRef(
                            properties.require(prefix + "trackRef"),
                        ),
                        mediaSha256 = digest,
                        mediaByteLength = byteLength,
                        title = properties.require(prefix + "title"),
                        artist = properties.getProperty(prefix + "artist").orEmpty(),
                        album = properties.getProperty(prefix + "album").orEmpty(),
                        genre = properties.getProperty(prefix + "genre").orEmpty(),
                        year = properties.getProperty(prefix + "year").orEmpty(),
                        durationSeconds = properties.getProperty(prefix + "durationSeconds")
                            ?.toDoubleOrNull()
                            ?: 0.0,
                        playbackStartSeconds =
                            properties.getProperty(prefix + "playbackStartSeconds")
                                ?.toDoubleOrNull()
                                ?: 0.0,
                        playbackEndSeconds = playbackEnd,
                    )

                    add(
                        VexStreamTrackState(
                            track = track,
                            hubAvailable = false,
                            localCopy = restoredCopy,
                        ),
                    )
                }
            }

            val restored =
                VexStreamMusicState(
                    hubState = VexStreamHubState.NotConfigured,
                    tracks = tracks,
                )

            if (migrationNeeded) {
                persist(
                    restored,
                )
            }

            restored
        }.getOrElse {
            VexStreamMusicState.Empty
        }
    }

    fun persist(
        state: VexStreamMusicState,
    ): Boolean =
        runCatching {
            val downloaded = state.tracks
                .filter { it.localCopy is VexStreamLocalCopyState.Downloaded }
                .sortedBy { it.track.trackRef.value }

            val properties = Properties().apply {
                setProperty("schemaVersion", SCHEMA_VERSION)
                setProperty("count", downloaded.size.toString())

                downloaded.forEachIndexed { index, item ->
                    val prefix = "entry.$index."
                    val local =
                        item.localCopy as VexStreamLocalCopyState.Downloaded
                    val track = item.track

                    setProperty(prefix + "trackRef", track.trackRef.value)
                    setProperty(prefix + "mediaSha256", track.mediaSha256.value)
                    setProperty(prefix + "mediaByteLength", track.mediaByteLength.toString())
                    setProperty(prefix + "localMediaRef", local.localMediaRef)
                    local.verificationReceipt
                        ?.let { receipt ->
                            setProperty(
                                prefix +
                                    "verificationMediaStoreVersion",
                                receipt.mediaStoreVersion,
                            )
                            setProperty(
                                prefix +
                                    "verificationGenerationModified",
                                receipt.generationModified
                                    .toString(),
                            )
                        }
                    setProperty(prefix + "title", track.title)
                    setProperty(prefix + "artist", track.artist)
                    setProperty(prefix + "album", track.album)
                    setProperty(prefix + "genre", track.genre)
                    setProperty(prefix + "year", track.year)
                    setProperty(prefix + "durationSeconds", track.durationSeconds.toString())
                    setProperty(
                        prefix + "playbackStartSeconds",
                        track.playbackStartSeconds.toString(),
                    )
                    setProperty(
                        prefix + "playbackEndSeconds",
                        track.playbackEndSeconds?.toString().orEmpty(),
                    )
                }
            }

            indexFile.parentFile?.mkdirs()
            val temp = File(
                indexFile.parentFile,
                indexFile.name + ".tmp",
            )

            try {
                FileOutputStream(temp).use { output ->
                    properties.store(
                        output,
                        "VexStream offline index",
                    )
                    output.fd.sync()
                }

                if (indexFile.exists()) {
                    check(indexFile.delete()) {
                        "unable to replace prior VexStream offline index"
                    }
                }

                check(
                    temp.renameTo(indexFile),
                ) {
                    "unable to promote VexStream offline index"
                }
            } finally {
                if (temp.exists()) {
                    temp.delete()
                }
            }

            true
        }.getOrDefault(false)

    companion object {
        private const val LEGACY_SCHEMA_VERSION =
            "vexstream.android.offline-index/v1"
        private const val SCHEMA_VERSION =
            "vexstream.android.offline-index/v2"

        fun production(
            contextFilesDir: File,
            mediaStore: VexStreamMediaStore,
        ): VexStreamOfflineIndex =
            VexStreamOfflineIndex(
                indexFile = File(
                    contextFilesDir,
                    "vexstream/offline-index.properties",
                ),
                mediaStore = mediaStore,
            )
    }
}

private fun Properties.require(
    key: String,
): String =
    requireNotNull(
        getProperty(key),
    ) {
        "missing offline index field $key"
    }

// [VXG RealForever]
