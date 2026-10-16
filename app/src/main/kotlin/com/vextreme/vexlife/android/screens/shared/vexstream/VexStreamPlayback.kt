package com.vextreme.vexlife.android.screens.shared.vexstream

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.vextreme.vexlife.android.data.vexstream.VexStreamLocalCopyState
import com.vextreme.vexlife.android.data.vexstream.VexStreamMediaStore
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackState
import com.vextreme.vexlife.android.vexstream.queue.vexStreamQueueShuffleTargetOrder

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

internal fun MediaController.playVexStreamContext(
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

    val order =
        if (shuffled) {
            vexStreamQueueShuffleTargetOrder(
                itemCount = items.size,
                currentIndex = -1,
            )
        } else {
            items.indices.toList()
        }
    val orderedItems =
        order.map(items::get)

    shuffleModeEnabled = false
    setMediaItems(orderedItems)
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

/**
 * Returns the adjacent queue index crossed by one drag gesture sample.
 *
 * The queue itself remains MediaSession-owned. Compose uses this only to decide when the dragged
 * row has crossed half of its current visual height and should request one Media3 move.
 */
internal fun vexStreamQueueAdjacentMoveTarget(
    currentIndex: Int,
    dragOffsetPx: Float,
    itemHeightPx: Int,
    lastIndex: Int,
): Int? {
    if (
        currentIndex !in 0..lastIndex ||
        itemHeightPx <= 0
    ) {
        return null
    }

    val threshold =
        itemHeightPx.toFloat() / 2f

    return when {
        dragOffsetPx > threshold &&
            currentIndex < lastIndex ->
            currentIndex + 1

        dragOffsetPx < -threshold &&
            currentIndex > 0 ->
            currentIndex - 1

        else -> null
    }
}

internal fun MediaController.toggleVexStreamRepeatAll() {
    repeatMode =
        if (repeatMode == Player.REPEAT_MODE_OFF) {
            Player.REPEAT_MODE_ALL
        } else {
            Player.REPEAT_MODE_OFF
        }
}

internal fun vexStreamSeekPositionMs(
    fraction: Float,
    durationMs: Long,
): Long {
    if (durationMs <= 0L) {
        return 0L
    }

    return (
        durationMs.toDouble() *
            fraction
                .coerceIn(
                    0f,
                    1f,
                )
                .toDouble()
    ).toLong()
        .coerceIn(
            0L,
            durationMs,
        )
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
