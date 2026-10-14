package com.vextreme.vexlife.android.vexstream.experience.v2.player.provider.media3

import androidx.media3.session.MediaController
import com.vextreme.vexlife.android.vexarchitecture.mediaplayer.provider.playback.VexMediaPlaybackSnapshot
import com.vextreme.vexlife.android.vexarchitecture.mediaplayer.provider.queue.VexMediaQueueSnapshot
import com.vextreme.vexlife.android.vexstream.experience.v2.player.provider.VexStreamPlayerProvider

/**
 * Media3 adapter that fulfills the VexStream Player provider contract.
 *
 * The Provider suffix is intentional: this class is runtime realization, not the Player Architecture
 * itself. A future VLC/partner/remote adapter can implement the same VexStream-facing provider
 * contract without changing Playback/Queue semantic vocabulary.
 */
class Media3VexStreamPlayerProvider(
    /**
     * Existing client of the service-owned MediaSession.
     *
     * This adapter neither creates nor releases it, preserving current playback lifetime ownership.
     */
    private val controller: MediaController,
) : VexStreamPlayerProvider {

    /** Read only the provider facts the Playback capability needs. */
    override fun observePlayback(): VexMediaPlaybackSnapshot =
        VexMediaPlaybackSnapshot(
            hasMedia = controller.mediaItemCount > 0,
            playbackRequested = controller.playWhenReady,
        )

    /** Read only the provider facts the Queue capability needs. */
    override fun observeQueue(): VexMediaQueueSnapshot =
        VexMediaQueueSnapshot(
            itemCount = controller.mediaItemCount,
            currentIndex = controller.currentMediaItemIndex,
            hasPrevious = controller.hasPreviousMediaItem(),
            hasNext = controller.hasNextMediaItem(),
        )

    /** Media3 realization of semantic Play. */
    override fun play() {
        controller.play()
    }

    /** Media3 realization of semantic Pause. */
    override fun pause() {
        controller.pause()
    }

    /** Media3 realization of semantic Queue.Next. */
    override fun next() {
        controller.seekToNextMediaItem()
    }

    /** Media3 realization of semantic Queue.Previous. */
    override fun previous() {
        controller.seekToPreviousMediaItem()
    }

    /** Media3 realization of selecting one existing queue item and requesting playback. */
    override fun playQueueIndex(
        index: Int,
    ) {
        if (index !in 0 until controller.mediaItemCount) {
            return
        }

        controller.seekToDefaultPosition(
            index,
        )
        controller.play()
    }
}

// [VXG RealForever]
