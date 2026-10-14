package com.vextreme.vexlife.android.vexarchitecture.mediaplayer.provider.playback

/**
 * Platform-neutral observation returned by a Playback provider.
 *
 * Snapshot means one read from the runtime provider. It is not retained as a second semantic truth
 * owner; the feature translates it into its current Playback Case.
 */
data class VexMediaPlaybackSnapshot(
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
