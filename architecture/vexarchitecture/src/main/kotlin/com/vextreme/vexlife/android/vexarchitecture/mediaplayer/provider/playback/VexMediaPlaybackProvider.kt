package com.vextreme.vexlife.android.vexarchitecture.mediaplayer.provider.playback

import com.vextreme.vexlife.android.vexarchitecture.mediaplayer.provider.VexMediaPlayerProvider

/**
 * Runtime capability a Vex Playback concern may depend on.
 *
 * A third-party SDK adapter can implement this without also claiming Queue support.
 */
interface VexMediaPlaybackProvider :
    VexMediaPlayerProvider {

    /** Observe actual provider playback truth. */
    fun observePlayback():
        VexMediaPlaybackSnapshot

    /** Ask the provider to request playback. */
    fun play()

    /** Ask the provider to clear the playback request while retaining current media. */
    fun pause()
}

// [VXG RealForever]
