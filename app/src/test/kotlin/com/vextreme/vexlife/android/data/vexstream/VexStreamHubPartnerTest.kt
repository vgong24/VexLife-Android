package com.vextreme.vexlife.android.data.vexstream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class VexStreamHubPartnerTest {
    @Test
    fun catalogConversionRootsSanitizedHubStatementInExistingSourceIdentity() {
        val signal =
            VexStreamHubPartner.Connection
                .CatalogConversion()
                .convert(
                    """
                    {
                      "hubRef": "hub.desktop.alpha",
                      "sourceVersion": "4",
                      "catalogRevision": "rev-42",
                      "tracks": [
                        {
                          "trackRef": "track.alpha",
                          "mediaSha256": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                          "mediaByteLength": 1234,
                          "title": "Alpha",
                          "artist": "Artist",
                          "artworkUrl": "http://not-admitted.example/art.jpg",
                          "artworkRef": "../not-admitted"
                        }
                      ],
                      "playlists": [
                        {
                          "playlistRef": "playlist.alpha",
                          "name": "Alpha List",
                          "trackRefs": ["track.alpha"]
                        }
                      ]
                    }
                    """.trimIndent(),
                )
                .single() as
                VexStreamHubPartner.Signal.Catalog

        assertEquals(
            VexStreamHubPartner.Source(
                hubRef =
                    "hub.desktop.alpha",
            ),
            signal.source,
        )
        assertEquals(
            "hub.desktop.alpha",
            signal.catalog.hubRef,
        )
        assertEquals(
            "rev-42",
            signal.catalog.catalogRevision,
        )
        assertEquals(
            1,
            signal.catalog.tracks.size,
        )
        assertEquals(
            null,
            signal.catalog.tracks
                .single()
                .artworkUrl,
        )
        assertEquals(
            null,
            signal.catalog.tracks
                .single()
                .artworkRef,
        )
        assertEquals(
            listOf(
                VexStreamTrackRef(
                    "track.alpha",
                ),
            ),
            signal.catalog.playlists
                .single()
                .trackRefs,
        )
    }

    @Test
    fun pairedCertificateIdentityIsPartnerConnectionIdentityNotMutableRoute() {
        val id =
            VexStreamHubPartner.Connection.Id(
                certificateSha256 =
                    "b".repeat(
                        64,
                    ),
            )

        assertEquals(
            "b".repeat(
                64,
            ),
            id.certificateSha256,
        )
    }

    @Test
    fun observationKeepsExternalSourceAndDeliveryOccurrenceDistinct() {
        val signal =
            VexStreamHubPartner.Signal.Catalog(
                source =
                    VexStreamHubPartner.Source(
                        hubRef =
                            "hub.desktop.alpha",
                    ),
                catalog =
                    VexStreamHubCatalogProjection(
                        hubRef =
                            "hub.desktop.alpha",
                        sourceVersion =
                            "4",
                        catalogRevision =
                            "rev-42",
                        tracks =
                            emptyList(),
                    ),
            )
        val connectionId =
            VexStreamHubPartner.Connection.Id(
                certificateSha256 =
                    "c".repeat(
                        64,
                    ),
            )
        val observation =
            VexStreamHubPartner.Connection.Observation(
                connectionId =
                    connectionId,
                signal =
                    signal,
                observedAtEpochMillis =
                    42L,
            )

        assertSame(
            connectionId,
            observation.connectionId,
        )
        assertSame(
            signal,
            observation.signal,
        )
        assertEquals(
            "hub.desktop.alpha",
            observation.signal
                .source
                .hubRef,
        )
    }
}

// [VXG RealForever][VEXSTREAM][HUB][PARTNER]
