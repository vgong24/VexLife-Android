package com.vextreme.vexlife.android.screens.shared.vexstream

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.vextreme.vexlife.android.R
import com.vextreme.vexlife.android.data.vexstream.VexStreamConnectionState
import com.vextreme.vexlife.android.data.vexstream.VexStreamHubState
import com.vextreme.vexlife.android.data.vexstream.VexStreamLocalCopyState
import com.vextreme.vexlife.android.data.vexstream.VexStreamMusicRepository
import com.vextreme.vexlife.android.data.vexstream.VexStreamMusicState
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackAvailability
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackState
import com.vextreme.vexlife.android.elements.ElementActionHandler
import com.vextreme.vexlife.android.elements.ElementIcon
import com.vextreme.vexlife.android.elements.VexActionFab
import com.vextreme.vexlife.android.elements.VexButton
import com.vextreme.vexlife.android.elements.VexContainer
import com.vextreme.vexlife.android.elements.VexElementStyles
import com.vextreme.vexlife.android.elements.VexIcon
import com.vextreme.vexlife.android.elements.VexIconButton
import com.vextreme.vexlife.android.elements.VexTextField
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

    var queueShuffleSelected by rememberSaveable {
        mutableStateOf(false)
    }
    fun toggleQueueShuffleSelection() {
        if (queueShuffleSelected) {
            queueShuffleSelected = false
        } else if (
            controller?.shuffleVexStreamQueue() == true
        ) {
            queueShuffleSelected = true
        }
    }

    var searchQuery by rememberSaveable {
        mutableStateOf("")
    }
    val libraryTracks =
        remember(state.tracks) {
            state.playableTracks()
        }
    val searchResults =
        remember(
            state.tracks,
            searchQuery,
        ) {
            state.playableTracks().filter {
                it.matchesSearch(
                    searchQuery,
                )
            }
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
                    shuffleSelected = queueShuffleSelected,
                    onShuffle = {
                        toggleQueueShuffleSelection()
                    },
                )
            }
        },
        floatingActionButton = {
            if (
                libraryTracks.isNotEmpty() &&
                controller != null
            ) {
                VexStreamLibraryFabActions(
                    runtime = runtime,
                    tracks = libraryTracks,
                    controller = controller,
                    resolver = resolver,
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (state.connectionState !is VexStreamConnectionState.Paired) {
                VexStreamConnectionSetup(
                    state = state,
                    runtime = runtime,
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                when (selected.elementId) {
                    VexStreamMusicScreen.AlbumsPanel.elementId ->
                        VexStreamAlbumsPane(
                            runtime = runtime,
                            tracks = libraryTracks,
                            controller = controller,
                            resolver = resolver,
                        )

                    VexStreamMusicScreen.ArtistsPanel.elementId ->
                        VexStreamArtistsPane(
                            runtime = runtime,
                            tracks = libraryTracks,
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
                            tracks = libraryTracks,
                            repository = repository,
                            controller = controller,
                            resolver = resolver,
                        )

                    else ->
                        VexStreamSongsPane(
                            runtime = runtime,
                            tracks = libraryTracks,
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
            shuffleSelected = queueShuffleSelected,
            onShuffle = {
                toggleQueueShuffleSelection()
            },
        )
    }

    if (runtime.presentation.isOpen(VexStreamMusicScreen.SearchSheet)) {
        VexStreamSearchSheet(
            runtime = runtime,
            tracks =
                if (searchQuery.isBlank()) {
                    emptyList()
                } else {
                    searchResults
                },
            repository = repository,
            controller = controller,
            resolver = resolver,
            query = searchQuery,
            onQueryChange = {
                searchQuery = it
            },
        )
    }

    if (runtime.presentation.isOpen(VexStreamMusicScreen.ConnectionMenuSheet)) {
        VexStreamConnectionMenuSheet(
            state = state,
            runtime = runtime,
        )
    }

    if (runtime.presentation.isOpen(VexStreamMusicScreen.ForgetConfirmationDialog)) {
        VexStreamForgetConfirmationDialog(
            runtime = runtime,
        )
    }
}

@Composable
private fun VexStreamLibraryTabs(
    runtime: VexRuntime,
) {
    val selectedElementId =
        runtime.presentation
            .selected(VexStreamMusicScreen.LibraryContent)
            .elementId
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
    }
}

@Composable
private fun VexStreamConnectionSetup(
    state: VexStreamMusicState,
    runtime: VexRuntime,
) {
    val connection = state.connectionState

    VexContainer(
        element = VexStreamMusicScreen.ConnectionSection,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 12.dp,
                vertical = 8.dp,
            ),
        style = VexElementStyles.RaisedSurface,
    ) {
        when (connection) {
            VexStreamConnectionState.NotPaired -> {
                Text(
                    text =
                        stringResource(
                            R.string.vexstream_music_status_not_configured,
                        ),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text =
                        stringResource(
                            R.string.vexstream_music_intro,
                        ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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

            is VexStreamConnectionState.Paired ->
                Unit
        }
    }
}

@Composable
private fun VexStreamLibraryFabActions(
    runtime: VexRuntime,
    tracks: List<VexStreamTrackState>,
    controller: androidx.media3.session.MediaController?,
    resolver: VexStreamPlaybackSourceResolver,
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

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        VexActionFab(
            element = VexStreamMusicScreen.PlayAll,
            handler = handler,
            enabled = canPlay,
        )
        VexActionFab(
            element = VexStreamMusicScreen.ShuffleAll,
            handler = handler,
            enabled = canPlay,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VexStreamSearchSheet(
    runtime: VexRuntime,
    tracks: List<VexStreamTrackState>,
    repository: VexStreamMusicRepository,
    controller: androidx.media3.session.MediaController?,
    resolver: VexStreamPlaybackSourceResolver,
    query: String,
    onQueryChange: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var selectedTrackRef by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    ModalBottomSheet(
        modifier = Modifier
            .fillMaxHeight()
            .vexElement(
                VexStreamMusicScreen.SearchSheet,
            ),
        onDismissRequest = {
            runtime.dismissPresentation(
                VexStreamMusicScreen.SearchSheet,
            )
        },
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        ),
        dragHandle = {
            BottomSheetDefaults.DragHandle()
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            VexTextField(
                element = VexStreamMusicScreen.SearchQuery,
                value = query,
                onValueChange = onQueryChange,
            )

            if (query.isBlank()) {
                Text(
                    text =
                        stringResource(
                            R.string.vexstream_music_search,
                        ),
                    modifier = Modifier.padding(
                        vertical = 20.dp,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (tracks.isEmpty()) {
                VexStreamEmptyMessage(
                    text =
                        stringResource(
                            R.string.vexstream_music_no_search_results,
                        ),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(
                        items = tracks,
                        key = {
                            it.track.trackRef.value
                        },
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

            VexButton(
                element = VexStreamMusicScreen.CloseSearch,
                handler = runtime,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VexStreamConnectionMenuSheet(
    state: VexStreamMusicState,
    runtime: VexRuntime,
) {
    val paired =
        state.connectionState as? VexStreamConnectionState.Paired

    ModalBottomSheet(
        modifier = Modifier.vexElement(
            VexStreamMusicScreen.ConnectionMenuSheet,
        ),
        onDismissRequest = {
            runtime.dismissPresentation(
                VexStreamMusicScreen.ConnectionMenuSheet,
            )
        },
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = false,
        ),
        dragHandle = {
            BottomSheetDefaults.DragHandle()
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(
                    R.string.vexstream_music_connection_title,
                ),
                style = MaterialTheme.typography.headlineSmall,
            )

            paired?.let {
                Text(
                    text = it.displayName,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text =
                        if (state.hubState is VexStreamHubState.Ready) {
                            stringResource(
                                R.string.vexstream_music_status_ready,
                            )
                        } else {
                            stringResource(
                                R.string.vexstream_music_status_unreachable,
                            )
                        },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (state.hubState is VexStreamHubState.Ready) {
                VexButton(
                    element = VexStreamMusicScreen.RefreshCatalog,
                    handler = runtime,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                VexButton(
                    element = VexStreamMusicScreen.Reconnect,
                    handler = runtime,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            VexButton(
                element = VexStreamMusicScreen.RequestForgetPaired,
                handler = runtime,
                modifier = Modifier.fillMaxWidth(),
            )

            VexButton(
                element = VexStreamMusicScreen.CloseConnectionMenu,
                handler = runtime,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun VexStreamForgetConfirmationDialog(
    runtime: VexRuntime,
) {
    AlertDialog(
        modifier = Modifier.vexElement(
            VexStreamMusicScreen.ForgetConfirmationDialog,
        ),
        onDismissRequest = {
            runtime.dismissPresentation(
                VexStreamMusicScreen.ForgetConfirmationDialog,
            )
        },
        title = {
            Text(
                stringResource(
                    R.string.vexstream_music_forget_confirm_title,
                ),
            )
        },
        text = {
            Text(
                stringResource(
                    R.string.vexstream_music_forget_confirm_body,
                ),
            )
        },
        confirmButton = {
            VexButton(
                element = VexStreamMusicScreen.ForgetPaired,
                handler = runtime,
            )
        },
        dismissButton = {
            VexButton(
                element = VexStreamMusicScreen.CancelForgetPaired,
                handler = runtime,
            )
        },
    )
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
            key = "songs-tabs",
        ) {
            VexStreamLibraryTabs(
                runtime = runtime,
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
            key = "downloads-tabs",
        ) {
            VexStreamLibraryTabs(
                runtime = runtime,
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

    val durationText =
        state.track.durationSeconds
            .takeIf { it > 0.0 }
            ?.let {
                formatVexStreamPlaybackTime(
                    (it * 1_000.0).toLong(),
                )
            }
    val artistText =
        state.track.artist
            .takeIf {
                it.isNotBlank() &&
                    !it.equals(
                        state.track.title,
                        ignoreCase = true,
                    )
            }
    val secondaryText =
        listOfNotNull(
            artistText,
            durationText,
        ).joinToString(" · ")
    val compactStatus =
        when {
            state.localCopy is VexStreamLocalCopyState.Downloading ->
                stringResource(
                    R.string.vexstream_music_downloading,
                )

            state.localCopy is VexStreamLocalCopyState.Downloaded ->
                stringResource(
                    R.string.vexstream_music_downloaded,
                )

            state.availability == VexStreamTrackAvailability.LOCAL_ONLY ->
                stringResource(
                    R.string.vexstream_music_local_only,
                )

            state.availability == VexStreamTrackAvailability.UNAVAILABLE ->
                stringResource(
                    R.string.vexstream_music_unavailable,
                )

            else ->
                null
        }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 8.dp,
                vertical = 2.dp,
            )
            .clickable(
                onClick = onSelect,
            ),
        colors =
            if (selected) {
                CardDefaults.cardColors(
                    containerColor =
                        MaterialTheme
                            .colorScheme
                            .secondaryContainer,
                )
            } else {
                CardDefaults.cardColors(
                    containerColor =
                        MaterialTheme
                            .colorScheme
                            .surface,
                )
            },
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 10.dp,
                vertical = 8.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    modifier = Modifier.size(54.dp),
                    shape = MaterialTheme.shapes.medium,
                    color =
                        MaterialTheme
                            .colorScheme
                            .surfaceContainerHigh,
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                    ) {
                        VexIcon(
                            icon = ElementIcon.Music,
                            contentDescription = null,
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = state.track.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    if (secondaryText.isNotBlank()) {
                        Text(
                            text = secondaryText,
                            style = MaterialTheme.typography.bodyMedium,
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    compactStatus?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelSmall,
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            if (selected) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    VexIconButton(
                        element = VexStreamMusicScreen.PlayTrack,
                        handler = handler,
                        enabled = controller != null,
                    )
                    VexIconButton(
                        element = VexStreamMusicScreen.PlayTrackNext,
                        handler = handler,
                        enabled = controller != null,
                    )
                    VexIconButton(
                        element = VexStreamMusicScreen.AddTrackToQueue,
                        handler = handler,
                        enabled = controller != null,
                    )

                    if (showDownloadAction) {
                        when (state.localCopy) {
                            is VexStreamLocalCopyState.Downloaded ->
                                VexIconButton(
                                    element =
                                        VexStreamMusicScreen
                                            .RemoveTrackDownload,
                                    handler = handler,
                                )

                            is VexStreamLocalCopyState.Downloading ->
                                Unit

                            else -> {
                                if (state.hubAvailable) {
                                    VexIconButton(
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
}

@Composable
private fun VexStreamMiniPlayer(
    playback: VexStreamPlaybackUiState,
    controller: androidx.media3.session.MediaController?,
    runtime: VexRuntime,
    shuffleSelected: Boolean,
    onShuffle: () -> Unit,
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

                VexStreamMusicScreen.ShuffleQueue ->
                    onShuffle()

                else -> Unit
            }
        }
    val queuePosition =
        playback.currentIndex
            .takeIf { it >= 0 }
            ?.let { index ->
                (index + 1).toString() +
                    "/" +
                    playback.queueMediaIds.size
            }

    VexContainer(
        element = VexStreamMusicScreen.PlayerSection,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        style = VexElementStyles.RaisedSurface,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text =
                        playback.currentTitle.ifBlank {
                            stringResource(
                                R.string.vexstream_music_now_playing,
                            )
                        },
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (playback.currentArtist.isNotBlank()) {
                    Text(
                        text = playback.currentArtist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            queuePosition?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        LinearProgressIndicator(
            progress = {
                playback.progressFraction
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text =
                    formatVexStreamPlaybackTime(
                        playback.currentPositionMs,
                    ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text =
                    if (playback.durationMs > 0L) {
                        formatVexStreamPlaybackTime(
                            playback.durationMs,
                        )
                    } else {
                        "--:--"
                    },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            VexIconButton(
                element = VexStreamMusicScreen.PreviousTrack,
                handler = handler,
                enabled =
                    controller?.hasPreviousMediaItem() == true,
            )
            VexIconButton(
                element = VexStreamMusicScreen.TogglePlayback,
                handler = handler,
                enabled = controller != null,
                iconOverride =
                    if (playback.isPlaying) {
                        ElementIcon.Pause
                    } else {
                        ElementIcon.Play
                    },
            )
            VexIconButton(
                element = VexStreamMusicScreen.NextTrack,
                handler = handler,
                enabled =
                    controller?.hasNextMediaItem() == true,
            )
            VexIconButton(
                element = VexStreamMusicScreen.ShuffleQueue,
                handler = handler,
                enabled = controller != null,
                selected = shuffleSelected,
            )
            VexIconButton(
                element = VexStreamMusicScreen.ShowQueue,
                handler = runtime,
            )
        }
    }
}

private data class VexStreamQueueEntry(
    val stableKey: String,
    val mediaId: String,
)

private fun vexStreamQueueEntries(
    mediaIds: List<String>,
): List<VexStreamQueueEntry> {
    val occurrences =
        mutableMapOf<String, Int>()

    return mediaIds.map { mediaId ->
        val occurrence =
            (occurrences[mediaId] ?: 0) + 1
        occurrences[mediaId] = occurrence
        VexStreamQueueEntry(
            stableKey = "$mediaId#$occurrence",
            mediaId = mediaId,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VexStreamQueueSheet(
    state: VexStreamMusicState,
    playback: VexStreamPlaybackUiState,
    controller: androidx.media3.session.MediaController?,
    runtime: VexRuntime,
    shuffleSelected: Boolean,
    onShuffle: () -> Unit,
) {
    val tracksByRef =
        remember(state.tracks) {
            state.tracks.associateBy {
                it.track.trackRef.value
            }
        }
    val queueEntries =
        remember(
            playback.queueMediaIds,
        ) {
            vexStreamQueueEntries(
                playback.queueMediaIds,
            )
        }
    val listState =
        rememberLazyListState()
    var draggedKey by remember {
        mutableStateOf<String?>(null)
    }
    var draggedIndex by remember {
        mutableIntStateOf(-1)
    }
    var dragOffsetPx by remember {
        mutableFloatStateOf(0f)
    }
    var dragMoved by remember {
        mutableStateOf(false)
    }

    fun finishQueueDrag() {
        if (dragMoved) {
            runtime.activate(
                VexStreamMusicScreen.QueueReorder,
            )
        }
        draggedKey = null
        draggedIndex = -1
        dragOffsetPx = 0f
        dragMoved = false
    }

    val sheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = false,
        )
    val handler =
        ElementActionHandler { element ->
            when (element) {
                VexStreamMusicScreen.QueueShuffle -> {
                    runtime.activate(
                        element,
                    )
                    onShuffle()
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
                VexIconButton(
                    element = VexStreamMusicScreen.QueueShuffle,
                    handler = handler,
                    enabled = controller != null,
                    selected = shuffleSelected,
                )
                VexIconButton(
                    element = VexStreamMusicScreen.ToggleRepeat,
                    handler = handler,
                    enabled = controller != null,
                    selected = playback.repeatEnabled,
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

            Text(
                text =
                    stringResource(
                        R.string.vexstream_music_queue_reorder_hint,
                    ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

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
                    state = listState,
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    itemsIndexed(
                        items = queueEntries,
                        key = { _, entry ->
                            entry.stableKey
                        },
                    ) { index, entry ->
                        val latestIndex by rememberUpdatedState(index)
                        val track =
                            tracksByRef[entry.mediaId]
                        val isCurrent =
                            index == playback.currentIndex
                        val isDragging =
                            draggedKey ==
                                entry.stableKey
                        val queuePlayEnabled =
                            controller != null &&
                                controller.isCommandAvailable(
                                    androidx.media3.common.Player
                                        .COMMAND_SEEK_TO_MEDIA_ITEM,
                                ) &&
                                controller.isCommandAvailable(
                                    androidx.media3.common.Player
                                        .COMMAND_PLAY_PAUSE,
                                )
                        val reorderEnabled =
                            controller != null &&
                                controller.isCommandAvailable(
                                    androidx.media3.common.Player
                                        .COMMAND_CHANGE_MEDIA_ITEMS,
                                )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .vexElement(
                                    VexStreamMusicScreen.QueuePlay,
                                )
                                .clickable(
                                    enabled = queuePlayEnabled,
                                ) {
                                    runtime.activate(
                                        VexStreamMusicScreen.QueuePlay,
                                    )
                                    if (
                                        latestIndex !=
                                        playback.currentIndex
                                    ) {
                                        controller
                                            ?.seekToDefaultPosition(
                                                latestIndex,
                                            )
                                    }
                                    controller?.play()
                                }
                                .graphicsLayer {
                                    translationY =
                                        if (isDragging) {
                                            dragOffsetPx
                                        } else {
                                            0f
                                        }
                                }
                                .zIndex(
                                    if (isDragging) {
                                        1f
                                    } else {
                                        0f
                                    },
                                )
                                .padding(
                                    vertical = 9.dp,
                                ),
                            horizontalArrangement =
                                Arrangement.spacedBy(
                                    10.dp,
                                ),
                            verticalAlignment =
                                Alignment.CenterVertically,
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
                                            ?: entry.mediaId,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (isCurrent) {
                                    Text(
                                        text =
                                            stringResource(
                                                R.string
                                                    .vexstream_music_queue_current,
                                            ),
                                        style =
                                            MaterialTheme
                                                .typography
                                                .labelSmall,
                                        color =
                                            MaterialTheme
                                                .colorScheme
                                                .primary,
                                    )
                                }
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
                                            maxLines = 1,
                                            overflow =
                                                TextOverflow.Ellipsis,
                                        )
                                    }
                            }

                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .vexElement(
                                        VexStreamMusicScreen.QueueReorder,
                                    )
                                    .pointerInput(
                                        entry.stableKey,
                                        reorderEnabled,
                                    ) {
                                        if (!reorderEnabled) {
                                            return@pointerInput
                                        }

                                        detectVerticalDragGestures(
                                            onDragStart = {
                                                draggedKey =
                                                    entry.stableKey
                                                draggedIndex =
                                                    latestIndex
                                                dragOffsetPx = 0f
                                                dragMoved = false
                                            },
                                            onDragCancel = {
                                                finishQueueDrag()
                                            },
                                            onDragEnd = {
                                                finishQueueDrag()
                                            },
                                        ) { change, dragAmount ->
                                            change.consume()
                                            if (
                                                draggedKey !=
                                                    entry.stableKey
                                            ) {
                                                return@detectVerticalDragGestures
                                            }

                                            dragOffsetPx +=
                                                dragAmount

                                            val itemInfo =
                                                listState
                                                    .layoutInfo
                                                    .visibleItemsInfo
                                                    .firstOrNull {
                                                        it.key ==
                                                            entry.stableKey
                                                    }
                                                    ?: return@detectVerticalDragGestures
                                            val target =
                                                vexStreamQueueAdjacentMoveTarget(
                                                    currentIndex =
                                                        draggedIndex,
                                                    dragOffsetPx =
                                                        dragOffsetPx,
                                                    itemHeightPx =
                                                        itemInfo.size,
                                                    lastIndex =
                                                        queueEntries.lastIndex,
                                                )
                                                    ?: return@detectVerticalDragGestures

                                            controller?.moveMediaItem(
                                                draggedIndex,
                                                target,
                                            )
                                            dragMoved = true

                                            val height =
                                                itemInfo.size
                                                    .toFloat()
                                            dragOffsetPx +=
                                                if (
                                                    target >
                                                    draggedIndex
                                                ) {
                                                    -height
                                                } else {
                                                    height
                                                }
                                            draggedIndex =
                                                target
                                        }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector =
                                        Icons.Rounded.DragHandle,
                                    contentDescription =
                                        stringResource(
                                            R.string
                                                .vexstream_music_queue_reorder,
                                        ),
                                    tint =
                                        if (reorderEnabled) {
                                            MaterialTheme
                                                .colorScheme
                                                .onSurfaceVariant
                                        } else {
                                            MaterialTheme
                                                .colorScheme
                                                .onSurfaceVariant
                                                .copy(
                                                    alpha = 0.38f,
                                                )
                                        },
                                )
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

private fun VexStreamTrackState.matchesSearch(
    query: String,
): Boolean {
    val normalized =
        query.trim()
    if (normalized.isBlank()) {
        return true
    }

    return sequenceOf(
        track.title,
        track.artist,
        track.album,
        track.genre,
        track.year,
    ).any {
        it.contains(
            normalized,
            ignoreCase = true,
        )
    }
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
