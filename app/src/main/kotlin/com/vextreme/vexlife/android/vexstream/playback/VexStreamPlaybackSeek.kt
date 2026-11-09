package com.vextreme.vexlife.android.vexstream.playback

import com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerPartner
import com.vextreme.vexlife.android.vexstream.player.seekTarget

internal data class VexStreamPlaybackSeekRequest(
    val mediaId:
        String,
    val mediaItemIndex:
        Int,
    val positionMs:
        Long,
) {
    init {
        require(
            mediaId.isNotBlank(),
        )
        require(
            mediaItemIndex >=
                0,
        )
        require(
            positionMs >=
                0L,
        )
    }
}

internal fun rememberVexStreamPlaybackSeekConnection():
    VexStreamPlayerPartner.Connection.Request? {
    val owner:
        VexStreamMusicViewModel =
        viewModel()

    return owner.playerRequestConnection
}

internal fun vexStreamPlaybackSeekTarget(
    request:
        VexStreamPlaybackSeekRequest,
    currentMediaId:
        String?,
    currentMediaItemIndex:
        Int,
    seekAvailable:
        Boolean,
):
    Long? =
    seekTarget(
        mediaId =
            request.mediaId,
        mediaItemIndex =
            request.mediaItemIndex,
        positionMs =
            request.positionMs,
        currentMediaId =
            currentMediaId,
        currentMediaItemIndex =
            currentMediaItemIndex,
        seekAvailable =
            seekAvailable,
    )

internal fun vexStreamPlaybackSeekPositionMs(
    fraction:
        Float,
    durationMs:
        Long,
):
    Long {
    if (
        durationMs <=
        0L
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
