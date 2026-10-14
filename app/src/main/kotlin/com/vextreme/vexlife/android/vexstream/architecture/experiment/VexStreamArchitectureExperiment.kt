package com.vextreme.vexlife.android.vexstream.architecture.experiment

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.session.MediaController
import com.vextreme.vexlife.android.vexstream.architecture.v2.player.Media3VexStreamPlayer
import com.vextreme.vexlife.android.vexstream.architecture.v2.player.playback.VexStreamPlaybackArchitecture
import com.vextreme.vexlife.android.vexstream.architecture.v2.player.queue.VexStreamQueueArchitecture

/**
 * Two intentionally coexisting VexStream implementation experiences.
 *
 * LEGACY means the current accepted direct VexStream/Media3 interaction path on main.
 * V2 means the new semantic directory -> concern realm -> Android bridge -> same Media3 player.
 */
enum class VexStreamArchitectureExperience {
    LEGACY,
    V2,
}

/**
 * Process-local Debug Menu selector for the VexStream architecture experiment.
 *
 * This object is deliberately NOT repository/product truth, user preference, or persistent state.
 * Restarting the app returns to LEGACY so an experiment can never silently promote V2 to canonical
 * behavior merely because somebody toggled it during debugging.
 */
object VexStreamArchitectureExperiment {

    /**
     * Current diagnostic route.
     *
     * mutableStateOf is used only so Debug Menu/VexStream Compose surfaces immediately reflect a
     * selection change; it does not turn the experiment into a domain state owner.
     */
    var selected: VexStreamArchitectureExperience by
        mutableStateOf(
            VexStreamArchitectureExperience.LEGACY,
        )
        private set

    /** Readable predicate for presentation/debug code that only needs to know whether V2 is selected. */
    val usesV2: Boolean
        get() =
            selected ==
                VexStreamArchitectureExperience.V2

    /**
     * Toggle is the Debug Menu human action: compare the same feature in the other architecture.
     */
    fun toggle() {
        selected =
            if (usesV2) {
                VexStreamArchitectureExperience.LEGACY
            } else {
                VexStreamArchitectureExperience.V2
            }
    }

    /**
     * Compare Play/Pause while preserving the exact same MediaController owner.
     *
     * LEGACY intentionally retains the current main behavior based on isPlaying.
     * V2 re-reads playWhenReady through PlaybackRequested/PlaybackNotRequested before realization.
     */
    fun requestPlayPause(
        controller: MediaController,
    ): Boolean {
        if (!usesV2) {
            if (controller.isPlaying) {
                controller.pause()
            } else {
                controller.play()
            }
            return true
        }

        return VexStreamPlaybackArchitecture
            .android
            .request(
                control =
                    VexStreamPlaybackArchitecture
                        .controls
                        .PlayPause,
                player =
                    Media3VexStreamPlayer(
                        controller,
                    ),
            )
    }

    /**
     * Compare Previous while keeping the real Media3 queue as authority.
     *
     * V2 adds a fresh semantic availability check; LEGACY performs the current direct command.
     */
    fun requestPrevious(
        controller: MediaController,
    ): Boolean {
        if (!usesV2) {
            controller.seekToPreviousMediaItem()
            return true
        }

        return VexStreamQueueArchitecture
            .android
            .request(
                control =
                    VexStreamQueueArchitecture
                        .controls
                        .PreviousControl,
                player =
                    Media3VexStreamPlayer(
                        controller,
                    ),
            )
    }

    /**
     * Compare Next while keeping the real Media3 queue as authority.
     */
    fun requestNext(
        controller: MediaController,
    ): Boolean {
        if (!usesV2) {
            controller.seekToNextMediaItem()
            return true
        }

        return VexStreamQueueArchitecture
            .android
            .request(
                control =
                    VexStreamQueueArchitecture
                        .controls
                        .NextControl,
                player =
                    Media3VexStreamPlayer(
                        controller,
                    ),
            )
    }

    /**
     * Compare selecting a queue row.
     *
     * This is useful because it crosses Queue selection and Playback request while still ending at
     * the same real player. V2 keeps the Queue relationship explicit; Legacy keeps the current direct
     * seek + play behavior.
     */
    fun requestQueueSelection(
        controller: MediaController,
        index: Int,
    ): Boolean {
        if (!usesV2) {
            if (
                index !in
                0 until controller.mediaItemCount
            ) {
                return false
            }

            controller.seekToDefaultPosition(
                index,
            )
            controller.play()
            return true
        }

        return VexStreamQueueArchitecture
            .android
            .request(
                control =
                    VexStreamQueueArchitecture
                        .controls
                        .PlaySelectedControl,
                player =
                    Media3VexStreamPlayer(
                        controller,
                    ),
                index = index,
            )
    }
}

// [VXG RealForever]
