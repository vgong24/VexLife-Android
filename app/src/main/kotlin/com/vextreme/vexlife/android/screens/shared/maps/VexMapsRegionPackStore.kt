package com.vextreme.vexlife.android.screens.shared.maps

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class VexMapsRegionPack(
    val tileExtract: File,
    val byteLength: Long,
    val lastModifiedMillis: Long,
) {
    val identity: String =
        tileExtract.absolutePath +
            ":" +
            byteLength +
            ":" +
            lastModifiedMillis
}

/**
 * App-private custody for the currently selected offline Valhalla routing region.
 *
 * The document picker grants access only long enough to copy the selected bytes. Navigation then
 * uses the private copy and no longer depends on the source document/provider URI.
 */
internal class VexMapsRegionPackStore(
    context: Context,
) {
    private val appContext =
        context.applicationContext
    private val regionDirectory =
        File(
            appContext.filesDir,
            "vexmaps/regions/current",
        )
    private val tileExtract =
        File(
            regionDirectory,
            "valhalla_tiles.tar",
        )

    fun currentPack(): VexMapsRegionPack? =
        tileExtract
            .takeIf { file ->
                file.isFile &&
                    file.canRead() &&
                    file.length() > 0L
            }
            ?.toPack()

    suspend fun import(
        source: Uri,
    ): VexMapsRegionPack =
        withContext(Dispatchers.IO) {
            check(
                regionDirectory.isDirectory ||
                    regionDirectory.mkdirs(),
            ) {
                "VexMaps region directory could not be created"
            }

            val staging =
                File(
                    regionDirectory,
                    "valhalla_tiles.tar.importing",
                )
            val backup =
                File(
                    regionDirectory,
                    "valhalla_tiles.tar.previous",
                )

            staging.delete()
            backup.delete()

            try {
                val copied =
                    appContext
                        .contentResolver
                        .openInputStream(source)
                        ?.use { input ->
                            FileOutputStream(
                                staging,
                            ).use { output ->
                                input.copyTo(output)
                                output.fd.sync()
                            }
                            staging.length()
                        }
                        ?: error(
                            "selected region could not be opened",
                        )

                require(copied > 0L) {
                    "selected region is empty"
                }

                if (tileExtract.exists()) {
                    check(
                        tileExtract.renameTo(
                            backup,
                        ),
                    ) {
                        "existing region could not be staged for replacement"
                    }
                }

                if (
                    !staging.renameTo(
                        tileExtract,
                    )
                ) {
                    if (backup.exists()) {
                        backup.renameTo(
                            tileExtract,
                        )
                    }
                    error(
                        "new region could not be installed",
                    )
                }

                val installed =
                    requireNotNull(
                        currentPack(),
                    ) {
                        "installed region is not readable"
                    }

                backup.delete()
                installed
            } finally {
                staging.delete()
                if (
                    backup.exists() &&
                    !tileExtract.exists()
                ) {
                    backup.renameTo(
                        tileExtract,
                    )
                }
            }
        }

    private fun File.toPack() =
        VexMapsRegionPack(
            tileExtract = this,
            byteLength = length(),
            lastModifiedMillis =
                lastModified(),
        )
}

// [VXG RealForever]
