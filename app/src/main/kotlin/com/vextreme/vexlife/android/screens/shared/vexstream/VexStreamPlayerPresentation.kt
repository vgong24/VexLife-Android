package com.vextreme.vexlife.android.screens.shared.vexstream

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Slider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vextreme.vexlife.android.R
import com.vextreme.vexlife.android.elements.ElementActionHandler
import com.vextreme.vexlife.android.elements.VexColumn
import com.vextreme.vexlife.android.elements.VexContainer
import com.vextreme.vexlife.android.elements.VexModifier
import com.vextreme.vexlife.android.elements.VexRow
import com.vextreme.vexlife.android.elements.VexText
import com.vextreme.vexlife.android.elements.VexElementStyles
import com.vextreme.vexlife.android.elements.VexIconButton
import com.vextreme.vexlife.android.elements.vexElement
import com.vextreme.vexlife.android.runtime.VexRuntime
import com.vextreme.vexlife.android.vexstream.playback.present
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackControl
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackExperience
import com.vextreme.vexlife.android.vexstream.queue.VexStreamQueueControl
import com.vextreme.vexlife.android.vexstream.queue.rememberVexStreamQueueExperience

/**
 * Mini-player presentation for the accepted VexStream Playback Experience.
 *
 * This layer projects current Playback Experience/read truth into Compose controls. Media3 remains
 * visible only for transport operations that have not yet earned a Behavior/Experience boundary.
 */
@Composable
internal fun VexStreamMiniPlayer(
    controller: androidx.media3.session.MediaController?,
    playbackExperience:
        VexStreamPlaybackExperience,
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
                    playbackExperience
                        .activate(
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
    var scrubFraction by remember(
        playback.currentMediaId,
    ) {
        mutableStateOf<Float?>(null)
    }
    val displayedProgress =
        scrubFraction
            ?: playback.progressFraction
    val displayedPositionMs =
        scrubFraction
            ?.let { fraction ->
                vexStreamSeekPositionMs(
                    fraction = fraction,
                    durationMs = playback.durationMs,
                )
            }
            ?: playback.currentPositionMs
    val canSeek =
        controller != null &&
            playback.durationMs > 0L &&
            controller.isCommandAvailable(
                androidx.media3.common.Player
                    .COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,
            )

    VexContainer(
        element = VexStreamMusicScreen.PlayerSection,
        modifier = VexModifier
            .fillMaxWidth()
            .padding(8.dp),
        style = VexElementStyles.RaisedSurface,
    ) {
        VexRow(
            id = "summary",
            modifier = VexModifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                if (playback.currentArtist.isNotBlank()) {
                    VexText(
                        id = "artist",
                        text = playback.currentArtist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

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
                    val targetPositionMs =
                        vexStreamSeekPositionMs(
                            fraction = targetFraction,
                            durationMs = playback.durationMs,
                        )
                    scrubFraction = null
                    runtime.activate(
                        VexStreamMusicScreen.SeekPlayback,
                    )
                    controller?.seekTo(
                        targetPositionMs,
                    )
                } else {
                    scrubFraction = null
                }
            },
            modifier = VexModifier
                .fillMaxWidth()
                .vexElement(
                    VexStreamMusicScreen.SeekPlayback,
                ),
            enabled = canSeek,
            valueRange = 0f..1f,
        )

        VexRow(
            id = "timeline",
            modifier = VexModifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            VexText(
                id = "position",
                text =
                    formatVexStreamPlaybackTime(
                        displayedPositionMs,
                    ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            VexText(
                id = "duration",
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
                enabled =
                    playPause.enabled,
                iconOverride =
                    playPause.icon,
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
                handler = runtime,
            )
        }
    }
}

// [VXG RealForever]
