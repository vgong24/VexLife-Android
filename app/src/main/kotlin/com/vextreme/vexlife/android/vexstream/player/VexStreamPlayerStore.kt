package com.vextreme.vexlife.android.vexstream.player

import com.vextreme.vexlife.android.behavior.VexPartner
import com.vextreme.vexlife.android.relay.VexConvergence
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackFamily
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackPhase
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackState
import com.vextreme.vexlife.android.vexstream.playback.reconcileVexStreamPlaybackObservation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Updates admitted by the Player Store.
 *
 * An Observation is outside-participant evidence. ConnectionClosed is deliberately separate:
 * connection teardown is relationship/lifecycle evidence, not a statement the Partner communicated.
 */
internal sealed interface VexStreamPlayerStoreUpdate {
    data class Observed(
        val observation:
            VexStreamPlayerPartner.Connection.Observation,
    ) :
        VexStreamPlayerStoreUpdate

    data class ConnectionClosed(
        val connectionId:
            VexStreamPlayerPartner.Connection.Id,
    ) :
        VexStreamPlayerStoreUpdate
}

internal data class VexStreamPlayerRetainedCurrent(
    val connectionId:
        VexStreamPlayerPartner.Connection.Id?,
    val current:
        VexStreamPlayerPartner.Current,
)

/**
 * Process-current outside-truth boundary for the VexStream Player Partner.
 *
 * This Store does not own Playback Family meaning and does not read Media3. It retains one coherent
 * Partner CURRENT from admitted Partner observations so consumers do not independently reconstruct
 * the outside player or repeatedly read unrelated provider facets.
 *
 * Connection identity is retained privately with CURRENT so a late event/close from an older
 * realization cannot overwrite a newer Connection's outside truth.
 */
internal class VexStreamPlayerStore :
    VexConvergence<
        VexStreamPlayerStoreUpdate,
        VexStreamPlayerRetainedCurrent
    >(
        initialCurrent =
            VexStreamPlayerRetainedCurrent(
                connectionId =
                    null,
                current =
                    VexStreamPlayerPartner.Current
                        .Unavailable,
            ),
    ) {
    data class Admission(
        val accepted:
            Boolean,
        val current:
            VexStreamPlayerPartner.Current,
    )

    val states:
        Flow<
            VexStreamPlayerPartner.Current
        > =
        convergedStates
            .map { retained ->
                retained.current
            }
            .distinctUntilChanged()

    val current:
        VexStreamPlayerPartner.Current
        get() =
            convergedCurrent
                .current

    internal fun receiveNow(
        observation:
            VexStreamPlayerPartner.Connection.Observation,
    ):
        Admission {
        val retained =
            admit(
                VexStreamPlayerStoreUpdate
                    .Observed(
                        observation,
                    ),
            )

        return Admission(
            accepted =
                retained.connectionId ==
                    observation.connectionId,
            current =
                retained.current,
        )
    }

    internal fun connectionClosed(
        connectionId:
            VexStreamPlayerPartner.Connection.Id,
    ):
        VexStreamPlayerPartner.Current =
        admit(
            VexStreamPlayerStoreUpdate
                .ConnectionClosed(
                    connectionId,
                ),
        )
            .current

    override fun converge(
        update:
            VexStreamPlayerStoreUpdate,
        current:
            VexStreamPlayerRetainedCurrent,
    ):
        VexStreamPlayerRetainedCurrent =
        when (
            update
        ) {
            is VexStreamPlayerStoreUpdate
                .ConnectionClosed ->
                if (
                    current.connectionId ==
                    update.connectionId
                ) {
                    VexStreamPlayerRetainedCurrent(
                        connectionId =
                            null,
                        current =
                            VexStreamPlayerPartner.Current
                                .Unavailable,
                    )
                } else {
                    current
                }

            is VexStreamPlayerStoreUpdate
                .Observed ->
                current
                    .admit(
                        update.observation,
                    )
        }

    private fun VexStreamPlayerRetainedCurrent.admit(
        observation:
            VexStreamPlayerPartner.Connection.Observation,
    ):
        VexStreamPlayerRetainedCurrent =
        when (
            val signal =
                observation.signal
        ) {
            is VexStreamPlayerPartner.Signal
                .Snapshot ->
                VexStreamPlayerRetainedCurrent(
                    connectionId =
                        observation.connectionId,
                    current =
                        signal.current,
                )

            else -> {
                if (
                    connectionId !=
                    observation.connectionId
                ) {
                    return this
                }

                val available =
                    current as?
                        VexStreamPlayerPartner.Current.Available
                        ?: return this

                val next =
                    when (
                        signal
                    ) {
                        is VexStreamPlayerPartner.Signal
                            .Playback ->
                            available.copy(
                                playback =
                                    signal,
                            )

                        is VexStreamPlayerPartner.Signal
                            .QueueItems ->
                            available.copy(
                                queueItems =
                                    signal,
                            )

                        is VexStreamPlayerPartner.Signal
                            .QueueNavigation ->
                            available.copy(
                                queueNavigation =
                                    signal,
                            )

                        is VexStreamPlayerPartner.Signal
                            .Repeat ->
                            available.copy(
                                repeat =
                                    signal,
                            )

                        is VexStreamPlayerPartner.Signal
                            .CurrentMedia ->
                            available.copy(
                                currentMedia =
                                    signal,
                            )

                        is VexStreamPlayerPartner.Signal
                            .Timeline ->
                            available.copy(
                                timeline =
                                    signal,
                            )

                        is VexStreamPlayerPartner.Signal
                            .Snapshot ->
                            error(
                                "Snapshot is handled before facet convergence",
                            )
                    }

                copy(
                    current =
                        next,
                )
            }
        }
}

/**
 * Partner Receiver that keeps outside CURRENT and Playback Family meaning distinct.
 *
 * Only observations accepted by the Store's exact current Connection may affect Playback Family.
 * This keeps a late provider callback from an older Connection from overwriting newer semantic
 * currentness.
 */
internal class VexStreamPlayerObservationReceiver(
    private val store:
        VexStreamPlayerStore,
    private val playbackFamily:
        VexStreamPlaybackFamily,
) :
    VexPartner.Connection.Receiver<
        VexStreamPlayerPartner,
        VexStreamPlayerPartner.Signal,
        VexStreamPlayerPartner.Connection.Observation
    > {
    internal fun receiveNow(
        observation:
            VexStreamPlayerPartner.Connection.Observation,
    ) {
        val admission =
            store.receiveNow(
                observation,
            )

        if (
            !admission.accepted
        ) {
            return
        }

        when (
            val signal =
                observation.signal
        ) {
            is VexStreamPlayerPartner.Signal
                .Snapshot ->
                reconcilePlayback(
                    signal.current.playback,
                )

            is VexStreamPlayerPartner.Signal
                .Playback ->
                reconcilePlayback(
                    signal,
                )

            else ->
                Unit
        }
    }

    internal fun connectionClosed(
        connectionId:
            VexStreamPlayerPartner.Connection.Id,
    ) {
        val current =
            store.connectionClosed(
                connectionId,
            )

        if (
            current ==
            VexStreamPlayerPartner.Current.Unavailable
        ) {
            playbackFamily
                .observe(
                    VexStreamPlaybackState
                        .NoMedia,
                )
        }
    }

    override suspend fun receive(
        observation:
            VexStreamPlayerPartner.Connection.Observation,
    ) {
        receiveNow(
            observation,
        )
    }

    private fun reconcilePlayback(
        signal:
            VexStreamPlayerPartner.Signal.Playback,
    ) {
        playbackFamily
            .observe(
                reconcileVexStreamPlaybackObservation(
                    observed =
                        signal
                            .toObservedPlaybackState(),
                    current =
                        playbackFamily
                            .currentState,
                ),
            )
    }
}

private fun VexStreamPlayerPartner.Signal.Playback.toObservedPlaybackState():
    VexStreamPlaybackState {
    if (
        !hasMedia
    ) {
        return VexStreamPlaybackState
            .NoMedia
    }

    return when (
        phase
    ) {
        VexStreamPlayerPartner
            .PlaybackPhase.Idle ->
            VexStreamPlaybackState
                .Media(
                    playRequested =
                        playRequested,
                    phase =
                        VexStreamPlaybackPhase
                            .Idle,
                    playPauseAvailable =
                        playPauseAvailable,
                )

        VexStreamPlayerPartner
            .PlaybackPhase.Buffering ->
            VexStreamPlaybackState
                .Media(
                    playRequested =
                        playRequested,
                    phase =
                        VexStreamPlaybackPhase
                            .Buffering,
                    playPauseAvailable =
                        playPauseAvailable,
                )

        VexStreamPlayerPartner
            .PlaybackPhase.Ready ->
            VexStreamPlaybackState
                .Media(
                    playRequested =
                        playRequested,
                    phase =
                        VexStreamPlaybackPhase
                            .Ready,
                    playPauseAvailable =
                        playPauseAvailable,
                )

        VexStreamPlayerPartner
            .PlaybackPhase.Ended ->
            VexStreamPlaybackState
                .Ended(
                    restartAvailable =
                        restartAvailable,
                )
    }
}

// [VXG RealForever][VEXSTREAM][PLAYER-STORE]
