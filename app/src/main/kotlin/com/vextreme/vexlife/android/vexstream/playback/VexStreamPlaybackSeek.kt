package com.vextreme.vexlife.android.vexstream.playback

import com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerConnectionViewModel
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * One exact request to seek the media item the listener actually scrubbed.
 *
 * Media identity + current index are part of the request so a delayed/stale scrub cannot seek
 * another occurrence of the same track or whichever item happens to be current later.
 */
internal data class VexStreamPlaybackSeekRequest(
    val mediaId: String,
    val mediaItemIndex: Int,
    val positionMs: Long,
) {
    init {
        require(
            mediaId.isNotBlank(),
        ) {
            "seek mediaId must not be blank"
        }
        require(
            mediaItemIndex >= 0,
        ) {
            "seek mediaItemIndex must not be negative"
        }
        require(
            positionMs >= 0L,
        ) {
            "seek positionMs must not be negative"
        }
    }
}

/**
 * Feature-local payload boundary for Playback Seek.
 *
 * Provider realization stays behind the ViewModel-owned Player Partner controller Connection.
 */
internal interface VexStreamPlaybackSeekConnection {
    val isAvailable: Boolean

    fun seek(
        request:
            VexStreamPlaybackSeekRequest,
    ):
        Boolean
}

@Composable
internal fun rememberVexStreamPlaybackSeekConnection():
    VexStreamPlaybackSeekConnection? {
    val owner:
        VexStreamPlayerConnectionViewModel =
        viewModel()

    return owner.seekConnection
}

internal fun vexStreamPlaybackSeekTarget(
    request: VexStreamPlaybackSeekRequest,
    currentMediaId: String?,
    currentMediaItemIndex: Int,
    seekAvailable: Boolean,
): Long? =
    request.positionMs
        .takeIf {
            seekAvailable &&
                currentMediaId ==
                request.mediaId &&
                currentMediaItemIndex ==
                request.mediaItemIndex
        }

internal fun vexStreamPlaybackSeekPositionMs(
    fraction: Float,
    durationMs: Long,
): Long {
    if (
        durationMs <= 0L
    ) {
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

// [VXG RealForever][VEXSTREAM][PLAYBACK][PLAYER-PARTNER]
