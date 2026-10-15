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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vextreme.vexlife.android.R
import com.vextreme.vexlife.android.data.vexstream.VexStreamConnectionState
import com.vextreme.vexlife.android.data.vexstream.VexStreamHubState
import com.vextreme.vexlife.android.data.vexstream.VexStreamLocalCopyState
import com.vextreme.vexlife.android.data.vexstream.VexStreamMusicRepository
import com.vextreme.vexlife.android.data.vexstream.VexStreamMusicState
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackAvailability
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackRef
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackState
import com.vextreme.vexlife.android.elements.ActionElement
import com.vextreme.vexlife.android.elements.ButtonElement
import com.vextreme.vexlife.android.elements.ElementActionHandler
import com.vextreme.vexlife.android.elements.ElementIcon
import com.vextreme.vexlife.android.elements.VexActionFab
import com.vextreme.vexlife.android.elements.VexButton
import com.vextreme.vexlife.android.elements.VexContainer
import com.vextreme.vexlife.android.elements.VexElementStyles
import com.vextreme.vexlife.android.elements.VexIcon
import com.vextreme.vexlife.android.elements.VexIconButton
import com.vextreme.vexlife.android.elements.VexModifier
import com.vextreme.vexlife.android.elements.VexStableLazyColumn
import com.vextreme.vexlife.android.elements.VexTextField
import com.vextreme.vexlife.android.elements.vex
import com.vextreme.vexlife.android.elements.vexElement
import com.vextreme.vexlife.android.elements.navigation.NavigationTrail
import com.vextreme.vexlife.android.elements.navigation.NavigationTrailContent
import com.vextreme.vexlife.android.runtime.VexRuntime
import com.vextreme.vexlife.android.vexstream.playback.present
import com.vextreme.vexlife.android.vexstream.playback.rememberVexStreamMediaController
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackControl
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackDisplayState
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackExperience
import com.vextreme.vexlife.android.vexstream.playback.rememberVexStreamPlaybackExperience
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
    val playbackExperience =
        if (inspectionMode) {
            null
        } else {
            rememberVexStreamPlaybackExperience()
        }
    val playbackDisplayState =
        playbackExperience
            ?.displayState
            ?: VexStreamPlaybackDisplayState.Unknown()
    val playback =
        playbackDisplayState
            .playback
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
    var artistVisitRequest by rememberSaveable {
        mutableStateOf<String?>(null)
    }
    fun visitArtist(artist: String) {
        if (artist.isBlank()) {
            return
        }

        artistVisitRequest = artist
        if (
            runtime.presentation.isOpen(
                VexStreamMusicScreen.SearchSheet,
            )
        ) {
            runtime.presentation.close(
                VexStreamMusicScreen.SearchSheet,
            )
        }
        runtime.presentation.select(
            container = VexStreamMusicScreen.LibraryContent,
            child = VexStreamMusicScreen.ArtistsPanel,
        )
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
            when (
                val displayState =
                    playbackDisplayState
            ) {
                is VexStreamPlaybackDisplayState.Unknown,
                is VexStreamPlaybackDisplayState.Inactive ->
                    Unit

                is VexStreamPlaybackDisplayState.Transitioning -> {
                    VexStreamMiniPlayer(
                        controller = controller,
                        playbackExperience =
                            requireNotNull(
                                playbackExperience,
                            ),
                        runtime = runtime,
                        shuffleSelected =
                            queueShuffleSelected,
                        onShuffle = {
                            toggleQueueShuffleSelection()
                        },
                    )
                }

                is VexStreamPlaybackDisplayState.Active -> {
                    VexStreamMiniPlayer(
                        controller = controller,
                        playbackExperience =
                            requireNotNull(
                                playbackExperience,
                            ),
                        runtime = runtime,
                        shuffleSelected =
                            queueShuffleSelected,
                        onShuffle = {
                            toggleQueueShuffleSelection()
                        },
                    )
                }
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
                            onVisitArtist = ::visitArtist,
                        )

                    VexStreamMusicScreen.ArtistsPanel.elementId ->
                        VexStreamArtistsPane(
                            runtime = runtime,
                            tracks = libraryTracks,
                            controller = controller,
                            resolver = resolver,
                            requestedArtist = artistVisitRequest,
                            onRequestedArtistConsumed = {
                                artistVisitRequest = null
                            },
                            onVisitArtist = ::visitArtist,
                        )

                    VexStreamMusicScreen.PlaylistsPanel.elementId ->
                        VexStreamPlaylistsPane(
                            runtime = runtime,
                            state = state,
                            controller = controller,
                            resolver = resolver,
                            onVisitArtist = ::visitArtist,
                        )

                    VexStreamMusicScreen.DownloadsPanel.elementId ->
                        VexStreamDownloadsPane(
                            runtime = runtime,
                            tracks = libraryTracks,
                            repository = repository,
                            controller = controller,
                            resolver = resolver,
                            onVisitArtist = ::visitArtist,
                        )

                    else ->
                        VexStreamSongsPane(
                            runtime = runtime,
                            tracks = libraryTracks,
                            repository = repository,
                            controller = controller,
                            resolver = resolver,
                            onVisitArtist = ::visitArtist,
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
            onVisitArtist = ::visitArtist,
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
internal fun VexStreamLibraryTabs(
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
    onVisitArtist: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()

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
                VexStableLazyColumn(
                    listRef =
                        VexStreamPresentationRefs
                            .SearchResultsList,
                    items = tracks,
                    itemRef = {
                        it.track.trackRef.value
                    },
                    contentType = {
                        "vexstream-track-row"
                    },
                    modifier = Modifier.weight(1f),
                    verticalArrangement =
                        Arrangement.spacedBy(4.dp),
                ) { track ->
                    VexStreamTrackCard(
                        runtime = runtime,
                        state = track,
                        contextTracks = tracks,
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
                        onVisitArtist = onVisitArtist,
                    )
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

@Composable
private fun VexStreamSongsPane(
    runtime: VexRuntime,
    tracks: List<VexStreamTrackState>,
    repository: VexStreamMusicRepository,
    controller: androidx.media3.session.MediaController?,
    resolver: VexStreamPlaybackSourceResolver,
    onVisitArtist: (String) -> Unit,
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
                    onVisitArtist = onVisitArtist,
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
    onVisitArtist: (String) -> Unit,
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
                    onVisitArtist = onVisitArtist,
                )
            }
        }
    }
}

@Composable
internal fun VexStreamEmptyMessage(
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

// [VXG RealForever]
