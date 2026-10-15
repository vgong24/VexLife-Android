package com.vextreme.vexlife.android.screens.shared.vexstream

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vextreme.vexlife.android.R
import com.vextreme.vexlife.android.data.vexstream.VexStreamMusicState
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackState
import com.vextreme.vexlife.android.elements.ElementActionHandler
import com.vextreme.vexlife.android.elements.VexButton
import com.vextreme.vexlife.android.elements.navigation.NavigationTrail
import com.vextreme.vexlife.android.elements.navigation.NavigationTrailContent
import com.vextreme.vexlife.android.runtime.VexRuntime

/**
 * Library collection presentation for Albums, Artists and Playlists.
 *
 * This neighborhood owns collection grouping and Screen-local drill-in presentation only. Track
 * rendering stays with VexStreamTrackPresentation; Media3 queue/playback truth remains outside.
 */
private sealed interface VexStreamCollectionRoute {
    data object Index : VexStreamCollectionRoute

    data class Detail(
        val key: String,
    ) : VexStreamCollectionRoute
}

private data class VexStreamCollectionRow(
    val key: String,
    val title: String,
    val subtitle: String,
    val tracks: List<VexStreamTrackState>,
)

@Composable
internal fun VexStreamAlbumsPane(
    runtime: VexRuntime,
    tracks: List<VexStreamTrackState>,
    controller: androidx.media3.session.MediaController?,
    resolver: VexStreamPlaybackSourceResolver,
    onVisitArtist: (String) -> Unit,
) {
    val unknownAlbum =
        stringResource(
            R.string.vexstream_music_unknown_album,
        )
    val rows =
        remember(
            tracks,
            unknownAlbum,
        ) {
            tracks
                .groupBy {
                    it.track.album
                }
                .entries
                .map { (album, albumTracks) ->
                    VexStreamCollectionRow(
                        key = "album:$album",
                        title =
                            album.ifBlank {
                                unknownAlbum
                            },
                        subtitle =
                            albumTracks
                                .firstOrNull()
                                ?.track
                                ?.artist
                                .orEmpty(),
                        tracks =
                            albumTracks.sortedBy {
                                it.track.title.lowercase()
                            },
                    )
                }
                .sortedBy {
                    it.title.lowercase()
                }
        }

    VexStreamCollectionBrowserPane(
        runtime = runtime,
        rows = rows,
        emptyText =
            stringResource(
                R.string.vexstream_music_empty,
            ),
        navigationLabel = "vexstream-album-depth",
        controller = controller,
        resolver = resolver,
        onVisitArtist = onVisitArtist,
    )
}

@Composable
internal fun VexStreamArtistsPane(
    runtime: VexRuntime,
    tracks: List<VexStreamTrackState>,
    controller: androidx.media3.session.MediaController?,
    resolver: VexStreamPlaybackSourceResolver,
    requestedArtist: String?,
    onRequestedArtistConsumed: () -> Unit,
    onVisitArtist: (String) -> Unit,
) {
    val unknownArtist =
        stringResource(
            R.string.vexstream_music_unknown_artist,
        )
    val rows =
        tracks
            .groupBy {
                it.track.artist
            }
            .entries
            .map { (artist, artistTracks) ->
                VexStreamCollectionRow(
                    key = "artist:$artist",
                    title =
                        artist.ifBlank {
                            unknownArtist
                        },
                    subtitle =
                        stringResource(
                            R.string.vexstream_music_track_count,
                            artistTracks.size,
                        ),
                    tracks =
                        artistTracks.sortedBy {
                            it.track.title.lowercase()
                        },
                )
            }
            .sortedBy {
                it.title.lowercase()
            }

    VexStreamCollectionBrowserPane(
        runtime = runtime,
        rows = rows,
        emptyText =
            stringResource(
                R.string.vexstream_music_empty,
            ),
        navigationLabel = "vexstream-artist-depth",
        controller = controller,
        resolver = resolver,
        onVisitArtist = onVisitArtist,
        requestedDetailKey =
            requestedArtist?.let {
                "artist:$it"
            },
        onRequestedDetailConsumed =
            onRequestedArtistConsumed,
    )
}

@Composable
internal fun VexStreamPlaylistsPane(
    runtime: VexRuntime,
    state: VexStreamMusicState,
    controller: androidx.media3.session.MediaController?,
    resolver: VexStreamPlaybackSourceResolver,
    onVisitArtist: (String) -> Unit,
) {
    val tracksByRef =
        remember(state.tracks) {
            state.tracks.associateBy {
                it.track.trackRef
            }
        }
    val syncedLabel =
        stringResource(
            R.string.vexstream_music_playlist_synced,
        )
    val rows =
        state.playlists
            .map { playlist ->
                VexStreamCollectionRow(
                    key =
                        "playlist:" +
                            playlist.playlistRef.value,
                    title = playlist.name,
                    subtitle = syncedLabel,
                    tracks =
                        playlist.trackRefs
                            .mapNotNull(
                                tracksByRef::get,
                            ),
                )
            }
            .sortedBy {
                it.title.lowercase()
            }

    VexStreamCollectionBrowserPane(
        runtime = runtime,
        rows = rows,
        emptyText =
            stringResource(
                R.string.vexstream_music_no_playlists,
            ),
        navigationLabel = "vexstream-playlist-depth",
        controller = controller,
        resolver = resolver,
        onVisitArtist = onVisitArtist,
    )
}

@Composable
private fun VexStreamCollectionBrowserPane(
    runtime: VexRuntime,
    rows: List<VexStreamCollectionRow>,
    emptyText: String,
    navigationLabel: String,
    controller: androidx.media3.session.MediaController?,
    resolver: VexStreamPlaybackSourceResolver,
    onVisitArtist: (String) -> Unit,
    requestedDetailKey: String? = null,
    onRequestedDetailConsumed: () -> Unit = {},
) {
    val trail =
        remember {
            NavigationTrail<VexStreamCollectionRoute>(
                root = VexStreamCollectionRoute.Index,
            )
        }

    val rowKeys =
        remember(rows) {
            rows.map(VexStreamCollectionRow::key)
        }
    LaunchedEffect(
        requestedDetailKey,
        rowKeys,
    ) {
        val key =
            requestedDetailKey
                ?: return@LaunchedEffect
        if (key in rowKeys) {
            trail.push(
                VexStreamCollectionRoute.Detail(
                    key = key,
                ),
            )
            onRequestedDetailConsumed()
        }
    }

    DisposableEffect(
        runtime,
        trail,
    ) {
        runtime.attachBackLayer(trail)
        onDispose {
            runtime.detachBackLayer(trail)
        }
    }

    NavigationTrailContent(
        trail = trail,
        modifier = Modifier.fillMaxSize(),
        label = navigationLabel,
    ) { route ->
        when (route) {
            VexStreamCollectionRoute.Index -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement =
                        Arrangement.spacedBy(
                            8.dp,
                        ),
                ) {
                    item(
                        key = "collection-tabs",
                    ) {
                        VexStreamLibraryTabs(
                            runtime = runtime,
                        )
                    }

                    if (rows.isEmpty()) {
                        item(
                            key = "collection-empty",
                        ) {
                            VexStreamEmptyMessage(
                                text = emptyText,
                            )
                        }
                    } else {
                        items(
                            items = rows,
                            key = {
                                it.key
                            },
                        ) { row ->
                            VexStreamCollectionCard(
                                title = row.title,
                                subtitle = row.subtitle,
                                tracks = row.tracks,
                                onOpen = {
                                    trail.push(
                                        VexStreamCollectionRoute.Detail(
                                            key = row.key,
                                        ),
                                    )
                                },
                            )
                        }
                    }
                }
            }

            is VexStreamCollectionRoute.Detail -> {
                val selected =
                    rows.firstOrNull {
                        it.key == route.key
                    }

                if (selected == null) {
                    VexStreamEmptyMessage(
                        text = emptyText,
                    )
                } else {
                    VexStreamCollectionDetail(
                        runtime = runtime,
                        collectionKey = selected.key,
                        title = selected.title,
                        tracks = selected.tracks,
                        controller = controller,
                        resolver = resolver,
                        onBack = trail::goBack,
                        onVisitArtist = onVisitArtist,
                    )
                }
            }
        }
    }
}

@Composable
private fun VexStreamCollectionDetail(
    runtime: VexRuntime,
    collectionKey: String,
    title: String,
    tracks: List<VexStreamTrackState>,
    controller: androidx.media3.session.MediaController?,
    resolver: VexStreamPlaybackSourceResolver,
    onBack: () -> Unit,
    onVisitArtist: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(
            key = "collection-header",
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                VexButton(
                    element =
                        VexStreamMusicScreen
                            .CollectionBack,
                    handler =
                        ElementActionHandler { element ->
                            runtime.activate(
                                element,
                            )
                            onBack()
                        },
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text =
                        stringResource(
                            R.string.vexstream_music_track_count,
                            tracks.size,
                        ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                VexStreamCollectionActions(
                    runtime = runtime,
                    tracks = tracks,
                    controller = controller,
                    resolver = resolver,
                )
            }
        }

        items(
            items = tracks,
            key = { it.track.trackRef.value },
        ) { track ->
            VexStreamTrackCard(
                runtime = runtime,
                presentationScope =
                    VexStreamTrackPresentationScope.collection(
                        collectionKey,
                    ),
                state = track,
                contextTracks = tracks,
                controller = controller,
                resolver = resolver,
                onDownload = {},
                onRemoveDownload = {},
                onVisitArtist = onVisitArtist,
                showDownloadAction = false,
            )
        }
    }
}

@Composable
private fun VexStreamCollectionCard(
    title: String,
    subtitle: String,
    tracks: List<VexStreamTrackState>,
    onOpen: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clickable(onClick = onOpen),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text =
                    stringResource(
                        R.string.vexstream_music_track_count,
                        tracks.size,
                    ),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun VexStreamCollectionActions(
    runtime: VexRuntime,
    tracks: List<VexStreamTrackState>,
    controller: androidx.media3.session.MediaController?,
    resolver: VexStreamPlaybackSourceResolver,
    modifier: Modifier = Modifier,
) {
    val canPlay =
        controller != null &&
            tracks.isNotEmpty()
    val handler =
        ElementActionHandler { element ->
            runtime.activate(
                element,
            )
            when (element) {
                VexStreamMusicScreen.PlayAll ->
                    controller?.playVexStreamCollection(
                        tracks = tracks,
                        resolver = resolver,
                    )

                VexStreamMusicScreen.ShuffleAll ->
                    controller?.playVexStreamCollection(
                        tracks = tracks,
                        resolver = resolver,
                        shuffled = true,
                    )

                else -> Unit
            }
        }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        VexButton(
            element = VexStreamMusicScreen.PlayAll,
            handler = handler,
            enabled = canPlay,
        )
        VexButton(
            element = VexStreamMusicScreen.ShuffleAll,
            handler = handler,
            enabled = canPlay,
        )
    }
}

// [VXG RealForever]
