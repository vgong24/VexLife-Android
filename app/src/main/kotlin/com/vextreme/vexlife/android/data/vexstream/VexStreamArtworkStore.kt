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
 * The local filename binds the portable media digest together with the exact admitted HTTPS artwork
 * URL. The media digest is an association address here, not artwork identity; changing the artwork
 * URL intentionally selects a different local asset.
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
                    "Unable to replace prior VexStream artwork"
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
    ): File? {
        val file =
            targetFile(track)
                ?: return null

        return file.takeIf {
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
     * Removes every artwork snapshot associated with one exact media object.
     *
     * This is intentionally broader than one source URL so stale artwork revisions do not accumulate
     * after the final local copy for that media object is removed.
     */
    fun remove(
        mediaSha256: VexStreamMediaSha256,
    ): Boolean {
        if (!root.isDirectory) {
            return true
        }

        val prefix =
            mediaSha256.value +
                "-"
        return root
            .listFiles()
            .orEmpty()
            .filter {
                it.name.startsWith(
                    prefix,
                ) &&
                    (
                        it.name.endsWith(
                            ARTWORK_EXTENSION,
                        ) ||
                            it.name.endsWith(
                                ARTWORK_EXTENSION +
                                    ".tmp",
                            )
                    )
            }
            .all {
                !it.exists() ||
                    it.delete()
            }
    }

    private fun targetFile(
        track: VexStreamTrack,
    ): File? {
        val artworkUrl =
            track.artworkUrl
                ?: return null

        val urlDigest =
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

        return File(
            root,
            track.mediaSha256.value +
                "-" +
                urlDigest +
                ARTWORK_EXTENSION,
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
