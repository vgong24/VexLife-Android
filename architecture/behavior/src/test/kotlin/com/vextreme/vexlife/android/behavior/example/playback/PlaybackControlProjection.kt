package com.vextreme.vexlife.android.behavior.example.playback

import com.vextreme.vexlife.android.behavior.VexBehaviorBlueprint
import com.vextreme.vexlife.android.behavior.VexBehaviorControl
import com.vextreme.vexlife.android.behavior.vexBehaviorControl

/**
 * Constructed Playback controls.
 *
 * These are not global singletons. Each Control describes which semantic Acts one human affordance
 * can represent; the Behavior graph decides which of those Acts are available from current State.
 */
class PlaybackControls {
    val rewind:
        VexBehaviorControl<PlaybackAct> =
        vexBehaviorControl(
            "example.playback.control.rewind",
            Rewind,
        )

    /**
     * Play wins only when Play AND Pause are simultaneously meaningful.
     *
     * That occurs in FastForwarding/Rewinding in the current practicum. From Playing only Pause is
     * available; from Paused only Play is available.
     */
    val playPause:
        VexBehaviorControl<PlaybackAct> =
        vexBehaviorControl(
            "example.playback.control.play-pause",
            Play,
            Pause,
        )

    val fastForward:
        VexBehaviorControl<PlaybackAct> =
        vexBehaviorControl(
            "example.playback.control.fast-forward",
            FastForward,
        )

    fun project(
        blueprint:
            VexBehaviorBlueprint<
                PlaybackState,
                PlaybackAct,
                PlaybackEffect
            >,
        current: PlaybackState,
    ): PlaybackControlProjection =
        PlaybackControlProjection(
            rewind =
                rewind.resolve(
                    blueprint = blueprint,
                    current = current,
                )?.act,
            playPause =
                playPause.resolve(
                    blueprint = blueprint,
                    current = current,
                )?.act,
            fastForward =
                fastForward.resolve(
                    blueprint = blueprint,
                    current = current,
                )?.act,
        )
}

/**
 * UI-facing semantic projection.
 *
 * Null means the Control currently has no Act to represent. A renderer may hide or disable it, but
 * that visual choice is intentionally not authored into the Behavior relationship.
 */
data class PlaybackControlProjection(
    val rewind: PlaybackAct?,
    val playPause: PlaybackAct?,
    val fastForward: PlaybackAct?,
)
