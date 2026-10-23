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
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackRef
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackState
import com.vextreme.vexlife.android.elements.ElementActionHandler
import com.vextreme.vexlife.android.elements.ScrollableColumnElement
import com.vextreme.vexlife.android.elements.VexButton
import com.vextreme.vexlife.android.elements.VexColumn
import com.vextreme.vexlife.android.elements.VexComposeScope
import com.vextreme.vexlife.android.elements.VexModifier
import com.vextreme.vexlife.android.elements.VexRow
import com.vextreme.vexlife.android.elements.VexText
import com.vextreme.vexlife.android.elements.vex
import com.vextreme.vexlife.android.elements.navigation.NavigationTrail
import com.vextreme.vexlife.android.elements.navigation.NavigationTrailContent
import com.vextreme.vexlife.android.runtime.VexRuntime
import com.vextreme.vexlife.android.vexstream.collection.VexStreamCollectionPlaybackOrder
import com.vextreme.vexlife.android.vexstream.collection.VexStreamCollectionPlaybackRequest
import com.vextreme.vexlife.android.vexstream.collection.rememberVexStreamCollectionPlaybackConnection
import com.vextreme.vexlife.android.vexstream.offline.VexStreamOfflineCollectionPlan
import com.vextreme.vexlife.android.vexstream.offline.VexStreamOfflineCopyConnection
import com.vextreme.vexlife.android.vexstream.offline.vexStreamOfflineCollectionPlan
import com.vextreme.vexlife.android.vexstream.track.rememberVexStreamTrackPlaybackConnection

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
    offlineCopyConnection: VexStreamOfflineCopyConnection?,
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
                        tracks = albumTracks,
                    )
                }
                .sortedBy {
                    it.title.lowercase()
                }
        }

    VexStreamCollectionBrowserPane(
        runtime = runtime,
        panel = VexStreamMusicScreen.AlbumsPanel,
        rows = rows,
        emptyText =
            stringResource(
                R.string.vexstream_music_empty,
            ),
        navigationLabel = "vexstream-album-depth",
        offlineCopyConnection =
            offlineCopyConnection,
        controller = controller,
        resolver = resolver,
        onVisitArtist = onVisitArtist,
    )
}

@Composable
internal fun VexStreamArtistsPane(
    runtime: VexRuntime,
    tracks: List<VexStreamTrackState>,
    offlineCopyConnection: VexStreamOfflineCopyConnection?,
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
                    tracks = artistTracks,
                )
            }
            .sortedBy {
                it.title.lowercase()
            }

    VexStreamCollectionBrowserPane(
        runtime = runtime,
        panel = VexStreamMusicScreen.ArtistsPanel,
        rows = rows,
        emptyText =
            stringResource(
                R.string.vexstream_music_empty,
            ),
        navigationLabel = "vexstream-artist-depth",
        offlineCopyConnection =
            offlineCopyConnection,
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
    visibleTrackRefs: Set<VexStreamTrackRef>?,
    offlineCopyConnection: VexStreamOfflineCopyConnection?,
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
    val sourceLabel =
        stringResource(
            R.string.vexstream_music_playlist_source,
        )
    val rows =
        state.playlists
            .map { playlist ->
                val visibleTracks =
                    playlist.trackRefs
                        .mapNotNull(
                            tracksByRef::get,
                        )
                        .filter {
                            visibleTrackRefs == null ||
                                it.track.trackRef in
                                visibleTrackRefs
                        }

                VexStreamCollectionRow(
                    key =
                        "playlist:" +
                            playlist.playlistRef.value,
                    title = playlist.name,
                    subtitle = sourceLabel,
                    tracks = visibleTracks,
                )
            }
            .filter {
                visibleTrackRefs == null ||
                    it.tracks.isNotEmpty()
            }
            .sortedBy {
                it.title.lowercase()
            }

    VexStreamCollectionBrowserPane(
        runtime = runtime,
        panel = VexStreamMusicScreen.PlaylistsPanel,
        rows = rows,
        emptyText =
            stringResource(
                R.string.vexstream_music_no_playlists,
            ),
        navigationLabel = "vexstream-playlist-depth",
        offlineCopyConnection =
            offlineCopyConnection,
        controller = controller,
        resolver = resolver,
        onVisitArtist = onVisitArtist,
    )
}

@Composable
private fun VexStreamCollectionBrowserPane(
    runtime: VexRuntime,
    panel: ScrollableColumnElement,
    rows: List<VexStreamCollectionRow>,
    emptyText: String,
    navigationLabel: String,
    offlineCopyConnection: VexStreamOfflineCopyConnection?,
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

    VexComposeScope(
        parent = panel,
    ) {
        NavigationTrailContent(
            trail = trail,
            modifier =
                VexModifier
                    .fillMaxSize()
                    .vex(panel),
            label = navigationLabel,
        ) { route ->
            when (route) {
                VexStreamCollectionRoute.Index -> {
                    LazyColumn(
                        modifier = VexModifier.fillMaxSize(),
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
                                    runtime = runtime,
                                    collectionKey = row.key,
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
                            offlineCopyConnection =
                                offlineCopyConnection,
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
}

@Composable
private fun VexStreamCollectionDetail(
    runtime: VexRuntime,
    collectionKey: String,
    title: String,
    tracks: List<VexStreamTrackState>,
    offlineCopyConnection: VexStreamOfflineCopyConnection?,
    controller: androidx.media3.session.MediaController?,
    resolver: VexStreamPlaybackSourceResolver,
    onBack: () -> Unit,
    onVisitArtist: (String) -> Unit,
) {
    val trackPlaybackConnection =
        rememberVexStreamTrackPlaybackConnection(
            controller = controller,
            resolver = resolver,
        )
    val offlinePlan =
        remember(
            tracks,
        ) {
            vexStreamOfflineCollectionPlan(
                tracks,
            )
        }

    VexComposeScope(
        parentRef =
            VexStreamPresentationRefs.collectionRow(
                collectionKey,
            ),
    ) {
        LazyColumn(
            modifier = VexModifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(
                key = "collection-header",
            ) {
                VexColumn(
                    id = "detail-header",
                    modifier = VexModifier.padding(12.dp),
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
                    VexText(
                        id = "title",
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    VexText(
                        id = "track-count",
                        text =
                            stringResource(
                                R.string.vexstream_music_track_count,
                                tracks.size,
                            ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    VexText(
                        id = "offline-status",
                        text =
                            vexStreamOfflineCollectionStatusText(
                                offlinePlan,
                            ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    VexStreamCollectionActions(
                        runtime = runtime,
                        tracks = tracks,
                        offlinePlan = offlinePlan,
                        offlineCopyConnection = offlineCopyConnection,
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
                    playbackConnection =
                        trackPlaybackConnection,
                    offlineCopyConnection =
                        offlineCopyConnection,
                    onVisitArtist = onVisitArtist,
                )
            }
        }
    }
}

@Composable
private fun VexStreamCollectionCard(
    runtime: VexRuntime,
    collectionKey: String,
    title: String,
    subtitle: String,
    tracks: List<VexStreamTrackState>,
    onOpen: () -> Unit,
) {
    val offlinePlan =
        remember(
            tracks,
        ) {
            vexStreamOfflineCollectionPlan(
                tracks,
            )
        }
    val openRef =
        VexStreamPresentationRefs.collectionRowAction(
            collectionKey = collectionKey,
            action = VexStreamMusicScreen.OpenCollection,
        )
    Card(
        modifier =
            VexModifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .vex(openRef)
                .clickable {
                    runtime.activateDynamic(
                        element =
                            VexStreamMusicScreen
                                .OpenCollection,
                        subjectRef = openRef,
                    )
                    onOpen()
                },
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
            Text(
                text =
                    vexStreamOfflineCollectionStatusText(
                        offlinePlan,
                    ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun vexStreamOfflineCollectionStatusText(
    plan: VexStreamOfflineCollectionPlan,
): String =
    if (
        plan.fullyOnDevice
    ) {
        stringResource(
            R.string.vexstream_music_collection_available_offline,
            plan.totalCount,
        )
    } else {
        stringResource(
            R.string.vexstream_music_on_device_count,
            plan.onDeviceCount,
            plan.totalCount,
        )
    }

@Composable
private fun VexStreamCollectionActions(
    runtime: VexRuntime,
    tracks: List<VexStreamTrackState>,
    offlinePlan: VexStreamOfflineCollectionPlan,
    offlineCopyConnection: VexStreamOfflineCopyConnection?,
    controller: androidx.media3.session.MediaController?,
    resolver: VexStreamPlaybackSourceResolver,
    modifier: Modifier = VexModifier,
) {
    val connection =
        rememberVexStreamCollectionPlaybackConnection(
            controller = controller,
            resolver = resolver,
        )
    val playRequest =
        remember(
            tracks,
        ) {
            VexStreamCollectionPlaybackRequest(
                tracks = tracks,
                order =
                    VexStreamCollectionPlaybackOrder.Natural,
            )
        }
    val shuffleRequest =
        remember(
            tracks,
        ) {
            VexStreamCollectionPlaybackRequest(
                tracks = tracks,
                order =
                    VexStreamCollectionPlaybackOrder.Shuffled,
            )
        }
    val downloadMissingRequest =
        remember(
            offlinePlan.targets,
        ) {
            offlinePlan.requestOrNull()
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

                VexStreamMusicScreen.DownloadMissing ->
                    downloadMissingRequest
                        ?.let { request ->
                            offlineCopyConnection
                                ?.request(
                                    request,
                                )
                        }

                else -> Unit
            }
        }

    VexRow(
        id = "actions",
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        VexButton(
            element = VexStreamMusicScreen.PlayAll,
            handler = handler,
            enabled =
                connection
                    ?.isAvailable(
                        playRequest,
                    ) == true,
        )
        VexButton(
            element = VexStreamMusicScreen.ShuffleAll,
            handler = handler,
            enabled =
                connection
                    ?.isAvailable(
                        shuffleRequest,
                    ) == true,
        )
        VexButton(
            element = VexStreamMusicScreen.DownloadMissing,
            handler = handler,
            enabled =
                offlineCopyConnection != null &&
                    downloadMissingRequest != null,
        )
    }
}

// [VXG RealForever]
