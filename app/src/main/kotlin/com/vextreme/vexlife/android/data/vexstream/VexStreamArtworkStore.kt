package com.vextreme.vexlife.android.data.vexstream

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest

/**
 * Persistent app-owned companion artwork for downloaded VexStream media.
 *
 * Audio bytes keep their existing verified MediaStore owner. Artwork is ancillary presentation
 * data: a missing or corrupt artwork file must never invalidate an otherwise verified audio copy.
 *
 * One exact admitted artwork source maps to one local asset. Existing external HTTPS sources keep
 * their URL-derived cache identity; Desktop-embedded artwork uses its opaque paired-producer ref.
 * Multiple tracks may share one source without duplicating the same image per MP3. The source digest
 * is a stable local association key, not media identity and not an authorization credential.
 */
class VexStreamArtworkStore(
    private val root: File,
    private val maxBytes: Long = MAX_ARTWORK_BYTES,
) {
    init {
        require(maxBytes > 0L) {
            "VexStream artwork maxBytes must be positive"
        }
    }

    fun store(
        track: VexStreamTrack,
        input: InputStream,
    ): File? {
        fileFor(track)
            ?.let {
                return it
            }

        val target =
            targetFile(track)
                ?: return null
        val temp =
            File(
                root,
                target.name + ".tmp",
            )

        return runCatching {
            ensureRoot()

            if (temp.exists()) {
                check(temp.delete()) {
                    "Unable to replace stale VexStream artwork staging file"
                }
            }

            var byteCount = 0L
            FileOutputStream(temp).use { output ->
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

                    byteCount += read
                    check(
                        byteCount <=
                            maxBytes,
                    ) {
                        "VexStream artwork exceeds bounded local size"
                    }
                    output.write(
                        buffer,
                        0,
                        read,
                    )
                }

                check(
                    byteCount > 0L,
                ) {
                    "VexStream artwork must not be empty"
                }
                output.fd.sync()
            }

            if (target.exists()) {
                check(target.delete()) {
                    "Unable to replace invalid prior VexStream artwork"
                }
            }
            check(
                temp.renameTo(
                    target,
                ),
            ) {
                "Unable to promote VexStream artwork"
            }

            target
        }.getOrNull()
            .also {
                if (temp.exists()) {
                    temp.delete()
                }
            }
    }

    fun fileFor(
        track: VexStreamTrack,
    ): File? =
        track.artworkUrl
            ?.let(
                ::fileForArtworkUrl,
            )
            ?: track.artworkRef
                ?.let(
                    ::fileForArtworkRef,
                )

    /**
     * Resolves one already-persisted artwork asset directly from the admitted source URL.
     *
     * Widget/playback projections may know the current Media3 artwork URI without reconstructing a
     * full [VexStreamTrack]. This remains a read-only lookup; it never fetches or creates artwork.
     */
    fun fileForArtworkUrl(
        artworkUrl: String,
    ): File? {
        if (
            !artworkUrl.startsWith(
                "https://",
            )
        ) {
            return null
        }

        val file =
            targetFile(
                artworkUrl,
            )

        return file.takeIf {
            it.isFile &&
                it.length() in
                1L..maxBytes
        }
    }

    /**
     * Resolves one already-persisted embedded-artwork asset from its opaque producer ref.
     *
     * The ref is not a network URL and carries no credential. Fetching remains repository-owned.
     */
    fun fileForArtworkRef(
        artworkRef: String,
    ): File? {
        if (
            artworkRef.isBlank() ||
            artworkRef.contains('/') ||
            artworkRef.contains('\\')
        ) {
            return null
        }

        val file =
            targetFileForArtworkRef(
                artworkRef,
            )

        return file.takeIf {
            it.isFile &&
                it.length() in
                1L..maxBytes
        }
    }

    /**
     * Resolves the current Media3 artwork URI without giving widget/presentation network authority.
     *
     * HTTPS continues to map through the admitted source URL. file:// is accepted only when it
     * points directly at this store's own root.
     */
    fun fileForPlaybackArtworkUri(
        artworkUri: String,
    ): File? {
        if (
            artworkUri.startsWith(
                "https://",
            )
        ) {
            return fileForArtworkUrl(
                artworkUri,
            )
        }
        if (
            !artworkUri.startsWith(
                "file:",
            )
        ) {
            return null
        }

        val requestedFile =
            runCatching {
                File(
                    java.net.URI(
                        artworkUri,
                    ),
                )
            }.getOrNull()
                ?: return null
        val canonicalCandidate =
            runCatching {
                requestedFile.canonicalFile
            }.getOrNull()
                ?: return null
        val canonicalRoot =
            runCatching {
                root.canonicalFile
            }.getOrNull()
                ?: return null

        if (
            canonicalCandidate.parentFile !=
            canonicalRoot
        ) {
            return null
        }

        val ownedFile =
            File(
                root,
                canonicalCandidate.name,
            )
        if (
            runCatching {
                ownedFile.canonicalFile
            }.getOrNull() !=
            canonicalCandidate
        ) {
            return null
        }

        return ownedFile.takeIf {
            it.isFile &&
                it.length() in
                1L..maxBytes
        }
    }

    fun open(
        track: VexStreamTrack,
    ): InputStream? =
        fileFor(track)
            ?.let(
                ::FileInputStream,
            )

    /**
     * Removes artwork files no downloaded track still references.
     *
     * Cleanup is driven by the current downloaded-track set rather than one song's media identity so
     * shared album art survives removing any one of several songs that still use it.
     */
    fun pruneTo(
        downloadedTracks: Iterable<VexStreamTrack>,
    ): Boolean {
        if (!root.isDirectory) {
            return true
        }

        val retainedNames =
            downloadedTracks
                .mapNotNull(
                    ::targetFile,
                )
                .map {
                    it.name
                }
                .toSet()

        var success = true
        root.listFiles()
            .orEmpty()
            .filter {
                (
                    it.name.endsWith(
                        ARTWORK_EXTENSION,
                    ) &&
                        it.name !in
                        retainedNames
                ) ||
                    it.name.endsWith(
                        ARTWORK_EXTENSION +
                            ".tmp",
                    )
            }
            .forEach {
                if (
                    it.exists() &&
                    !it.delete()
                ) {
                    success = false
                }
            }

        return success
    }

    private fun targetFile(
        track: VexStreamTrack,
    ): File? =
        track.artworkUrl
            ?.let(
                ::targetFile,
            )
            ?: track.artworkRef
                ?.let(
                    ::targetFileForArtworkRef,
                )

    private fun targetFile(
        artworkUrl: String,
    ): File =
        File(
            root,
            sourceDigest(
                artworkUrl,
            ) +
                ARTWORK_EXTENSION,
        )

    private fun targetFileForArtworkRef(
        artworkRef: String,
    ): File =
        File(
            root,
            sourceDigest(
                EMBEDDED_ARTWORK_KEY_PREFIX +
                    artworkRef,
            ) +
                ARTWORK_EXTENSION,
        )

    private fun sourceDigest(
        artworkUrl: String,
    ): String =
        MessageDigest
            .getInstance(
                "SHA-256",
            )
            .digest(
                artworkUrl
                    .toByteArray(
                        Charsets.UTF_8,
                    ),
            )
            .joinToString(
                separator = "",
            ) { byte ->
                "%02x".format(
                    byte,
                )
            }

    private fun ensureRoot() {
        if (root.exists()) {
            require(
                root.isDirectory,
            ) {
                "VexStream artwork root must be a directory"
            }
            return
        }

        check(
            root.mkdirs(),
        ) {
            "Unable to create VexStream artwork root"
        }
    }

    companion object {
        const val MAX_ARTWORK_BYTES: Long =
            8L * 1024L * 1024L

        private const val ARTWORK_EXTENSION =
            ".artwork"
        private const val EMBEDDED_ARTWORK_KEY_PREFIX =
            "vexstream.embedded-artwork:"

        fun production(
            contextFilesDir: File,
        ): VexStreamArtworkStore =
            VexStreamArtworkStore(
                File(
                    contextFilesDir,
                    "vexstream/artwork",
                ),
            )
    }
}

// [VXG RealForever]
