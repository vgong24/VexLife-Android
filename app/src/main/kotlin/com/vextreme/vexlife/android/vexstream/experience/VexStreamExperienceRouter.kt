package com.vextreme.vexlife.android.vexstream.experience

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.session.MediaController
import com.vextreme.vexlife.android.vexstream.experience.v2.player.playback.VexStreamPlayback
import com.vextreme.vexlife.android.vexstream.experience.v2.player.provider.media3.Media3VexStreamPlayerProvider
import com.vextreme.vexlife.android.vexstream.experience.v2.player.queue.VexStreamQueue

/**
 * Version choices intentionally visible only at the comparison boundary.
 *
 * Normal V2 classes remain production-shaped; the package and this debug router carry versioning.
 */
enum class VexStreamExperienceVersion {
    LEGACY,
    V2,
}

/**
 * Process-local router used to compare current VexStream behavior with the V2 experience.
 *
 * This is diagnostic routing, not VexStream domain truth or a persisted user preference. App restart
 * returns to [VexStreamExperienceVersion.LEGACY].
 */
object VexStreamExperienceRouter {
    /** Currently selected comparison version. */
    var selectedVersion: VexStreamExperienceVersion by
        mutableStateOf(
            VexStreamExperienceVersion.LEGACY,
        )
        private set

    /** Convenience read used by Debug Menu presentation. */
    val usesV2: Boolean
        get() =
            selectedVersion ==
                VexStreamExperienceVersion.V2

    /** Switch between the two diagnostic routes. */
    fun toggleVersion() {
        selectedVersion =
            if (usesV2) {
                VexStreamExperienceVersion.LEGACY
            } else {
                VexStreamExperienceVersion.V2
            }
    }

    /** Compare Play/Pause through the same real MediaController. */
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

        return VexStreamPlayback
            .android
            .request(
                control =
                    VexStreamPlayback
                        .controls
                        .PlayPause,
                provider =
                    Media3VexStreamPlayerProvider(
                        controller,
                    ),
            )
    }

    /** Compare Previous through the same real MediaController. */
    fun requestPrevious(
        controller: MediaController,
    ): Boolean {
        if (!usesV2) {
            controller.seekToPreviousMediaItem()
            return true
        }

        return VexStreamQueue
            .android
            .request(
                control =
                    VexStreamQueue
                        .controls
                        .PreviousControl,
                provider =
                    Media3VexStreamPlayerProvider(
                        controller,
                    ),
            )
    }

    /** Compare Next through the same real MediaController. */
    fun requestNext(
        controller: MediaController,
    ): Boolean {
        if (!usesV2) {
            controller.seekToNextMediaItem()
            return true
        }

        return VexStreamQueue
            .android
            .request(
                control =
                    VexStreamQueue
                        .controls
                        .NextControl,
                provider =
                    Media3VexStreamPlayerProvider(
                        controller,
                    ),
            )
    }

    /** Compare queue-row selection through the same real MediaController. */
    fun requestQueueSelection(
        controller: MediaController,
        index: Int,
    ): Boolean {
        if (!usesV2) {
            if (index !in 0 until controller.mediaItemCount) {
                return false
            }

            controller.seekToDefaultPosition(
                index,
            )
            controller.play()
            return true
        }

        return VexStreamQueue
            .android
            .request(
                control =
                    VexStreamQueue
                        .controls
                        .PlaySelectedControl,
                provider =
                    Media3VexStreamPlayerProvider(
                        controller,
                    ),
                index = index,
            )
    }
}

// [VXG RealForever]
