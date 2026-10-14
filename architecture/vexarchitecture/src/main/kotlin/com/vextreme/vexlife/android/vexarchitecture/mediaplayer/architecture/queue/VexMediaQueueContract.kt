package com.vextreme.vexlife.android.vexarchitecture.mediaplayer.architecture.queue

import com.vextreme.vexlife.android.vexarchitecture.mediaplayer.architecture.VexMediaPlayerArchitecture

/**
 * Reusable semantic contract for ordered media around the current item.
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
