package com.vextreme.vexlife.android.screens.shared.vexstream

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.vextreme.vexlife.android.R
import com.vextreme.vexlife.android.elements.ElementActionHandler
import com.vextreme.vexlife.android.elements.ElementIcon
import com.vextreme.vexlife.android.elements.VexColumn
import com.vextreme.vexlife.android.elements.VexContainer
import com.vextreme.vexlife.android.elements.VexElementStyles
import com.vextreme.vexlife.android.elements.VexIcon
import com.vextreme.vexlife.android.elements.VexIconButton
import com.vextreme.vexlife.android.elements.VexModalBottomSheet
import com.vextreme.vexlife.android.elements.VexModifier
import com.vextreme.vexlife.android.elements.VexRow
import com.vextreme.vexlife.android.elements.VexText
import com.vextreme.vexlife.android.elements.vex
import com.vextreme.vexlife.android.runtime.VexRuntime
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackControl
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackExperience
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackReadState
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackSeekConnection
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackSeekRequest
import com.vextreme.vexlife.android.vexstream.playback.present
import com.vextreme.vexlife.android.vexstream.playback.rememberVexStreamPlaybackSeekConnection
import com.vextreme.vexlife.android.vexstream.playback.vexStreamPlaybackSeekPositionMs
import com.vextreme.vexlife.android.vexstream.queue.VexStreamQueueControl
import com.vextreme.vexlife.android.vexstream.queue.rememberVexStreamQueueExperience
import com.vextreme.vexlife.android.vexstream.repeat.VexStreamRepeatControl
import com.vextreme.vexlife.android.vexstream.repeat.VexStreamRepeatState
import com.vextreme.vexlife.android.vexstream.repeat.rememberVexStreamRepeatExperience

/**
 * Compact always-available playback projection.
 *
 * Playback/Queue remain feature-owned semantic families. The mini player is only a doorway into the
 * same current truth and into [VexStreamMusicScreen.FullPlayerSheet].
 */
@Composable
internal fun VexStreamMiniPlayer(
    controller: androidx.media3.session.MediaController?,
    playbackExperience: VexStreamPlaybackExperience,
    runtime: VexRuntime,
) {
    val playback =
        playbackExperience
            .displayState
            .playback
    val queueExperience =
        rememberVexStreamQueueExperience(
            controller,
        )
    val queueProjection =
        queueExperience.projection
    val seekConnection =
        rememberVexStreamPlaybackSeekConnection(
            controller,
        )
    val playbackProjection =
        requireNotNull(
            playbackExperience
                .displayState
                .projection,
        )
    val playPause =
        playbackProjection.present(
            VexStreamPlaybackControl.PlayPause,
        )
    val queuePosition =
        playback.currentIndex
            .takeIf { it >= 0 }
            ?.let { index ->
                (index + 1).toString() +
                    "/" +
                    playback.queueMediaIds.size
            }

    val handler =
        ElementActionHandler { element ->
            runtime.activate(
                element,
            )
            when (element) {
                VexStreamMusicScreen.PreviousTrack ->
                    queueExperience.activate(
                        VexStreamQueueControl.Previous,
                    )

                VexStreamMusicScreen.TogglePlayback ->
                    playbackExperience.activate(
                        VexStreamPlaybackControl.PlayPause,
                    )

                VexStreamMusicScreen.NextTrack ->
                    queueExperience.activate(
                        VexStreamQueueControl.Next,
                    )

                VexStreamMusicScreen.ShuffleQueue ->
                    queueExperience.activate(
                        VexStreamQueueControl.Shuffle,
                    )

                else -> Unit
            }
        }

    VexContainer(
        element = VexStreamMusicScreen.PlayerSection,
        modifier =
            VexModifier
                .fillMaxWidth()
                .padding(8.dp),
        style = VexElementStyles.RaisedSurface,
    ) {
        androidx.compose.foundation.layout.Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .vex(
                        VexStreamMusicScreen.OpenFullPlayer,
                    )
                    .clickable {
                        runtime.activate(
                            VexStreamMusicScreen.OpenFullPlayer,
                        )
                    },
            horizontalArrangement =
                Arrangement.spacedBy(
                    8.dp,
                ),
            verticalAlignment =
                Alignment.CenterVertically,
        ) {
            VexColumn(
                id = "metadata",
                modifier = VexModifier.weight(1f),
            ) {
                VexText(
                    id = "title",
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
                if (
                    playback.currentArtist.isNotBlank()
                ) {
                    VexText(
                        id = "artist",
                        text = playback.currentArtist,
                        style = MaterialTheme.typography.bodySmall,
                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            queuePosition?.let {
                VexText(
                    id = "queue-position",
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant,
                )
            }
        }

        VexStreamPlaybackTimeline(
            playback = playback,
            seekConnection = seekConnection,
            runtime = runtime,
            semanticRef =
                VexStreamMusicScreen
                    .SeekPlayback
                    .elementId,
            idPrefix = "mini",
        )

        VexRow(
            id = "controls",
            modifier = VexModifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            VexIconButton(
                element = VexStreamMusicScreen.PreviousTrack,
                handler = handler,
                enabled =
                    queueProjection
                        ?.get(
                            VexStreamQueueControl.Previous,
                        )
                        ?.isAvailable == true,
            )
            VexIconButton(
                element = VexStreamMusicScreen.TogglePlayback,
                handler = handler,
                enabled = playPause.enabled,
                iconOverride = playPause.icon,
            )
            VexIconButton(
                element = VexStreamMusicScreen.NextTrack,
                handler = handler,
                enabled =
                    queueProjection
                        ?.get(
                            VexStreamQueueControl.Next,
                        )
                        ?.isAvailable == true,
            )
            VexIconButton(
                element = VexStreamMusicScreen.ShuffleQueue,
                handler = handler,
                enabled =
                    queueProjection
                        ?.get(
                            VexStreamQueueControl.Shuffle,
                        )
                        ?.isAvailable == true,
            )
            VexIconButton(
                element = VexStreamMusicScreen.ShowQueue,
                handler = handler,
            )
        }
    }
}

/**
 * One full-height transient player surface.
 *
 * Now Playing and Queue are presentation modes inside one modal sheet. Playback, Queue and Repeat
 * remain independent semantic owners and Media3 remains the runtime/provider realization.
 */
@Composable
internal fun VexStreamFullPlayerSheet(
    state: com.vextreme.vexlife.android.data.vexstream.VexStreamMusicState,
    controller: androidx.media3.session.MediaController?,
    playbackExperience: VexStreamPlaybackExperience,
    runtime: VexRuntime,
) {
    val playback =
        playbackExperience
            .displayState
            .playback
    val playbackProjection =
        requireNotNull(
            playbackExperience
                .displayState
                .projection,
        )
    val playPause =
        playbackProjection.present(
            VexStreamPlaybackControl.PlayPause,
        )
    val queueExperience =
        rememberVexStreamQueueExperience(
            controller,
        )
    val queueProjection =
        queueExperience.projection
    val repeatExperience =
        rememberVexStreamRepeatExperience(
            controller,
        )
    val repeatProjection =
        repeatExperience.projection
    val seekConnection =
        rememberVexStreamPlaybackSeekConnection(
            controller,
        )
    val selected =
        runtime.presentation.selected(
            VexStreamMusicScreen.FullPlayerContent,
        )
    val sheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        )
    val handler =
        ElementActionHandler { element ->
            runtime.activate(
                element,
            )
            when (element) {
                VexStreamMusicScreen.PreviousTrack ->
                    queueExperience.activate(
                        VexStreamQueueControl.Previous,
                    )

                VexStreamMusicScreen.TogglePlayback ->
                    playbackExperience.activate(
                        VexStreamPlaybackControl.PlayPause,
                    )

                VexStreamMusicScreen.NextTrack ->
                    queueExperience.activate(
                        VexStreamQueueControl.Next,
                    )

                VexStreamMusicScreen.ShuffleQueue ->
                    queueExperience.activate(
                        VexStreamQueueControl.Shuffle,
                    )

                VexStreamMusicScreen.ToggleRepeat ->
                    repeatExperience.activate(
                        VexStreamRepeatControl.Toggle,
                    )

                else -> Unit
            }
        }

    VexModalBottomSheet(
        element = VexStreamMusicScreen.FullPlayerSheet,
        modifier = VexModifier.fillMaxHeight(),
        onDismissRequest = {
            runtime.dismissPresentation(
                VexStreamMusicScreen.FullPlayerSheet,
            )
        },
        sheetState = sheetState,
    ) {
        if (
            selected.elementId ==
            VexStreamMusicScreen.QueuePanel.elementId
        ) {
            VexStreamQueueContent(
                state = state,
                playback = playback,
                controller = controller,
                runtime = runtime,
            )
        } else {
            val swipeThresholdPx =
                with(
                    LocalDensity.current,
                ) {
                    72.dp.toPx()
                }
            var swipeDragPx by remember(
                playback.currentMediaId,
            ) {
                mutableFloatStateOf(
                    0f,
                )
            }

            VexColumn(
                element =
                    VexStreamMusicScreen
                        .FullPlayerNowPlayingPanel,
                modifier =
                    VexModifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .verticalScroll(
                            rememberScrollState(),
                        )
                        .padding(
                            horizontal = 20.dp,
                            vertical = 8.dp,
                        ),
                verticalArrangement =
                    Arrangement.spacedBy(
                        16.dp,
                    ),
            ) {
                VexRow(
                    id = "header",
                    modifier = VexModifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.SpaceBetween,
                    verticalAlignment =
                        Alignment.CenterVertically,
                ) {
                    VexText(
                        id = "title",
                        text =
                            stringResource(
                                R.string.vexstream_music_now_playing,
                            ),
                        style =
                            MaterialTheme.typography
                                .headlineSmall,
                    )
                    VexIconButton(
                        element =
                            VexStreamMusicScreen
                                .CloseFullPlayer,
                        handler = handler,
                    )
                }

                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .vex(
                                VexStreamPresentationRefs
                                    .FullPlayerGestureRegion,
                            )
                            .pointerInput(
                                playback.currentMediaId,
                                queueProjection,
                            ) {
                                detectHorizontalDragGestures(
                                    onDragStart = {
                                        swipeDragPx = 0f
                                    },
                                    onDragCancel = {
                                        swipeDragPx = 0f
                                    },
                                    onDragEnd = {
                                        val control =
                                            vexStreamFullPlayerSwipeControl(
                                                dragDistancePx =
                                                    swipeDragPx,
                                                thresholdPx =
                                                    swipeThresholdPx,
                                            )
                                        swipeDragPx = 0f

                                        if (
                                            control != null &&
                                            queueProjection
                                                ?.get(
                                                    control,
                                                )
                                                ?.isAvailable == true
                                        ) {
                                            runtime
                                                .recordDynamicInteraction(
                                                    subjectRef =
                                                        VexStreamPresentationRefs
                                                            .fullPlayerSwipe(
                                                                control,
                                                            ),
                                                    action = "Swiped",
                                                )
                                            queueExperience.activate(
                                                control,
                                            )
                                        }
                                    },
                                    onHorizontalDrag = {
                                            change,
                                            dragAmount,
                                            ->
                                        change.consume()
                                        swipeDragPx +=
                                            dragAmount
                                    },
                                )
                            },
                    contentAlignment = Alignment.Center,
                ) {
                    val artworkModifier =
                        Modifier
                            .fillMaxWidth(
                                0.82f,
                            )
                            .aspectRatio(
                                1f,
                            )
                            .clip(
                                MaterialTheme.shapes
                                    .extraLarge,
                            )
                            .vex(
                                VexStreamPresentationRefs
                                    .FullPlayerArtwork,
                            )

                    playback.currentArtworkUri
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?.let { artworkUri ->
                            AsyncImage(
                                model = artworkUri,
                                contentDescription =
                                    stringResource(
                                        R.string
                                            .vexstream_music_artwork,
                                    ),
                                modifier =
                                    artworkModifier,
                                contentScale =
                                    ContentScale.Crop,
                            )
                        }
                        ?: Surface(
                            modifier =
                                artworkModifier,
                            shape =
                                MaterialTheme.shapes
                                    .extraLarge,
                            color =
                                MaterialTheme.colorScheme
                                    .surfaceContainerHighest,
                        ) {
                            Box(
                                contentAlignment =
                                    Alignment.Center,
                            ) {
                                VexIcon(
                                    icon =
                                        ElementIcon.Music,
                                    contentDescription =
                                        stringResource(
                                            R.string
                                                .vexstream_music_artwork,
                                        ),
                                    modifier =
                                        VexModifier.size(
                                            72.dp,
                                        ),
                                )
                            }
                        }
                }

                VexColumn(
                    id = "metadata",
                    modifier = VexModifier.fillMaxWidth(),
                    horizontalAlignment =
                        Alignment.CenterHorizontally,
                ) {
                    VexText(
                        id = "track-title",
                        text =
                            playback.currentTitle.ifBlank {
                                stringResource(
                                    R.string.vexstream_music_now_playing,
                                )
                            },
                        style =
                            MaterialTheme.typography
                                .headlineSmall,
                        maxLines = 2,
                        overflow =
                            TextOverflow.Ellipsis,
                    )
                    if (
                        playback.currentArtist
                            .isNotBlank()
                    ) {
                        VexText(
                            id = "track-artist",
                            text =
                                playback.currentArtist,
                            style =
                                MaterialTheme.typography
                                    .bodyLarge,
                            color =
                                MaterialTheme.colorScheme
                                    .onSurfaceVariant,
                            maxLines = 1,
                            overflow =
                                TextOverflow.Ellipsis,
                        )
                    }
                }

                VexStreamPlaybackTimeline(
                    playback = playback,
                    seekConnection = seekConnection,
                    runtime = runtime,
                    semanticRef =
                        VexStreamPresentationRefs
                            .fullPlayerAction(
                                VexStreamMusicScreen
                                    .SeekPlayback,
                            ),
                    idPrefix = "full",
                )

                VexRow(
                    id = "transport-controls",
                    modifier = VexModifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.SpaceEvenly,
                    verticalAlignment =
                        Alignment.CenterVertically,
                ) {
                    VexIconButton(
                        element =
                            VexStreamMusicScreen
                                .PreviousTrack,
                        handler = handler,
                        enabled =
                            queueProjection
                                ?.get(
                                    VexStreamQueueControl
                                        .Previous,
                                )
                                ?.isAvailable == true,
                        semanticRef =
                            VexStreamPresentationRefs
                                .fullPlayerAction(
                                    VexStreamMusicScreen
                                        .PreviousTrack,
                                ),
                    )
                    VexIconButton(
                        element =
                            VexStreamMusicScreen
                                .TogglePlayback,
                        handler = handler,
                        enabled = playPause.enabled,
                        iconOverride = playPause.icon,
                        semanticRef =
                            VexStreamPresentationRefs
                                .fullPlayerAction(
                                    VexStreamMusicScreen
                                        .TogglePlayback,
                                ),
                    )
                    VexIconButton(
                        element =
                            VexStreamMusicScreen
                                .NextTrack,
                        handler = handler,
                        enabled =
                            queueProjection
                                ?.get(
                                    VexStreamQueueControl.Next,
                                )
                                ?.isAvailable == true,
                        semanticRef =
                            VexStreamPresentationRefs
                                .fullPlayerAction(
                                    VexStreamMusicScreen
                                        .NextTrack,
                                ),
                    )
                }

                VexRow(
                    id = "secondary-controls",
                    modifier = VexModifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.SpaceEvenly,
                    verticalAlignment =
                        Alignment.CenterVertically,
                ) {
                    VexIconButton(
                        element =
                            VexStreamMusicScreen
                                .ShuffleQueue,
                        handler = handler,
                        enabled =
                            queueProjection
                                ?.get(
                                    VexStreamQueueControl.Shuffle,
                                )
                                ?.isAvailable == true,
                        semanticRef =
                            VexStreamPresentationRefs
                                .fullPlayerAction(
                                    VexStreamMusicScreen
                                        .ShuffleQueue,
                                ),
                    )
                    VexIconButton(
                        element =
                            VexStreamMusicScreen
                                .ToggleRepeat,
                        handler = handler,
                        enabled =
                            repeatProjection
                                ?.get(
                                    VexStreamRepeatControl.Toggle,
                                )
                                ?.isAvailable == true,
                        selected =
                            repeatProjection
                                ?.state is
                                VexStreamRepeatState.Enabled,
                        semanticRef =
                            VexStreamPresentationRefs
                                .fullPlayerAction(
                                    VexStreamMusicScreen
                                        .ToggleRepeat,
                                ),
                    )
                    VexIconButton(
                        element =
                            VexStreamMusicScreen
                                .ShowQueue,
                        handler = handler,
                        semanticRef =
                            VexStreamPresentationRefs
                                .fullPlayerAction(
                                    VexStreamMusicScreen
                                        .ShowQueue,
                                ),
                    )
                }
            }
        }
    }
}

@Composable
private fun VexStreamPlaybackTimeline(
    playback: VexStreamPlaybackReadState,
    seekConnection: VexStreamPlaybackSeekConnection?,
    runtime: VexRuntime,
    semanticRef: String,
    idPrefix: String,
) {
    var scrubFraction by remember(
        playback.currentMediaId,
        playback.currentIndex,
        idPrefix,
    ) {
        mutableStateOf<Float?>(
            null,
        )
    }
    val displayedProgress =
        scrubFraction
            ?: playback.progressFraction
    val displayedPositionMs =
        scrubFraction
            ?.let { fraction ->
                vexStreamPlaybackSeekPositionMs(
                    fraction = fraction,
                    durationMs =
                        playback.durationMs,
                )
            }
            ?: playback.currentPositionMs
    val canSeek =
        seekConnection
            ?.isAvailable == true &&
            playback.currentMediaId
                ?.isNotBlank() == true &&
            playback.currentIndex >= 0 &&
            playback.durationMs > 0L

    Slider(
        value = displayedProgress,
        onValueChange = { value ->
            scrubFraction = value
        },
        onValueChangeFinished = {
            val targetFraction =
                scrubFraction
            val targetMediaId =
                playback.currentMediaId
            val targetMediaItemIndex =
                playback.currentIndex

            if (
                targetFraction != null &&
                !targetMediaId.isNullOrBlank() &&
                targetMediaItemIndex >= 0 &&
                canSeek
            ) {
                val targetPositionMs =
                    vexStreamPlaybackSeekPositionMs(
                        fraction =
                            targetFraction,
                        durationMs =
                            playback.durationMs,
                    )
                scrubFraction = null
                runtime.activate(
                    VexStreamMusicScreen.SeekPlayback,
                )
                seekConnection?.seek(
                    VexStreamPlaybackSeekRequest(
                        mediaId =
                            targetMediaId,
                        mediaItemIndex =
                            targetMediaItemIndex,
                        positionMs =
                            targetPositionMs,
                    ),
                )
            } else {
                scrubFraction = null
            }
        },
        modifier =
            Modifier
                .fillMaxWidth()
                .vex(
                    semanticRef,
                ),
        enabled = canSeek,
        valueRange = 0f..1f,
    )

    VexRow(
        id = "$idPrefix-timeline",
        modifier = VexModifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.SpaceBetween,
    ) {
        VexText(
            id = "position",
            text =
                formatVexStreamPlaybackTime(
                    displayedPositionMs,
                ),
            style =
                MaterialTheme.typography
                    .labelSmall,
            color =
                MaterialTheme.colorScheme
                    .onSurfaceVariant,
        )
        VexText(
            id = "duration",
            text =
                if (
                    playback.durationMs > 0L
                ) {
                    formatVexStreamPlaybackTime(
                        playback.durationMs,
                    )
                } else {
                    "--:--"
                },
            style =
                MaterialTheme.typography
                    .labelSmall,
            color =
                MaterialTheme.colorScheme
                    .onSurfaceVariant,
        )
    }
}

internal fun vexStreamFullPlayerSwipeControl(
    dragDistancePx: Float,
    thresholdPx: Float,
): VexStreamQueueControl? {
    require(
        thresholdPx > 0f,
    ) {
        "full-player swipe threshold must be positive"
    }

    return when {
        dragDistancePx >= thresholdPx ->
            VexStreamQueueControl.Previous

        dragDistancePx <= -thresholdPx ->
            VexStreamQueueControl.Next

        else ->
            null
    }
}

// [VXG RealForever]
