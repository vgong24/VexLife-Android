package com.vextreme.vexlife.android.media.vexstream

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.session.MediaController
import com.vextreme.vexlife.android.data.vexstream.VexStreamLocalCopyState
import com.vextreme.vexlife.android.data.vexstream.VexStreamMediaStore
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackState

/**
 * Android media-source resolution for VexStream playback.
 *
 * Local verified bytes win. Hub-backed tracks become opaque vexstream:// items whose endpoint/token/
 * certificate resolution remains inside the playback service network data source.
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
 * Android realization for the PlayTrack Control after architecture admission.
 */
internal fun MediaController.replaceVexStreamContextAndPlay(
    selected: VexStreamTrackState,
    contextTracks: List<VexStreamTrackState>,
    resolver: VexStreamPlaybackSourceResolver,
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
    shuffleModeEnabled = false
    prepare()
    play()
    return true
}

// [VXG RealForever]
