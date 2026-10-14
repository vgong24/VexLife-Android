package com.vextreme.vexlife.android.vexstream.architecture.v2.player

import com.vextreme.vexlife.android.vexarchitecture.VexArchitectureContract
import com.vextreme.vexlife.android.vexstream.architecture.v2.VexStreamMusicArchitecture
import com.vextreme.vexlife.android.vexstream.architecture.v2.player.playback.VexStreamPlaybackArchitecture
import com.vextreme.vexlife.android.vexstream.architecture.v2.player.queue.VexStreamQueueArchitecture

/**
 * Human-facing Player composition root.
 *
 * "Player" is used here because a person naturally asks "what is the player doing?" while the
 * architecture underneath separates the independent truths that answer that question.
 *
 * Player intentionally owns no PlayerCase. Combining Playback x Queue into one Case family would
 * produce names such as PlayingAtQueueStart and PausedAtQueueEnd and recreate combinatorial state.
 */
object VexStreamPlayerArchitecture :
    VexArchitectureContract {

    /** Absolute semantic coordinate for the Player composition. */
    override val architectureRef: String =
        "vexstream.music.player"

    /** Player belongs to the Music domain. */
    override val parent: VexArchitectureContract =
        VexStreamMusicArchitecture

    /** No independent Player feature flag has been earned; children inherit Music participation. */
    override val enabled: Boolean =
        true

    /**
     * Playback answers "is playback requested for the current media?".
     *
     * It deliberately does not answer queue-position questions.
     */
    val playback: VexArchitectureContract
        get() =
            VexStreamPlaybackArchitecture

    /**
     * Queue answers "what is the listening order and where am I in it?".
     *
     * It deliberately does not answer whether the current media should be playing.
     */
    val queue: VexArchitectureContract
        get() =
            VexStreamQueueArchitecture

    /** Immediate directory children exposed to a fresh developer entering at Player. */
    override val children: List<VexArchitectureContract>
        get() =
            listOf(
                playback,
                queue,
            )
}

/**
 * Platform-neutral facts Playback needs from the actual player.
 *
 * The name Snapshot is deliberate: this is an observation at one instant, not another retained truth
 * owner beside MediaSession/Media3.
 */
data class VexStreamPlayerPlaybackSnapshot(
    /** True when the real player currently contains at least one media item. */
    val hasMedia: Boolean,
    /** True when the real player is currently requesting playback, even while buffering/preparing. */
    val playbackRequested: Boolean,
)

/**
 * Platform-neutral facts Queue needs from the actual player.
 *
 * These fields are observed rather than re-derived from index arithmetic because the actual player
 * remains authoritative for traversal semantics.
 */
data class VexStreamPlayerQueueSnapshot(
    /** Number of media items currently owned by the real player queue. */
    val itemCount: Int,
    /** Current player queue index, or -1 when the player reports no current item. */
    val currentIndex: Int,
    /** Player-owned answer to whether Previous is currently meaningful. */
    val hasPrevious: Boolean,
    /** Player-owned answer to whether Next is currently meaningful. */
    val hasNext: Boolean,
)

/**
 * Product-level boundary for "the thing that can actually play VexStream music".
 *
 * VexArchitecture stops here. The word Player is clearer than continuing to invent architecture
 * wrappers around MediaController/MediaSession/ExoPlayer.
 *
 * The port contains only the behavior needed by the first V2 comparison slice. Queue mutation,
 * Play Song, Play All and offline/streaming resolution remain explicit next questions rather than a
 * speculative giant player API.
 */
interface VexStreamPlayer {
    /** Observe current playback request truth without changing it. */
    fun observePlayback(): VexStreamPlayerPlaybackSnapshot

    /** Observe current queue-position truth without changing it. */
    fun observeQueue(): VexStreamPlayerQueueSnapshot

    /** Request playback of the already-current media. */
    fun play()

    /** Clear the playback request while preserving the current media/queue. */
    fun pause()

    /** Move to the real player's next media item. */
    fun next()

    /** Move to the real player's previous media item. */
    fun previous()

    /**
     * Select one existing queue index and request playback.
     *
     * This is the first cross-concern operation intentionally kept at the real player boundary:
     * Queue selects the item and Playback remains independently observable afterward.
     */
    fun playQueueIndex(
        index: Int,
    )
}

// [VXG RealForever]
