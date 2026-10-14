package com.vextreme.vexlife.android.vexarchitecture.mediaplayer.queue

import com.vextreme.vexlife.android.vexarchitecture.mediaplayer.VexMediaPlayerProvider

/**
 * Runtime capability that can realize the Queue part of a media player.
 *
 * Queue is separate from Playback so a provider can truthfully implement one without the other.
 */
interface VexMediaQueueProvider :
    VexMediaPlayerProvider {

    /** Read the provider's current Queue status. */
    fun queueStatus():
        VexMediaQueueStatus

    /** Move to the provider's next media item. */
    fun next()

    /** Move to the provider's previous media item. */
    fun previous()

    /** Select one provider-owned queue index and request playback. */
    fun playQueueIndex(
        index: Int,
    )
}

// [VXG RealForever]
