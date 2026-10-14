package com.vextreme.vexlife.android.vexstream.playback

import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.vextreme.vexlife.android.behavior.VexBehaviorProjection
import com.vextreme.vexlife.android.behavior.VexObservableBehaviorConnection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Android Media3 realization of the reusable VexBehaviorConnection contract.
 *
 * MediaSession/Player remain the actual playback truth owners. This class is a retained semantic
 * projection + Effect adapter over one MediaController; it does not own player or queue lifetime.
 */
internal class Media3VexStreamPlaybackConnection(
    private val controller: MediaController,
) :
    VexObservableBehaviorConnection<
        VexStreamPlaybackState,
        VexStreamPlaybackControl,
        VexStreamPlaybackAct
    >,
    AutoCloseable {
    private val behavior =
        vexStreamPlaybackBehavior()

    private val controls =
        behavior.vexStreamPlaybackControls()

    private val mutableProjections =
        MutableStateFlow(
            controls.projection(
                controller.toVexStreamPlaybackState(),
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
                publish(
                    player.toVexStreamPlaybackState(),
                )
            }
        }

    init {
        controller.addListener(
            listener,
        )
    }

    /**
     * Resolve from live Media3 truth at interaction time, never from a previously rendered Act.
     */
    override fun activate(
        control: VexStreamPlaybackControl,
    ) {
        val current =
            controller.toVexStreamPlaybackState()

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

        realize(
            relationship.effect,
        )

        /*
         * Media3 listener events remain authoritative for eventual truth. This immediate reread lets
         * synchronous playWhenReady changes become visible without waiting for another callback.
         */
        publish(
            controller.toVexStreamPlaybackState(),
        )
    }

    override fun close() {
        controller.removeListener(
            listener,
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
