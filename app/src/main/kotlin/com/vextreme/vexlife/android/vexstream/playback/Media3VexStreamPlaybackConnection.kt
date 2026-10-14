package com.vextreme.vexlife.android.vexstream.playback

import androidx.media3.common.Player
import androidx.media3.session.MediaController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Android Media3 realization of the reusable VexPortFlow contract.
 *
 * The concrete implementation keeps an ordinary Connection name. VexPortFlow is the generic
 * reusable interface boundary it fulfills.
 */
internal class Media3VexStreamPlaybackConnection(
    private val controller: MediaController,
) :
    VexStreamPlaybackConnectionFlow,
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
            VexStreamPlaybackProjection
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

        pending =
            pending.after(
                effect,
            )

        val admitted =
            current.afterIntent(
                effect,
            )

        check(
            relationship.admittedStateKind
                .isInstance(
                    admitted,
                ),
        ) {
            "admitted State value " +
                admitted::class.simpleName +
                " does not match behavior admission " +
                relationship.admittedStateKind.simpleName
        }

        publish(
            admitted,
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
