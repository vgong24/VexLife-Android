package com.vextreme.vexlife.android.vexstream.playback

/**
 * Current Media3 lifecycle flavor while one media item exists.
 *
 * This does not claim human intent by itself. [VexStreamPlaybackState.Media.playRequested] carries
 * whether playback is currently requested while the lifecycle may still be buffering/preparing.
 */
enum class VexStreamPlaybackPhase {
    Idle,
    Buffering,
    Ready,
}

/**
 * Current semantic Playback truth projected from the Android player.
 */
sealed interface VexStreamPlaybackState {
    /** No current media exists, so transport Controls have no semantic target. */
    data object NoMedia :
        VexStreamPlaybackState

    /**
     * Media exists and is not terminally ended.
     *
     * playRequested is intentionally distinct from actual audible playback. A buffering player may
     * have playRequested=true while Player.isPlaying is still false.
     */
    data class Media(
        val playRequested: Boolean,
        val phase: VexStreamPlaybackPhase,
        val playPauseAvailable: Boolean,
    ) : VexStreamPlaybackState

    /**
     * Current media reached its terminal end.
     *
     * Restart capability is explicit because Play from here means restart-current, not merely
     * setting playWhenReady.
     */
    data class Ended(
        val restartAvailable: Boolean,
    ) : VexStreamPlaybackState
}
