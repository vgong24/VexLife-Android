package com.vextreme.vexlife.android.vexstream.playback

import com.vextreme.vexlife.android.behavior.VexFamily

/** Runtime Media3 transformation requested by one lawful Playback Family Act. */
sealed interface VexStreamPlaybackEffect :
    VexFamily.Effect<
        VexStreamPlaybackFamily
    > {
    data object RequestPlay :
        VexStreamPlaybackEffect

    data object RequestPause :
        VexStreamPlaybackEffect

    data object RestartCurrent :
        VexStreamPlaybackEffect
}

// [VXG RealForever][VEXSTREAM][PLAYBACK][DIRECT-FAMILY]
