package com.vextreme.vexlife.android.screens.shared.vexstream

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import com.vextreme.vexlife.android.data.vexstream.VexStreamConnectionState
import com.vextreme.vexlife.android.data.vexstream.VexStreamMusicRepository
import com.vextreme.vexlife.android.elements.VexText
import com.vextreme.vexlife.android.elements.navigation.NavigationTrail
import com.vextreme.vexlife.android.runtime.VexRuntime
import com.vextreme.vexlife.android.vexstream.offline.VexStreamOfflineCopyConnection
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackDisplayState
import com.vextreme.vexlife.android.vexstream.playback.rememberVexStreamMediaController
import com.vextreme.vexlife.android.vexstream.playback.rememberVexStreamPlaybackExperience

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
    offlineCopyConnection: VexStreamOfflineCopyConnection,
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

    var searchQuery by rememberSaveable {
        mutableStateOf("")
    }
    var organization by remember {
        mutableStateOf(
            VexStreamLibraryOrganization(),
        )
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

    val libraryProjection =
        remember(
            state.tracks,
            state.libraryLoadState,
            searchQuery,
            organization,
        ) {
            state.toVexStreamLibraryProjection(
                searchQuery = searchQuery,
                organization = organization,
            )
        }
    val libraryTracks =
        libraryProjection.playableTracks
    val hasKnownPlayableTracks =
        state.playableTracks()
            .isNotEmpty()
    val organizationOptions =
        remember(
            state.tracks,
        ) {
            state.toVexStreamLibraryOrganizationOptions()
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

            if (
                libraryProjection.isLoading &&
                hasKnownPlayableTracks
            ) {
                VexStreamLibraryLoadingStatus(
                    loadState =
                        libraryProjection.loadState,
                )
            }

            if (organization.hasActiveFilters) {
                VexText(
                    element =
                        if (libraryTracks.isEmpty()) {
                            VexStreamMusicScreen.FiltersNoMatches
                        } else {
                            VexStreamMusicScreen.FiltersActive
                        },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 12.dp,
                                vertical = 6.dp,
                            ),
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                when {
                    libraryProjection.isLoading &&
                        !hasKnownPlayableTracks ->
                        VexStreamLibraryLoadingPane(
                            runtime = runtime,
                            loadState =
                                libraryProjection.loadState,
                        )

                    selected.elementId ==
                        VexStreamMusicScreen.AlbumsPanel.elementId ->
                        VexStreamAlbumsPane(
                            runtime = runtime,
                            tracks = libraryTracks,
                            offlineCopyConnection =
                                offlineCopyConnection,
                            controller = controller,
                            resolver = resolver,
                            onVisitArtist = ::visitArtist,
                        )

                    selected.elementId ==
                        VexStreamMusicScreen.ArtistsPanel.elementId ->
                        VexStreamArtistsPane(
                            runtime = runtime,
                            tracks = libraryTracks,
                            offlineCopyConnection =
                                offlineCopyConnection,
                            controller = controller,
                            resolver = resolver,
                            requestedArtist = artistVisitRequest,
                            onRequestedArtistConsumed = {
                                artistVisitRequest = null
                            },
                            onVisitArtist = ::visitArtist,
                        )

                    selected.elementId ==
                        VexStreamMusicScreen.PlaylistsPanel.elementId ->
                        VexStreamPlaylistsPane(
                            runtime = runtime,
                            state = state,
                            visibleTrackRefs =
                                if (organization.hasActiveFilters) {
                                    libraryTracks
                                        .map {
                                            it.track.trackRef
                                        }
                                        .toSet()
                                } else {
                                    null
                                },
                            offlineCopyConnection =
                                offlineCopyConnection,
                            controller = controller,
                            resolver = resolver,
                            onVisitArtist = ::visitArtist,
                        )

                    selected.elementId ==
                        VexStreamMusicScreen.DownloadsPanel.elementId ->
                        VexStreamDownloadsPane(
                            runtime = runtime,
                            tracks = libraryTracks,
                            libraryLoadState =
                                libraryProjection.loadState,
                            offlineCopyConnection =
                                offlineCopyConnection,
                            controller = controller,
                            resolver = resolver,
                            onVisitArtist = ::visitArtist,
                        )

                    else ->
                        VexStreamSongsPane(
                            runtime = runtime,
                            tracks = libraryTracks,
                            libraryLoadState =
                                libraryProjection.loadState,
                            offlineCopyConnection =
                                offlineCopyConnection,
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
        )
    }

    if (runtime.presentation.isOpen(VexStreamMusicScreen.SearchSheet)) {
        VexStreamSearchSheet(
            runtime = runtime,
            tracks =
                if (searchQuery.isBlank()) {
                    emptyList()
                } else {
                    libraryProjection.searchResults
                },
            offlineCopyConnection =
                offlineCopyConnection,
            controller = controller,
            resolver = resolver,
            query = searchQuery,
            onQueryChange = {
                searchQuery = it
            },
            onVisitArtist = ::visitArtist,
        )
    }

    if (runtime.presentation.isOpen(VexStreamMusicScreen.OrganizationSheet)) {
        VexStreamLibraryOrganizationSheet(
            runtime = runtime,
            organization = organization,
            options = organizationOptions,
            resultCount = libraryTracks.size,
            onOrganizationChange = {
                organization = it
            },
        )
    }

    if (runtime.presentation.isOpen(VexStreamMusicScreen.ConnectionMenuSheet)) {
        VexStreamConnectionMenuSheet(
            state = state,
            runtime = runtime,
            offlineCopyConnection =
                offlineCopyConnection,
        )
    }

    if (runtime.presentation.isOpen(VexStreamMusicScreen.ForgetConfirmationDialog)) {
        VexStreamForgetConfirmationDialog(
            runtime = runtime,
        )
    }
}

// [VXG RealForever]
