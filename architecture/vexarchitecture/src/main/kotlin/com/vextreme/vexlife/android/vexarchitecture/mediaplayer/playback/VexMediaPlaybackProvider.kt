package com.vextreme.vexlife.android.vexarchitecture.mediaplayer.playback

import com.vextreme.vexlife.android.vexarchitecture.mediaplayer.VexMediaPlayerProvider

/**
 * Runtime capability that can realize the Playback part of a media player.
 *
 * A third-party SDK adapter can implement Playback without also claiming Queue support.
 */
interface VexMediaPlaybackProvider :
    VexMediaPlayerProvider {

    /** Read the provider's current Playback status. */
    fun playbackStatus():
        VexMediaPlaybackStatus

    /** Ask the provider to request playback. */
    fun play()

    /** Ask the provider to clear the playback request while retaining current media. */
    fun pause()
}

// [VXG RealForever]
