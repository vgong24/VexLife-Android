package com.vextreme.vexlife.android.media.vexstream

import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.vextreme.vexlife.android.vexarchitecture.VexCurrent
import com.vextreme.vexlife.android.vexarchitecture.VexTransition
import com.vextreme.vexlife.android.vexarchitecture.vexCurrent
import com.vextreme.vexlife.android.vexarchitecture.vexPlacement
import com.vextreme.vexlife.android.vexarchitecture.vexTransition
import com.vextreme.vexlife.android.vexarchitecture.vexTransitionSlot

/**
 * The concrete Play/Pause-control condition currently observed from Media3.
 *
 * These names describe what is true, not what button should be drawn:
 *
 * - [QueueEmpty]      -> no queued media exists, so Play/Pause has no meaningful transition;
 * - [ReadyToPlay]     -> media exists and playback is not currently requested;
 * - [PlayRequested]   -> Media3 playWhenReady is true, including prepare/buffer time before audio.
 */
sealed interface VexStreamPlaybackControlCase {
    data object QueueEmpty :
        VexStreamPlaybackControlCase

    data object ReadyToPlay :
        VexStreamPlaybackControlCase

    data object PlayRequested :
        VexStreamPlaybackControlCase
}

/**
 * Platform facts needed to answer exactly one question:
 *
 *     "What does the VexStream Play/Pause control mean right now?"
 *
 * Media3 names stay at this boundary. The relational architecture below reasons only from these
 * explicit observed facts.
 */
data class VexStreamPlaybackObservation(
    val queueHasItems: Boolean,
    val playRequested: Boolean,
)

/** Named intention accepted by the single playback-control effect owner. */
sealed interface VexStreamPlaybackControlAction {
    data object TogglePlayPause :
        VexStreamPlaybackControlAction
}

/**
 * Human-readable VexStream Play/Pause relationship.
 *
 * Read top-to-bottom as English:
 *
 *     ReadyToPlay --Play--> PlayRequested
 *     PlayRequested --Pause--> ReadyToPlay
 *
 * The When... values answer what relationships exist while one Case is current. They do not perform
 * Media3 commands.
 */
object VexStreamPlaybackArchitecture {
    /** Human requests playback from a state where media is ready but playback is not requested. */
    val Play =
        vexTransition<
            VexStreamPlaybackControlCase.ReadyToPlay,
            VexStreamPlaybackControlCase.PlayRequested
        >(
            "vexstream.playback.play",
        )

    /** Human requests playback to stop being pursued while playback is currently requested. */
    val Pause =
        vexTransition<
            VexStreamPlaybackControlCase.PlayRequested,
            VexStreamPlaybackControlCase.ReadyToPlay
        >(
            "vexstream.playback.pause",
        )

    /** No queue means the Play/Pause control currently has no legal transition. */
    val WhenQueueIsEmpty =
        vexPlacement<
            VexStreamPlaybackControlCase.QueueEmpty
        > {}

    /** Media exists and Play is the one legal Play/Pause transition. */
    val WhenReadyToPlay =
        vexPlacement<
            VexStreamPlaybackControlCase.ReadyToPlay
        > {
            arrivedBy(Pause)
            can(Play)
        }

    /** Playback is requested and Pause is the one legal Play/Pause transition. */
    val WhenPlayIsRequested =
        vexPlacement<
            VexStreamPlaybackControlCase.PlayRequested
        > {
            arrivedBy(Play)
            can(Pause)
        }

    /** One physical/semantic Play-Pause control whose meaning depends on exact current Case. */
    val PlayPauseControl =
        vexTransitionSlot(
            slotRef = "vexstream.playback.play-pause",
            Play,
            Pause,
        )

    /**
     * Translate one explicit platform observation into current relational meaning.
     *
     * This is pure classification:
     *
     *     observation -> current Case + its lawful relationships
     *
     * It does not execute playback and does not own Media3 state.
     */
    fun currentControl(
        observation: VexStreamPlaybackObservation,
    ): VexCurrent<out VexStreamPlaybackControlCase> =
        when {
            !observation.queueHasItems ->
                vexCurrent(
                    case =
                        VexStreamPlaybackControlCase
                            .QueueEmpty,
                    placement = WhenQueueIsEmpty,
                )

            observation.playRequested ->
                vexCurrent(
                    case =
                        VexStreamPlaybackControlCase
                            .PlayRequested,
                    placement = WhenPlayIsRequested,
                )

            else ->
                vexCurrent(
                    case =
                        VexStreamPlaybackControlCase
                            .ReadyToPlay,
                    placement = WhenReadyToPlay,
                )
        }
}

/**
 * Translate Media3 names exactly once at the platform boundary.
 *
 * UI/domain callers should not reinterpret playWhenReady or queue count themselves.
 */
internal fun Player.vexStreamPlaybackObservation():
    VexStreamPlaybackObservation =
    VexStreamPlaybackObservation(
        queueHasItems =
            mediaItemCount > 0,
        playRequested =
            playWhenReady,
    )

/**
 * One effect owner for VexStream Play/Pause control intentions.
 *
 * UI surfaces may render from VexArchitecture current meaning, but execution always travels here:
 *
 *     TogglePlayPause
 *     -> re-read exact Media3 facts now
 *     -> currentControl(...)
 *     -> available Play/Pause transition
 *     -> realize that transition through MediaController
 *
 * Re-reading current Media3 truth here prevents a stale UI snapshot from becoming execution
 * authority.
 */
internal object VexStreamPlaybackControlOwner {
    fun request(
        action: VexStreamPlaybackControlAction,
        controller: MediaController,
    ): Boolean =
        when (action) {
            VexStreamPlaybackControlAction.TogglePlayPause -> {
                val currentControl =
                    VexStreamPlaybackArchitecture.currentControl(
                        controller.vexStreamPlaybackObservation(),
                    )

                val availableTransition =
                    currentControl.availableTransition(
                        VexStreamPlaybackArchitecture.PlayPauseControl,
                    )
                        ?: return false

                controller.realizeVexStreamPlaybackTransition(
                    availableTransition,
                )
            }
        }
}

/**
 * Android realization of one already-selected semantic playback relationship.
 *
 * This function deliberately does not choose Play versus Pause. Selection belongs to
 * [VexStreamPlaybackControlOwner], which resolves against exact current playback truth first.
 */
private fun MediaController.realizeVexStreamPlaybackTransition(
    transition: VexTransition<*, *>,
): Boolean =
    when (transition) {
        VexStreamPlaybackArchitecture.Play -> {
            play()
            true
        }

        VexStreamPlaybackArchitecture.Pause -> {
            pause()
            true
        }

        else -> false
    }

// [VXG RealForever]
