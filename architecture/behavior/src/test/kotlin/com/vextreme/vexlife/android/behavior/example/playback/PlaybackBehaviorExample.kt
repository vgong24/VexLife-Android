package com.vextreme.vexlife.android.behavior.example.playback

import com.vextreme.vexlife.android.behavior.VexBehaviorBlueprint
import com.vextreme.vexlife.android.behavior.VexBehaviorFamily
import com.vextreme.vexlife.android.behavior.vexBehaviorBlueprint

sealed interface PlaybackFamily :
    VexBehaviorFamily<PlaybackFamily>

enum class PlaybackSpeed {
    Rewind,
    Normal,
    FastForward,
}

sealed interface PlaybackState :
    VexBehaviorFamily.State<
        PlaybackFamily
    > {
    data object Paused :
        PlaybackState

    data class Playing(
        val speed: PlaybackSpeed,
    ) : PlaybackState
}

sealed interface PlaybackAct :
    VexBehaviorFamily.Act<
        PlaybackFamily
    > {
    data object Play :
        PlaybackAct

    data object Pause :
        PlaybackAct

    data object FastForward :
        PlaybackAct

    data object Rewind :
        PlaybackAct
}

sealed interface PlaybackEffect :
    VexBehaviorFamily.Effect<
        PlaybackFamily
    > {
    data class SetSpeed(
        val speed: PlaybackSpeed,
    ) : PlaybackEffect

    data object PausePlayback :
        PlaybackEffect
}

fun playbackBehaviorBlueprint():
    VexBehaviorBlueprint<
        PlaybackFamily,
        PlaybackState,
        PlaybackAct,
        PlaybackEffect
    > =
    vexBehaviorBlueprint(
        behaviorRef =
            "example.playback.transport",
    ) {
        state<PlaybackState.Paused> {
            on(
                PlaybackAct.Play,
                PlaybackEffect.SetSpeed(
                    PlaybackSpeed.Normal,
                ),
            ).admit<PlaybackState.Playing>()

            on(
                PlaybackAct.FastForward,
                PlaybackEffect.SetSpeed(
                    PlaybackSpeed.FastForward,
                ),
            ).admit<PlaybackState.Playing>()

            on(
                PlaybackAct.Rewind,
                PlaybackEffect.SetSpeed(
                    PlaybackSpeed.Rewind,
                ),
            ).admit<PlaybackState.Playing>()
        }

        state<PlaybackState.Playing> {
            on(
                act = PlaybackAct.Play,
                effect =
                    PlaybackEffect.SetSpeed(
                        PlaybackSpeed.Normal,
                    ),
                conditionName =
                    "not-normal-speed",
            ) { current ->
                current.speed !=
                    PlaybackSpeed.Normal
            }.stay()

            on(
                PlaybackAct.Pause,
                PlaybackEffect.PausePlayback,
            ).admit<PlaybackState.Paused>()

            on(
                act = PlaybackAct.FastForward,
                effect =
                    PlaybackEffect.SetSpeed(
                        PlaybackSpeed.FastForward,
                    ),
                conditionName =
                    "not-fast-forward-speed",
            ) { current ->
                current.speed !=
                    PlaybackSpeed.FastForward
            }.stay()

            on(
                act = PlaybackAct.Rewind,
                effect =
                    PlaybackEffect.SetSpeed(
                        PlaybackSpeed.Rewind,
                    ),
                conditionName =
                    "not-rewind-speed",
            ) { current ->
                current.speed !=
                    PlaybackSpeed.Rewind
            }.stay()
        }
    }

// [VXG RealForever]
