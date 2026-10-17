package com.vextreme.vexlife.android.vexstream.playback

import androidx.media3.common.Player
import androidx.media3.session.MediaController

/**
 * One exact request to seek the media item the listener actually scrubbed.
 *
 * The media identity is part of the request so a delayed/stale scrub cannot seek whichever item
 * happens to be current later.
 */
internal data class VexStreamPlaybackSeekRequest(
    val mediaId: String,
    val positionMs: Long,
) {
    init {
        require(
            mediaId.isNotBlank(),
        ) {
            "seek mediaId must not be blank"
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
 * Seek intentionally does not pretend an arbitrary millisecond value is a finite Behavior Control.
 * UI availability is informative only; [seek] re-checks provider command availability and exact
 * current media identity at activation time.
 */
internal interface VexStreamPlaybackSeekConnection {
    val isAvailable: Boolean

    fun seek(
        request: VexStreamPlaybackSeekRequest,
    ): Boolean
}

internal class Media3VexStreamPlaybackSeekConnection(
    private val controller: MediaController,
) :
    VexStreamPlaybackSeekConnection {
    override val isAvailable: Boolean
        get() =
            controller.currentMediaItem != null &&
                controller.isCommandAvailable(
                    Player
                        .COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,
                )

    override fun seek(
        request: VexStreamPlaybackSeekRequest,
    ): Boolean {
        val currentMediaId =
            controller
                .currentMediaItem
                ?.mediaId
        val seekAvailable =
            controller.isCommandAvailable(
                Player
                    .COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,
            )
        val target =
            vexStreamPlaybackSeekTarget(
                request = request,
                currentMediaId =
                    currentMediaId,
                seekAvailable =
                    seekAvailable,
            )
                ?: return false

        controller.seekTo(
            target,
        )
        return true
    }
}

internal fun vexStreamPlaybackSeekTarget(
    request: VexStreamPlaybackSeekRequest,
    currentMediaId: String?,
    seekAvailable: Boolean,
): Long? =
    request.positionMs
        .takeIf {
            seekAvailable &&
                currentMediaId ==
                request.mediaId
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

// [VXG RealForever]
