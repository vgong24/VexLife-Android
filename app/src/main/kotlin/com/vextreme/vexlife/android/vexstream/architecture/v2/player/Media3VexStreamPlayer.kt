package com.vextreme.vexlife.android.vexstream.architecture.v2.player

import androidx.media3.session.MediaController

/**
 * Media3 realization of the small VexStreamPlayer boundary.
 *
 * The class name deliberately says Media3: below VexStreamPlayer the platform library has the clearer
 * vocabulary, so VexArchitecture stops rather than wrapping MediaController/MediaSession/ExoPlayer in
 * more semantic architecture layers.
 */
class Media3VexStreamPlayer(
    /**
     * Existing MediaController client connected to the service-owned MediaSession.
     *
     * This adapter does not create/release the controller and therefore does not take playback
     * lifetime ownership away from the existing VexStreamPlaybackService/ViewModel boundaries.
     */
    private val controller: MediaController,
) : VexStreamPlayer {

    /**
     * Read only the facts the Playback realm needs.
     *
     * playWhenReady is translated later into PlaybackRequested/PlaybackNotRequested; Media3 naming is
     * intentionally preserved at this boundary instead of leaking upward.
     */
    override fun observePlayback(): VexStreamPlayerPlaybackSnapshot =
        VexStreamPlayerPlaybackSnapshot(
            hasMedia =
                controller.mediaItemCount > 0,
            playbackRequested =
                controller.playWhenReady,
        )

    /**
     * Read Queue facts from Media3 itself rather than reimplementing traversal rules in architecture.
     */
    override fun observeQueue(): VexStreamPlayerQueueSnapshot =
        VexStreamPlayerQueueSnapshot(
            itemCount =
                controller.mediaItemCount,
            currentIndex =
                controller.currentMediaItemIndex,
            hasPrevious =
                controller.hasPreviousMediaItem(),
            hasNext =
                controller.hasNextMediaItem(),
        )

    /** Realize semantic Play with the existing Media3 controller. */
    override fun play() {
        controller.play()
    }

    /** Realize semantic Pause with the existing Media3 controller. */
    override fun pause() {
        controller.pause()
    }

    /** Realize Queue.Next using Media3's own queue traversal operation. */
    override fun next() {
        controller.seekToNextMediaItem()
    }

    /** Realize Queue.Previous using Media3's own queue traversal operation. */
    override fun previous() {
        controller.seekToPreviousMediaItem()
    }

    /**
     * Realize Queue.PlaySelected without inventing another queue owner.
     *
     * Invalid indices are ignored here because the Player boundary is the final defensive owner of
     * its concrete queue. V2 Queue normally rejects unavailable semantic requests before this point.
     */
    override fun playQueueIndex(
        index: Int,
    ) {
        if (
            index !in
            0 until controller.mediaItemCount
        ) {
            return
        }

        controller.seekToDefaultPosition(
            index,
        )
        controller.play()
    }
}

// [VXG RealForever]
