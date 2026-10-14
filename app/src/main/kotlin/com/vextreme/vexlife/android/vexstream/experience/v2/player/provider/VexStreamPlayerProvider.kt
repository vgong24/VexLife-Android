package com.vextreme.vexlife.android.vexstream.experience.v2.player.provider

import com.vextreme.vexlife.android.vexarchitecture.mediaplayer.provider.playback.VexMediaPlaybackProvider
import com.vextreme.vexlife.android.vexarchitecture.mediaplayer.provider.queue.VexMediaQueueProvider

/**
 * Runtime provider shape required by the current VexStream Player Architecture.
 *
 * This is intentionally an intersection of the capabilities VexStream currently uses, not a claim
 * that every possible media player must support Queue. A partner/VLC/remote adapter can implement
 * this interface when it can satisfy the same VexStream experience.
 */
interface VexStreamPlayerProvider :
    VexMediaPlaybackProvider,
    VexMediaQueueProvider

// [VXG RealForever]
