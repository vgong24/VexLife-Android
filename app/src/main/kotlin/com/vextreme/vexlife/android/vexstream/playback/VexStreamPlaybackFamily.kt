package com.vextreme.vexlife.android.vexstream.playback

import com.vextreme.vexlife.android.behavior.VexFamily

/**
 * Process-shareable semantic owner for VexStream Playback.
 *
 * UI, Android Service, Widget-facing projection, and process-level Relays may all receive this same
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
            VexStreamPlaybackControl.PlayPause ->
                when (
                    currentState
                ) {
                    VexStreamPlaybackState.NoMedia ->
                        null

                    is VexStreamPlaybackState.Media -> {
                        if (
                            !currentState
                                .playPauseAvailable
                        ) {
                            null
                        } else {
                            val effect =
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

                            admit(
                                state =
                                    currentState
                                        .afterIntent(
                                            effect,
                                        ),
                                begin =
                                    begin(
                                        effect,
                                    ),
                            )
                        }
                    }

                    is VexStreamPlaybackState.Ended -> {
                        if (
                            currentState
                                .restartAvailable &&
                            !currentState
                                .restartPending
                        ) {
                            val effect =
                                VexStreamPlaybackEffect
                                    .RestartCurrent

                            admit(
                                state =
                                    currentState
                                        .afterIntent(
                                            effect,
                                        ),
                                begin =
                                    begin(
                                        effect,
                                    ),
                            )
                        } else {
                            null
                        }
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
