package com.vextreme.vexlife.android.vexstream.playback

import com.vextreme.vexlife.android.behavior.VexBehaviorBlueprint
import com.vextreme.vexlife.android.behavior.vexBehaviorBlueprint

internal fun vexStreamPlaybackBehavior():
    VexBehaviorBlueprint<
        VexStreamPlaybackFamily,
        VexStreamPlaybackState,
        VexStreamPlaybackAct,
        VexStreamPlaybackEffect
    > =
    vexBehaviorBlueprint(
        behaviorRef = "vexstream.playback",
    ) {
        state<VexStreamPlaybackState.NoMedia>()

        state<VexStreamPlaybackState.Media> {
            on(
                act = VexStreamPlaybackAct.Play,
                effect =
                    VexStreamPlaybackEffect.RequestPlay,
                conditionName =
                    "play-not-requested-and-control-available",
            ) { current ->
                !current.playRequested &&
                    current.playPauseAvailable
            }.stay()

            on(
                act = VexStreamPlaybackAct.Pause,
                effect =
                    VexStreamPlaybackEffect.RequestPause,
                conditionName =
                    "play-requested-and-control-available",
            ) { current ->
                current.playRequested &&
                    current.playPauseAvailable
            }.stay()
        }

        state<VexStreamPlaybackState.Ended> {
            on(
                act = VexStreamPlaybackAct.Play,
                effect =
                    VexStreamPlaybackEffect.RestartCurrent,
                conditionName =
                    "restart-available-and-not-pending",
            ) { current ->
                current.restartAvailable &&
                    !current.restartPending
            }.stay()
        }
    }

// [VXG RealForever]
