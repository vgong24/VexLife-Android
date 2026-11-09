@file:OptIn(com.vextreme.vexlife.android.execution.VexExecution.RelationshipFoundationApi::class)

package com.vextreme.vexlife.android.vexstream.playback

import com.vextreme.vexlife.android.behavior.VexFamily
import com.vextreme.vexlife.android.behavior.VexMethod
import com.vextreme.vexlife.android.execution.VexExecution
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerPartner
import com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerStore
import kotlinx.coroutines.Job

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
    /*
     * Playback-specific currentness fence, following the accepted ThemeModeFamily pattern.
     *
     * Provider reconciliation depends on exact CURRENT semantic intent while a Play/Pause Control
     * may be admitted concurrently. Keep that feature-specific race inside the Family that owns the
     * meaning rather than wrapping Family in another admission/coordinator object.
     */
    private val providerCurrentnessLock =
        Any()

    internal fun activatePlayback(
        control:
            VexStreamPlaybackControl,
    ):
        VexFamily.Act<
            VexStreamPlaybackFamily,
            VexStreamPlaybackState,
            VexStreamPlaybackEffect
        >? =
        synchronized(
            providerCurrentnessLock,
        ) {
            activate(
                control,
            )
        }

    internal fun observePlayer(
        observed:
            VexStreamPlayerPartner.Signal.Playback?,
    ):
        VexStreamPlaybackState =
        synchronized(
            providerCurrentnessLock,
        ) {
            val observedState =
                observed
                    ?.toVexStreamPlaybackState()
                    ?: VexStreamPlaybackState.NoMedia

            observe(
                reconcileVexStreamPlaybackObservation(
                    observed =
                        observedState,
                    current =
                        currentState,
                ),
            )
        }

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

    object Act {
        class Method(
            private val connection:
                VexStreamPlayerPartner.Connection.Request,
            private val effect:
                VexStreamPlaybackEffect,
        ) :
            VexFamily.Act.Method<
                VexStreamPlaybackFamily,
                Unit,
                Nothing
            > {
            override suspend fun invoke():
                VexMethod.Result<
                    Unit,
                    Nothing
                > {
                when (
                    effect
                ) {
                    VexStreamPlaybackEffect.RequestPlay ->
                        connection.play()

                    VexStreamPlaybackEffect.RequestPause ->
                        connection.pause()

                    VexStreamPlaybackEffect.RestartCurrent ->
                        connection.restartCurrent()
                }

                return VexMethod.Result.Success(
                    Unit,
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
        vexStreamPlaybackProjectionFor(
            state,
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
        vexStreamPlaybackPlayPauseAvailable(
            state,
        )
}


internal fun activateVexStreamPlayback(
    family:
        VexStreamPlaybackFamily,
    control:
        VexStreamPlaybackControl,
    connection:
        VexStreamPlayerPartner.Connection.Request,
    executionRoot:
        VexExecutionRoot,
):
    Job? {
    val act =
        family.activatePlayback(
            control,
        )
            ?: return null
    val effect =
        act.effect
            ?: return null
    val execution =
        act.launchWith(
            executionRoot,
        )
            ?: return null

    return execution.method(
        VexStreamPlaybackFamily.Act.Method(
            connection =
                connection,
            effect =
                effect,
        ),
    )
}


internal fun observeVexStreamPlaybackPlayerStore(
    family:
        VexStreamPlaybackFamily,
    store:
        VexStreamPlayerStore,
    executionRoot:
        VexExecutionRoot,
) {
    VexExecution.observe(
        executionRoot =
            executionRoot,
        owner =
            family,
        source =
            store::playbackSignals,
    ) { observed ->
        family.observePlayer(
            observed,
        )
    }
}

private fun VexStreamPlayerPartner.Signal.Playback.toVexStreamPlaybackState():
    VexStreamPlaybackState {
    if (
        !hasMedia
    ) {
        return VexStreamPlaybackState.NoMedia
    }

    return when (
        phase
    ) {
        VexStreamPlayerPartner.PlaybackPhase.Idle ->
            VexStreamPlaybackState.Media(
                playRequested =
                    playRequested,
                phase =
                    VexStreamPlaybackPhase.Idle,
                playPauseAvailable =
                    playPauseAvailable,
                seekAvailable =
                    seekAvailable,
            )

        VexStreamPlayerPartner.PlaybackPhase.Buffering ->
            VexStreamPlaybackState.Media(
                playRequested =
                    playRequested,
                phase =
                    VexStreamPlaybackPhase.Buffering,
                playPauseAvailable =
                    playPauseAvailable,
                seekAvailable =
                    seekAvailable,
            )

        VexStreamPlayerPartner.PlaybackPhase.Ready ->
            VexStreamPlaybackState.Media(
                playRequested =
                    playRequested,
                phase =
                    VexStreamPlaybackPhase.Ready,
                playPauseAvailable =
                    playPauseAvailable,
                seekAvailable =
                    seekAvailable,
            )

        VexStreamPlayerPartner.PlaybackPhase.Ended ->
            VexStreamPlaybackState.Ended(
                restartAvailable =
                    restartAvailable,
                seekAvailable =
                    seekAvailable,
            )
    }
}

internal fun vexStreamPlaybackProjectionFor(
    state:
        VexStreamPlaybackState,
):
    VexStreamPlaybackProjection =
    VexStreamPlaybackProjection(
        state =
            state,
        playPauseAvailable =
            vexStreamPlaybackPlayPauseAvailable(
                state,
            ),
        seekAvailable =
            when (
                state
            ) {
                VexStreamPlaybackState.NoMedia ->
                    false

                is VexStreamPlaybackState.Media ->
                    state.seekAvailable

                is VexStreamPlaybackState.Ended ->
                    state.seekAvailable
            },
    )

private fun vexStreamPlaybackPlayPauseAvailable(
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

// [VXG RealForever][VEXSTREAM][PLAYBACK][DIRECT-FAMILY]
