package com.vextreme.vexlife.android.vexstream.playback

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
