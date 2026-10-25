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
 * One exact admitted HTTPS artwork source maps to one local asset. Multiple downloaded songs may
 * therefore share one album/cover file without duplicating the same image per MP3. The source digest
 * is a stable local association key, not a claim that the URL itself is immutable content identity.
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
                ::sourceDigest,
            )
            ?.let { digest ->
                File(
                    root,
                    digest +
                        ARTWORK_EXTENSION,
                )
            }

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
