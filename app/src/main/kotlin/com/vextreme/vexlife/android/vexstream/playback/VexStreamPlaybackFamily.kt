package com.vextreme.vexlife.android.vexstream.playback

import androidx.media3.session.MediaController
import com.vextreme.vexlife.android.behavior.VexFamily
import com.vextreme.vexlife.android.behavior.VexMethod

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
                        begin =
                            beginLatest(
                                effect,
                            ),
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

    /**
     * Fully-bound Media3 realization vocabulary for one accepted Playback Act.
     *
     * The Family already authored the Effect and Latest overlap relationship. This Method carries
     * only the concrete provider required to attempt that Effect. VexExecution owns the Job and
     * provider-affinity handoff; sustained Player observation later returns provider truth.
     */
    object Act {
        class Method(
            private val controller:
                MediaController,
            val effect:
                VexStreamPlaybackEffect,
        ) :
            VexFamily.Act.Method<
                VexStreamPlaybackFamily,
                Unit,
                Unit
            > {
            override suspend fun invoke():
                VexMethod.Result<
                    Unit,
                    Unit
                > {
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

                return VexMethod.Result.Success(
                    output =
                        Unit,
                )
            }
        }
    }

}

// [VXG RealForever][VEXSTREAM][PLAYBACK][DIRECT-FAMILY]
