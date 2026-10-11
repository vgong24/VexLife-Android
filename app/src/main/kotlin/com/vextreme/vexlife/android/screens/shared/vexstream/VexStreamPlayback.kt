package com.vextreme.vexlife.android.screens.shared.vexstream

import android.content.ComponentName
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.vextreme.vexlife.android.data.vexstream.VexStreamLocalCopyState
import com.vextreme.vexlife.android.data.vexstream.VexStreamMediaStore
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackState
import com.vextreme.vexlife.android.media.vexstream.VexStreamPlaybackService
import android.content.Context

/**
 * Resolves one VexStream track into the best currently valid Media3 source.
 *
 * A verified local copy wins. The Hub URL is used only while the current repository projection says
 * the track is Hub-available. The source choice is therefore playback plumbing, not another library
 * truth owner.
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

/**
 * Connects the Compose feature to the MediaSessionService without moving player ownership into UI.
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

internal fun MediaController.playVexStream(
    state: VexStreamTrackState,
    resolver: VexStreamPlaybackSourceResolver,
): Boolean {
    val mediaItem = resolver.mediaItem(state)
        ?: return false

    setMediaItem(mediaItem)
    prepare()
    play()
    return true
}

// [VXG RealForever]
