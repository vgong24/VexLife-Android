package com.vextreme.vexlife.android.screens.shared.maps

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class VexMapsPlaceSearchPack(
    val database: File,
    val byteLength: Long,
    val lastModifiedMillis: Long,
) {
    val identity: String =
        database.absolutePath +
            ":" +
            byteLength +
            ":" +
            lastModifiedMillis
}

data class VexMapsPlaceSearchResult(
    val placeRef: String,
    val name: String,
    val subtitle: String?,
    val coordinate: VexMapCoordinate,
) {
    init {
        require(placeRef.isNotBlank())
        require(name.isNotBlank())
    }
}

/**
 * App-private local place-search custody.
 *
 * A place pack is a prepared SQLite database. VexMaps does not require a geocoding API or network
 * access to search it. The pack must expose:
 *
 * - vex_place_pack_meta(key TEXT PRIMARY KEY, value TEXT), including
 *   schemaRef=vex.maps.place-pack/v1;
 * - vex_places_fts, an FTS3/4-compatible table with columns:
 *   place_ref, name, subtitle, latitude, longitude, search_text.
 *
 * [search_text] is prepared by the pack producer; this consumer only forms safe prefix tokens.
 */
internal class VexMapsPlaceSearchStore(
    context: Context,
) :
    VexMapsPlaceSearchSource {
    private val appContext =
        context.applicationContext
    private val placeDirectory =
        File(
            appContext.filesDir,
            "vexmaps/places/current",
        )
    private val placeDatabase =
        File(
            placeDirectory,
            "places.sqlite",
        )
    private val staging =
        File(
            placeDirectory,
            "places.sqlite.importing",
        )
    private val backup =
        File(
            placeDirectory,
            "places.sqlite.previous",
        )

    init {
        replacementGate.recoverOnce {
            recoverVexMapsInterruptedPackReplacement(
                current = placeDatabase,
                staging = staging,
                backup = backup,
            )
        }
    }

    override fun currentPack(): VexMapsPlaceSearchPack? =
        placeDatabase
            .takeIf { file ->
                file.isFile &&
                    file.canRead() &&
                    file.length() > 0L
            }
            ?.toPack()

    override suspend fun import(
        source: Uri,
    ): VexMapsPlaceSearchPack =
        withContext(Dispatchers.IO) {
            replacementGate.withExclusiveReplacement {
                check(
                    placeDirectory.isDirectory ||
                        placeDirectory.mkdirs(),
                ) {
                    "VexMaps place-search directory could not be created"
                }

                staging.delete()
                backup.delete()

                val previousPack =
                    currentPack()

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
                                "selected place index could not be opened",
                            )

                    require(copied > 0L) {
                        "selected place index is empty"
                    }

                    validatePack(staging)

                    if (
                        previousPack != null &&
                        previousPack.byteLength ==
                        copied
                    ) {
                        ensureVexMapsPackReplacementTimestamp(
                            staging = staging,
                            previousLastModifiedMillis =
                                previousPack
                                    .lastModifiedMillis,
                        )
                    }

                    if (placeDatabase.exists()) {
                        check(
                            placeDatabase.renameTo(
                                backup,
                            ),
                        ) {
                            "existing place index could not be staged for replacement"
                        }
                    }

                    if (
                        !staging.renameTo(
                            placeDatabase,
                        )
                    ) {
                        if (backup.exists()) {
                            backup.renameTo(
                                placeDatabase,
                            )
                        }
                        error(
                            "new place index could not be installed",
                        )
                    }

                    val installed =
                        requireNotNull(
                            currentPack(),
                        ) {
                            "installed place index is not readable"
                        }

                    backup.delete()
                    installed
                } finally {
                    staging.delete()
                    if (
                        backup.exists() &&
                        !placeDatabase.exists()
                    ) {
                        backup.renameTo(
                            placeDatabase,
                        )
                    }
                }
            }
        }

    override suspend fun search(
        pack: VexMapsPlaceSearchPack,
        query: String,
        limit: Int,
    ): List<VexMapsPlaceSearchResult> =
        withContext(Dispatchers.IO) {
            require(limit in 1..20)
            require(
                pack.database.isFile &&
                    pack.database.canRead() &&
                    pack.database.length() ==
                    pack.byteLength &&
                    pack.database.lastModified() ==
                    pack.lastModifiedMillis,
            ) {
                "place index identity changed"
            }

            val matchQuery =
                vexMapsPlaceMatchQuery(query)
                    ?: return@withContext emptyList()

            SQLiteDatabase.openDatabase(
                pack.database.absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY,
            ).use { database ->
                require(
                    metadataValue(
                        database,
                        "schemaRef",
                    ) ==
                        PLACE_PACK_SCHEMA_REF,
                ) {
                    "place index schema is not supported"
                }

                database.rawQuery(
                    """
                    SELECT place_ref, name, subtitle, latitude, longitude
                    FROM vex_places_fts
                    WHERE vex_places_fts MATCH ?
                    ORDER BY
                        CASE
                            WHEN name LIKE ? COLLATE NOCASE THEN 0
                            ELSE 1
                        END,
                        length(name),
                        name COLLATE NOCASE
                    LIMIT ?
                    """.trimIndent(),
                    arrayOf(
                        matchQuery,
                        query.trim() + "%",
                        limit.toString(),
                    ),
                ).use { cursor ->
                    buildList {
                        while (cursor.moveToNext()) {
                            val placeRef =
                                cursor.getString(0)
                            val name =
                                cursor.getString(1)
                            val subtitle =
                                if (cursor.isNull(2)) {
                                    null
                                } else {
                                    cursor.getString(2)
                                }
                            val latitude =
                                cursor.getDouble(3)
                            val longitude =
                                cursor.getDouble(4)

                            add(
                                VexMapsPlaceSearchResult(
                                    placeRef = placeRef,
                                    name = name,
                                    subtitle = subtitle,
                                    coordinate =
                                        VexMapCoordinate(
                                            latitude =
                                                latitude,
                                            longitude =
                                                longitude,
                                        ),
                                ),
                            )
                        }
                    }
                }
            }
        }

    private fun validatePack(
        file: File,
    ) {
        SQLiteDatabase.openDatabase(
            file.absolutePath,
            null,
            SQLiteDatabase.OPEN_READONLY,
        ).use { database ->
            require(
                metadataValue(
                    database,
                    "schemaRef",
                ) ==
                    PLACE_PACK_SCHEMA_REF,
            ) {
                "place index schema is not supported"
            }

            database.rawQuery(
                """
                SELECT place_ref, name, subtitle, latitude, longitude
                FROM vex_places_fts
                LIMIT 1
                """.trimIndent(),
                null,
            ).use { cursor ->
                require(
                    cursor.moveToFirst(),
                ) {
                    "place index contains no places"
                }

                VexMapsPlaceSearchResult(
                    placeRef =
                        cursor.getString(0),
                    name =
                        cursor.getString(1),
                    subtitle =
                        if (cursor.isNull(2)) {
                            null
                        } else {
                            cursor.getString(2)
                        },
                    coordinate =
                        VexMapCoordinate(
                            latitude =
                                cursor.getDouble(3),
                            longitude =
                                cursor.getDouble(4),
                        ),
                )
            }
        }
    }

    private fun metadataValue(
        database: SQLiteDatabase,
        key: String,
    ): String? =
        database.query(
            "vex_place_pack_meta",
            arrayOf("value"),
            "key = ?",
            arrayOf(key),
            null,
            null,
            null,
            "1",
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getString(0)
            } else {
                null
            }
        }

    private fun File.toPack() =
        VexMapsPlaceSearchPack(
            database = this,
            byteLength = length(),
            lastModifiedMillis =
                lastModified(),
        )

    private companion object {
        val replacementGate =
            VexMapsPackReplacementGate()
    }
}

internal fun vexMapsPlaceMatchQuery(
    query: String,
): String? {
    val tokens =
        VEX_PLACE_TOKEN_REGEX
            .findAll(
                query
                    .lowercase(
                        Locale.ROOT,
                    ),
            )
            .map { match ->
                match.value
                    .take(
                        MAX_PLACE_TOKEN_LENGTH,
                    )
            }
            .filter {
                it.isNotBlank()
            }
            .take(
                MAX_PLACE_QUERY_TOKENS,
            )
            .toList()

    if (tokens.isEmpty()) {
        return null
    }

    return tokens.joinToString(
        separator = " AND ",
    ) { token ->
        "$token*"
    }
}

private const val PLACE_PACK_SCHEMA_REF =
    "vex.maps.place-pack/v1"

private const val MAX_PLACE_QUERY_TOKENS =
    8

private const val MAX_PLACE_TOKEN_LENGTH =
    48

private val VEX_PLACE_TOKEN_REGEX =
    Regex(
        """[\p{L}\p{N}]+""",
    )

// [VXG RealForever]
