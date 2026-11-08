package com.vextreme.vexlife.android.vexstream.playback

import com.vextreme.vexlife.android.behavior.VexFamily
import com.vextreme.vexlife.android.execution.VexExecution

/**
 * Process-shareable semantic owner for VexStream Playback.
 *
 * UI, Android Service, Widget-facing projection, and app-scope convergence may all receive this same
 * live Family instance when their rightful lifetime is the Android process. The Family owns Playback
 * State / Control / Effect meaning; Media3 remains provider reality and Android Service remains the
 * sustained playback realization.
 */
internal class VexStreamPlaybackFamily(
    initialState:
        VexStreamPlaybackState =
        VexStreamPlaybackState.NoMedia,
) :
    VexFamily<
        VexStreamPlaybackFamily,
        VexStreamPlaybackState,
        VexStreamPlaybackControl,
        VexStreamPlaybackEffect
    >(
        initialState =
            initialState,
    ) {
    override fun act(
        currentState:
            VexStreamPlaybackState,
        control:
            VexStreamPlaybackControl,
    ):
        VexFamily.Act<
            VexStreamPlaybackFamily,
            VexStreamPlaybackState,
            VexStreamPlaybackEffect
        >? =
        when (
            control
        ) {
            VexStreamPlaybackControl.PlayPause -> {
                if (
                    !isPlayPauseAvailable(
                        currentState,
                    )
                ) {
                    null
                } else {
                    val effect =
                        when (
                            currentState
                        ) {
                            VexStreamPlaybackState.NoMedia ->
                                error(
                                    "NoMedia cannot admit PlayPause",
                                )

                            is VexStreamPlaybackState.Media ->
                                if (
                                    currentState
                                        .playRequested
                                ) {
                                    VexStreamPlaybackEffect
                                        .RequestPause
                                } else {
                                    VexStreamPlaybackEffect
                                        .RequestPlay
                                }

                            is VexStreamPlaybackState.Ended ->
                                VexStreamPlaybackEffect
                                    .RestartCurrent
                        }

                    admit(
                        state =
                            currentState
                                .afterIntent(
                                    effect,
                                ),
                        effect =
                            effect,
                        approach =
                            VexExecution.Approach.Latest,
                    )
                }
            }
        }

    /**
     * Feature projection derived from one exact Family State snapshot.
     *
     * Availability is resolved by the same semantic law as activation; callers do not duplicate the
     * Play/Pause conditions in UI or Service code.
     */
    fun playbackProjectionFor(
        state:
            VexStreamPlaybackState,
    ):
        VexStreamPlaybackProjection =
        VexStreamPlaybackProjection(
            state =
                state,
            playPauseAvailable =
                isPlayPauseAvailable(
                    state,
                ),
        )

    val currentPlaybackProjection:
        VexStreamPlaybackProjection
        get() =
            playbackProjectionFor(
                currentState,
            )

    private fun isPlayPauseAvailable(
        state:
            VexStreamPlaybackState,
    ):
        Boolean =
        when (
            state
        ) {
            VexStreamPlaybackState.NoMedia ->
                false

            is VexStreamPlaybackState.Media ->
                state.playPauseAvailable

            is VexStreamPlaybackState.Ended ->
                state.restartAvailable &&
                    !state.restartPending
        }
}

// [VXG RealForever][VEXSTREAM][PLAYBACK][DIRECT-FAMILY]
