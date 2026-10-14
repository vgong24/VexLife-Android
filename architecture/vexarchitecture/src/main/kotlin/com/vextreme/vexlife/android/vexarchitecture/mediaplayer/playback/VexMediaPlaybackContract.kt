package com.vextreme.vexlife.android.vexarchitecture.mediaplayer.playback

import com.vextreme.vexlife.android.vexarchitecture.mediaplayer.VexMediaPlayerArchitecture

/**
 * Reusable semantic contract for the familiar Play/Pause part of a media player.
 *
 * This file describes what Playback means structurally. Runtime realization is the Provider sibling
 * in the same playback/ area.
 */
interface VexMediaPlaybackContract {
    /** Media-player architecture whose Playback capability this object belongs to. */
    val playerArchitecture:
        VexMediaPlayerArchitecture
}

// [VXG RealForever]
