package com.vextreme.vexlife.android.vexarchitecture.mediaplayer.queue

import com.vextreme.vexlife.android.vexarchitecture.mediaplayer.VexMediaPlayerArchitecture

/**
 * Reusable semantic contract for the Queue part of a media player.
 *
 * Queue remains optional. A media-player architecture implements this only when ordered neighboring
 * media is actually part of the experience.
 */
interface VexMediaQueueContract {
    /** Media-player architecture whose Queue capability this object belongs to. */
    val playerArchitecture:
        VexMediaPlayerArchitecture
}

// [VXG RealForever]
