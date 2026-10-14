package com.vextreme.vexlife.android.vexstream.playback

/**
 * Android playback transformations selected by semantic Acts.
 *
 * Effects are attempted runtime verbs, never claims about the resulting State.
 */
sealed interface VexStreamPlaybackEffect {
    data object RequestPlay :
        VexStreamPlaybackEffect

    data object RequestPause :
        VexStreamPlaybackEffect

    data object RestartCurrent :
        VexStreamPlaybackEffect
}
