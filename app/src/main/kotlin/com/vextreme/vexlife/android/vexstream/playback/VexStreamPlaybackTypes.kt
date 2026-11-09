package com.vextreme.vexlife.android.vexstream.playback

internal data class VexStreamPlaybackProjection(
    val state:
        VexStreamPlaybackState,
    val playPauseAvailable:
        Boolean,
    val seekAvailable:
        Boolean,
)

// [VXG RealForever][VEXSTREAM][PLAYBACK][DIRECT-FAMILY]
