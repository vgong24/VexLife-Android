package com.vextreme.vexlife.android.vexstream.playback

import android.os.Handler
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import kotlinx.coroutines.Job
import kotlinx.coroutines.android.asCoroutineDispatcher

/**
 * Media3 realization of accepted Playback Family Effects.
 *
 * This object does not own Playback State. It receives the process-shared [VexStreamPlaybackFamily],
 * re-reads current provider truth before activation, realizes the accepted Effect, then reconciles
 * Media3 truth back into that same Family.
 */
internal class Media3VexStreamPlaybackConnection(
    private val controller:
        MediaController,
    private val family:
        VexStreamPlaybackFamily,
    private val executionRoot:
        VexExecutionRoot,
) :
    VexStreamPlaybackConnection {
    override fun activate(
        control:
            VexStreamPlaybackControl,
    ):
        Job? {
        val act =
            family
                .activate(
                    control,
                )
                ?: return null
        val effect =
            act.effect
                ?: return null
        val method =
            VexStreamPlaybackFamily
                .Act
                .Method(
                    controller =
                        controller,
                    effect =
                        effect,
                )
        val providerDispatcher =
            Handler(
                controller.applicationLooper,
            ).asCoroutineDispatcher(
                "VexStream Media3",
            )

        return act
            .launchWith(
                executionRoot,
            )
            ?.method(
                method =
                    method,
                handoffDispatcher =
                    providerDispatcher,
            )
    }

internal fun Player.toVexStreamPlaybackProjection(
    family:
        VexStreamPlaybackFamily,
):
    VexStreamPlaybackProjection {
    val state =
        reconcileVexStreamPlaybackObservation(
            observed =
                toVexStreamPlaybackState(),
            current =
                family
                    .currentState,
        )

    family.observe(
        state,
    )

    return family
        .currentPlaybackProjection
}

internal fun Player.toVexStreamPlaybackState():
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
    mediaItemCount:
        Int,
    playWhenReady:
        Boolean,
    playbackState:
        Int,
    playPauseAvailable:
        Boolean,
    restartAvailable:
        Boolean,
):
    VexStreamPlaybackState {
    require(
        mediaItemCount >=
            0,
    ) {
        "mediaItemCount must not be negative"
    }

    if (
        mediaItemCount ==
        0
    ) {
        return VexStreamPlaybackState
            .NoMedia
    }

    return when (
        playbackState
    ) {
        Player.STATE_ENDED ->
            VexStreamPlaybackState
                .Ended(
                    restartAvailable =
                        restartAvailable,
                )

        Player.STATE_IDLE ->
            VexStreamPlaybackState
                .Media(
                    playRequested =
                        playWhenReady,
                    phase =
                        VexStreamPlaybackPhase
                            .Idle,
                    playPauseAvailable =
                        playPauseAvailable,
                )

        Player.STATE_BUFFERING ->
            VexStreamPlaybackState
                .Media(
                    playRequested =
                        playWhenReady,
                    phase =
                        VexStreamPlaybackPhase
                            .Buffering,
                    playPauseAvailable =
                        playPauseAvailable,
                )

        Player.STATE_READY ->
            VexStreamPlaybackState
                .Media(
                    playRequested =
                        playWhenReady,
                    phase =
                        VexStreamPlaybackPhase
                            .Ready,
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

// [VXG RealForever][VEXSTREAM][PLAYBACK][MEDIA3-CONNECTION]
