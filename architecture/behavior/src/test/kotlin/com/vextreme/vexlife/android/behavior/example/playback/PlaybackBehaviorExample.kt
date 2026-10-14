package com.vextreme.vexlife.android.behavior.example.playback

import com.vextreme.vexlife.android.behavior.VexBehaviorBlueprint
import com.vextreme.vexlife.android.behavior.vexBehaviorBlueprint

/**
 * Current transport truth.
 *
 * These are deliberately semantic transport modes, not button labels. FastForwarding and Rewinding
 * are real current modes in this practicum rather than hidden history attached to Playing.
 */
sealed interface PlaybackState

data object Paused :
    PlaybackState

data object Playing :
    PlaybackState

data object FastForwarding :
    PlaybackState

data object Rewinding :
    PlaybackState

/**
 * Human/semantic acts that may be meaningful from a current PlaybackState.
 *
 * A physical Play/Pause button may choose Play or Pause from the current state. That control mapping
 * is outside this behavior graph; the graph owns what each Act means once selected.
 */
sealed interface PlaybackAct

data object Play :
    PlaybackAct

data object Pause :
    PlaybackAct

data object FastForward :
    PlaybackAct

data object Rewind :
    PlaybackAct

/**
 * Provider-facing meanings produced by lawful Acts.
 *
 * Effect is kept separate from Act so the runtime implementation can change without changing the
 * human behavioral vocabulary.
 */
sealed interface PlaybackEffect

data object PlayAtNormalSpeed :
    PlaybackEffect

data object PausePlayback :
    PlaybackEffect

data object StartFastForward :
    PlaybackEffect

data object StartRewind :
    PlaybackEffect

/**
 * Worked cause -> effect -> new cause blueprint.
 *
 * Read each state block as:
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
        state<Playing> {
            on(
                Pause,
                PausePlayback,
            ).to<Paused>()

            on(
                FastForward,
                StartFastForward,
            ).to<FastForwarding>()

            on(
                Rewind,
                StartRewind,
            ).to<Rewinding>()
        }

        state<Paused> {
            on(
                Play,
                PlayAtNormalSpeed,
            ).to<Playing>()

            on(
                FastForward,
                StartFastForward,
            ).to<FastForwarding>()

            on(
                Rewind,
                StartRewind,
            ).to<Rewinding>()
        }

        state<FastForwarding> {
            on(
                Play,
                PlayAtNormalSpeed,
            ).to<Playing>()

            on(
                Pause,
                PausePlayback,
            ).to<Paused>()

            on(
                Rewind,
                StartRewind,
            ).to<Rewinding>()
        }

        state<Rewinding> {
            on(
                Play,
                PlayAtNormalSpeed,
            ).to<Playing>()

            on(
                Pause,
                PausePlayback,
            ).to<Paused>()

            on(
                FastForward,
                StartFastForward,
            ).to<FastForwarding>()
        }
    }

// [VXG RealForever]
