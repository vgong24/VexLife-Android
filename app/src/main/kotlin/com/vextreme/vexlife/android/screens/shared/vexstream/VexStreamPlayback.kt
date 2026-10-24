package com.vextreme.vexlife.android.screens.shared.vexstream

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import com.vextreme.vexlife.android.data.vexstream.VexStreamLocalCopyState
import com.vextreme.vexlife.android.data.vexstream.VexStreamMediaStore
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackState
import com.vextreme.vexlife.android.vexstream.track.VexStreamTrackMediaItemResolver

/**
 * Resolves product-level track availability into one Media3 item.
 *
 * Local verified bytes win. A Hub-backed track becomes an opaque vexstream:// item whose real
 * endpoint/token/certificate are resolved by VexStreamNetworkDataSource inside the playback service.
 */
internal class VexStreamPlaybackSourceResolver(
    context: Context,
) :
    VexStreamTrackMediaItemResolver {
    private val mediaStore =
        VexStreamMediaStore.production(
            context = context,
        )

    override fun mediaItem(
        state: VexStreamTrackState,
    ): MediaItem? {
        val localCopy =
            state.localCopy as? VexStreamLocalCopyState.Downloaded

        val uri =
            localCopy
                ?.let { copy ->
                    mediaStore
                        .fileForPlayback(
                            copy,
                        )
                        ?.let(
                            Uri::fromFile,
                        )
                        ?: mediaStore
                            .contentUriForPlayback(
                                copy,
                            )
                            ?.let(
                                Uri::parse,
                            )
                }
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
                    .apply {
                        state.track.artworkUrl
                            ?.let { artworkUrl ->
                                setArtworkUri(
                                    Uri.parse(
                                        artworkUrl,
                                    ),
                                )
                            }
                    }
                    .build(),
            )
            .setClippingConfiguration(clipping)
            .build()
    }
}

internal fun formatVexStreamPlaybackTime(
    positionMs: Long,
): String {
    val totalSeconds =
        positionMs
            .coerceAtLeast(0L) / 1_000L
    val hours = totalSeconds / 3_600L
    val minutes =
        (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L

    return if (hours > 0L) {
        "$hours:" +
            minutes.toString().padStart(2, '0') +
            ":" +
            seconds.toString().padStart(2, '0')
    } else {
        "$minutes:" +
            seconds.toString().padStart(2, '0')
    }
}

// [VXG RealForever]
