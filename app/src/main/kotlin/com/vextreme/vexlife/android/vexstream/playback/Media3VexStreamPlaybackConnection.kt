package com.vextreme.vexlife.android.vexstream.playback

import androidx.media3.common.Player
import androidx.media3.session.MediaController

/**
 * Media3 realization of accepted Playback Family Effects.
 *
 * This object does not own Playback State or provider observation. It receives the process-shared
 * [VexStreamPlaybackFamily], realizes its accepted Effect, and leaves continuous provider truth to
 * the Service-hosted VexStream Player Partner Connection.
 */
internal class Media3VexStreamPlaybackConnection(
    private val controller:
        MediaController,
    private val family:
        VexStreamPlaybackFamily,
) :
    VexStreamPlaybackConnection {
    override fun activate(
        control:
            VexStreamPlaybackControl,
    ) {
        val effect =
            family
                .activate(
                    control,
                )
                ?.effect
                ?: return

        realize(
            effect,
        )
    }

    private fun realize(
        effect:
            VexStreamPlaybackEffect,
    ) {
        when (
            effect
        ) {
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

/*
 * Player.toVexStreamPlaybackState remains only for the explicitly named Widget bootstrap bridge in
 * VexStreamPlaybackReadState. It is not the canonical continuous provider-observation path.
 */
// [VXG RealForever][VEXSTREAM][PLAYBACK][MEDIA3-CONNECTION]
