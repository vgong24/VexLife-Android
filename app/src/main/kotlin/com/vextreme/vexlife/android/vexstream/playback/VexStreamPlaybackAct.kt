package com.vextreme.vexlife.android.vexstream.playback

import com.vextreme.vexlife.android.behavior.VexBehaviorFamily

/** Human/product verbs admitted by VexStream Playback. */
sealed interface VexStreamPlaybackAct :
    VexBehaviorFamily.Act<
        VexStreamPlaybackFamily
    > {
    data object Play :
        VexStreamPlaybackAct

    data object Pause :
        VexStreamPlaybackAct
}

// [VXG RealForever]
