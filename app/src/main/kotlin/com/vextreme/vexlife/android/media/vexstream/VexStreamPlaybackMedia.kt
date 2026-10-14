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
internal fun MediaController.replaceVexStreamQueueAndPlay(
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


internal fun vexStreamQueueShuffleTargetOrder(
    itemCount: Int,
    currentIndex: Int,
    random: kotlin.random.Random = kotlin.random.Random.Default,
): List<Int> {
    if (itemCount <= 0) {
        return emptyList()
    }

    val lockedThrough =
        currentIndex.coerceIn(
            -1,
            itemCount - 1,
        )
    val prefix =
        if (lockedThrough >= 0) {
            (0..lockedThrough).toList()
        } else {
            emptyList()
        }
    val future =
        ((lockedThrough + 1) until itemCount)
            .toList()

    if (future.size <= 1) {
        return prefix + future
    }

    val randomized =
        future.shuffled(
            random,
        )
    val changed =
        if (randomized == future) {
            randomized.drop(1) +
                randomized.first()
        } else {
            randomized
        }

    return prefix + changed
}

internal fun MediaController.replaceVexStreamCollectionAndPlay(
    tracks: List<VexStreamTrackState>,
    resolver: VexStreamPlaybackSourceResolver,
    shuffled: Boolean,
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
        order.map(
            items::get,
        )

    shuffleModeEnabled = false
    setMediaItems(
        orderedItems,
    )
    prepare()
    play()
    return true
}

// [VXG RealForever]
