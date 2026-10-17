package com.vextreme.vexlife.android.screens.shared.vexstream

// VEX-COVERAGE-DEBT: LIVE_REFACTOR_DEBT | Track play/queue/download actions still combine runtime track identity with Media3 or repository effects; split only as payload and execution-lifetime owners are admitted.

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vextreme.vexlife.android.R
import com.vextreme.vexlife.android.data.vexstream.VexStreamLocalCopyState
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackAvailability
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackRef
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackState
import com.vextreme.vexlife.android.elements.ActionElement
import com.vextreme.vexlife.android.elements.ButtonElement
import com.vextreme.vexlife.android.elements.ElementActionHandler
import com.vextreme.vexlife.android.elements.ElementIcon
import com.vextreme.vexlife.android.elements.TextRole
import com.vextreme.vexlife.android.elements.VexBox
import com.vextreme.vexlife.android.elements.VexColumn
import com.vextreme.vexlife.android.elements.VexComposeScope
import com.vextreme.vexlife.android.elements.VexIcon
import com.vextreme.vexlife.android.elements.VexIconButton
import com.vextreme.vexlife.android.elements.VexModifier
import com.vextreme.vexlife.android.elements.VexText
import com.vextreme.vexlife.android.elements.vex
import com.vextreme.vexlife.android.runtime.VexRuntime

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
    presentationScope: VexStreamTrackPresentationScope,
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
                        scope =
                            presentationScope,
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
                        scope =
                            presentationScope,
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

    val trackPresentationRef =
        VexStreamPresentationRefs.trackRow(
            scope = presentationScope,
            trackRef = state.track.trackRef,
        )
    val playPresentationRef =
        VexStreamPresentationRefs.trackRowAction(
            scope = presentationScope,
            trackRef = state.track.trackRef,
            action = VexStreamMusicScreen.PlayTrack,
        )
    val menuPresentationRef =
        VexStreamPresentationRefs.trackRowAction(
            scope = presentationScope,
            trackRef = state.track.trackRef,
            action = VexStreamMusicScreen.TrackMenu,
        )

    /*
     * Runtime-data identity becomes the semantic parent exactly once. Nested Vex Compose cells can
     * now name only their local role while the existing row/action refs remain byte-stable for
     * Journey, logging and diagnostics.
     */
    VexComposeScope(
        parentRef = trackPresentationRef,
    ) {
        Card(
            modifier =
                VexModifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 8.dp,
                        vertical = 2.dp,
                    )
                    .vex(
                        playPresentationRef,
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
            /*
             * This Row intentionally keeps the exact full runtime row ref rather than inventing an
             * appended local ID. The inherited scope beneath it owns the new nested addresses.
             */
            Row(
                modifier =
                    VexModifier
                        .padding(
                            horizontal = 10.dp,
                            vertical = 8.dp,
                        )
                        .vex(
                            trackPresentationRef,
                        ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement =
                    Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    modifier =
                        VexModifier.size(54.dp),
                    shape = MaterialTheme.shapes.medium,
                    color =
                        MaterialTheme
                            .colorScheme
                            .surfaceContainerHigh,
                ) {
                    VexBox(
                        id = "artwork",
                        contentAlignment = Alignment.Center,
                    ) {
                        VexIcon(
                            icon = ElementIcon.Music,
                            contentDescription = null,
                        )
                    }
                }

                VexColumn(
                    id = "copy",
                    modifier =
                        VexModifier.weight(1f),
                    verticalArrangement =
                        Arrangement.spacedBy(2.dp),
                ) {
                    VexText(
                        id = "title",
                        text = state.track.title,
                        style =
                            MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    if (secondaryText.isNotBlank()) {
                        VexText(
                            id = "secondary",
                            text = secondaryText,
                            role = TextRole.Supporting,
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    compactStatus?.let { status ->
                        VexText(
                            id = "status",
                            text = status,
                            role = TextRole.Label,
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                VexBox(
                    id = "menu",
                ) {
                    VexIconButton(
                        element = VexStreamMusicScreen.TrackMenu,
                        handler = menuOpenHandler,
                        semanticRef = menuPresentationRef,
                    )

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = {
                            menuExpanded = false
                        },
                    ) {
                        VexStreamTrackMenuItem(
                            presentationScope =
                                presentationScope,
                            trackRef = state.track.trackRef,
                            element = VexStreamMusicScreen.PlayTrack,
                            handler = menuHandler,
                            enabled = controller != null,
                        )
                        VexStreamTrackMenuItem(
                            presentationScope =
                                presentationScope,
                            trackRef = state.track.trackRef,
                            element = VexStreamMusicScreen.PlayTrackNext,
                            handler = menuHandler,
                            enabled = controller != null,
                        )
                        VexStreamTrackMenuItem(
                            presentationScope =
                                presentationScope,
                            trackRef = state.track.trackRef,
                            element =
                                VexStreamMusicScreen.AddTrackToQueue,
                            handler = menuHandler,
                            enabled = controller != null,
                        )

                        if (showDownloadAction) {
                            when (state.localCopy) {
                                is VexStreamLocalCopyState.Downloaded ->
                                    VexStreamTrackMenuItem(
                                        presentationScope =
                                            presentationScope,
                                        trackRef =
                                            state.track.trackRef,
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
                                            presentationScope =
                                                presentationScope,
                                            trackRef =
                                                state.track.trackRef,
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
                                presentationScope =
                                    presentationScope,
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
}

@Composable
private fun VexStreamTrackMenuItem(
    presentationScope: VexStreamTrackPresentationScope,
    trackRef: VexStreamTrackRef,
    element: ButtonElement,
    handler: ElementActionHandler,
    enabled: Boolean = true,
) {
    DropdownMenuItem(
        modifier =
            VexModifier.vex(
VexStreamPresentationRefs.trackMenuAction(
                        scope =
                            presentationScope,
                        trackRef = trackRef,
                        action = element,
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
