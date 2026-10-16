package com.vextreme.vexlife.android.screens.shared.vexstream

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.vextreme.vexlife.android.R
import com.vextreme.vexlife.android.data.vexstream.VexStreamMusicState
import com.vextreme.vexlife.android.elements.ElementActionHandler
import com.vextreme.vexlife.android.elements.VexButton
import com.vextreme.vexlife.android.elements.VexColumn
import com.vextreme.vexlife.android.elements.VexIconButton
import com.vextreme.vexlife.android.elements.VexModalBottomSheet
import com.vextreme.vexlife.android.elements.VexModifier
import com.vextreme.vexlife.android.elements.VexRow
import com.vextreme.vexlife.android.elements.VexText
import com.vextreme.vexlife.android.elements.vex
import com.vextreme.vexlife.android.runtime.VexRuntime
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackReadState
import com.vextreme.vexlife.android.vexstream.queue.VexStreamQueueControl
import com.vextreme.vexlife.android.vexstream.queue.rememberVexStreamQueueExperience

/**
 * Queue presentation neighborhood for VexStream.
 *
 * MediaSession/Media3 remains the current queue/provider truth owner. This file only contains the
 * Android presentation mechanics that were previously embedded in VexStreamLibrarySurface, giving
 * the earned Queue semantic question one source neighborhood without changing its behavior.
 */
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
            stableKey =
                VexStreamPresentationRefs.queueEntryKey(
                    mediaId = mediaId,
                    occurrence = occurrence,
                ),
            mediaId = mediaId,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VexStreamQueueSheet(
    state: VexStreamMusicState,
    playback: VexStreamPlaybackReadState,
    controller: androidx.media3.session.MediaController?,
    runtime: VexRuntime,
) {
    val queueExperience =
        rememberVexStreamQueueExperience(
            controller,
        )
    val queueProjection =
        queueExperience.projection

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
        val completedKey =
            draggedKey
        if (
            dragMoved &&
            completedKey != null
        ) {
            runtime.activateDynamic(
                element =
                    VexStreamMusicScreen.QueueReorder,
                subjectRef =
                    VexStreamPresentationRefs
                        .queueEntryAction(
                            stableKey = completedKey,
                            action =
                                VexStreamMusicScreen
                                    .QueueReorder,
                        ),
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
                    queueExperience.activate(
                        VexStreamQueueControl.Shuffle,
                    )
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
                    queueExperience.activate(
                        VexStreamQueueControl.Clear,
                    )
                }

                else ->
                    runtime.activate(
                        element,
                    )
            }
        }

    VexModalBottomSheet(
        element = VexStreamMusicScreen.QueueSheet,
        modifier =
            VexModifier.fillMaxHeight(),
        onDismissRequest = {
            runtime.dismissPresentation(
                VexStreamMusicScreen.QueueSheet,
            )
        },
        sheetState = sheetState,
    ) {
        VexColumn(
            id = "content",
            modifier =
                VexModifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            VexText(
                id = "title",
                text =
                    stringResource(
                        R.string.vexstream_music_queue,
                    ),
                style = MaterialTheme.typography.headlineSmall,
            )

            VexRow(
                id = "actions",
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                VexIconButton(
                    element = VexStreamMusicScreen.QueueShuffle,
                    handler = handler,
                    enabled =
                        queueProjection
                            ?.get(
                                VexStreamQueueControl.Shuffle,
                            )
                            ?.isAvailable == true,
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
                        queueProjection
                            ?.get(
                                VexStreamQueueControl.Clear,
                            )
                            ?.isAvailable == true,
                )
            }

            HorizontalDivider()

            VexText(
                id = "reorder-hint",
                text =
                    stringResource(
                        R.string.vexstream_music_queue_reorder_hint,
                    ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (playback.queueMediaIds.isEmpty()) {
                VexText(
                    id = "empty",
                    text =
                        stringResource(
                            R.string.vexstream_music_queue_empty,
                        ),
                    modifier = VexModifier.padding(
                        vertical = 20.dp,
                    ),
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier =
                        Modifier
                            .weight(1f)
                            .vex(
VexStreamPresentationRefs
                                        .QueueList,
),
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
                                )
                                .vex(
VexStreamPresentationRefs
                                            .queueEntry(
                                                entry.stableKey,
),
                                ),
                            horizontalArrangement =
                                Arrangement.spacedBy(
                                    10.dp,
                                ),
                            verticalAlignment =
                                Alignment.CenterVertically,
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .vex(
VexStreamPresentationRefs
                                                .queueEntryAction(
                                                    stableKey =
                                                        entry.stableKey,
                                                    action =
                                                        VexStreamMusicScreen
                                                            .QueuePlay,
),
                                    )
                                    .clickable(
                                        enabled = queuePlayEnabled,
                                    ) {
                                        runtime.activateDynamic(
                                            element =
                                                VexStreamMusicScreen.QueuePlay,
                                            subjectRef =
                                                VexStreamPresentationRefs
                                                    .queueEntryAction(
                                                        stableKey =
                                                            entry.stableKey,
                                                        action =
                                                            VexStreamMusicScreen
                                                                .QueuePlay,
                                                    ),
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
                                    },
                                horizontalArrangement =
                                    Arrangement.spacedBy(
                                        10.dp,
                                    ),
                                verticalAlignment =
                                    Alignment.CenterVertically,
                            ) {
                                Text(
                                    text =
                                        if (isCurrent) {
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
                            }

                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .vex(
VexStreamPresentationRefs
                                                .queueEntryAction(
                                                    stableKey =
                                                        entry.stableKey,
                                                    action =
                                                        VexStreamMusicScreen
                                                            .QueueReorder,
),
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
                modifier = VexModifier.fillMaxWidth(),
            )
        }
    }
}

// [VXG RealForever]
