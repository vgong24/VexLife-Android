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
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
                    runtime = runtime,
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
                            runtime = runtime,
                            tracks = state.playableTracks(),
                            repository = repository,
                            controller = controller,
                            resolver = resolver,
                        )

                    else ->
                        VexStreamSongsPane(
                            runtime = runtime,
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
            VexStreamMusicScreen.SongsTab to
                VexStreamMusicScreen.SongsPanel.elementId,
            VexStreamMusicScreen.AlbumsTab to
                VexStreamMusicScreen.AlbumsPanel.elementId,
            VexStreamMusicScreen.ArtistsTab to
                VexStreamMusicScreen.ArtistsPanel.elementId,
            VexStreamMusicScreen.PlaylistsTab to
                VexStreamMusicScreen.PlaylistsPanel.elementId,
            VexStreamMusicScreen.DownloadsTab to
                VexStreamMusicScreen.DownloadsPanel.elementId,
        )

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(
                        rememberScrollState(),
                    ),
            horizontalArrangement =
                Arrangement.spacedBy(
                    8.dp,
                ),
        ) {
            tabs.forEach { (tab, panelId) ->
                VexButton(
                    element = tab,
                    handler = runtime,
                    style =
                        if (selectedElementId == panelId) {
                            VexElementStyles.PrimaryAction
                        } else {
                            VexElementStyles.Navigation
                        },
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            VexButton(
                element = VexStreamMusicScreen.ShowQueue,
                handler = runtime,
            )
        }
    }
}

@Composable
private fun VexStreamConnectionCard(
    state: VexStreamMusicState,
    runtime: VexRuntime,
    modifier: Modifier = Modifier,
) {
    VexContainer(
        element = VexStreamMusicScreen.ConnectionSection,
        modifier = modifier,
        style = VexElementStyles.RaisedSurface,
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
                VexButton(
                    element = VexStreamMusicScreen.FindNearby,
                    handler = runtime,
                )
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
                VexButton(
                    element =
                        VexStreamMusicScreen
                            .ConfirmPairingCode,
                    handler = runtime,
                )
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
                    VexButton(
                        element =
                            if (state.hubState is VexStreamHubState.Ready) {
                                VexStreamMusicScreen.RefreshCatalog
                            } else {
                                VexStreamMusicScreen.FindNearby
                            },
                        handler = runtime,
                    )
                    VexButton(
                        element = VexStreamMusicScreen.ForgetPaired,
                        handler = runtime,
                    )
                }
            }

            is VexStreamConnectionState.Failed -> {
                Text(
                    text =
                        stringResource(
                            R.string.vexstream_music_pairing_failed,
                        ),
                )
                VexButton(
                    element = VexStreamMusicScreen.FindNearby,
                    handler = runtime,
                )
            }
        }
    }
}

@Composable
private fun VexStreamSongsPane(
    runtime: VexRuntime,
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
    var selectedTrackRef by rememberSaveable {
        mutableStateOf<String?>(null)
    }
    val sortedRefs =
        remember(sorted) {
            sorted.map {
                it.track.trackRef.value
            }
        }
    LaunchedEffect(sortedRefs) {
        if (selectedTrackRef !in sortedRefs) {
            selectedTrackRef =
                sortedRefs.firstOrNull()
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
                runtime = runtime,
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
                    runtime = runtime,
                    state = track,
                    contextTracks = sorted,
                    selected =
                        selectedTrackRef ==
                            track.track.trackRef.value,
                    onSelect = {
                        selectedTrackRef =
                            track.track.trackRef.value
                    },
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
    runtime: VexRuntime,
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
    var selectedTrackRef by rememberSaveable {
        mutableStateOf<String?>(null)
    }
    val downloadedRefs =
        remember(downloaded) {
            downloaded.map {
                it.track.trackRef.value
            }
        }
    LaunchedEffect(downloadedRefs) {
        if (selectedTrackRef !in downloadedRefs) {
            selectedTrackRef =
                downloadedRefs.firstOrNull()
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
                runtime = runtime,
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
                    runtime = runtime,
                    state = track,
                    contextTracks = downloaded,
                    selected =
                        selectedTrackRef ==
                            track.track.trackRef.value,
                    onSelect = {
                        selectedTrackRef =
                            track.track.trackRef.value
                    },
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
    runtime: VexRuntime,
    title: String,
    tracks: List<VexStreamTrackState>,
    controller: androidx.media3.session.MediaController?,
    resolver: VexStreamPlaybackSourceResolver,
    onBack: () -> Unit,
) {
    var selectedTrackRef by rememberSaveable(
        title,
    ) {
        mutableStateOf<String?>(null)
    }
    val trackRefs =
        remember(tracks) {
            tracks.map {
                it.track.trackRef.value
            }
        }
    LaunchedEffect(trackRefs) {
        if (selectedTrackRef !in trackRefs) {
            selectedTrackRef =
                trackRefs.firstOrNull()
        }
    }

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
                state = track,
                contextTracks = tracks,
                selected =
                    selectedTrackRef ==
                        track.track.trackRef.value,
                onSelect = {
                    selectedTrackRef =
                        track.track.trackRef.value
                },
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

@Composable
private fun VexStreamTrackCard(
    runtime: VexRuntime,
    state: VexStreamTrackState,
    contextTracks: List<VexStreamTrackState>,
    selected: Boolean,
    onSelect: () -> Unit,
    controller: androidx.media3.session.MediaController?,
    resolver: VexStreamPlaybackSourceResolver,
    onDownload: () -> Unit,
    onRemoveDownload: () -> Unit,
    showDownloadAction: Boolean = true,
) {
    val handler =
        ElementActionHandler { element ->
            runtime.activate(
                element,
            )
            when (element) {
                VexStreamMusicScreen.PlayTrack ->
                    controller?.playVexStreamContext(
                        selected = state,
                        contextTracks = contextTracks,
                        resolver = resolver,
                    )

                VexStreamMusicScreen.PlayTrackNext ->
                    controller?.playNextVexStream(
                        state = state,
                        resolver = resolver,
                    )

                VexStreamMusicScreen.AddTrackToQueue ->
                    controller?.queueVexStream(
                        state = state,
                        resolver = resolver,
                    )

                VexStreamMusicScreen.DownloadTrack ->
                    onDownload()

                VexStreamMusicScreen.RemoveTrackDownload ->
                    onRemoveDownload()

                else -> Unit
            }
        }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clickable(
                onClick = onSelect,
            ),
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

            if (selected) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    VexButton(
                        element = VexStreamMusicScreen.PlayTrack,
                        handler = handler,
                        enabled = controller != null,
                    )
                    VexButton(
                        element = VexStreamMusicScreen.PlayTrackNext,
                        handler = handler,
                        enabled = controller != null,
                    )
                    VexButton(
                        element = VexStreamMusicScreen.AddTrackToQueue,
                        handler = handler,
                        enabled = controller != null,
                    )
                }

                if (showDownloadAction) {
                    when (state.localCopy) {
                        is VexStreamLocalCopyState.Downloaded -> {
                            VexButton(
                                element =
                                    VexStreamMusicScreen
                                        .RemoveTrackDownload,
                                handler = handler,
                            )
                        }

                        is VexStreamLocalCopyState.Downloading -> {
                            Text(
                                text =
                                    stringResource(
                                        R.string.vexstream_music_downloading,
                                    ),
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

                        else -> {
                            if (state.hubAvailable) {
                                VexButton(
                                    element =
                                        VexStreamMusicScreen
                                            .DownloadTrack,
                                    handler = handler,
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
    runtime: VexRuntime,
) {
    val handler =
        ElementActionHandler { element ->
            runtime.activate(
                element,
            )
            when (element) {
                VexStreamMusicScreen.PreviousTrack ->
                    controller?.seekToPreviousMediaItem()

                VexStreamMusicScreen.TogglePlayback ->
                    controller?.toggleVexStreamPlayback()

                VexStreamMusicScreen.NextTrack ->
                    controller?.seekToNextMediaItem()

                VexStreamMusicScreen.ToggleShuffle ->
                    controller?.toggleVexStreamShuffle()

                else -> Unit
            }
        }

    VexContainer(
        element = VexStreamMusicScreen.PlayerSection,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        style = VexElementStyles.RaisedSurface,
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
                VexButton(
                    element = VexStreamMusicScreen.PreviousTrack,
                    handler = handler,
                    enabled =
                        controller?.hasPreviousMediaItem() == true,
                )
                VexButton(
                    element = VexStreamMusicScreen.TogglePlayback,
                    handler = handler,
                    enabled = controller != null,
                )
                VexButton(
                    element = VexStreamMusicScreen.NextTrack,
                    handler = handler,
                    enabled =
                        controller?.hasNextMediaItem() == true,
                )
                VexButton(
                    element = VexStreamMusicScreen.ToggleShuffle,
                    handler = handler,
                    style =
                        if (playback.shuffleEnabled) {
                            VexElementStyles.PrimaryAction
                        } else {
                            VexElementStyles.SecondaryAction
                        },
                    enabled = controller != null,
                )
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
    val sheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = false,
        )
    val handler =
        ElementActionHandler { element ->
            when (element) {
                VexStreamMusicScreen.QueueToggleShuffle -> {
                    runtime.activate(
                        element,
                    )
                    controller?.toggleVexStreamShuffle()
                }

                VexStreamMusicScreen.ToggleRepeat -> {
                    runtime.activate(
                        element,
                    )
                    controller?.toggleVexStreamRepeatAll()
                }

                VexStreamMusicScreen.ClearQueue -> {
                    runtime.activate(
                        element,
                    )
                    controller?.clearMediaItems()
                }

                else ->
                    runtime.activate(
                        element,
                    )
            }
        }

    ModalBottomSheet(
        modifier = Modifier
            .fillMaxHeight()
            .vexElement(
                VexStreamMusicScreen.QueueSheet,
            ),
        onDismissRequest = {
            runtime.dismissPresentation(
                VexStreamMusicScreen.QueueSheet,
            )
        },
        sheetState = sheetState,
        dragHandle = {
            BottomSheetDefaults.DragHandle()
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
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
                VexButton(
                    element = VexStreamMusicScreen.QueueToggleShuffle,
                    handler = handler,
                    style =
                        if (playback.shuffleEnabled) {
                            VexElementStyles.PrimaryAction
                        } else {
                            VexElementStyles.SecondaryAction
                        },
                    enabled = controller != null,
                )
                VexButton(
                    element = VexStreamMusicScreen.ToggleRepeat,
                    handler = handler,
                    style =
                        if (playback.repeatEnabled) {
                            VexElementStyles.PrimaryAction
                        } else {
                            VexElementStyles.SecondaryAction
                        },
                    enabled = controller != null,
                )
                VexButton(
                    element = VexStreamMusicScreen.ClearQueue,
                    handler = handler,
                    enabled =
                        controller != null &&
                            playback.hasQueue,
                )
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
                    modifier = Modifier.weight(1f),
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

            VexButton(
                element = VexStreamMusicScreen.CloseQueue,
                handler = handler,
                modifier = Modifier.fillMaxWidth(),
            )
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
