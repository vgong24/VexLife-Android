package com.vextreme.vexlife.android.screens.shared.vexstream

import android.content.Context
import android.net.Uri
import com.vextreme.vexlife.android.data.vexstream.VexStreamArtworkStore
import com.vextreme.vexlife.android.data.vexstream.VexStreamLocalCopyState
import com.vextreme.vexlife.android.data.vexstream.VexStreamMediaStore
import com.vextreme.vexlife.android.data.vexstream.VexStreamMusicRepository
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackState
import com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerPartner
import com.vextreme.vexlife.android.vexstream.track.VexStreamTrackPlaybackResolver

/**
 * Resolves product-level track availability into one Media3 item.
 *
 * Local verified bytes win. A Hub-backed track becomes an opaque vexstream:// item whose real
 * endpoint/token/certificate are resolved by VexStreamNetworkDataSource inside the playback service.
 */
internal class VexStreamPlaybackSourceResolver(
    context: Context,
    private val repository: VexStreamMusicRepository,
) :
    VexStreamTrackPlaybackResolver {
    private val mediaStore =
        VexStreamMediaStore.production(
            context = context,
        )
    private val artworkStore =
        VexStreamArtworkStore.production(
            context.filesDir,
        )

    override suspend fun prepareArtwork(
        state: VexStreamTrackState,
    ) {
        repository.preparePlaybackArtwork(
            trackRef =
                state.track.trackRef,
            expectedMediaSha256 =
                state.track.mediaSha256,
        )
    }

    override fun media(
        state:
            VexStreamTrackState,
    ):
        VexStreamPlayerPartner.Media? {
        val localCopy =
            state.localCopy as? VexStreamLocalCopyState.Downloaded

        val uri =
            vexStreamAdmittedSharedPlaybackRef(
                state,
            )
                ?.let(
                    Uri::parse,
                )
                ?: localCopy
                    ?.takeIf { copy ->
                        copy.mediaSha256 ==
                            state.track.mediaSha256 &&
                            copy.byteLength ==
                            state.track.mediaByteLength
                    }
                    ?.let { copy ->
                        /*
                         * Legacy app-private copies retain activation-time verification. Durable
                         * shared MediaStore copies already crossed media-store verification before
                         * the repository admitted Downloaded truth and therefore take the cheap
                         * content-ref projection above.
                         */
                        mediaStore
                            .fileForPlayback(
                                copy,
                            )
                            ?.let(
                                Uri::fromFile,
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

        val playbackStartMs =
            (
                state.track.playbackStartSeconds *
                    1000.0
            )
                .toLong()
        val playbackEndMs =
            state.track
                .playbackEndSeconds
                ?.let { end ->
                    (
                        end *
                            1000.0
                    )
                        .toLong()
                }

        val artworkRef =
            artworkStore
                .fileFor(
                    state.track,
                )
                ?.let(
                    Uri::fromFile,
                )
                ?.toString()
                ?: state.track
                    .artworkUrl

        return VexStreamPlayerPartner.Media(
            mediaId =
                state.track.trackRef.value,
            sourceRef =
                uri.toString(),
            mimeType =
                "audio/mpeg",
            title =
                state.track.title,
            artist =
                state.track.artist,
            album =
                state.track.album,
            artworkRef =
                artworkRef,
            playbackStartMs =
                playbackStartMs,
            playbackEndMs =
                playbackEndMs,
        )
    }
}

/**
 * Cheap playback projection for current shared local truth.
 *
 * Downloaded is admitted only after the media-store owner has verified exact digest + byte length.
 * Playback therefore reuses that admitted content reference when it still matches the current Track
 * identity instead of re-querying/re-hashing the same shared object for every item on every Play.
 *
 * Legacy app-private refs deliberately return null so their existing activation-time verification
 * path remains intact.
 */
internal fun vexStreamAdmittedSharedPlaybackRef(
    state: VexStreamTrackState,
): String? {
    val localCopy =
        state.localCopy as?
            VexStreamLocalCopyState.Downloaded
            ?: return null

    if (
        localCopy.mediaSha256 !=
            state.track.mediaSha256 ||
        localCopy.byteLength !=
            state.track.mediaByteLength
    ) {
        return null
    }

    return localCopy.localMediaRef
        .takeIf {
            it.startsWith(
                "content://",
            )
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
