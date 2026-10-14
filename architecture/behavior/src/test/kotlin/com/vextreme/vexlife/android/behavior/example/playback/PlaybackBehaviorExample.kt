package com.vextreme.vexlife.android.behavior.example.playback

import com.vextreme.vexlife.android.behavior.VexBehaviorBlueprint
import com.vextreme.vexlife.android.behavior.vexBehaviorBlueprint

/**
 * Playback speed is State flavor, not another architecture layer.
 *
 * The current practicum uses three intentionally coarse transport speeds. A real provider may use a
 * richer numeric representation without changing the State / Act / Effect / Control grammar.
 */
enum class PlaybackSpeed {
    Rewind,
    Normal,
    FastForward,
}

/**
 * Current Playback truth.
 *
 * Paused and Playing have different behavioral neighborhoods, so they earn different State kinds.
 * Fast-forward/rewind remain flavor inside Playing because "playing, but at another speed" can be
 * represented without inventing another architecture category.
 */
sealed interface PlaybackState {
    data object Paused :
        PlaybackState

    data class Playing(
        val speed: PlaybackSpeed,
    ) : PlaybackState
}

/**
 * Human semantic verbs admitted by Playback.
 */
sealed interface PlaybackAct {
    data object Play :
        PlaybackAct

    data object Pause :
        PlaybackAct

    data object FastForward :
        PlaybackAct

    data object Rewind :
        PlaybackAct
}

/**
 * Runtime verbs produced by lawful Playback Acts.
 *
 * Effect is NOT resulting truth. It is the attempted transformation between observed States.
 */
sealed interface PlaybackEffect {
    data class SetSpeed(
        val speed: PlaybackSpeed,
    ) : PlaybackEffect

    data object PausePlayback :
        PlaybackEffect
}

/**
 * Worked State -> Act -> Effect -> observed-State blueprint.
 *
 * A relationship names the expected destination State KIND only. The Effect carries the attempted
 * data change; the runtime/provider remains responsible for what State is actually observed next.
 */
fun playbackBehaviorBlueprint():
    VexBehaviorBlueprint<
        PlaybackState,
        PlaybackAct,
        PlaybackEffect
    > =
    vexBehaviorBlueprint(
        behaviorRef = "example.playback.transport",
    ) {
        state<PlaybackState.Paused> {
            on(
                PlaybackAct.Play,
                PlaybackEffect.SetSpeed(
                    PlaybackSpeed.Normal,
                ),
            ).to<PlaybackState.Playing>()

            on(
                PlaybackAct.FastForward,
                PlaybackEffect.SetSpeed(
                    PlaybackSpeed.FastForward,
                ),
            ).to<PlaybackState.Playing>()

            on(
                PlaybackAct.Rewind,
                PlaybackEffect.SetSpeed(
                    PlaybackSpeed.Rewind,
                ),
            ).to<PlaybackState.Playing>()
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
            ).to<PlaybackState.Paused>()

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
