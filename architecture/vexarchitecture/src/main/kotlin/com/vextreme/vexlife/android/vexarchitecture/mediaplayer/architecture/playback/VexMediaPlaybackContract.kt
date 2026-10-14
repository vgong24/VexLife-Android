package com.vextreme.vexlife.android.vexarchitecture.mediaplayer.architecture.playback

import com.vextreme.vexlife.android.vexarchitecture.mediaplayer.architecture.VexMediaPlayerArchitecture

/**
 * Reusable semantic contract for the familiar Play/Pause concern of a media player.
 *
 * This belongs to the architecture side of the package: it describes what Playback means, not how an
 * SDK performs it.
 */
interface VexMediaPlaybackContract {
    /** Media-player architecture whose Playback capability this object belongs to. */
    val playerArchitecture:
        VexMediaPlayerArchitecture
}

// [VXG RealForever]
