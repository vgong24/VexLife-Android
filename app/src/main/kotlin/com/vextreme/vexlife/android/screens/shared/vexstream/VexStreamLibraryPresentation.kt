package com.vextreme.vexlife.android.screens.shared.vexstream

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vextreme.vexlife.android.R
import com.vextreme.vexlife.android.data.vexstream.VexStreamLibraryLoadState
import com.vextreme.vexlife.android.data.vexstream.VexStreamLocalCopyState
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackState
import com.vextreme.vexlife.android.elements.ElementActionHandler
import com.vextreme.vexlife.android.elements.VexActionFab
import com.vextreme.vexlife.android.elements.VexButton
import com.vextreme.vexlife.android.elements.VexElementStyles
import com.vextreme.vexlife.android.elements.VexModifier
import com.vextreme.vexlife.android.elements.VexStableLazyColumn
import com.vextreme.vexlife.android.elements.VexText
import com.vextreme.vexlife.android.runtime.VexRuntime
import com.vextreme.vexlife.android.vexstream.collection.VexStreamCollectionPlaybackOrder
import com.vextreme.vexlife.android.vexstream.collection.VexStreamCollectionPlaybackRequest
import com.vextreme.vexlife.android.vexstream.collection.rememberVexStreamCollectionPlaybackConnection
import com.vextreme.vexlife.android.vexstream.offline.VexStreamOfflineCopyConnection
import com.vextreme.vexlife.android.vexstream.track.rememberVexStreamTrackPlaybackConnection

/**
 * Library-level VexStream Android presentation.
 *
 * This file renders library navigation and track-list surfaces from already-owned repository and
 * playback inputs. It owns no catalog, Playback, Queue, download, or Media3 truth.
 */
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
internal fun VexStreamLibraryLoadingStatus(
    loadState: VexStreamLibraryLoadState,
    modifier: Modifier = Modifier,
) {
    if (
        loadState ==
        VexStreamLibraryLoadState.READY
    ) {
        return
    }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 20.dp,
                    vertical = 14.dp,
                ),
        horizontalArrangement =
            Arrangement.spacedBy(
                12.dp,
            ),
        verticalAlignment =
            Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            modifier =
                Modifier.size(
                    20.dp,
                ),
            strokeWidth = 2.dp,
        )
        VexText(
            element =
                when (loadState) {
                    VexStreamLibraryLoadState.RESTORING_LOCAL ->
                        VexStreamMusicScreen
                            .LibraryRestoringDownloads

                    VexStreamLibraryLoadState.REFRESHING_CATALOG ->
                        VexStreamMusicScreen
                            .LibraryRefreshing

                    VexStreamLibraryLoadState.READY ->
                        error(
                            "READY library state has no loading presentation",
                        )
                },
        )
    }
}

@Composable
internal fun VexStreamLibraryLoadingPane(
    runtime: VexRuntime,
    loadState: VexStreamLibraryLoadState,
) {
    LazyColumn(
        modifier =
            Modifier.fillMaxSize(),
        verticalArrangement =
            Arrangement.spacedBy(
                8.dp,
            ),
    ) {
        item(
            key = "library-loading-tabs",
        ) {
            VexStreamLibraryTabs(
                runtime = runtime,
            )
        }
        item(
            key = "library-loading-status",
        ) {
            VexStreamLibraryLoadingStatus(
                loadState = loadState,
            )
        }
    }
}

internal fun vexStreamLibraryPlaybackRequest(
    tracks: List<VexStreamTrackState>,
    order: VexStreamCollectionPlaybackOrder,
): VexStreamCollectionPlaybackRequest =
    VexStreamCollectionPlaybackRequest(
        tracks = tracks,
        order = order,
    )

@Composable
internal fun VexStreamLibraryFabActions(
    runtime: VexRuntime,
    tracks: List<VexStreamTrackState>,
    controller: androidx.media3.session.MediaController?,
    resolver: VexStreamPlaybackSourceResolver?,
) {
    val connection =
        rememberVexStreamCollectionPlaybackConnection(
            resolver =
                resolver,
        )
    val playRequest =
        remember(
            tracks,
        ) {
            vexStreamLibraryPlaybackRequest(
                tracks = tracks,
                order =
                    VexStreamCollectionPlaybackOrder.Natural,
            )
        }
    val shuffleRequest =
        remember(
            tracks,
        ) {
            vexStreamLibraryPlaybackRequest(
                tracks = tracks,
                order =
                    VexStreamCollectionPlaybackOrder.Shuffled,
            )
        }
    val handler =
        ElementActionHandler { element ->
            runtime.activate(
                element,
            )
            when (element) {
                VexStreamMusicScreen.PlayAll ->
                    connection?.play(
                        playRequest,
                    )

                VexStreamMusicScreen.ShuffleAll ->
                    connection?.play(
                        shuffleRequest,
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
            enabled =
                connection
                    ?.isAvailable(
                        playRequest,
                    ) == true,
        )
        VexActionFab(
            element = VexStreamMusicScreen.ShuffleAll,
            handler = handler,
            enabled =
                connection
                    ?.isAvailable(
                        shuffleRequest,
                    ) == true,
        )
    }
}

/**
 * Songs panel realization.
 *
 * The registered panel identity is also the repeated-item container identity. Stable item identity
 * remains the trackRef owned by VexStream; the shared list owns only Android repeated-item mechanics.
 * This makes the population relationship inspectable without registering runtime tracks as static
 * Screen elements or teaching a downstream projector feature-specific row semantics.
 */
@Composable
internal fun VexStreamSongsPane(
    runtime: VexRuntime,
    tracks: List<VexStreamTrackState>,
    canShowTerminalEmptyState: Boolean = true,
    offlineCopyConnection: VexStreamOfflineCopyConnection?,
    controller: androidx.media3.session.MediaController?,
    resolver: VexStreamPlaybackSourceResolver?,
    onVisitArtist: (String) -> Unit,
) {
    val trackPlaybackConnection =
        rememberVexStreamTrackPlaybackConnection(
            resolver =
                resolver,
        )
    VexStableLazyColumn(
        listRef =
            VexStreamMusicScreen.SongsPanel.elementId,
        items = tracks,
        itemRef = {
            it.track.trackRef.value
        },
        contentType = {
            "vexstream-track-row"
        },
        modifier = VexModifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        headerContent = {
            VexStreamLibraryTabs(
                runtime = runtime,
            )
        },
        emptyContent = {
            if (canShowTerminalEmptyState) {
                VexStreamEmptyMessage(
                    text =
                        stringResource(
                            R.string.vexstream_music_empty,
                        ),
                )
            }
        },
    ) { track ->
        VexStreamTrackCard(
            runtime = runtime,
            presentationScope =
                VexStreamTrackPresentationScope.Songs,
            state = track,
            contextTracks = tracks,
            playbackConnection =
                trackPlaybackConnection,
            offlineCopyConnection =
                offlineCopyConnection,
            onVisitArtist = onVisitArtist,
        )
    }
}

/**
 * Downloads panel realization.
 *
 * The same repeated-row law is reused under the registered Downloads panel. Filtering remains
 * VexStream feature truth; the stable-list owner receives only the already-selected runtime items.
 */
@Composable
internal fun VexStreamDownloadsPane(
    runtime: VexRuntime,
    tracks: List<VexStreamTrackState>,
    canShowTerminalEmptyState: Boolean = true,
    offlineCopyConnection: VexStreamOfflineCopyConnection?,
    controller: androidx.media3.session.MediaController?,
    resolver: VexStreamPlaybackSourceResolver?,
    onVisitArtist: (String) -> Unit,
) {
    val trackPlaybackConnection =
        rememberVexStreamTrackPlaybackConnection(
            resolver =
                resolver,
        )
    val downloaded =
        remember(tracks) {
            tracks.filter {
                it.localCopy is
                    VexStreamLocalCopyState.Downloaded
            }
        }
    VexStableLazyColumn(
        listRef =
            VexStreamMusicScreen.DownloadsPanel.elementId,
        items = downloaded,
        itemRef = {
            it.track.trackRef.value
        },
        contentType = {
            "vexstream-track-row"
        },
        modifier = VexModifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        headerContent = {
            VexStreamLibraryTabs(
                runtime = runtime,
            )
        },
        emptyContent = {
            if (canShowTerminalEmptyState) {
                VexStreamEmptyMessage(
                    text =
                        stringResource(
                            R.string.vexstream_music_no_downloads,
                        ),
                )
            }
        },
    ) { track ->
        VexStreamTrackCard(
            runtime = runtime,
            presentationScope =
                VexStreamTrackPresentationScope.Downloads,
            state = track,
            contextTracks = downloaded,
            playbackConnection =
                trackPlaybackConnection,
            offlineCopyConnection =
                offlineCopyConnection,
            onVisitArtist = onVisitArtist,
        )
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

// [VXG RealForever]
