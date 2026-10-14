package com.vextreme.vexlife.android.screens.shared.vexstream

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.vextreme.vexlife.android.R
import com.vextreme.vexlife.android.elements.ElementIcon
import com.vextreme.vexlife.android.elements.VexIcon
import com.vextreme.vexlife.android.elements.contextdock.VexContextDockPage
import com.vextreme.vexlife.android.elements.contextdock.VexContextDockPageRef
import com.vextreme.vexlife.android.runtime.VexRuntime
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackState
import com.vextreme.vexlife.android.vexstream.playback.rememberVexStreamPlaybackBehaviorProjection
import com.vextreme.vexlife.android.vexstream.playback.rememberVexStreamPlaybackBehaviorConnection
import com.vextreme.vexlife.android.vexstream.playback.rememberVexStreamMediaController
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackControl
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackAct
import com.vextreme.vexlife.android.behavior.VexBehaviorProjection
import com.vextreme.vexlife.android.behavior.VexBehaviorConnection

private const val VEXSTREAM_CONTEXT_DOCK_PAGE_REF =
    "vexstream.now-playing"

/**
 * VexStream-owned projection for any host that admits the shared Context Dock.
 *
 * Playback remains MediaSessionService-owned. The host receives a projection only; it does not gain
 * VexStream repository, queue, or transport ownership.
 */
@Composable
internal fun rememberVexStreamContextDockPage(
    runtime: VexRuntime,
): VexContextDockPage? {
    val controller =
        rememberVexStreamMediaController()
    val behaviorConnection =
        rememberVexStreamPlaybackBehaviorConnection()
    val behaviorProjection =
        rememberVexStreamPlaybackBehaviorProjection(
            behaviorConnection,
        )
    val playback by
        rememberVexStreamPlaybackUiState(
            controller,
        )

    if (!playback.hasQueue) {
        return null
    }

    return VexContextDockPage(
        pageRef =
            VexContextDockPageRef(
                VEXSTREAM_CONTEXT_DOCK_PAGE_REF,
            ),
    ) { expanded ->
        VexStreamContextDockContent(
            runtime = runtime,
            controller = controller,
            playback = playback,
            behaviorConnection =
                behaviorConnection,
            behaviorProjection =
                behaviorProjection,
            expanded = expanded,
        )
    }
}

@Composable
private fun VexStreamContextDockContent(
    runtime: VexRuntime,
    controller: MediaController?,
    playback: VexStreamPlaybackUiState,
    behaviorConnection:
        VexBehaviorConnection<
            VexStreamPlaybackState,
            VexStreamPlaybackControl,
            VexStreamPlaybackAct
        >?,
    behaviorProjection:
        VexBehaviorProjection<
            VexStreamPlaybackState,
            VexStreamPlaybackControl,
            VexStreamPlaybackAct
        >?,
    expanded: Boolean,
) {
    var scrubFraction by
        remember(
            playback.currentMediaId,
        ) {
            mutableStateOf<Float?>(null)
        }
    val displayedProgress =
        scrubFraction
            ?: playback.progressFraction
    val canSeek =
        controller != null &&
            playback.durationMs > 0L &&
            controller.isCommandAvailable(
                Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,
            )
    val playPause =
        behaviorProjection?.get(
            VexStreamPlaybackControl.PlayPause,
        )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement =
            Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment =
                Alignment.CenterVertically,
            horizontalArrangement =
                Arrangement.spacedBy(8.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text =
                        playback.currentTitle
                            .ifBlank {
                                stringResource(
                                    R.string
                                        .vexstream_music_now_playing,
                                )
                            },
                    style =
                        MaterialTheme
                            .typography
                            .titleSmall,
                    maxLines = 1,
                    overflow =
                        TextOverflow.Ellipsis,
                )
                if (
                    playback.currentArtist
                        .isNotBlank()
                ) {
                    Text(
                        text =
                            playback.currentArtist,
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

            VexStreamDockIconButton(
                icon = ElementIcon.Previous,
                contentDescription =
                    stringResource(
                        R.string
                            .vexstream_music_previous,
                    ),
                enabled =
                    controller
                        ?.hasPreviousMediaItem() ==
                        true,
            ) {
                runtime.recordDynamicInteraction(
                    subjectRef =
                        "presentation.context-dock.vexstream.previous",
                )
                controller
                    ?.seekToPreviousMediaItem()
            }
            VexStreamDockIconButton(
                icon =
                    when (playPause?.act) {
                        VexStreamPlaybackAct.Play ->
                            ElementIcon.Play

                        VexStreamPlaybackAct.Pause ->
                            ElementIcon.Pause

                        null ->
                            ElementIcon.PlayPause
                    },
                contentDescription =
                    stringResource(
                        R.string
                            .vexstream_music_play_pause,
                    ),
                enabled =
                    playPause?.available ==
                        true,
            ) {
                runtime.recordDynamicInteraction(
                    subjectRef =
                        "presentation.context-dock.vexstream.play-pause",
                )
                behaviorConnection
                    ?.activate(
                        VexStreamPlaybackControl.PlayPause,
                    )
            }
            VexStreamDockIconButton(
                icon = ElementIcon.Next,
                contentDescription =
                    stringResource(
                        R.string
                            .vexstream_music_next,
                    ),
                enabled =
                    controller
                        ?.hasNextMediaItem() ==
                        true,
            ) {
                runtime.recordDynamicInteraction(
                    subjectRef =
                        "presentation.context-dock.vexstream.next",
                )
                controller
                    ?.seekToNextMediaItem()
            }
        }

        if (expanded) {
            Slider(
                value = displayedProgress,
                onValueChange = { value ->
                    scrubFraction = value
                },
                onValueChangeFinished = {
                    val targetFraction =
                        scrubFraction
                    if (
                        targetFraction != null &&
                        canSeek
                    ) {
                        runtime.recordDynamicInteraction(
                            subjectRef =
                                "presentation.context-dock.vexstream.seek",
                            action = "Changed",
                        )
                        controller?.seekTo(
                            vexStreamSeekPositionMs(
                                fraction =
                                    targetFraction,
                                durationMs =
                                    playback.durationMs,
                            ),
                        )
                    }
                    scrubFraction = null
                },
                modifier =
                    Modifier.fillMaxWidth(),
                enabled = canSeek,
                valueRange = 0f..1f,
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween,
            ) {
                Text(
                    text =
                        formatVexStreamPlaybackTime(
                            playback.currentPositionMs,
                        ),
                    style =
                        MaterialTheme
                            .typography
                            .labelSmall,
                )
                Text(
                    text =
                        if (
                            playback.durationMs >
                            0L
                        ) {
                            formatVexStreamPlaybackTime(
                                playback.durationMs,
                            )
                        } else {
                            "--:--"
                        },
                    style =
                        MaterialTheme
                            .typography
                            .labelSmall,
                )
            }

            val queueStart =
                (playback.currentIndex + 1)
                    .coerceAtLeast(0)
            val queueEnd =
                (
                    queueStart + 3
                ).coerceAtMost(
                    controller?.mediaItemCount
                        ?: 0,
                )
            if (
                controller != null &&
                queueStart < queueEnd
            ) {
                HorizontalDivider()
                Text(
                    text =
                        stringResource(
                            R.string
                                .vexstream_context_up_next,
                        ),
                    style =
                        MaterialTheme
                            .typography
                            .labelLarge,
                )

                for (
                    index in
                    queueStart until queueEnd
                ) {
                    val item =
                        controller
                            .getMediaItemAt(
                                index,
                            )
                    val title =
                        item.mediaMetadata
                            .title
                            ?.toString()
                            ?.takeIf {
                                it.isNotBlank()
                            }
                            ?: item.mediaId

                    TextButton(
                        onClick = {
                            runtime.recordDynamicInteraction(
                                subjectRef =
                                    "presentation.context-dock.vexstream.queue-item",
                                action = "Selected",
                            )
                            controller.seekToDefaultPosition(
                                index,
                            )
                            controller.play()
                        },
                    ) {
                        Text(
                            text = title,
                            maxLines = 1,
                            overflow =
                                TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            Button(
                onClick = {
                    runtime.openExternalScreen(
                        subjectRef =
                            "presentation.context-dock.vexstream.open",
                        screen =
                            VexStreamMusicScreen,
                    )
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            top = 2.dp,
                        ),
            ) {
                Text(
                    stringResource(
                        R.string
                            .vexstream_context_open,
                    ),
                )
            }
        }
    }
}

@Composable
private fun VexStreamDockIconButton(
    icon: ElementIcon,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
    ) {
        VexIcon(
            icon = icon,
            contentDescription =
                contentDescription,
        )
    }
}

// [VXG RealForever]
