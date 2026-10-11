package com.vextreme.vexlife.android.screens.shared.vexstream

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vextreme.vexlife.android.R
import com.vextreme.vexlife.android.data.vexstream.VexStreamConnectionState
import com.vextreme.vexlife.android.data.vexstream.VexStreamHubState
import com.vextreme.vexlife.android.data.vexstream.VexStreamLocalCopyState
import com.vextreme.vexlife.android.data.vexstream.VexStreamMusicRepository
import com.vextreme.vexlife.android.data.vexstream.VexStreamMusicState
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackAvailability
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackState
import com.vextreme.vexlife.android.elements.ElementActionHandler
import com.vextreme.vexlife.android.elements.VexButton
import com.vextreme.vexlife.android.elements.VexContainer
import com.vextreme.vexlife.android.elements.VexElementStyles
import com.vextreme.vexlife.android.elements.vexElement
import com.vextreme.vexlife.android.elements.navigation.NavigationTrail
import com.vextreme.vexlife.android.elements.navigation.NavigationTrailContent
import com.vextreme.vexlife.android.runtime.VexRuntime
import kotlinx.coroutines.launch

/**
 * Android realization of the VexStream feature.
 *
 * Durable semantic navigation is declared by [VexStreamMusicScreen]. Dynamic library rows stay
 * feature-local; [NavigationTrail] owns collection depth and MediaSessionService owns playback/queue.
 */
@Composable
internal fun VexStreamLibrarySurface(
    runtime: VexRuntime,
    repository: VexStreamMusicRepository,
) {
    val state by repository.state.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val inspectionMode = LocalInspectionMode.current

    val controller =
        if (inspectionMode) {
            null
        } else {
            rememberVexStreamMediaController()
        }
    val playback by rememberVexStreamPlaybackUiState(controller)
    val resolver =
        remember(context.filesDir) {
            VexStreamPlaybackSourceResolver(
                context = context,
            )
        }

    LaunchedEffect(repository) {
        if (repository.state.value.connectionState is VexStreamConnectionState.Paired) {
            repository.refreshCatalog()
        }
    }

    val selected =
        runtime.presentation.selected(
            VexStreamMusicScreen.LibraryContent,
        )

    Scaffold(
        bottomBar = {
            if (playback.hasQueue) {
                VexStreamMiniPlayer(
                    playback = playback,
                    controller = controller,
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            VexStreamConnectionCard(
                state = state,
                runtime = runtime,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 12.dp,
                        vertical = 8.dp,
                    ),
            )

            VexStreamLibraryTabs(
                selectedElementId = selected.elementId,
                runtime = runtime,
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                when (selected.elementId) {
                    VexStreamMusicScreen.AlbumsPanel.elementId ->
                        VexStreamAlbumsPane(
                            runtime = runtime,
                            tracks = state.playableTracks(),
                            controller = controller,
                            resolver = resolver,
                        )

                    VexStreamMusicScreen.ArtistsPanel.elementId ->
                        VexStreamArtistsPane(
                            runtime = runtime,
                            tracks = state.playableTracks(),
                            controller = controller,
                            resolver = resolver,
                        )

                    VexStreamMusicScreen.PlaylistsPanel.elementId ->
                        VexStreamPlaylistsPane(
                            runtime = runtime,
                            state = state,
                            controller = controller,
                            resolver = resolver,
                        )

                    VexStreamMusicScreen.DownloadsPanel.elementId ->
                        VexStreamDownloadsPane(
                            tracks = state.playableTracks(),
                            repository = repository,
                            controller = controller,
                            resolver = resolver,
                        )

                    else ->
                        VexStreamSongsPane(
                            tracks = state.playableTracks(),
                            repository = repository,
                            controller = controller,
                            resolver = resolver,
                        )
                }
            }
        }
    }

    if (runtime.presentation.isOpen(VexStreamMusicScreen.QueueSheet)) {
        VexStreamQueueSheet(
            state = state,
            playback = playback,
            controller = controller,
            runtime = runtime,
        )
    }
}

@Composable
private fun VexStreamLibraryTabs(
    selectedElementId: String,
    runtime: VexRuntime,
) {
    val tabs =
        listOf(
            Triple(
                VexStreamMusicScreen.SongsTab,
                VexStreamMusicScreen.SongsPanel.elementId,
                R.string.vexstream_music_songs,
            ),
            Triple(
                VexStreamMusicScreen.AlbumsTab,
                VexStreamMusicScreen.AlbumsPanel.elementId,
                R.string.vexstream_music_albums,
            ),
            Triple(
                VexStreamMusicScreen.ArtistsTab,
                VexStreamMusicScreen.ArtistsPanel.elementId,
                R.string.vexstream_music_artists,
            ),
            Triple(
                VexStreamMusicScreen.PlaylistsTab,
                VexStreamMusicScreen.PlaylistsPanel.elementId,
                R.string.vexstream_music_playlists,
            ),
            Triple(
                VexStreamMusicScreen.DownloadsTab,
                VexStreamMusicScreen.DownloadsPanel.elementId,
                R.string.vexstream_music_downloads,
            ),
        )

    Column {
        ScrollableTabRow(
            selectedTabIndex =
                tabs.indexOfFirst { (_, panelId, _) ->
                    panelId == selectedElementId
                }.coerceAtLeast(0),
            edgePadding = 8.dp,
        ) {
            tabs.forEach { (tab, panelId, labelRes) ->
                Tab(
                    selected = selectedElementId == panelId,
                    onClick = {
                        runtime.activate(tab)
                    },
                    modifier = Modifier.vexElement(tab),
                    text = {
                        Text(
                            stringResource(labelRes),
                        )
                    },
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(
                onClick = {
                    runtime.activate(
                        VexStreamMusicScreen.ShowQueue,
                    )
                },
                modifier =
                    Modifier.vexElement(
                        VexStreamMusicScreen.ShowQueue,
                    ),
            ) {
                Text(
                    stringResource(
                        R.string.vexstream_music_queue,
                    ),
                )
            }
        }
    }
}

@Composable
private fun VexStreamConnectionCard(
    state: VexStreamMusicState,
    runtime: VexRuntime,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when (
                val connection = state.connectionState
            ) {
                VexStreamConnectionState.NotPaired -> {
                    Text(
                        text =
                            stringResource(
                                R.string.vexstream_music_status_not_configured,
                            ),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Button(
                        onClick = {
                            runtime.activate(
                                VexStreamMusicScreen.FindNearby,
                            )
                        },
                        modifier =
                            Modifier.vexElement(
                                VexStreamMusicScreen.FindNearby,
                            ),
                    ) {
                        Text(
                            stringResource(
                                R.string.vexstream_music_find,
                            ),
                        )
                    }
                }

                VexStreamConnectionState.Searching -> {
                    Text(
                        text =
                            stringResource(
                                R.string.vexstream_music_status_searching,
                            ),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }

                is VexStreamConnectionState.ConfirmCode -> {
                    Text(
                        text = connection.displayName,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text =
                            stringResource(
                                R.string.vexstream_music_pairing_code_title,
                            ),
                    )
                    Text(
                        text =
                            formatPairingCode(
                                connection.pairingCode,
                            ),
                        style = MaterialTheme.typography.headlineLarge,
                    )
                    Text(
                        text =
                            stringResource(
                                R.string.vexstream_music_pairing_code_help,
                            ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = {
                            runtime.activate(
                                VexStreamMusicScreen.ConfirmPairingCode,
                            )
                        },
                        modifier =
                            Modifier.vexElement(
                                VexStreamMusicScreen.ConfirmPairingCode,
                            ),
                    ) {
                        Text(
                            stringResource(
                                R.string.vexstream_music_confirm_code,
                            ),
                        )
                    }
                }

                is VexStreamConnectionState.WaitingForDesktopApproval -> {
                    Text(
                        text = connection.displayName,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text =
                            stringResource(
                                R.string.vexstream_music_waiting_approval,
                            ),
                    )
                }

                is VexStreamConnectionState.Paired -> {
                    Text(
                        text =
                            if (state.hubState is VexStreamHubState.Ready) {
                                stringResource(
                                    R.string.vexstream_music_status_ready,
                                ) + " · " + connection.displayName
                            } else {
                                stringResource(
                                    R.string.vexstream_music_status_unreachable,
                                ) + " · " + connection.displayName
                            },
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = {
                                if (state.hubState is VexStreamHubState.Ready) {
                                    runtime.activate(
                                        VexStreamMusicScreen.RefreshCatalog,
                                    )
                                } else {
                                    runtime.activate(
                                        VexStreamMusicScreen.FindNearby,
                                    )
                                }
                            },
                            modifier =
                                Modifier.vexElement(
                                    if (state.hubState is VexStreamHubState.Ready) {
                                        VexStreamMusicScreen.RefreshCatalog
                                    } else {
                                        VexStreamMusicScreen.FindNearby
                                    },
                                ),
                        ) {
                            Text(
                                stringResource(
                                    if (state.hubState is VexStreamHubState.Ready) {
                                        R.string.vexstream_music_refresh
                                    } else {
                                        R.string.vexstream_music_find
                                    },
                                ),
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                runtime.activate(
                                    VexStreamMusicScreen.ForgetPaired,
                                )
                            },
                            modifier =
                                Modifier.vexElement(
                                    VexStreamMusicScreen.ForgetPaired,
                                ),
                        ) {
                            Text(
                                stringResource(
                                    R.string.vexstream_music_forget,
                                ),
                            )
                        }
                    }
                }

                is VexStreamConnectionState.Failed -> {
                    Text(
                        text =
                            stringResource(
                                R.string.vexstream_music_pairing_failed,
                            ),
                    )
                    Button(
                        onClick = {
                            runtime.activate(
                                VexStreamMusicScreen.FindNearby,
                            )
                        },
                        modifier =
                            Modifier.vexElement(
                                VexStreamMusicScreen.FindNearby,
                            ),
                    ) {
                        Text(
                            stringResource(
                                R.string.vexstream_music_find,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VexStreamSongsPane(
    tracks: List<VexStreamTrackState>,
    repository: VexStreamMusicRepository,
    controller: androidx.media3.session.MediaController?,
    resolver: VexStreamPlaybackSourceResolver,
) {
    val scope = rememberCoroutineScope()
    val sorted =
        remember(tracks) {
            tracks.sortedBy {
                it.track.title.lowercase()
            }
        }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(
            key = "songs-actions",
        ) {
            VexStreamCollectionActions(
                tracks = sorted,
                controller = controller,
                resolver = resolver,
                modifier = Modifier.padding(
                    horizontal = 12.dp,
                    vertical = 10.dp,
                ),
            )
        }

        if (sorted.isEmpty()) {
            item(
                key = "songs-empty",
            ) {
                VexStreamEmptyMessage(
                    text =
                        stringResource(
                            R.string.vexstream_music_empty,
                        ),
                )
            }
        } else {
            items(
                items = sorted,
                key = { it.track.trackRef.value },
            ) { track ->
                VexStreamTrackCard(
                    state = track,
                    contextTracks = sorted,
                    controller = controller,
                    resolver = resolver,
                    onDownload = {
                        scope.launch {
                            repository.downloadForOffline(
                                track.track.trackRef,
                            )
                        }
                    },
                    onRemoveDownload = {
                        scope.launch {
                            repository.removeOfflineCopy(
                                track.track.trackRef,
                            )
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun VexStreamDownloadsPane(
    tracks: List<VexStreamTrackState>,
    repository: VexStreamMusicRepository,
    controller: androidx.media3.session.MediaController?,
    resolver: VexStreamPlaybackSourceResolver,
) {
    val scope = rememberCoroutineScope()
    val downloaded =
        remember(tracks) {
            tracks
                .filter {
                    it.localCopy is VexStreamLocalCopyState.Downloaded
                }
                .sortedBy {
                    it.track.title.lowercase()
                }
        }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(
            key = "downloads-actions",
        ) {
            VexStreamCollectionActions(
                tracks = downloaded,
                controller = controller,
                resolver = resolver,
                modifier = Modifier.padding(
                    horizontal = 12.dp,
                    vertical = 10.dp,
                ),
            )
        }

        if (downloaded.isEmpty()) {
            item(
                key = "downloads-empty",
            ) {
                VexStreamEmptyMessage(
                    text =
                        stringResource(
                            R.string.vexstream_music_no_downloads,
                        ),
                )
            }
        } else {
            items(
                items = downloaded,
                key = { it.track.trackRef.value },
            ) { track ->
                VexStreamTrackCard(
                    state = track,
                    contextTracks = downloaded,
                    controller = controller,
                    resolver = resolver,
                    onDownload = {},
                    onRemoveDownload = {
                        scope.launch {
                            repository.removeOfflineCopy(
                                track.track.trackRef,
                            )
                        }
                    },
                )
            }
        }
    }
}

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
private fun VexStreamAlbumsPane(
    runtime: VexRuntime,
    tracks: List<VexStreamTrackState>,
    controller: androidx.media3.session.MediaController?,
    resolver: VexStreamPlaybackSourceResolver,
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
    )
}

@Composable
private fun VexStreamArtistsPane(
    runtime: VexRuntime,
    tracks: List<VexStreamTrackState>,
    controller: androidx.media3.session.MediaController?,
    resolver: VexStreamPlaybackSourceResolver,
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
    )
}

@Composable
private fun VexStreamPlaylistsPane(
    runtime: VexRuntime,
    state: VexStreamMusicState,
    controller: androidx.media3.session.MediaController?,
    resolver: VexStreamPlaybackSourceResolver,
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
) {
    val trail =
        remember {
            NavigationTrail<VexStreamCollectionRoute>(
                root = VexStreamCollectionRoute.Index,
            )
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
                                controller = controller,
                                resolver = resolver,
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
                        title = selected.title,
                        tracks = selected.tracks,
                        controller = controller,
                        resolver = resolver,
                        onBack = trail::goBack,
                    )
                }
            }
        }
    }
}

@Composable
private fun VexStreamCollectionDetail(
    title: String,
    tracks: List<VexStreamTrackState>,
    controller: androidx.media3.session.MediaController?,
    resolver: VexStreamPlaybackSourceResolver,
    onBack: () -> Unit,
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
                TextButton(
                    onClick = onBack,
                ) {
                    Text(
                        stringResource(
                            R.string.back,
                        ),
                    )
                }
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
                state = track,
                contextTracks = tracks,
                controller = controller,
                resolver = resolver,
                onDownload = {},
                onRemoveDownload = {},
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
    controller: androidx.media3.session.MediaController?,
    resolver: VexStreamPlaybackSourceResolver,
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
            VexStreamCollectionActions(
                tracks = tracks,
                controller = controller,
                resolver = resolver,
            )
        }
    }
}

@Composable
private fun VexStreamCollectionActions(
    tracks: List<VexStreamTrackState>,
    controller: androidx.media3.session.MediaController?,
    resolver: VexStreamPlaybackSourceResolver,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(
            onClick = {
                controller?.playVexStreamCollection(
                    tracks = tracks,
                    resolver = resolver,
                )
            },
            enabled =
                controller != null &&
                    tracks.isNotEmpty(),
        ) {
            Text(
                stringResource(
                    R.string.vexstream_music_play_all,
                ),
            )
        }
        OutlinedButton(
            onClick = {
                controller?.playVexStreamCollection(
                    tracks = tracks,
                    resolver = resolver,
                    shuffled = true,
                )
            },
            enabled =
                controller != null &&
                    tracks.isNotEmpty(),
        ) {
            Text(
                stringResource(
                    R.string.vexstream_music_shuffle_all,
                ),
            )
        }
    }
}

@Composable
private fun VexStreamTrackCard(
    state: VexStreamTrackState,
    contextTracks: List<VexStreamTrackState>,
    controller: androidx.media3.session.MediaController?,
    resolver: VexStreamPlaybackSourceResolver,
    onDownload: () -> Unit,
    onRemoveDownload: () -> Unit,
    showDownloadAction: Boolean = true,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                text = state.track.title,
                style = MaterialTheme.typography.titleMedium,
            )
            if (state.track.artist.isNotBlank()) {
                Text(
                    text = state.track.artist,
                )
            }
            if (state.track.album.isNotBlank()) {
                Text(
                    text = state.track.album,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = trackAvailabilityText(state),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Button(
                    onClick = {
                        controller?.playVexStreamContext(
                            selected = state,
                            contextTracks = contextTracks,
                            resolver = resolver,
                        )
                    },
                    enabled = controller != null,
                ) {
                    Text(
                        stringResource(
                            R.string.vexstream_music_play,
                        ),
                    )
                }
                TextButton(
                    onClick = {
                        controller?.playNextVexStream(
                            state = state,
                            resolver = resolver,
                        )
                    },
                    enabled = controller != null,
                ) {
                    Text(
                        stringResource(
                            R.string.vexstream_music_play_next,
                        ),
                    )
                }
                TextButton(
                    onClick = {
                        controller?.queueVexStream(
                            state = state,
                            resolver = resolver,
                        )
                    },
                    enabled = controller != null,
                ) {
                    Text(
                        stringResource(
                            R.string.vexstream_music_add_queue,
                        ),
                    )
                }
            }

            if (showDownloadAction) {
                when (state.localCopy) {
                    is VexStreamLocalCopyState.Downloaded -> {
                        OutlinedButton(
                            onClick = onRemoveDownload,
                        ) {
                            Text(
                                stringResource(
                                    R.string.vexstream_music_remove_download,
                                ),
                            )
                        }
                    }

                    is VexStreamLocalCopyState.Downloading -> {
                        Button(
                            onClick = {},
                            enabled = false,
                        ) {
                            Text(
                                stringResource(
                                    R.string.vexstream_music_downloading,
                                ),
                            )
                        }
                    }

                    else -> {
                        if (state.hubAvailable) {
                            OutlinedButton(
                                onClick = onDownload,
                            ) {
                                Text(
                                    stringResource(
                                        R.string.vexstream_music_download,
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VexStreamMiniPlayer(
    playback: VexStreamPlaybackUiState,
    controller: androidx.media3.session.MediaController?,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = 8.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                text =
                    playback.currentTitle.ifBlank {
                        stringResource(
                            R.string.vexstream_music_now_playing,
                        )
                    },
                style = MaterialTheme.typography.titleSmall,
            )
            if (playback.currentArtist.isNotBlank()) {
                Text(
                    text = playback.currentArtist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TextButton(
                    onClick = {
                        controller?.seekToPreviousMediaItem()
                    },
                    enabled =
                        controller?.hasPreviousMediaItem() == true,
                ) {
                    Text(
                        stringResource(
                            R.string.vexstream_music_previous,
                        ),
                    )
                }
                Button(
                    onClick = {
                        controller?.toggleVexStreamPlayback()
                    },
                    enabled = controller != null,
                ) {
                    Text(
                        stringResource(
                            if (playback.isPlaying) {
                                R.string.vexstream_music_pause
                            } else {
                                R.string.vexstream_music_play
                            },
                        ),
                    )
                }
                TextButton(
                    onClick = {
                        controller?.seekToNextMediaItem()
                    },
                    enabled =
                        controller?.hasNextMediaItem() == true,
                ) {
                    Text(
                        stringResource(
                            R.string.vexstream_music_next,
                        ),
                    )
                }
                TextButton(
                    onClick = {
                        controller?.toggleVexStreamShuffle()
                    },
                    enabled = controller != null,
                ) {
                    Text(
                        if (playback.shuffleEnabled) {
                            "✓ " +
                                stringResource(
                                    R.string.vexstream_music_shuffle,
                                )
                        } else {
                            stringResource(
                                R.string.vexstream_music_shuffle,
                            )
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VexStreamQueueSheet(
    state: VexStreamMusicState,
    playback: VexStreamPlaybackUiState,
    controller: androidx.media3.session.MediaController?,
    runtime: VexRuntime,
) {
    val tracksByRef =
        remember(state.tracks) {
            state.tracks.associateBy {
                it.track.trackRef.value
            }
        }

    ModalBottomSheet(
        onDismissRequest = {
            runtime.dismissPresentation(
                VexStreamMusicScreen.QueueSheet,
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text =
                    stringResource(
                        R.string.vexstream_music_queue,
                    ),
                style = MaterialTheme.typography.headlineSmall,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                TextButton(
                    onClick = {
                        controller?.toggleVexStreamShuffle()
                    },
                    enabled = controller != null,
                ) {
                    Text(
                        (if (playback.shuffleEnabled) "✓ " else "") +
                            stringResource(
                                R.string.vexstream_music_shuffle,
                            ),
                    )
                }
                TextButton(
                    onClick = {
                        controller?.toggleVexStreamRepeatAll()
                    },
                    enabled = controller != null,
                ) {
                    Text(
                        (if (playback.repeatEnabled) "✓ " else "") +
                            stringResource(
                                R.string.vexstream_music_repeat,
                            ),
                    )
                }
                TextButton(
                    onClick = {
                        controller?.clearMediaItems()
                    },
                    enabled =
                        controller != null &&
                            playback.hasQueue,
                ) {
                    Text(
                        stringResource(
                            R.string.vexstream_music_clear_queue,
                        ),
                    )
                }
            }

            HorizontalDivider()

            if (playback.queueMediaIds.isEmpty()) {
                Text(
                    text =
                        stringResource(
                            R.string.vexstream_music_queue_empty,
                        ),
                    modifier = Modifier.padding(
                        vertical = 20.dp,
                    ),
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(
                        items =
                            playback.queueMediaIds
                                .mapIndexed { index, mediaId ->
                                    index to mediaId
                                },
                        key = { (index, mediaId) ->
                            "$index:$mediaId"
                        },
                    ) { (index, mediaId) ->
                        val track =
                            tracksByRef[mediaId]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    controller?.seekToDefaultPosition(
                                        index,
                                    )
                                    controller?.play()
                                }
                                .padding(
                                    vertical = 9.dp,
                                ),
                            horizontalArrangement =
                                Arrangement.spacedBy(
                                    10.dp,
                                ),
                        ) {
                            Text(
                                text =
                                    if (index == playback.currentIndex) {
                                        "▶"
                                    } else {
                                        (index + 1).toString()
                                    },
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    text =
                                        track
                                            ?.track
                                            ?.title
                                            ?: mediaId,
                                )
                                track
                                    ?.track
                                    ?.artist
                                    ?.takeIf {
                                        it.isNotBlank()
                                    }
                                    ?.let { artist ->
                                        Text(
                                            text = artist,
                                            style =
                                                MaterialTheme
                                                    .typography
                                                    .bodySmall,
                                            color =
                                                MaterialTheme
                                                    .colorScheme
                                                    .onSurfaceVariant,
                                        )
                                    }
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    runtime.activate(
                        VexStreamMusicScreen.CloseQueue,
                    )
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .vexElement(
                            VexStreamMusicScreen.CloseQueue,
                        ),
            ) {
                Text(
                    stringResource(
                        R.string.close,
                    ),
                )
            }
        }
    }
}

@Composable
private fun VexStreamEmptyMessage(
    text: String,
) {
    Text(
        text = text,
        modifier = Modifier.padding(20.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun trackAvailabilityText(
    state: VexStreamTrackState,
): String =
    when {
        state.localCopy is VexStreamLocalCopyState.Downloading ->
            stringResource(
                R.string.vexstream_music_downloading,
            )

        state.localCopy is VexStreamLocalCopyState.Downloaded ->
            if (state.hubAvailable) {
                stringResource(
                    R.string.vexstream_music_downloaded,
                )
            } else {
                stringResource(
                    R.string.vexstream_music_local_only,
                )
            }

        state.availability == VexStreamTrackAvailability.HUB_ONLY ->
            stringResource(
                R.string.vexstream_music_hub_only,
            )

        state.availability == VexStreamTrackAvailability.LOCAL_ONLY ->
            stringResource(
                R.string.vexstream_music_local_only,
            )

        else ->
            stringResource(
                R.string.vexstream_music_unavailable,
            )
    }

private fun formatPairingCode(
    code: String,
): String =
    if (code.length == 6) {
        code.take(3) + " " + code.takeLast(3)
    } else {
        code
    }

// [VXG RealForever]
