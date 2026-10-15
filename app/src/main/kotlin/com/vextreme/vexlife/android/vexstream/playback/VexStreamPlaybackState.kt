package com.vextreme.vexlife.android.vexstream.playback

import com.vextreme.vexlife.android.behavior.VexBehaviorFamily

enum class VexStreamPlaybackPhase {
    Idle,
    Buffering,
    Ready,
}

sealed interface VexStreamPlaybackState :
    VexBehaviorFamily.State<
        VexStreamPlaybackFamily
    > {
    data object NoMedia :
        VexStreamPlaybackState

    data class Media(
        val playRequested: Boolean,
        val requestPending: Boolean = false,
        val phase: VexStreamPlaybackPhase,
        val playPauseAvailable: Boolean,
    ) : VexStreamPlaybackState

    data class Ended(
        val restartAvailable: Boolean,
        val restartPending: Boolean = false,
    ) : VexStreamPlaybackState
}

// [VXG RealForever]
