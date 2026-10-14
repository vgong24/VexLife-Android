package com.vextreme.vexlife.android.vexarchitecture.mediaplayer.provider.queue

import com.vextreme.vexlife.android.vexarchitecture.mediaplayer.provider.VexMediaPlayerProvider

/**
 * Runtime capability a Vex Queue concern may depend on.
 *
 * Queue is separate from Playback so a provider can truthfully implement one without the other.
 */
interface VexMediaQueueProvider :
    VexMediaPlayerProvider {

    /** Observe actual provider queue-position truth. */
    fun observeQueue():
        VexMediaQueueSnapshot

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
