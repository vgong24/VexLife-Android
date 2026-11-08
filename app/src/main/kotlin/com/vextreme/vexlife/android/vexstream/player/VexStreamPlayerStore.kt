package com.vextreme.vexlife.android.vexstream.player

import com.vextreme.vexlife.android.behavior.VexPartner
import com.vextreme.vexlife.android.relay.VexConvergence
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackFamily
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackPhase
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackState
import com.vextreme.vexlife.android.vexstream.playback.reconcileVexStreamPlaybackObservation
import kotlinx.coroutines.flow.StateFlow

/**
 * Process-current outside-truth boundary for the VexStream Player Partner.
 *
 * This Store does not own Playback Family meaning and does not read Media3. It retains one coherent
 * Partner CURRENT from admitted Partner Signals so consumers do not independently reconstruct the
 * outside player or repeatedly read unrelated provider facets.
 */
internal class VexStreamPlayerStore :
    VexConvergence<
        VexStreamPlayerPartner.Signal,
        VexStreamPlayerPartner.Current
    >(
        initialCurrent =
            VexStreamPlayerPartner.Current
                .Unavailable,
    ) {
    val states:
        StateFlow<
            VexStreamPlayerPartner.Current
        >
        get() =
            convergedStates

    val current:
        VexStreamPlayerPartner.Current
        get() =
            convergedCurrent

    internal fun receiveNow(
        observation:
            VexStreamPlayerPartner.Connection.Observation,
    ):
        VexStreamPlayerPartner.Current =
        admit(
            observation.signal,
        )

    override fun converge(
        update:
            VexStreamPlayerPartner.Signal,
        current:
            VexStreamPlayerPartner.Current,
    ):
        VexStreamPlayerPartner.Current =
        when (
            update
        ) {
            VexStreamPlayerPartner.Signal
                .Unavailable ->
                VexStreamPlayerPartner.Current
                    .Unavailable

            is VexStreamPlayerPartner.Signal
                .Snapshot ->
                update.current

            is VexStreamPlayerPartner.Signal
                .Playback ->
                current
                    .availableFor(
                        update,
                    )
                    .copy(
                        playback =
                            update,
                    )

            is VexStreamPlayerPartner.Signal
                .QueueItems ->
                current
                    .availableFor(
                        update,
                    )
                    .copy(
                        queueItems =
                            update,
                    )

            is VexStreamPlayerPartner.Signal
                .QueueNavigation ->
                current
                    .availableFor(
                        update,
                    )
                    .copy(
                        queueNavigation =
                            update,
                    )

            is VexStreamPlayerPartner.Signal
                .Repeat ->
                current
                    .availableFor(
                        update,
                    )
                    .copy(
                        repeat =
                            update,
                    )

            is VexStreamPlayerPartner.Signal
                .CurrentMedia ->
                current
                    .availableFor(
                        update,
                    )
                    .copy(
                        currentMedia =
                            update,
                    )

            is VexStreamPlayerPartner.Signal
                .Timeline ->
                current
                    .availableFor(
                        update,
                    )
                    .copy(
                        timeline =
                            update,
                    )
        }

    private fun VexStreamPlayerPartner.Current.availableFor(
        signal:
            VexStreamPlayerPartner.Signal,
    ):
        VexStreamPlayerPartner.Current.Available =
        this as?
            VexStreamPlayerPartner.Current.Available
            ?: error(
                "VexStream Player facet Signal requires an established Snapshot before $signal",
            )
}

/**
 * Partner Receiver that keeps outside CURRENT and Playback Family meaning distinct.
 *
 * Provider Playback statements are first interpreted against the exact Family CURRENT, then the
 * Partner observation is admitted into the outside-truth Store. Queue/Repeat/media/timeline Signals
 * remain Partner truth until their rightful semantic consumers interpret them.
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
        when (
            val signal =
                observation.signal
        ) {
            VexStreamPlayerPartner.Signal
                .Unavailable ->
                playbackFamily
                    .observe(
                        VexStreamPlaybackState
                            .NoMedia,
                    )

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

        store.receiveNow(
            observation,
        )
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
