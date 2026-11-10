package com.vextreme.vexlife.android.vexstream.playback

import com.vextreme.vexlife.android.behavior.VexFamily
import com.vextreme.vexlife.android.behavior.VexMethod
import com.vextreme.vexlife.android.execution.VexExecution
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerPartner
import com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerStore
import kotlinx.coroutines.Job

/**
 * One live semantic owner for VexStream Play/Pause/Restart meaning.
 *
 * Outside Player truth arrives through the Player Store. Accepted local intent is admitted into this
 * Family immediately and remains visibly pending until later Player truth acknowledges it.
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
     * Feature-owned currentness fence between accepted semantic intent and later Store observation.
     * This is not a generation/revision protocol. The lock exists only so an older provider
     * observation cannot interleave between reading current pending intent and admitting the
     * reconciled State.
     */
    private val observationLock =
        Any()

    internal fun activatePlayback(
        control:
            VexStreamPlaybackControl,
    ):
        VexFamily.Act.Accepted<
            VexStreamPlaybackFamily,
            VexStreamPlaybackState,
            VexStreamPlaybackEffect
        >? =
        synchronized(
            observationLock,
        ) {
            activate(
                control,
            )
        }

    internal fun observePlayer(
        playback:
            VexStreamPlayerPartner.Signal.Playback?,
    ):
        VexStreamPlaybackState =
        synchronized(
            observationLock,
        ) {
            val observed =
                playback
                    .toVexStreamPlaybackState()
            observe(
                reconcileVexStreamPlaybackObservation(
                    observed =
                        observed,
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
            currentState
        ) {
            VexStreamPlaybackState.NoMedia ->
                null

            is VexStreamPlaybackState.Media -> {
                if (
                    !currentState.playPauseAvailable
                ) {
                    null
                } else {
                    val effect =
                        if (
                            currentState.playRequested
                        ) {
                            VexStreamPlaybackEffect.RequestPause
                        } else {
                            VexStreamPlaybackEffect.RequestPlay
                        }

                    admit(
                        currentState.afterIntent(
                            effect,
                        ),
                        begin =
                            beginLatest(
                                effect,
                            ),
                    )
                }
            }

            is VexStreamPlaybackState.Ended ->
                VexStreamPlaybackEffect.RestartCurrent
                    .takeIf {
                        currentState.restartAvailable
                    }
                    ?.let { effect ->
                        admit(
                            currentState.afterIntent(
                                effect,
                            ),
                            begin =
                                beginLatest(
                                    effect,
                                ),
                        )
                    }
        }

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

    object Act {
        class Method(
            private val connection:
                VexStreamPlayerPartner.Connection.Request,
            override val effect:
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
            when (
                state
            ) {
                VexStreamPlaybackState.NoMedia ->
                    false

                is VexStreamPlaybackState.Media ->
                    state.playPauseAvailable

                is VexStreamPlaybackState.Ended ->
                    state.restartAvailable
            },
    )

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
    val accepted =
        family.activatePlayback(
            control,
        )
            ?: return null
    val begin =
        accepted.realizationBegin
            ?: return null

    return accepted.launch(
        executionRoot =
            executionRoot,
        method =
            VexStreamPlaybackFamily.Act.Method(
                connection =
                    connection,
                effect =
                    begin.effect,
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
    ) { playback ->
        family.observePlayer(
            playback,
        )
    }
}

internal fun VexStreamPlayerPartner.Signal.Playback?.toVexStreamPlaybackState():
    VexStreamPlaybackState =
    when {
        this == null ||
            !hasMedia ->
            VexStreamPlaybackState.NoMedia

        phase ==
            VexStreamPlayerPartner.PlaybackPhase.Ended ->
            VexStreamPlaybackState.Ended(
                restartAvailable =
                    restartAvailable,
            )

        else ->
            VexStreamPlaybackState.Media(
                playRequested =
                    playRequested,
                phase =
                    when (
                        phase
                    ) {
                        VexStreamPlayerPartner.PlaybackPhase.Idle ->
                            VexStreamPlaybackPhase.Idle

                        VexStreamPlayerPartner.PlaybackPhase.Buffering ->
                            VexStreamPlaybackPhase.Buffering

                        VexStreamPlayerPartner.PlaybackPhase.Ready ->
                            VexStreamPlaybackPhase.Ready

                        VexStreamPlayerPartner.PlaybackPhase.Ended ->
                            error(
                                "Ended Player truth must map to Ended Playback State",
                            )
                    },
                playPauseAvailable =
                    playPauseAvailable,
            )
    }

// [VXG RealForever][VEXSTREAM][PLAYBACK][DIRECT-FAMILY]
