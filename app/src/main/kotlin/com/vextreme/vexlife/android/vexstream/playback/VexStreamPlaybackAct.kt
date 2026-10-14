package com.vextreme.vexlife.android.vexstream.playback

/** Human/product verbs admitted by VexStream Playback. */
sealed interface VexStreamPlaybackAct {
    data object Play :
        VexStreamPlaybackAct

    data object Pause :
        VexStreamPlaybackAct
}
