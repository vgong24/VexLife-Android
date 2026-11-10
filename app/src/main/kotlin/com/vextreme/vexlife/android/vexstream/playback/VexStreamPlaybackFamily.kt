package com.vextreme.vexlife.android.vexstream.playback

import com.vextreme.vexlife.android.behavior.VexFamily
import com.vextreme.vexlife.android.behavior.VexMethod
import com.vextreme.vexlife.android.execution.VexExecution
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerPartner
import com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerStore
import kotlinx.coroutines.Job

private typealias AcceptedPlayback =
    VexFamily.Act.Accepted<
        VexStreamPlaybackFamily,
        VexStreamPlaybackState,
        VexStreamPlaybackEffect
    >

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
     * Feature-owned fence between accepted semantic intent, later provider observation, and
     * realization failure. The already-existing Accepted occurrence is the identity; no feature
     * generation/revision/operation ID is minted.
     */
    private val observationLock =
        Any()
    private var unsettledAccepted:
        AcceptedPlayback? =
        null

    internal fun activatePlayback(
        control:
            VexStreamPlaybackControl,
    ):
        AcceptedPlayback? =
        synchronized(
            observationLock,
        ) {
            activate(
                control,
            ).also { accepted ->
                if (
                    accepted !=
                    null
                ) {
                    unsettledAccepted =
                        accepted
                }
            }
        }

    internal fun observePlayer(
        playback:
            VexStreamPlayerPartner.Signal.Playback?,
    ):
        VexStreamPlaybackState =
        synchronized(
            observationLock,
        ) {
            val after =
                observe(
                    reconcileVexStreamPlaybackObservation(
                        observed =
                            playback
                                .toVexStreamPlaybackState(),
                        current =
                            currentState,
                    ),
                )

            if (
                !after.hasPendingPlaybackIntent()
            ) {
                unsettledAccepted =
                    null
            }

            after
        }

    /**
     * Roll back one exact still-current accepted intent when its realization could not be dispatched.
     *
     * An older completion cannot clear a newer A -> B -> A intent because Accepted identity, not
     * State/effect equality, guards this correction.
     */
    internal fun settlePlaybackRealizationFailure(
        accepted:
            AcceptedPlayback,
        playback:
            VexStreamPlayerPartner.Signal.Playback?,
    ):
        VexStreamPlaybackState =
        synchronized(
            observationLock,
        ) {
            if (
                unsettledAccepted !==
                accepted
            ) {
                return@synchronized currentState
            }

            unsettledAccepted =
                null
            observe(
                playback
                    .toVexStreamPlaybackState(),
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
                Unit
            > {
            override suspend fun invoke():
                VexMethod.Result<
                    Unit,
                    Unit
                > {
                val dispatched =
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

                return if (
                    dispatched
                ) {
                    VexMethod.Result.Success(
                        Unit,
                    )
                } else {
                    VexMethod.Result.Failure(
                        Unit,
                    )
                }
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
    playerStore:
        VexStreamPlayerStore,
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

    if (
        begin ==
        null
    ) {
        family.settlePlaybackRealizationFailure(
            accepted =
                accepted,
            playback =
                playerStore.currentPlaybackSignal(),
        )
        return null
    }

    fun settleFailure() {
        family.settlePlaybackRealizationFailure(
            accepted =
                accepted,
            playback =
                playerStore.currentPlaybackSignal(),
        )
    }

    val job =
        try {
            accepted.launch(
                executionRoot =
                    executionRoot,
                method =
                    VexStreamPlaybackFamily.Act.Method(
                        connection =
                            connection,
                        effect =
                            begin.effect,
                    ),
                onResult = { result ->
                    when (
                        result
                    ) {
                        is VexMethod.Result.Success ->
                            Unit

                        is VexMethod.Result.Failure ->
                            settleFailure()

                        is VexMethod.Result.Error ->
                            settleFailure()
                    }
                },
            )
        } catch (
            failure:
                Throwable,
        ) {
            settleFailure()
            throw failure
        }

    if (
        job ==
        null
    ) {
        settleFailure()
    } else {
        job.invokeOnCompletion { failure ->
            if (
                failure !=
                null
            ) {
                settleFailure()
            }
        }
    }

    return job
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

private fun VexStreamPlaybackState.hasPendingPlaybackIntent():
    Boolean =
    when (
        this
    ) {
        VexStreamPlaybackState.NoMedia ->
            false

        is VexStreamPlaybackState.Media ->
            requestPending

        is VexStreamPlaybackState.Ended ->
            restartPending
    }

private fun VexStreamPlayerStore.currentPlaybackSignal():
    VexStreamPlayerPartner.Signal.Playback? =
    (
        current as?
            VexStreamPlayerStore.Snapshot.Available
    )
        ?.playback

// [VXG RealForever][VEXSTREAM][PLAYBACK][DIRECT-FAMILY]
