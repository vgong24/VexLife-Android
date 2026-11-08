package com.vextreme.vexlife.android.data.vexstream

import com.vextreme.vexlife.android.behavior.VexPartner
import org.json.JSONObject

/**
 * External-participant vocabulary for one paired VexStream Hub/Desktop.
 *
 * This is not the Android repository, Media3 player, Store, or a mirrored Family.
 *
 * Source
 *     which external Hub authored a statement;
 *
 * Connection.Id
 *     which already-pinned paired endpoint relationship delivered it;
 *
 * Signal
 *     sanitized Hub-owned meaning after provider representation is converted;
 *
 * Conversion
 *     provider representation -> Partner Signal(s).
 *
 * Repository admission and product Family reconciliation remain consumer-owned boundaries.
 */
internal sealed interface VexStreamHubPartner :
    VexPartner<VexStreamHubPartner> {
    data class Source(
        val hubRef:
            String,
    ) :
        VexPartner.Source<
            VexStreamHubPartner
        > {
        init {
            require(
                hubRef.isNotBlank(),
            ) {
                "hubRef must be non-blank"
            }
        }
    }

    sealed interface Signal :
        VexPartner.Signal<
            VexStreamHubPartner
        > {
        val source:
            Source

        data class Catalog(
            override val source:
                Source,
            val catalog:
                VexStreamHubCatalogProjection,
        ) :
            Signal {
            init {
                require(
                    catalog.hubRef ==
                        source.hubRef,
                ) {
                    "catalog hubRef must match Partner Source"
                }
            }
        }
    }

    interface Connection :
        VexPartner.Connection<
            VexStreamHubPartner,
            Signal
        > {
        override val id:
            Id

        /**
         * Current paired-endpoint identity already accepted by VexStream source.
         *
         * The pinned certificate digest, not mutable host/port routing, identifies which paired
         * endpoint relationship this Connection represents.
         */
        data class Id(
            val certificateSha256:
                String,
        ) :
            VexPartner.Connection.Id<
                VexStreamHubPartner
            > {
            init {
                require(
                    certificateSha256.matches(
                        Regex(
                            "^[0-9a-f]{64}$",
                        ),
                    ),
                ) {
                    "certificateSha256 must be a lowercase SHA-256 digest"
                }
            }
        }

        data class Observation(
            override val connectionId:
                Id,
            override val signal:
                Signal,
            override val observedAtEpochMillis:
                Long,
        ) :
            VexPartner.Connection.Observation<
                VexStreamHubPartner,
                Signal
            > {
            init {
                require(
                    observedAtEpochMillis >=
                        0L,
                )
            }
        }

        /**
         * Exact current Hub catalog response -> one sanitized external-participant statement.
         *
         * JSON/HTTP representation does not become Signal identity. The resulting Catalog Signal is
         * rooted by the Hub's own hubRef and preserves the existing sanitized projection contract.
         */
        class CatalogConversion :
            VexPartner.Connection.Conversion<
                String,
                VexStreamHubPartner,
                Signal
            > {
            override fun convert(
                input:
                    String,
            ):
                List<Signal> {
                val catalog =
                    parseCatalog(
                        input,
                    )

                return listOf(
                    Signal.Catalog(
                        source =
                            Source(
                                hubRef =
                                    catalog.hubRef,
                            ),
                        catalog =
                            catalog,
                    ),
                )
            }

            private fun parseCatalog(
                body:
                    String,
            ):
                VexStreamHubCatalogProjection {
                val root =
                    JSONObject(
                        body,
                    )
                val tracksJson =
                    root.getJSONArray(
                        "tracks",
                    )
                val tracks =
                    buildList {
                        repeat(
                            tracksJson.length(),
                        ) { index ->
                            val track =
                                tracksJson
                                    .getJSONObject(
                                        index,
                                    )
                            add(
                                VexStreamHubTrackProjection(
                                    trackRef =
                                        VexStreamTrackRef(
                                            track.getString(
                                                "trackRef",
                                            ),
                                        ),
                                    mediaSha256 =
                                        VexStreamMediaSha256(
                                            track.getString(
                                                "mediaSha256",
                                            ),
                                        ),
                                    mediaByteLength =
                                        track.getLong(
                                            "mediaByteLength",
                                        ),
                                    title =
                                        track.getString(
                                            "title",
                                        ),
                                    artist =
                                        track.optString(
                                            "artist",
                                        ),
                                    artworkUrl =
                                        track.optString(
                                            "artworkUrl",
                                        )
                                            .trim()
                                            .takeIf {
                                                it.startsWith(
                                                    "https://",
                                                )
                                            },
                                    artworkRef =
                                        track.optString(
                                            "artworkRef",
                                        )
                                            .trim()
                                            .takeIf {
                                                it.isNotBlank() &&
                                                    !it.contains(
                                                        '/',
                                                    ) &&
                                                    !it.contains(
                                                        '\\',
                                                    )
                                            },
                                    album =
                                        track.optString(
                                            "album",
                                        ),
                                    genre =
                                        track.optString(
                                            "genre",
                                        ),
                                    year =
                                        track.optString(
                                            "year",
                                        ),
                                    durationSeconds =
                                        track.optDouble(
                                            "durationSeconds",
                                            0.0,
                                        ),
                                    playbackStartSeconds =
                                        track.optDouble(
                                            "playbackStartSeconds",
                                            0.0,
                                        ),
                                    playbackEndSeconds =
                                        if (
                                            track.has(
                                                "playbackEndSeconds",
                                            ) &&
                                            !track.isNull(
                                                "playbackEndSeconds",
                                            )
                                        ) {
                                            track.getDouble(
                                                "playbackEndSeconds",
                                            )
                                        } else {
                                            null
                                        },
                                ),
                            )
                        }
                    }

                val playlistsJson =
                    root.optJSONArray(
                        "playlists",
                    )
                val playlists =
                    buildList {
                        if (
                            playlistsJson !=
                            null
                        ) {
                            repeat(
                                playlistsJson.length(),
                            ) { index ->
                                val playlist =
                                    playlistsJson
                                        .getJSONObject(
                                            index,
                                        )
                                val refsJson =
                                    playlist
                                        .getJSONArray(
                                            "trackRefs",
                                        )
                                val refs =
                                    buildList {
                                        repeat(
                                            refsJson.length(),
                                        ) { refIndex ->
                                            add(
                                                VexStreamTrackRef(
                                                    refsJson
                                                        .getString(
                                                            refIndex,
                                                        ),
                                                ),
                                            )
                                        }
                                    }
                                add(
                                    VexStreamHubPlaylistProjection(
                                        playlistRef =
                                            VexStreamPlaylistRef(
                                                playlist
                                                    .getString(
                                                        "playlistRef",
                                                    ),
                                            ),
                                        name =
                                            playlist
                                                .getString(
                                                    "name",
                                                ),
                                        trackRefs =
                                            refs,
                                    ),
                                )
                            }
                        }
                    }

                return VexStreamHubCatalogProjection(
                    hubRef =
                        root.getString(
                            "hubRef",
                        ),
                    sourceVersion =
                        root.getString(
                            "sourceVersion",
                        ),
                    catalogRevision =
                        root.getString(
                            "catalogRevision",
                        ),
                    tracks =
                        tracks,
                    playlists =
                        playlists,
                )
            }
        }
    }
}

// [VXG RealForever][VEXSTREAM][HUB][PARTNER]
