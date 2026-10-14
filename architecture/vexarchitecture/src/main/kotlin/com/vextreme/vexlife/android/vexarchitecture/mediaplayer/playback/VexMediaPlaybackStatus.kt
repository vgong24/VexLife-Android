package com.vextreme.vexlife.android.vexarchitecture.mediaplayer.playback

/**
 * Current Playback facts reported by a runtime provider.
 *
 * Status is deliberately not the semantic truth owner. The feature translates these provider facts
 * into its own current Playback Case.
 */
data class VexMediaPlaybackStatus(
    /** Whether the provider currently has media that could be played. */
    val hasMedia: Boolean,
    /**
     * Whether playback is requested.
     *
     * This wording allows buffering/preparing without falsely claiming sound/video is already being
     * rendered.
     */
    val playbackRequested: Boolean,
)

// [VXG RealForever]
