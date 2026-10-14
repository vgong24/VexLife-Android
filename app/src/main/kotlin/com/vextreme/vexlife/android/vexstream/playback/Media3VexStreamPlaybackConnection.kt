package com.vextreme.vexlife.android.vexstream.playback

import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.vextreme.vexlife.android.behavior.VexBehaviorProjection
import com.vextreme.vexlife.android.behavior.VexPortFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Android Media3 realization of the reusable VexPortFlow contract.
 *
 * MediaSession/Player remain the actual playback truth owners. This connection is a retained semantic
 * projection + Effect adapter over one MediaController; it does not own player or queue lifetime.
 *
 * The connection also owns one small pending-intent shock absorber so admitted local Acts become visible
 * synchronously while delayed Media3 observations catch up.
 */
internal class Media3VexStreamPlaybackConnection(
    private val controller: MediaController,
) :
    VexPortFlow<
        VexStreamPlaybackState,
        VexStreamPlaybackControl,
        VexStreamPlaybackAct
    >,
    AutoCloseable {
    private val behavior =
        vexStreamPlaybackBehavior()

    private val controls =
        behavior.vexStreamPlaybackControls()

    private var pending =
        VexStreamPlaybackPending()

    private val mutableProjections =
        MutableStateFlow(
            controls.projection(
                currentSemanticState(
                    controller,
                ),
            ),
        )

    override val projections:
        StateFlow<
            VexBehaviorProjection<
                VexStreamPlaybackState,
                VexStreamPlaybackControl,
                VexStreamPlaybackAct
            >
        > =
        mutableProjections.asStateFlow()

    private val listener =
        object : Player.Listener {
            override fun onEvents(
                player: Player,
                events: Player.Events,
            ) {
                publishObserved(
                    player,
                )
            }
        }

    init {
        controller.addListener(
            listener,
        )
    }

    /**
     * Resolve from current semantic truth, emit admitted intent synchronously, then dispatch Effect.
     *
     * The UI never waits for Media3 callback latency to learn that the system has accepted a new
     * playback request. Pending State remains explicitly pending until provider observation agrees.
     */
    override fun activate(
        control: VexStreamPlaybackControl,
    ) {
        val current =
            currentSemanticState(
                controller,
            )

        publish(
            current,
        )

        val act =
            controls.resolve(
                current = current,
                control = control,
            )?.act
                ?: return

        val relationship =
            behavior.resolve(
                current = current,
                act = act,
            )
                ?: return

        val effect =
            relationship.effect

        /*
         * Synchronous intent emission:
         * 1. record newest pending semantic request;
         * 2. publish truthful pending State;
         * 3. only then dispatch to Media3.
         */
        pending =
            pending.after(
                effect,
            )
        publish(
            current.afterIntent(
                effect,
            ),
        )

        try {
            realize(
                effect,
            )
        } catch (failure: Throwable) {
            pending =
                VexStreamPlaybackPending()
            publishObserved(
                controller,
            )
            throw failure
        }

        /*
         * Immediate provider reread may already acknowledge the request. If not, reconciliation
         * preserves the pending semantic request until a later listener observation catches up.
         */
        publishObserved(
            controller,
        )
    }

    override fun close() {
        controller.removeListener(
            listener,
        )
    }

    private fun currentSemanticState(
        player: Player,
    ): VexStreamPlaybackState {
        val reconciled =
            reconcileVexStreamPlaybackObservation(
                observed =
                    player.toVexStreamPlaybackState(),
                pending = pending,
            )

        pending =
            reconciled.pending

        return reconciled.state
    }

    private fun publishObserved(
        player: Player,
    ) {
        publish(
            currentSemanticState(
                player,
            ),
        )
    }

    private fun publish(
        state: VexStreamPlaybackState,
    ) {
        val projection =
            controls.projection(
                state,
            )

        if (
            mutableProjections.value !=
            projection
        ) {
            mutableProjections.value =
                projection
        }
    }

    private fun realize(
        effect: VexStreamPlaybackEffect,
    ) {
        when (effect) {
            VexStreamPlaybackEffect.RequestPlay ->
                controller.play()

            VexStreamPlaybackEffect.RequestPause ->
                controller.pause()

            VexStreamPlaybackEffect.RestartCurrent -> {
                controller.seekToDefaultPosition()
                controller.play()
            }
        }
    }
}

/**
 * Translate provider facts into semantic State without using Player.isPlaying as a Paused alias.
 */
private fun Player.toVexStreamPlaybackState():
    VexStreamPlaybackState =
    vexStreamPlaybackState(
        mediaItemCount = mediaItemCount,
        playWhenReady = playWhenReady,
        playbackState = playbackState,
        playPauseAvailable =
            isCommandAvailable(
                Player.COMMAND_PLAY_PAUSE,
            ),
        restartAvailable =
            isCommandAvailable(
                Player.COMMAND_PLAY_PAUSE,
            ) &&
                isCommandAvailable(
                    Player.COMMAND_SEEK_TO_DEFAULT_POSITION,
                ),
    )

/**
 * Pure Media3 -> semantic-State translation seam for focused JVM proof.
 */
internal fun vexStreamPlaybackState(
    mediaItemCount: Int,
    playWhenReady: Boolean,
    playbackState: Int,
    playPauseAvailable: Boolean,
    restartAvailable: Boolean,
): VexStreamPlaybackState {
    require(mediaItemCount >= 0) {
        "mediaItemCount must not be negative"
    }

    if (mediaItemCount == 0) {
        return VexStreamPlaybackState.NoMedia
    }

    return when (playbackState) {
        Player.STATE_ENDED ->
            VexStreamPlaybackState.Ended(
                restartAvailable =
                    restartAvailable,
            )

        Player.STATE_IDLE ->
            VexStreamPlaybackState.Media(
                playRequested =
                    playWhenReady,
                phase =
                    VexStreamPlaybackPhase.Idle,
                playPauseAvailable =
                    playPauseAvailable,
            )

        Player.STATE_BUFFERING ->
            VexStreamPlaybackState.Media(
                playRequested =
                    playWhenReady,
                phase =
                    VexStreamPlaybackPhase.Buffering,
                playPauseAvailable =
                    playPauseAvailable,
            )

        Player.STATE_READY ->
            VexStreamPlaybackState.Media(
                playRequested =
                    playWhenReady,
                phase =
                    VexStreamPlaybackPhase.Ready,
                playPauseAvailable =
                    playPauseAvailable,
            )

        else ->
            error(
                "unknown Media3 playbackState: " +
                    playbackState,
            )
    }
}

// [VXG RealForever]
