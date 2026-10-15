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
    shuffleSelected: Boolean,
    onShuffle: () -> Unit,
) {
    val playback =
        playbackExperience
            .displayState
            .playback

    val handler =
        ElementActionHandler { element ->
            runtime.activate(
                element,
            )
            when (element) {
                VexStreamMusicScreen.PreviousTrack ->
                    controller?.seekToPreviousMediaItem()

                VexStreamMusicScreen.TogglePlayback ->
                    playbackExperience
                        .activate(
                            VexStreamPlaybackControl.PlayPause,
                        )

                VexStreamMusicScreen.NextTrack ->
                    controller?.seekToNextMediaItem()

                VexStreamMusicScreen.ShuffleQueue ->
                    onShuffle()

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
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        style = VexElementStyles.RaisedSurface,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
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
                    Text(
                        text = playback.currentArtist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            queuePosition?.let {
                Text(
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
            modifier = Modifier
                .fillMaxWidth()
                .vexElement(
                    VexStreamMusicScreen.SeekPlayback,
                ),
            enabled = canSeek,
            valueRange = 0f..1f,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text =
                    formatVexStreamPlaybackTime(
                        displayedPositionMs,
                    ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            VexIconButton(
                element = VexStreamMusicScreen.PreviousTrack,
                handler = handler,
                enabled =
                    controller?.hasPreviousMediaItem() == true,
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
                    controller?.hasNextMediaItem() == true,
            )
            VexIconButton(
                element = VexStreamMusicScreen.ShuffleQueue,
                handler = handler,
                enabled = controller != null,
                selected = shuffleSelected,
            )
            VexIconButton(
                element = VexStreamMusicScreen.ShowQueue,
                handler = runtime,
            )
        }
    }
}

// [VXG RealForever]
