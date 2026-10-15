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
 * Connection/pairing presentation for VexStream Android.
 *
 * This file owns only the human-visible NotPaired/Search/Confirm/Paired management surfaces.
 * Repository pairing, authorization, transport and Hub truth remain outside presentation.
 */
@Composable
internal fun VexStreamConnectionSetup(
    state: VexStreamMusicState,
    runtime: VexRuntime,
) {
    val connection = state.connectionState

    VexContainer(
        element = VexStreamMusicScreen.ConnectionSection,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 12.dp,
                vertical = 8.dp,
            ),
        style = VexElementStyles.RaisedSurface,
    ) {
        when (connection) {
            VexStreamConnectionState.NotPaired -> {
                Text(
                    text =
                        stringResource(
                            R.string.vexstream_music_status_not_configured,
                        ),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text =
                        stringResource(
                            R.string.vexstream_music_intro,
                        ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                VexButton(
                    element = VexStreamMusicScreen.FindNearby,
                    handler = runtime,
                )
            }

            VexStreamConnectionState.Searching -> {
                Text(
                    text =
                        stringResource(
                            R.string.vexstream_music_status_searching,
                        ),
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            is VexStreamConnectionState.ConfirmCode -> {
                Text(
                    text = connection.displayName,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text =
                        stringResource(
                            R.string.vexstream_music_pairing_code_title,
                        ),
                )
                Text(
                    text =
                        formatPairingCode(
                            connection.pairingCode,
                        ),
                    style = MaterialTheme.typography.headlineLarge,
                )
                Text(
                    text =
                        stringResource(
                            R.string.vexstream_music_pairing_code_help,
                        ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                VexButton(
                    element =
                        VexStreamMusicScreen
                            .ConfirmPairingCode,
                    handler = runtime,
                )
            }

            is VexStreamConnectionState.WaitingForDesktopApproval -> {
                Text(
                    text = connection.displayName,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text =
                        stringResource(
                            R.string.vexstream_music_waiting_approval,
                        ),
                )
            }

            is VexStreamConnectionState.Failed -> {
                Text(
                    text =
                        stringResource(
                            R.string.vexstream_music_pairing_failed,
                        ),
                )
                VexButton(
                    element = VexStreamMusicScreen.FindNearby,
                    handler = runtime,
                )
            }

            is VexStreamConnectionState.Paired ->
                Unit
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VexStreamConnectionMenuSheet(
    state: VexStreamMusicState,
    runtime: VexRuntime,
) {
    val paired =
        state.connectionState as? VexStreamConnectionState.Paired

    ModalBottomSheet(
        modifier = Modifier.vexElement(
            VexStreamMusicScreen.ConnectionMenuSheet,
        ),
        onDismissRequest = {
            runtime.dismissPresentation(
                VexStreamMusicScreen.ConnectionMenuSheet,
            )
        },
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = false,
        ),
        dragHandle = {
            BottomSheetDefaults.DragHandle()
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(
                    R.string.vexstream_music_connection_title,
                ),
                style = MaterialTheme.typography.headlineSmall,
            )

            paired?.let {
                Text(
                    text = it.displayName,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text =
                        if (state.hubState is VexStreamHubState.Ready) {
                            stringResource(
                                R.string.vexstream_music_status_ready,
                            )
                        } else {
                            stringResource(
                                R.string.vexstream_music_status_unreachable,
                            )
                        },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (state.hubState is VexStreamHubState.Ready) {
                VexButton(
                    element = VexStreamMusicScreen.RefreshCatalog,
                    handler = runtime,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                VexButton(
                    element = VexStreamMusicScreen.Reconnect,
                    handler = runtime,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            VexButton(
                element = VexStreamMusicScreen.RequestForgetPaired,
                handler = runtime,
                modifier = Modifier.fillMaxWidth(),
            )

            VexButton(
                element = VexStreamMusicScreen.CloseConnectionMenu,
                handler = runtime,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}


@Composable
internal fun VexStreamForgetConfirmationDialog(
    runtime: VexRuntime,
) {
    AlertDialog(
        modifier = Modifier.vexElement(
            VexStreamMusicScreen.ForgetConfirmationDialog,
        ),
        onDismissRequest = {
            runtime.dismissPresentation(
                VexStreamMusicScreen.ForgetConfirmationDialog,
            )
        },
        title = {
            Text(
                stringResource(
                    R.string.vexstream_music_forget_confirm_title,
                ),
            )
        },
        text = {
            Text(
                stringResource(
                    R.string.vexstream_music_forget_confirm_body,
                ),
            )
        },
        confirmButton = {
            VexButton(
                element = VexStreamMusicScreen.ForgetPaired,
                handler = runtime,
            )
        },
        dismissButton = {
            VexButton(
                element = VexStreamMusicScreen.CancelForgetPaired,
                handler = runtime,
            )
        },
    )
}


private fun formatPairingCode(
    code: String,
): String =
    if (code.length == 6) {
        code.take(3) + " " + code.takeLast(3)
    } else {
        code
    }

// [VXG RealForever]
