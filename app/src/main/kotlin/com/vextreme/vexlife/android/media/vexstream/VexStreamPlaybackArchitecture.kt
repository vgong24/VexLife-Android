package com.vextreme.vexlife.android.media.vexstream

import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.vextreme.vexlife.android.vexarchitecture.VexArchitectureContract
import com.vextreme.vexlife.android.vexarchitecture.VexArchitectureMap
import com.vextreme.vexlife.android.vexarchitecture.VexControl
import com.vextreme.vexlife.android.vexarchitecture.VexCurrent
import com.vextreme.vexlife.android.vexarchitecture.VexPlacementView
import com.vextreme.vexlife.android.vexarchitecture.VexTransition
import com.vextreme.vexlife.android.vexarchitecture.architectureMap
import com.vextreme.vexlife.android.vexarchitecture.control
import com.vextreme.vexlife.android.vexarchitecture.placement
import com.vextreme.vexlife.android.vexarchitecture.transition
import com.vextreme.vexlife.android.vexarchitecture.vexCurrent

/**
 * ROOT DESIGN for VexStream playback.
 *
 * Start here when the symptom is "something about playback is wrong" and you do not yet know which
 * UI/service file matters.
 *
 * The root intentionally owns orientation, realm gating and the derived map only. It is not a
 * playback manager.
 */
object VexStreamPlaybackArchitecture :
    VexArchitectureContract {

    override val architectureRef: String =
        "vexstream.playback"

    /**
     * Feature-realm gate.
     *
     * Replacing this constant with a real product flag later turns every Control below this root off
     * through the shared contract; descendants must not scatter their own flag checks.
     */
    override val enabled: Boolean =
        true

    /** Feature-local child contracts. Only playback-owned layers implement these. */
    interface CaseContract
    interface ControlContract
    interface PlacementContract
    interface AndroidContract

    val controls: VexStreamPlaybackControl
        get() = VexStreamPlaybackControl

    val placements: VexStreamPlaybackPlacement
        get() = VexStreamPlaybackPlacement

    internal val android: VexStreamPlaybackAndroid
        get() = VexStreamPlaybackAndroid

    override val map: VexArchitectureMap by lazy {
        architectureMap(
            placements =
                placements.all,
            controls =
                controls.all,
        )
    }
}

/** Current relational playback truth. These names describe reality, not UI controls. */
sealed interface VexStreamPlaybackCase :
    VexStreamPlaybackArchitecture.CaseContract {

    data object QueueEmpty :
        VexStreamPlaybackCase

    data object ReadyToPlay :
        VexStreamPlaybackCase

    /**
     * Media3 is currently pursuing playback, including prepare/buffer time before audio is audible.
     */
    data object PlayRequested :
        VexStreamPlaybackCase
}

/**
 * Controls and their typed relationships.
 *
 *     Play  : ReadyToPlay -> PlayRequested
 *     Pause : PlayRequested -> ReadyToPlay
 *     PlayPause : whichever one current Placement offers
 */
object VexStreamPlaybackControl :
    VexStreamPlaybackArchitecture.ControlContract {

    val Play =
        VexStreamPlaybackArchitecture.transition<
            VexStreamPlaybackCase.ReadyToPlay,
            VexStreamPlaybackCase.PlayRequested
        >(
            "vexstream.playback.play",
        )

    val Pause =
        VexStreamPlaybackArchitecture.transition<
            VexStreamPlaybackCase.PlayRequested,
            VexStreamPlaybackCase.ReadyToPlay
        >(
            "vexstream.playback.pause",
        )

    val PlayPause =
        VexStreamPlaybackArchitecture.control(
            controlRef =
                "vexstream.playback.play-pause",
            Play,
            Pause,
        )

    val all: List<VexControl> =
        listOf(
            PlayPause,
        )
}

/**
 * Local relational surroundings for each playback Case.
 */
object VexStreamPlaybackPlacement :
    VexStreamPlaybackArchitecture.PlacementContract {

    val QueueEmpty =
        VexStreamPlaybackArchitecture.placement<
            VexStreamPlaybackCase.QueueEmpty
        > {}

    val ReadyToPlay =
        VexStreamPlaybackArchitecture.placement<
            VexStreamPlaybackCase.ReadyToPlay
        > {
            arrivedBy(
                VexStreamPlaybackControl.Pause,
            )
            can(
                VexStreamPlaybackControl.Play,
            )
        }

    val PlayRequested =
        VexStreamPlaybackArchitecture.placement<
            VexStreamPlaybackCase.PlayRequested
        > {
            arrivedBy(
                VexStreamPlaybackControl.Play,
            )
            can(
                VexStreamPlaybackControl.Pause,
            )
        }

    val all: List<VexPlacementView> =
        listOf(
            QueueEmpty,
            ReadyToPlay,
            PlayRequested,
        )
}

/** Platform facts needed to answer exactly: "What playback Case is current?" */
data class VexStreamPlaybackObservation(
    val queueHasItems: Boolean,
    val playRequested: Boolean,
)

/**
 * Android observation/effect boundary for the playback architecture.
 *
 * UI surfaces consume Cases/Controls. Android effects are performed here after exact Media3 truth is
 * re-read.
 */
internal object VexStreamPlaybackAndroid :
    VexStreamPlaybackArchitecture.AndroidContract {

    fun current(
        observation: VexStreamPlaybackObservation,
    ): VexCurrent<out VexStreamPlaybackCase> =
        when {
            !observation.queueHasItems ->
                vexCurrent(
                    case =
                        VexStreamPlaybackCase
                            .QueueEmpty,
                    placement =
                        VexStreamPlaybackPlacement
                            .QueueEmpty,
                )

            observation.playRequested ->
                vexCurrent(
                    case =
                        VexStreamPlaybackCase
                            .PlayRequested,
                    placement =
                        VexStreamPlaybackPlacement
                            .PlayRequested,
                )

            else ->
                vexCurrent(
                    case =
                        VexStreamPlaybackCase
                            .ReadyToPlay,
                    placement =
                        VexStreamPlaybackPlacement
                            .ReadyToPlay,
                )
        }

    fun current(
        player: Player,
    ): VexCurrent<out VexStreamPlaybackCase> =
        current(
            observation =
                VexStreamPlaybackObservation(
                    queueHasItems =
                        player.mediaItemCount > 0,
                    playRequested =
                        player.playWhenReady,
                ),
        )

    /** Perform one Control against exact current platform truth. */
    fun request(
        control: VexControl,
        controller: MediaController,
    ): Boolean {
        val transition =
            current(
                controller,
            ).availableTransition(
                control,
            )
                ?: return false

        return realize(
            controller = controller,
            transition = transition,
        )
    }

    private fun realize(
        controller: MediaController,
        transition: VexTransition<*, *>,
    ): Boolean =
        when (transition) {
            VexStreamPlaybackControl.Play -> {
                controller.play()
                true
            }

            VexStreamPlaybackControl.Pause -> {
                controller.pause()
                true
            }

            else ->
                false
        }
}

// [VXG RealForever]
