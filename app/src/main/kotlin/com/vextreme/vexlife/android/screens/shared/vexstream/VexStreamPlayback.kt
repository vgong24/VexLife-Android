package com.vextreme.vexlife.android.screens.shared.vexstream

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.vextreme.vexlife.android.data.vexstream.VexStreamLocalCopyState
import com.vextreme.vexlife.android.data.vexstream.VexStreamMediaStore
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackState
import com.vextreme.vexlife.android.media.vexstream.VexStreamPlaybackService

/**
 * Resolves product-level track availability into one Media3 item.
 *
 * Local verified bytes win. A Hub-backed track becomes an opaque vexstream:// item whose real
 * endpoint/token/certificate are resolved by VexStreamNetworkDataSource inside the playback service.
 */
internal class VexStreamPlaybackSourceResolver(
    context: Context,
) {
    private val mediaStore =
        VexStreamMediaStore.production(
            contextFilesDir = context.filesDir,
        )

    fun mediaItem(
        state: VexStreamTrackState,
    ): MediaItem? {
        val localCopy =
            state.localCopy as? VexStreamLocalCopyState.Downloaded

        val uri =
            localCopy
                ?.let(mediaStore::fileForPlayback)
                ?.let(Uri::fromFile)
                ?: if (state.hubAvailable) {
                    Uri.Builder()
                        .scheme("vexstream")
                        .authority("media")
                        .appendPath(
                            state.track.trackRef.value,
                        )
                        .build()
                } else {
                    return null
                }

        val clipping =
            MediaItem.ClippingConfiguration.Builder()
                .setStartPositionMs(
                    (state.track.playbackStartSeconds * 1000.0)
                        .toLong(),
                )
                .apply {
                    state.track.playbackEndSeconds?.let { end ->
                        setEndPositionMs(
                            (end * 1000.0).toLong(),
                        )
                    }
                }
                .build()

        return MediaItem.Builder()
            .setMediaId(state.track.trackRef.value)
            .setUri(uri)
            .setMimeType(MimeTypes.AUDIO_MPEG)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(state.track.title)
                    .setArtist(state.track.artist)
                    .setAlbumTitle(state.track.album)
                    .build(),
            )
            .setClippingConfiguration(clipping)
            .build()
    }
}

internal data class VexStreamPlaybackUiState(
    val currentMediaId: String? = null,
    val currentTitle: String = "",
    val currentArtist: String = "",
    val isPlaying: Boolean = false,
    val shuffleEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val queueMediaIds: List<String> = emptyList(),
    val currentIndex: Int = -1,
) {
    val hasQueue: Boolean
        get() = queueMediaIds.isNotEmpty()

    val repeatEnabled: Boolean
        get() = repeatMode != Player.REPEAT_MODE_OFF

    companion object {
        val Empty = VexStreamPlaybackUiState()
    }
}

/**
 * Connects Compose to the Android-owned MediaSessionService without moving player lifetime into UI.
 */
@Composable
internal fun rememberVexStreamMediaController(): MediaController? {
    val context = LocalContext.current
    var controller by remember {
        mutableStateOf<MediaController?>(null)
    }

    DisposableEffect(context) {
        val sessionToken =
            SessionToken(
                context,
                ComponentName(
                    context,
                    VexStreamPlaybackService::class.java,
                ),
            )
        val controllerFuture =
            MediaController.Builder(
                context,
                sessionToken,
            ).buildAsync()

        controllerFuture.addListener(
            {
                runCatching {
                    controllerFuture.get()
                }.onSuccess { connected ->
                    controller = connected
                }
            },
            ContextCompat.getMainExecutor(context),
        )

        onDispose {
            controller = null
            MediaController.releaseFuture(controllerFuture)
        }
    }

    return controller
}

@Composable
internal fun rememberVexStreamPlaybackUiState(
    controller: MediaController?,
): State<VexStreamPlaybackUiState> {
    val projected =
        remember(controller) {
            mutableStateOf(
                controller?.toVexStreamUiState()
                    ?: VexStreamPlaybackUiState.Empty,
            )
        }

    DisposableEffect(controller) {
        if (controller == null) {
            projected.value = VexStreamPlaybackUiState.Empty
            return@DisposableEffect onDispose {}
        }

        val listener =
            object : Player.Listener {
                override fun onEvents(
                    player: Player,
                    events: Player.Events,
                ) {
                    projected.value = player.toVexStreamUiState()
                }
            }

        controller.addListener(listener)
        projected.value = controller.toVexStreamUiState()

        onDispose {
            controller.removeListener(listener)
        }
    }

    return projected
}

internal fun MediaController.playVexStreamContext(
    selected: VexStreamTrackState,
    contextTracks: List<VexStreamTrackState>,
    resolver: VexStreamPlaybackSourceResolver,
    shuffled: Boolean = false,
): Boolean {
    val items =
        contextTracks
            .mapNotNull(resolver::mediaItem)

    if (items.isEmpty()) {
        return false
    }

    val selectedIndex =
        items.indexOfFirst {
            it.mediaId == selected.track.trackRef.value
        }
    if (selectedIndex < 0) {
        return false
    }

    setMediaItems(
        items,
        selectedIndex,
        0L,
    )
    shuffleModeEnabled = shuffled
    prepare()
    play()
    return true
}

internal fun MediaController.playVexStreamCollection(
    tracks: List<VexStreamTrackState>,
    resolver: VexStreamPlaybackSourceResolver,
    shuffled: Boolean = false,
): Boolean {
    val items =
        tracks.mapNotNull(
            resolver::mediaItem,
        )
    if (items.isEmpty()) {
        return false
    }

    setMediaItems(items)
    shuffleModeEnabled = shuffled
    if (shuffled && items.size > 1) {
        seekToDefaultPosition(
            (0 until items.size).random(),
        )
    }
    prepare()
    play()
    return true
}

internal fun MediaController.playNextVexStream(
    state: VexStreamTrackState,
    resolver: VexStreamPlaybackSourceResolver,
): Boolean {
    val item =
        resolver.mediaItem(state)
            ?: return false

    if (mediaItemCount == 0) {
        setMediaItem(item)
        prepare()
        play()
        return true
    }

    val insertAt =
        (currentMediaItemIndex + 1)
            .coerceIn(
                0,
                mediaItemCount,
            )
    addMediaItem(
        insertAt,
        item,
    )
    return true
}

internal fun MediaController.queueVexStream(
    state: VexStreamTrackState,
    resolver: VexStreamPlaybackSourceResolver,
): Boolean {
    val item =
        resolver.mediaItem(state)
            ?: return false

    addMediaItem(item)
    return true
}

internal fun MediaController.toggleVexStreamPlayback() {
    if (isPlaying) {
        pause()
    } else {
        play()
    }
}

internal fun MediaController.toggleVexStreamShuffle() {
    shuffleModeEnabled = !shuffleModeEnabled
}

internal fun MediaController.toggleVexStreamRepeatAll() {
    repeatMode =
        if (repeatMode == Player.REPEAT_MODE_OFF) {
            Player.REPEAT_MODE_ALL
        } else {
            Player.REPEAT_MODE_OFF
        }
}

private fun Player.toVexStreamUiState(): VexStreamPlaybackUiState {
    val current =
        currentMediaItem

    return VexStreamPlaybackUiState(
        currentMediaId = current?.mediaId,
        currentTitle =
            current
                ?.mediaMetadata
                ?.title
                ?.toString()
                .orEmpty(),
        currentArtist =
            current
                ?.mediaMetadata
                ?.artist
                ?.toString()
                .orEmpty(),
        isPlaying = isPlaying,
        shuffleEnabled = shuffleModeEnabled,
        repeatMode = repeatMode,
        queueMediaIds =
            (0 until mediaItemCount)
                .map { index ->
                    getMediaItemAt(index).mediaId
                },
        currentIndex =
            if (mediaItemCount > 0) {
                currentMediaItemIndex
            } else {
                -1
            },
    )
}

// [VXG RealForever]
