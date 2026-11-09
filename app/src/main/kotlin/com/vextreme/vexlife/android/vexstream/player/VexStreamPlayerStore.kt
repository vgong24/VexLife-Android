package com.vextreme.vexlife.android.vexstream.player

import com.vextreme.vexlife.android.behavior.VexPartner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal sealed interface VexStreamPlayerStoreUpdate {
    data class ConnectionOpened(
        val connectionId:
            VexStreamPlayerPartner.Connection.Id,
        val observations:
            List<
                VexStreamPlayerPartner.Connection.Observation
            >,
    ) :
        VexStreamPlayerStoreUpdate

    data class Observed(
        val observations:
            List<
                VexStreamPlayerPartner.Connection.Observation
            >,
    ) :
        VexStreamPlayerStoreUpdate

    data class ConnectionClosed(
        val connectionId:
            VexStreamPlayerPartner.Connection.Id,
    ) :
        VexStreamPlayerStoreUpdate
}

internal data class VexStreamPlayerRetainedCurrent(
    val latestConnectionOccurrence:
        Long =
        0L,
    val connectionId:
        VexStreamPlayerPartner.Connection.Id?,
    val current:
        VexStreamPlayerPartner.Current,
)

/**
 * Process-current outside-truth boundary for the VexStream Player Partner.
 *
 * One provider occurrence is admitted as one observation batch, so several facet Signals from that
 * occurrence converge into one coherent Player CURRENT emission.
 */
internal class VexStreamPlayerStore {
    private val lock =
        Any()

    private val mutableRetainedStates =
        MutableStateFlow(
            VexStreamPlayerRetainedCurrent(
                connectionId =
                    null,
                current =
                    VexStreamPlayerPartner.Current
                        .Unavailable,
            ),
        )

    private val retainedStates:
        StateFlow<
            VexStreamPlayerRetainedCurrent
        > =
        mutableRetainedStates
            .asStateFlow()

    private val retainedCurrent:
        VexStreamPlayerRetainedCurrent
        get() =
            retainedStates.value

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
        retainedStates
            .map { retained ->
                retained.current
            }
            .distinctUntilChanged()

    val playbackSignals:
        Flow<
            VexStreamPlayerPartner.Signal.Playback?
        > =
        states
            .map { current ->
                (
                    current as?
                        VexStreamPlayerPartner.Current.Available
                )
                    ?.playback
            }
            .distinctUntilChanged()

    val queueCurrents:
        Flow<
            VexStreamPlayerPartner.QueueCurrent?
        > =
        states
            .map { current ->
                (
                    current as?
                        VexStreamPlayerPartner.Current.Available
                )
                    ?.queue
            }
            .distinctUntilChanged()

    val repeatSignals:
        Flow<
            VexStreamPlayerPartner.Signal.Repeat?
        > =
        states
            .map { current ->
                (
                    current as?
                        VexStreamPlayerPartner.Current.Available
                )
                    ?.repeat
            }
            .distinctUntilChanged()

    val currentMediaSignals:
        Flow<
            VexStreamPlayerPartner.Signal.CurrentMedia?
        > =
        states
            .map { current ->
                (
                    current as?
                        VexStreamPlayerPartner.Current.Available
                )
                    ?.currentMedia
            }
            .distinctUntilChanged()

    val current:
        VexStreamPlayerPartner.Current
        get() =
            retainedCurrent
                .current

    val currentQueue:
        VexStreamPlayerPartner.QueueCurrent?
        get() =
            (
                current as?
                    VexStreamPlayerPartner.Current.Available
            )
                ?.queue

    internal fun connectionOpened(
        connectionId:
            VexStreamPlayerPartner.Connection.Id,
        observations:
            List<
                VexStreamPlayerPartner.Connection.Observation
            >,
    ):
        Admission =
        admitAndReport(
            update =
                VexStreamPlayerStoreUpdate
                    .ConnectionOpened(
                        connectionId =
                            connectionId,
                        observations =
                            observations,
                    ),
            expectedConnectionId =
                connectionId,
        )

    internal fun receive(
        observations:
            List<
                VexStreamPlayerPartner.Connection.Observation
            >,
    ):
        Admission {
        if (
            observations.isEmpty()
        ) {
            return Admission(
                accepted =
                    false,
                current =
                    current,
            )
        }

        val connectionId =
            observations
                .first()
                .connectionId

        return admitAndReport(
            update =
                VexStreamPlayerStoreUpdate
                    .Observed(
                        observations,
                    ),
            expectedConnectionId =
                connectionId,
        )
    }

    internal fun connectionClosed(
        connectionId:
            VexStreamPlayerPartner.Connection.Id,
    ):
        Admission {
        val before =
            retainedCurrent
        val after =
            admit(
                VexStreamPlayerStoreUpdate
                    .ConnectionClosed(
                        connectionId,
                    ),
            )

        return Admission(
            accepted =
                before.connectionId ==
                    connectionId &&
                    after.connectionId ==
                    null,
            current =
                after.current,
        )
    }

    private fun converge(
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
                .ConnectionOpened -> {
                update.observations
                    .requireConnection(
                        update.connectionId,
                    )

                if (
                    update.connectionId.occurrence <
                    current.latestConnectionOccurrence
                ) {
                    return current
                }

                VexStreamPlayerRetainedCurrent(
                    latestConnectionOccurrence =
                        update.connectionId
                            .occurrence,
                    connectionId =
                        update.connectionId,
                    current =
                        completeCurrent(
                            update.observations,
                        ),
                )
            }

            is VexStreamPlayerStoreUpdate
                .Observed -> {
                if (
                    update.observations
                        .isEmpty()
                ) {
                    return current
                }

                val connectionId =
                    update.observations
                        .first()
                        .connectionId

                update.observations
                    .requireConnection(
                        connectionId,
                    )

                if (
                    current.connectionId !=
                    connectionId
                ) {
                    return current
                }

                val available =
                    current.current as?
                        VexStreamPlayerPartner.Current.Available
                        ?: return current

                current.copy(
                    current =
                        update.observations
                            .fold(
                                available,
                            ) { accumulated, observation ->
                                accumulated
                                    .withSignal(
                                        observation.signal,
                                    )
                            },
                )
            }

            is VexStreamPlayerStoreUpdate
                .ConnectionClosed ->
                if (
                    current.connectionId ==
                    update.connectionId
                ) {
                    current.copy(
                        connectionId =
                            null,
                        current =
                            VexStreamPlayerPartner.Current
                                .Unavailable,
                    )
                } else {
                    current
                }
        }

    private fun admit(
        update:
            VexStreamPlayerStoreUpdate,
    ):
        VexStreamPlayerRetainedCurrent =
        synchronized(
            lock,
        ) {
            val current =
                mutableRetainedStates.value
            val next =
                converge(
                    update =
                        update,
                    current =
                        current,
                )

            if (
                next !=
                current
            ) {
                mutableRetainedStates.value =
                    next
            }

            mutableRetainedStates.value
        }

    private fun admitAndReport(
        update:
            VexStreamPlayerStoreUpdate,
        expectedConnectionId:
            VexStreamPlayerPartner.Connection.Id,
    ):
        Admission {
        val after =
            admit(
                update,
            )

        return Admission(
            accepted =
                after.connectionId ==
                    expectedConnectionId,
            current =
                after.current,
        )
    }

    private fun completeCurrent(
        observations:
            List<
                VexStreamPlayerPartner.Connection.Observation
            >,
    ):
        VexStreamPlayerPartner.Current.Available =
        VexStreamPlayerPartner.Current.Available(
            playback =
                observations
                    .lastSignal<
                        VexStreamPlayerPartner.Signal.Playback
                    >()
                    ?: error(
                        "Player Connection open requires Playback Signal",
                    ),
            queueItems =
                observations
                    .lastSignal<
                        VexStreamPlayerPartner.Signal.QueueItems
                    >()
                    ?: error(
                        "Player Connection open requires QueueItems Signal",
                    ),
            queueNavigation =
                observations
                    .lastSignal<
                        VexStreamPlayerPartner.Signal.QueueNavigation
                    >()
                    ?: error(
                        "Player Connection open requires QueueNavigation Signal",
                    ),
            repeat =
                observations
                    .lastSignal<
                        VexStreamPlayerPartner.Signal.Repeat
                    >()
                    ?: error(
                        "Player Connection open requires Repeat Signal",
                    ),
            currentMedia =
                observations
                    .lastSignal<
                        VexStreamPlayerPartner.Signal.CurrentMedia
                    >()
                    ?: error(
                        "Player Connection open requires CurrentMedia Signal",
                    ),
        )
}

private inline fun <
    reified Signal :
        VexStreamPlayerPartner.Signal,
> List<
    VexStreamPlayerPartner.Connection.Observation
>.lastSignal():
    Signal? =
    asReversed()
        .firstNotNullOfOrNull { observation ->
            observation.signal as?
                Signal
        }

private fun List<
    VexStreamPlayerPartner.Connection.Observation
>.requireConnection(
    connectionId:
        VexStreamPlayerPartner.Connection.Id,
) {
    require(
        all { observation ->
            observation.connectionId ==
                connectionId
        },
    ) {
        "One Player provider occurrence cannot mix Connection IDs"
    }
}

private fun VexStreamPlayerPartner.Current.Available.withSignal(
    signal:
        VexStreamPlayerPartner.Signal,
):
    VexStreamPlayerPartner.Current.Available =
    when (
        signal
    ) {
        is VexStreamPlayerPartner.Signal.Playback ->
            copy(
                playback =
                    signal,
            )

        is VexStreamPlayerPartner.Signal.QueueItems ->
            copy(
                queueItems =
                    signal,
            )

        is VexStreamPlayerPartner.Signal.QueueNavigation ->
            copy(
                queueNavigation =
                    signal,
            )

        is VexStreamPlayerPartner.Signal.Repeat ->
            copy(
                repeat =
                    signal,
            )

        is VexStreamPlayerPartner.Signal.CurrentMedia ->
            copy(
                currentMedia =
                    signal,
            )
    }

internal class VexStreamPlayerObservationReceiver(
    private val store:
        VexStreamPlayerStore,
) :
    VexPartner.Connection.Receiver<
        VexStreamPlayerPartner,
        VexStreamPlayerPartner.Signal,
        VexStreamPlayerPartner.Connection.Observation
    > {
    suspend fun connectionOpened(
        connectionId:
            VexStreamPlayerPartner.Connection.Id,
        observations:
            List<
                VexStreamPlayerPartner.Connection.Observation
            >,
    ) {
        store.connectionOpened(
            connectionId =
                connectionId,
            observations =
                observations,
        )
    }

    suspend fun receive(
        observations:
            List<
                VexStreamPlayerPartner.Connection.Observation
            >,
    ) {
        store.receive(
            observations,
        )
    }

    override suspend fun receive(
        observation:
            VexStreamPlayerPartner.Connection.Observation,
    ) {
        receive(
            listOf(
                observation,
            ),
        )
    }

    suspend fun connectionClosed(
        connectionId:
            VexStreamPlayerPartner.Connection.Id,
    ) {
        store.connectionClosed(
            connectionId,
        )
    }
}

// [VXG RealForever][VEXSTREAM][PLAYER-STORE]
