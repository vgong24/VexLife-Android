package com.vextreme.vexlife.android.vexarchitecture.mediaplayer

import com.vextreme.vexlife.android.vexarchitecture.VexArchitectureContract

/**
 * Reusable human vocabulary for architectures that behave like a modern media player.
 *
 * This object is a contract namespace, not a player instance and not a manager. A feature-specific
 * player architecture implements [Contract], then implements only the capability contracts it truly
 * supports. This avoids teaching that every music/video/live player must have the same feature set.
 */
object VexMediaPlayerArchitecture {
    /**
     * Marks one feature-specific media-player architecture.
     *
     * The contract deliberately does not require Queue, Timeline, Tracks, Output, or Presentation.
     * Those should become capability contracts only when a real consumer implements them.
     */
    interface Contract : VexArchitectureContract

    /**
     * Contract for the familiar Play/Pause concern of a media player.
     *
     * Implementations may use richer current Cases internally, but the word Playback should retain
     * the ordinary meaning "what is the current media being asked to do?".
     */
    interface PlaybackContract {
        /** Player architecture whose Playback capability this object belongs to. */
        val playerArchitecture: VexMediaPlayerArchitecture.Contract
    }

    /**
     * Contract for ordered media around the current item.
     *
     * Queue is optional: a single video or live stream can still be a media player without one.
     */
    interface QueueContract {
        /** Player architecture whose Queue capability this object belongs to. */
        val playerArchitecture: VexMediaPlayerArchitecture.Contract
    }
}

/**
 * Platform-neutral observation used by a Playback capability.
 *
 * "Snapshot" means one read from the real provider; it is not retained as a second truth owner.
 */
data class VexMediaPlaybackSnapshot(
    /** Whether the provider currently has media that could be played. */
    val hasMedia: Boolean,
    /**
     * Whether playback is requested.
     *
     * This wording intentionally allows buffering/preparing without falsely claiming sound/video is
     * already being rendered.
     */
    val playbackRequested: Boolean,
)

/** Platform-neutral observation used by a Queue capability. */
data class VexMediaQueueSnapshot(
    /** Number of media items currently owned by the provider queue. */
    val itemCount: Int,
    /** Current provider index, or -1 when no item is selected. */
    val currentIndex: Int,
    /** Provider-owned answer to whether Previous is meaningful now. */
    val hasPrevious: Boolean,
    /** Provider-owned answer to whether Next is meaningful now. */
    val hasNext: Boolean,
)

/**
 * Marker for an implementation that can serve as a Vex media-player provider.
 *
 * Provider means runtime realization. It is deliberately separate from Architecture Contract, which
 * describes meaning/source organization rather than a specific SDK implementation.
 */
interface VexMediaPlayerProvider

/**
 * Runtime capability a Vex Playback concern may depend on.
 *
 * A third-party SDK adapter can implement this without also claiming Queue support.
 */
interface VexMediaPlaybackProvider : VexMediaPlayerProvider {
    /** Observe actual provider playback truth. */
    fun observePlayback(): VexMediaPlaybackSnapshot

    /** Ask the provider to request playback. */
    fun play()

    /** Ask the provider to clear the playback request while retaining current media. */
    fun pause()
}

/**
 * Runtime capability a Vex Queue concern may depend on.
 *
 * Queue is separate from Playback so a provider can truthfully implement one without the other.
 */
interface VexMediaQueueProvider : VexMediaPlayerProvider {
    /** Observe actual provider queue-position truth. */
    fun observeQueue(): VexMediaQueueSnapshot

    /** Move to the provider's next media item. */
    fun next()

    /** Move to the provider's previous media item. */
    fun previous()

    /** Select one provider-owned queue index and request playback. */
    fun playQueueIndex(index: Int)
}

// [VXG RealForever]
