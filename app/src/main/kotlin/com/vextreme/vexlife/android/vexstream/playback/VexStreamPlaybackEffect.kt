package com.vextreme.vexlife.android.vexstream.playback

import com.vextreme.vexlife.android.behavior.VexBehaviorFamily

/** Runtime playback transformations selected by semantic Acts. */
sealed interface VexStreamPlaybackEffect :
    VexBehaviorFamily.Effect<
        VexStreamPlaybackFamily
    > {
    data object RequestPlay :
        VexStreamPlaybackEffect

    data object RequestPause :
        VexStreamPlaybackEffect

    data object RestartCurrent :
        VexStreamPlaybackEffect
}

// [VXG RealForever]
