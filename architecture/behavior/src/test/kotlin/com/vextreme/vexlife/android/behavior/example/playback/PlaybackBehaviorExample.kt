package com.vextreme.vexlife.android.behavior.example.playback

import com.vextreme.vexlife.android.behavior.VexBehaviorBlueprint
import com.vextreme.vexlife.android.behavior.vexBehaviorBlueprint

/**
 * Current transport truth.
 *
 * The finite semantic family is nested so IDE completion keeps Playback State possibilities inside
 * their own scope instead of leaking Playing/Paused/etc into unrelated domains.
 */
sealed interface PlaybackState {
    data object Paused :
        PlaybackState

    data object Playing :
        PlaybackState

    data object FastForwarding :
        PlaybackState

    data object Rewinding :
        PlaybackState
}

/**
 * Human/semantic causes admitted by Playback.
 *
 * These are nested under PlaybackAct so a developer sees the bounded family while typing and cannot
 * accidentally substitute another domain's Act type into this Behavior.
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
 * Provider-facing meanings produced by lawful Playback Acts.
 *
 * Effect remains distinct from Act so product vocabulary is not coupled to one provider API.
 */
sealed interface PlaybackEffect {
    data object PlayAtNormalSpeed :
        PlaybackEffect

    data object PausePlayback :
        PlaybackEffect

    data object StartFastForward :
        PlaybackEffect

    data object StartRewind :
        PlaybackEffect
}

/**
 * Worked cause -> effect -> new cause blueprint.
 *
 * Read each State block as:
 *
 * "Given THIS is true now, which Acts have meaning from here, what Effect do they request, and what
 * State should be expected if that Effect converges?"
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
        state<PlaybackState.Playing> {
            on(
                PlaybackAct.Pause,
                PlaybackEffect.PausePlayback,
            ).to<PlaybackState.Paused>()

            on(
                PlaybackAct.FastForward,
                PlaybackEffect.StartFastForward,
            ).to<PlaybackState.FastForwarding>()

            on(
                PlaybackAct.Rewind,
                PlaybackEffect.StartRewind,
            ).to<PlaybackState.Rewinding>()
        }

        state<PlaybackState.Paused> {
            on(
                PlaybackAct.Play,
                PlaybackEffect.PlayAtNormalSpeed,
            ).to<PlaybackState.Playing>()

            on(
                PlaybackAct.FastForward,
                PlaybackEffect.StartFastForward,
            ).to<PlaybackState.FastForwarding>()

            on(
                PlaybackAct.Rewind,
                PlaybackEffect.StartRewind,
            ).to<PlaybackState.Rewinding>()
        }

        state<PlaybackState.FastForwarding> {
            on(
                PlaybackAct.Play,
                PlaybackEffect.PlayAtNormalSpeed,
            ).to<PlaybackState.Playing>()

            on(
                PlaybackAct.Pause,
                PlaybackEffect.PausePlayback,
            ).to<PlaybackState.Paused>()

            on(
                PlaybackAct.Rewind,
                PlaybackEffect.StartRewind,
            ).to<PlaybackState.Rewinding>()
        }

        state<PlaybackState.Rewinding> {
            on(
                PlaybackAct.Play,
                PlaybackEffect.PlayAtNormalSpeed,
            ).to<PlaybackState.Playing>()

            on(
                PlaybackAct.Pause,
                PlaybackEffect.PausePlayback,
            ).to<PlaybackState.Paused>()

            on(
                PlaybackAct.FastForward,
                PlaybackEffect.StartFastForward,
            ).to<PlaybackState.FastForwarding>()
        }
    }

// [VXG RealForever]
