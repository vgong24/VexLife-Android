package com.vextreme.vexlife.android.screens.shared.vexstream

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vextreme.vexlife.android.R
import com.vextreme.vexlife.android.data.AppRepositories
import com.vextreme.vexlife.android.data.vexstream.InMemoryVexStreamMusicRepository
import com.vextreme.vexlife.android.data.vexstream.VexStreamConnectionState
import com.vextreme.vexlife.android.data.vexstream.VexStreamHubState
import com.vextreme.vexlife.android.data.vexstream.VexStreamLocalCopyState
import com.vextreme.vexlife.android.data.vexstream.VexStreamMediaSha256
import com.vextreme.vexlife.android.data.vexstream.VexStreamMusicRepository
import com.vextreme.vexlife.android.data.vexstream.VexStreamMusicState
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrack
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackAvailability
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackRef
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackState
import com.vextreme.vexlife.android.elements.TextRole
import com.vextreme.vexlife.android.elements.VexElementStyles
import com.vextreme.vexlife.android.elements.invoke
import com.vextreme.vexlife.android.elements.vexElement
import com.vextreme.vexlife.android.preview.VexPreviewHost
import com.vextreme.vexlife.android.preview.VexScreenPreviews
import com.vextreme.vexlife.android.runtime.AppActionRegistrations
import com.vextreme.vexlife.android.runtime.AppActions
import com.vextreme.vexlife.android.runtime.VexRuntime
import com.vextreme.vexlife.android.runtime.appAction
import com.vextreme.vexlife.android.screens.AppScreenBlueprint
import com.vextreme.vexlife.android.screens.ScreenActionContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object VexStreamMusicScreen : AppScreenBlueprint(
    screenKey = "vexstream-music",
    titleRes = R.string.vexstream_music_title,
) {
    val FindNearby = buttonRef(
        id = "body.connection.find",
        labelRes = R.string.vexstream_music_find,
        style = VexElementStyles.PrimaryAction,
        intent = invoke(AppActions.FindNearbyVexStream),
    )

    val ConfirmPairingCode = buttonRef(
        id = "body.connection.confirm-code",
        labelRes = R.string.vexstream_music_confirm_code,
        style = VexElementStyles.PrimaryAction,
        intent = invoke(AppActions.ConfirmVexStreamPairingCode),
    )

    val RefreshCatalog = buttonRef(
        id = "body.connection.refresh",
        labelRes = R.string.vexstream_music_refresh,
        style = VexElementStyles.SecondaryAction,
        intent = invoke(AppActions.RefreshVexStreamCatalog),
    )

    val ForgetPaired = buttonRef(
        id = "body.connection.forget",
        labelRes = R.string.vexstream_music_forget,
        style = VexElementStyles.SecondaryAction,
        intent = invoke(AppActions.ForgetPairedVexStream),
    )

    override val content = layout {
        scrollableColumn("body") {
            card("connection") {
                text(
                    id = "intro",
                    textRes = R.string.vexstream_music_intro,
                    role = TextRole.Supporting,
                )
                button(FindNearby)
                button(ConfirmPairingCode)
                button(RefreshCatalog)
                button(ForgetPaired)
            }
        }
    }

    internal override fun appActionRegistrations(
        context: ScreenActionContext,
    ): AppActionRegistrations =
        vexStreamActionRegistrations(
            repository = context.repositories.vexStream,
            scope = context.scope,
        )

    @Composable
    internal override fun renderContent(
        runtime: VexRuntime,
        repositories: AppRepositories,
        darkTheme: Boolean,
    ) {
        VexStreamMusicScreenContent(
            runtime = runtime,
            repository = repositories.vexStream,
        )
    }
}

internal fun vexStreamActionRegistrations(
    repository: VexStreamMusicRepository,
    scope: CoroutineScope,
): AppActionRegistrations =
    AppActionRegistrations.of(
        appAction(
            action = AppActions.FindNearbyVexStream,
            ownerRef = VexStreamMusicScreen.screenId,
        ) {
            scope.launch {
                repository.findNearbyVexStream()
            }
        },
        appAction(
            action = AppActions.ConfirmVexStreamPairingCode,
            ownerRef = VexStreamMusicScreen.screenId,
        ) {
            scope.launch {
                repository.confirmPairingCode()
            }
        },
        appAction(
            action = AppActions.RefreshVexStreamCatalog,
            ownerRef = VexStreamMusicScreen.screenId,
        ) {
            scope.launch {
                repository.refreshCatalog()
            }
        },
        appAction(
            action = AppActions.ForgetPairedVexStream,
            ownerRef = VexStreamMusicScreen.screenId,
        ) {
            scope.launch {
                repository.forgetPairedVexStream()
            }
        },
    )

@Composable
fun VexStreamMusicScreenContent(
    runtime: VexRuntime,
    repository: VexStreamMusicRepository,
) {
    val state by repository.state.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val inspectionMode = LocalInspectionMode.current
    val mediaController =
        if (inspectionMode) {
            null
        } else {
            rememberVexStreamMediaController()
        }
    val playbackResolver =
        remember(context.filesDir) {
            VexStreamPlaybackSourceResolver(
                context = context,
            )
        }

    LaunchedEffect(repository) {
        if (
            repository.state.value.connectionState is VexStreamConnectionState.Paired
        ) {
            repository.refreshCatalog()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "vexstream-connection") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = stringResource(R.string.vexstream_music_intro),
                        style = MaterialTheme.typography.bodyLarge,
                    )

                    VexStreamConnectionCardBody(
                        state = state,
                        runtime = runtime,
                    )
                }
            }
        }

        val tracks = state.playableTracks()
        if (tracks.isEmpty()) {
            item(key = "vexstream-empty") {
                Text(
                    text = stringResource(R.string.vexstream_music_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }
        } else {
            items(
                items = tracks,
                key = { it.track.trackRef.value },
            ) { trackState ->
                VexStreamTrackCard(
                    state = trackState,
                    canPlay = mediaController != null,
                    onPlay = {
                        mediaController?.playVexStream(
                            state = trackState,
                            resolver = playbackResolver,
                        )
                    },
                    onDownload = {
                        scope.launch {
                            repository.downloadForOffline(
                                trackState.track.trackRef,
                            )
                        }
                    },
                    onRemoveDownload = {
                        scope.launch {
                            repository.removeOfflineCopy(
                                trackState.track.trackRef,
                            )
                        }
                    },
                )
            }
        }

        item(key = "vexstream-bottom-space") {
            Text(
                text = "",
                modifier = Modifier.padding(bottom = 20.dp),
            )
        }
    }
}

@Composable
private fun VexStreamConnectionCardBody(
    state: VexStreamMusicState,
    runtime: VexRuntime,
) {
    when (
        val connection = state.connectionState
    ) {
        VexStreamConnectionState.NotPaired -> {
            Text(
                text = stringResource(
                    R.string.vexstream_music_status_not_configured,
                ),
                style = MaterialTheme.typography.titleMedium,
            )
            Button(
                onClick = {
                    runtime.activate(
                        VexStreamMusicScreen.FindNearby,
                    )
                },
                modifier = Modifier.vexElement(
                    VexStreamMusicScreen.FindNearby,
                ),
            ) {
                Text(
                    stringResource(
                        R.string.vexstream_music_find,
                    ),
                )
            }
        }

        VexStreamConnectionState.Searching -> {
            Text(
                text = stringResource(
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
                text = stringResource(
                    R.string.vexstream_music_pairing_code_title,
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = formatPairingCode(
                    connection.pairingCode,
                ),
                style = MaterialTheme.typography.headlineLarge,
            )
            Text(
                text = stringResource(
                    R.string.vexstream_music_pairing_code_help,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = {
                    runtime.activate(
                        VexStreamMusicScreen.ConfirmPairingCode,
                    )
                },
                modifier = Modifier.vexElement(
                    VexStreamMusicScreen.ConfirmPairingCode,
                ),
            ) {
                Text(
                    stringResource(
                        R.string.vexstream_music_confirm_code,
                    ),
                )
            }
        }

        is VexStreamConnectionState.WaitingForDesktopApproval -> {
            Text(
                text = connection.displayName,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(
                    R.string.vexstream_music_waiting_approval,
                ),
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        is VexStreamConnectionState.Paired -> {
            Text(
                text =
                    if (state.hubState is VexStreamHubState.Ready) {
                        stringResource(
                            R.string.vexstream_music_status_ready,
                        ) + " · " + connection.displayName
                    } else {
                        stringResource(
                            R.string.vexstream_music_status_unreachable,
                        ) + " · " + connection.displayName
                    },
                style = MaterialTheme.typography.titleMedium,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (state.hubState is VexStreamHubState.Ready) {
                    Button(
                        onClick = {
                            runtime.activate(
                                VexStreamMusicScreen.RefreshCatalog,
                            )
                        },
                        modifier = Modifier.vexElement(
                            VexStreamMusicScreen.RefreshCatalog,
                        ),
                    ) {
                        Text(
                            stringResource(
                                R.string.vexstream_music_refresh,
                            ),
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            runtime.activate(
                                VexStreamMusicScreen.FindNearby,
                            )
                        },
                        modifier = Modifier.vexElement(
                            VexStreamMusicScreen.FindNearby,
                        ),
                    ) {
                        Text(
                            stringResource(
                                R.string.vexstream_music_find,
                            ),
                        )
                    }
                }
                OutlinedButton(
                    onClick = {
                        runtime.activate(
                            VexStreamMusicScreen.ForgetPaired,
                        )
                    },
                    modifier = Modifier.vexElement(
                        VexStreamMusicScreen.ForgetPaired,
                    ),
                ) {
                    Text(
                        stringResource(
                            R.string.vexstream_music_forget,
                        ),
                    )
                }
            }
        }

        is VexStreamConnectionState.Failed -> {
            Text(
                text = stringResource(
                    R.string.vexstream_music_pairing_failed,
                ),
                style = MaterialTheme.typography.bodyLarge,
            )
            Button(
                onClick = {
                    runtime.activate(
                        VexStreamMusicScreen.FindNearby,
                    )
                },
                modifier = Modifier.vexElement(
                    VexStreamMusicScreen.FindNearby,
                ),
            ) {
                Text(
                    stringResource(
                        R.string.vexstream_music_find,
                    ),
                )
            }
        }
    }
}

@Composable
private fun VexStreamTrackCard(
    state: VexStreamTrackState,
    canPlay: Boolean,
    onPlay: () -> Unit,
    onDownload: () -> Unit,
    onRemoveDownload: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = state.track.title,
                style = MaterialTheme.typography.titleMedium,
            )
            if (state.track.artist.isNotBlank()) {
                Text(
                    text = state.track.artist,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (state.track.album.isNotBlank()) {
                Text(
                    text = state.track.album,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = trackAvailabilityText(state),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onPlay,
                    enabled = canPlay,
                ) {
                    Text(
                        stringResource(
                            R.string.vexstream_music_play,
                        ),
                    )
                }

                when (state.localCopy) {
                    is VexStreamLocalCopyState.Downloaded -> {
                        OutlinedButton(
                            onClick = onRemoveDownload,
                        ) {
                            Text(
                                stringResource(
                                    R.string.vexstream_music_remove_download,
                                ),
                            )
                        }
                    }

                    is VexStreamLocalCopyState.Downloading -> {
                        Button(
                            onClick = {},
                            enabled = false,
                        ) {
                            Text(
                                stringResource(
                                    R.string.vexstream_music_downloading,
                                ),
                            )
                        }
                    }

                    else -> {
                        if (state.hubAvailable) {
                            Button(
                                onClick = onDownload,
                            ) {
                                Text(
                                    stringResource(
                                        R.string.vexstream_music_download,
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun trackAvailabilityText(
    state: VexStreamTrackState,
): String =
    when {
        state.localCopy is VexStreamLocalCopyState.Downloading ->
            stringResource(R.string.vexstream_music_downloading)

        state.localCopy is VexStreamLocalCopyState.Downloaded ->
            if (state.hubAvailable) {
                stringResource(R.string.vexstream_music_downloaded)
            } else {
                stringResource(R.string.vexstream_music_local_only)
            }

        state.availability == VexStreamTrackAvailability.HUB_ONLY ->
            stringResource(R.string.vexstream_music_hub_only)

        state.availability == VexStreamTrackAvailability.LOCAL_ONLY ->
            stringResource(R.string.vexstream_music_local_only)

        else ->
            stringResource(R.string.vexstream_music_unavailable)
    }

private fun formatPairingCode(
    code: String,
): String =
    if (code.length == 6) {
        code.take(3) + " " + code.takeLast(3)
    } else {
        code
    }

@VexScreenPreviews
@Composable
private fun VexStreamMusicScreenPreview() {
    VexPreviewHost(
        id = "vexstream-music",
        startScreen = VexStreamMusicScreen,
        repositoriesFactory = {
            val repository =
                InMemoryVexStreamMusicRepository(
                    initialState = previewState(),
                )
            com.vextreme.vexlife.android.preview.VexPreviewRepositories
                .empty()
                .copy(
                    vexStream = repository,
                )
        },
    ) { context ->
        VexStreamMusicScreenContent(
            runtime = context.runtime,
            repository = context.repositories.vexStream,
        )
    }
}

private fun previewState(): VexStreamMusicState {
    val streamed =
        VexStreamTrack(
            trackRef =
                VexStreamTrackRef(
                    "preview.streamed",
                ),
            mediaSha256 =
                VexStreamMediaSha256(
                    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                ),
            mediaByteLength = 5_000_000,
            title = "VexStream Mobile",
            artist = "Vex",
            album = "Desktop Hub",
            durationSeconds = 205.0,
        )
    val downloaded =
        VexStreamTrack(
            trackRef =
                VexStreamTrackRef(
                    "preview.downloaded",
                ),
            mediaSha256 =
                VexStreamMediaSha256(
                    "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
                ),
            mediaByteLength = 4_000_000,
            title = "Take It With You",
            artist = "Vex",
            album = "Offline",
            durationSeconds = 184.0,
        )

    return VexStreamMusicState(
        hubState =
            VexStreamHubState.Ready(
                hubRef = "vexstream.preview",
                catalogRevision = "preview-1",
            ),
        connectionState =
            VexStreamConnectionState.Paired(
                displayName = "VexStream on Home PC",
            ),
        tracks =
            listOf(
                VexStreamTrackState(
                    track = streamed,
                    hubAvailable = true,
                ),
                VexStreamTrackState(
                    track = downloaded,
                    hubAvailable = true,
                    localCopy =
                        VexStreamLocalCopyState.Downloaded(
                            mediaSha256 = downloaded.mediaSha256,
                            byteLength = downloaded.mediaByteLength,
                            localMediaRef = "preview.local",
                        ),
                ),
            ),
    )
}

// [VXG RealForever]
