package com.vextreme.vexlife.android.vexstream.playback

/**
 * Current Media3 lifecycle flavor while one media item exists.
 *
 * This does not claim human intent by itself. [VexStreamPlaybackState.Media.playRequested] carries
 * the Port's current semantic playback request while [VexStreamPlaybackState.Media.requestPending]
 * says whether the provider has not yet acknowledged that request.
 */
enum class VexStreamPlaybackPhase {
    Idle,
    Buffering,
    Ready,
}

/**
 * Current semantic Playback truth projected through the VexPort.
 *
 * State may contain both provider-observed facts and a truthful local pending intent. A pending
 * request is not treated as provider convergence; it is the current system fact that an admitted Act
 * has already selected an Effect and is waiting for Media3 to acknowledge the requested value.
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
     *
     * requestPending=true means the VexPort has synchronously admitted a newer local request that
     * Media3 has not yet reflected through playWhenReady.
     */
    data class Media(
        val playRequested: Boolean,
        val requestPending: Boolean = false,
        val phase: VexStreamPlaybackPhase,
        val playPauseAvailable: Boolean,
    ) : VexStreamPlaybackState

    /**
     * Current media reached its terminal end.
     *
     * restartPending=true means Play has synchronously selected RestartCurrent but Media3 has not yet
     * left the terminal state.
     */
    data class Ended(
        val restartAvailable: Boolean,
        val restartPending: Boolean = false,
    ) : VexStreamPlaybackState
}
