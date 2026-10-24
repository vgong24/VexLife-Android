package com.vextreme.vexlife.android.screens.shared.vexstream

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vextreme.vexlife.android.R
import com.vextreme.vexlife.android.data.vexstream.VexStreamConnectionState
import com.vextreme.vexlife.android.data.vexstream.VexStreamHubState
import com.vextreme.vexlife.android.data.vexstream.VexStreamMusicState
import com.vextreme.vexlife.android.elements.ElementActionHandler
import com.vextreme.vexlife.android.elements.TextRole
import com.vextreme.vexlife.android.elements.VexAlertDialog
import com.vextreme.vexlife.android.elements.VexButton
import com.vextreme.vexlife.android.elements.VexColumn
import com.vextreme.vexlife.android.elements.VexContainer
import com.vextreme.vexlife.android.elements.VexElementStyles
import com.vextreme.vexlife.android.elements.VexModalBottomSheet
import com.vextreme.vexlife.android.elements.VexModifier
import com.vextreme.vexlife.android.elements.VexText
import com.vextreme.vexlife.android.runtime.VexRuntime
import com.vextreme.vexlife.android.vexstream.offline.VexStreamOfflineCopyConnection
import com.vextreme.vexlife.android.vexstream.offline.vexStreamOfflineCollectionPlan
import com.vextreme.vexlife.android.vexstream.offline.vexStreamOfflineCollectionRemovalRequest

/**
 * Connection/settings presentation for VexStream Android.
 *
 * This file owns the human-visible NotPaired/Search/Confirm/Paired management surfaces plus bounded
 * library-wide local-copy actions that reuse the existing Offline Copy owner. Repository pairing,
 * catalog, storage, authorization, transport and Hub truth remain outside presentation.
 */
@Composable
internal fun VexStreamConnectionSetup(
    state: VexStreamMusicState,
    runtime: VexRuntime,
) {
    val connection = state.connectionState

    VexContainer(
        element = VexStreamMusicScreen.ConnectionSection,
        modifier =
            VexModifier
                .fillMaxWidth()
                .padding(
                    horizontal = 12.dp,
                    vertical = 8.dp,
                ),
        style = VexElementStyles.RaisedSurface,
    ) {
        when (connection) {
            VexStreamConnectionState.NotPaired -> {
                VexText(
                    id = "status",
                    text =
                        stringResource(
                            R.string.vexstream_music_status_not_configured,
                        ),
                    style = MaterialTheme.typography.titleMedium,
                )
                VexText(
                    id = "intro",
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
                VexText(
                    id = "status",
                    text =
                        stringResource(
                            R.string.vexstream_music_status_searching,
                        ),
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            is VexStreamConnectionState.ConfirmCode -> {
                VexText(
                    id = "display-name",
                    text = connection.displayName,
                    style = MaterialTheme.typography.titleMedium,
                )
                VexText(
                    id = "pairing-title",
                    text =
                        stringResource(
                            R.string.vexstream_music_pairing_code_title,
                        ),
                    role = TextRole.Body,
                )
                VexText(
                    id = "pairing-code",
                    text =
                        formatPairingCode(
                            connection.pairingCode,
                        ),
                    style = MaterialTheme.typography.headlineLarge,
                )
                VexText(
                    id = "help",
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
                VexText(
                    id = "display-name",
                    text = connection.displayName,
                    style = MaterialTheme.typography.titleMedium,
                )
                VexText(
                    id = "status",
                    text =
                        stringResource(
                            R.string.vexstream_music_waiting_approval,
                        ),
                    role = TextRole.Body,
                )
            }

            is VexStreamConnectionState.Failed -> {
                VexText(
                    id = "status",
                    text =
                        stringResource(
                            R.string.vexstream_music_pairing_failed,
                        ),
                    role = TextRole.Body,
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
    offlineCopyConnection: VexStreamOfflineCopyConnection,
) {
    val paired =
        state.connectionState as? VexStreamConnectionState.Paired
    val songsOfflinePlan =
        remember(
            state.tracks,
        ) {
            vexStreamOfflineCollectionPlan(
                state.playableTracks(),
            )
        }
    val downloadAllMissingRequest =
        remember(
            songsOfflinePlan.targets,
        ) {
            songsOfflinePlan.requestOrNull()
        }
    val removeAllDownloadedRequest =
        remember(
            state.tracks,
        ) {
            vexStreamOfflineCollectionRemovalRequest(
                state.playableTracks(),
            )
        }
    val libraryActionHandler =
        ElementActionHandler { element ->
            runtime.activate(
                element,
            )

            when (element) {
                VexStreamMusicScreen.DownloadAllMissingSongs ->
                    downloadAllMissingRequest
                        ?.let(
                            offlineCopyConnection::request,
                        )

                VexStreamMusicScreen.RemoveAllDownloadedSongs ->
                    removeAllDownloadedRequest
                        ?.let(
                            offlineCopyConnection::request,
                        )

                else -> Unit
            }
        }

    VexModalBottomSheet(
        element = VexStreamMusicScreen.ConnectionMenuSheet,
        onDismissRequest = {
            runtime.dismissPresentation(
                VexStreamMusicScreen.ConnectionMenuSheet,
            )
        },
        sheetState =
            rememberModalBottomSheetState(
                skipPartiallyExpanded = false,
            ),
    ) {
        VexColumn(
            id = "content",
            modifier =
                VexModifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 20.dp),
            verticalArrangement =
                Arrangement.spacedBy(12.dp),
        ) {
            VexText(
                id = "title",
                text =
                    stringResource(
                        R.string.vexstream_music_connection_title,
                    ),
                style = MaterialTheme.typography.headlineSmall,
            )

            paired?.let {
                VexText(
                    id = "paired-name",
                    text = it.displayName,
                    style = MaterialTheme.typography.titleMedium,
                )
                VexText(
                    id = "status",
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
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (state.hubState is VexStreamHubState.Ready) {
                VexButton(
                    element = VexStreamMusicScreen.RefreshCatalog,
                    handler = runtime,
                    modifier =
                        VexModifier.fillMaxWidth(),
                )
            } else {
                VexButton(
                    element = VexStreamMusicScreen.Reconnect,
                    handler = runtime,
                    modifier =
                        VexModifier.fillMaxWidth(),
                )
            }

            VexText(
                id = "library-download-status",
                text =
                    stringResource(
                        R.string.vexstream_music_library_download_summary,
                        songsOfflinePlan.onDeviceCount,
                        songsOfflinePlan.totalCount,
                        songsOfflinePlan.downloadableMissingCount,
                    ),
                style = MaterialTheme.typography.bodySmall,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant,
            )

            VexButton(
                element =
                    VexStreamMusicScreen
                        .DownloadAllMissingSongs,
                handler = libraryActionHandler,
                modifier =
                    VexModifier.fillMaxWidth(),
                enabled =
                    downloadAllMissingRequest != null,
            )

            VexButton(
                element =
                    VexStreamMusicScreen
                        .RemoveAllDownloadedSongs,
                handler = libraryActionHandler,
                modifier =
                    VexModifier.fillMaxWidth(),
                enabled =
                    removeAllDownloadedRequest != null,
            )

            VexButton(
                element = VexStreamMusicScreen.RequestForgetPaired,
                handler = runtime,
                modifier =
                    VexModifier.fillMaxWidth(),
            )

            VexButton(
                element = VexStreamMusicScreen.CloseConnectionMenu,
                handler = runtime,
                modifier =
                    VexModifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
internal fun VexStreamForgetConfirmationDialog(
    runtime: VexRuntime,
) {
    VexAlertDialog(
        element = VexStreamMusicScreen.ForgetConfirmationDialog,
        onDismissRequest = {
            runtime.dismissPresentation(
                VexStreamMusicScreen.ForgetConfirmationDialog,
            )
        },
        title = {
            VexText(
                id = "title",
                text =
                    stringResource(
                        R.string.vexstream_music_forget_confirm_title,
                    ),
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            VexText(
                id = "body",
                text =
                    stringResource(
                        R.string.vexstream_music_forget_confirm_body,
                    ),
                style = MaterialTheme.typography.bodyMedium,
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
