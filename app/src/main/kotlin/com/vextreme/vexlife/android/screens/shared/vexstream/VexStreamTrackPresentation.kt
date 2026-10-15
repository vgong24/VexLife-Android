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
 * Runtime track-row presentation for VexStream Android.
 *
 * Track identity comes from repository/domain truth. This file owns only how one current track
 * is rendered and how its static action meaning is paired with the exact runtime track instance.
 * It intentionally does not own library ordering, search, collection navigation, or Media3
 * provider truth.
 */
@Composable
internal fun VexStreamTrackCard(
    runtime: VexRuntime,
    state: VexStreamTrackState,
    contextTracks: List<VexStreamTrackState>,
    controller: androidx.media3.session.MediaController?,
    resolver: VexStreamPlaybackSourceResolver,
    onDownload: () -> Unit,
    onRemoveDownload: () -> Unit,
    onVisitArtist: (String) -> Unit,
    showDownloadAction: Boolean = true,
) {
    var menuExpanded by remember(
        state.track.trackRef.value,
    ) {
        mutableStateOf(false)
    }

    fun realizeTrackAction(
        element: ActionElement,
    ) {
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

            VexStreamMusicScreen.VisitTrackArtist ->
                onVisitArtist(
                    state.track.artist,
                )

            else -> Unit
        }
    }

    val rowHandler =
        ElementActionHandler { element ->
            runtime.activateDynamic(
                element = element,
                subjectRef =
                    VexStreamPresentationRefs.trackRowAction(
                        trackRef =
                            state.track.trackRef,
                        action = element,
                    ),
            )
            realizeTrackAction(
                element,
            )
        }
    val menuHandler =
        ElementActionHandler { element ->
            menuExpanded = false
            runtime.activateDynamic(
                element = element,
                subjectRef =
                    VexStreamPresentationRefs.trackMenuAction(
                        trackRef =
                            state.track.trackRef,
                        action = element,
                    ),
            )
            realizeTrackAction(
                element,
            )
        }
    val menuOpenHandler =
        ElementActionHandler { element ->
            rowHandler.activate(
                element,
            )
            menuExpanded = true
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
            .vex(
                VexModifier.semantic(
                    VexStreamPresentationRefs.trackRowAction(
                        trackRef =
                            state.track.trackRef,
                        action =
                            VexStreamMusicScreen.PlayTrack,
                    ),
                ),
            )
            .clickable(
                enabled = controller != null,
            ) {
                rowHandler.activate(
                    VexStreamMusicScreen.PlayTrack,
                )
            },
        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme
                        .colorScheme
                        .surface,
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .padding(
                        horizontal = 10.dp,
                        vertical = 8.dp,
                    )
                    .vex(
                        VexModifier.semantic(
                            VexStreamPresentationRefs.trackRow(
                                state.track.trackRef,
                            ),
                        ),
                    ),
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

            Box {
                VexIconButton(
                    element = VexStreamMusicScreen.TrackMenu,
                    handler = menuOpenHandler,
                    vexModifier =
                        VexModifier.semantic(
                            VexStreamPresentationRefs.trackRowAction(
                                trackRef =
                                    state.track.trackRef,
                                action =
                                    VexStreamMusicScreen.TrackMenu,
                            ),
                        ),
                )

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = {
                        menuExpanded = false
                    },
                ) {
                    VexStreamTrackMenuItem(
                        trackRef = state.track.trackRef,
                        element = VexStreamMusicScreen.PlayTrack,
                        handler = menuHandler,
                        enabled = controller != null,
                    )
                    VexStreamTrackMenuItem(
                        trackRef = state.track.trackRef,
                        element = VexStreamMusicScreen.PlayTrackNext,
                        handler = menuHandler,
                        enabled = controller != null,
                    )
                    VexStreamTrackMenuItem(
                        trackRef = state.track.trackRef,
                        element = VexStreamMusicScreen.AddTrackToQueue,
                        handler = menuHandler,
                        enabled = controller != null,
                    )

                    if (showDownloadAction) {
                        when (state.localCopy) {
                            is VexStreamLocalCopyState.Downloaded ->
                                VexStreamTrackMenuItem(
                                    trackRef = state.track.trackRef,
                                    element =
                                        VexStreamMusicScreen
                                            .RemoveTrackDownload,
                                    handler = menuHandler,
                                )

                            is VexStreamLocalCopyState.Downloading ->
                                Unit

                            else -> {
                                if (state.hubAvailable) {
                                    VexStreamTrackMenuItem(
                                        trackRef = state.track.trackRef,
                                        element =
                                            VexStreamMusicScreen
                                                .DownloadTrack,
                                        handler = menuHandler,
                                    )
                                }
                            }
                        }
                    }

                    if (state.track.artist.isNotBlank()) {
                        VexStreamTrackMenuItem(
                            trackRef = state.track.trackRef,
                            element =
                                VexStreamMusicScreen
                                    .VisitTrackArtist,
                            handler = menuHandler,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VexStreamTrackMenuItem(
    trackRef: VexStreamTrackRef,
    element: ButtonElement,
    handler: ElementActionHandler,
    enabled: Boolean = true,
) {
    DropdownMenuItem(
        modifier =
            Modifier.vex(
                VexModifier.semantic(
                    VexStreamPresentationRefs.trackMenuAction(
                        trackRef = trackRef,
                        action = element,
                    ),
                ),
            ),
        text = {
            Text(
                stringResource(
                    requireNotNull(element.labelRes),
                ),
            )
        },
        onClick = {
            handler.activate(element)
        },
        leadingIcon = {
            element.icon?.let {
                VexIcon(
                    icon = it,
                    contentDescription = null,
                )
            }
        },
        enabled = enabled,
    )
}

// [VXG RealForever]
