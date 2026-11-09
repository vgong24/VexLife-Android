package com.vextreme.vexlife.android.vexstream.player

import com.vextreme.vexlife.android.behavior.VexPartner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal class VexStreamPlayerStore {
    /**
     * Retained outside truth assembled from exact Player Partner Signals.
     *
     * Partner owns Source / Signal / Connection vocabulary. Store owns the complete retained
     * snapshot consumers can observe after those Signals have been admitted.
     */
    sealed interface Snapshot {
        data object Unavailable :
            Snapshot

        data class Available(
            val playback:
                VexStreamPlayerPartner.Signal.Playback,
            val queueItems:
                VexStreamPlayerPartner.Signal.QueueItems,
            val queueNavigation:
                VexStreamPlayerPartner.Signal.QueueNavigation,
            val repeat:
                VexStreamPlayerPartner.Signal.Repeat,
            val currentMedia:
                VexStreamPlayerPartner.Signal.CurrentMedia,
        ) :
            Snapshot {
            val queue:
                QueueSnapshot
                get() =
                    QueueSnapshot(
                        items =
                            queueItems,
                        navigation =
                            queueNavigation,
                    )
        }
    }

    data class QueueSnapshot(
        val items:
            VexStreamPlayerPartner.Signal.QueueItems,
        val navigation:
            VexStreamPlayerPartner.Signal.QueueNavigation,
    ) {
        val reorderAvailable:
            Boolean
            get() =
                items.mediaIds.size >
                    1 &&
                    navigation.changeMediaItemsAvailable
    }

    private sealed interface Update {
        data class ConnectionOpened(
            val connectionId:
                VexStreamPlayerPartner.Connection.Id,
            val observations:
                List<
                    VexStreamPlayerPartner.Connection.Observation
                >,
        ) :
            Update

        data class Observed(
            val observations:
                List<
                    VexStreamPlayerPartner.Connection.Observation
                >,
        ) :
            Update

        data class ConnectionClosed(
            val connectionId:
                VexStreamPlayerPartner.Connection.Id,
        ) :
            Update
    }

    private data class Retained(
        val latestConnectionOccurrence:
            Long =
            0L,
        val connectionId:
            VexStreamPlayerPartner.Connection.Id?,
        val snapshot:
            Snapshot,
    )

    private val lock =
        Any()

    private val mutableRetainedStates =
        MutableStateFlow(
            Retained(
                connectionId =
                    null,
                snapshot =
                    Snapshot.Unavailable,
            ),
        )

    private val retainedStates:
        StateFlow<
            Retained
        > =
        mutableRetainedStates
            .asStateFlow()

    private val retainedCurrent:
        Retained
        get() =
            retainedStates.value

    val states:
        Flow<
            Snapshot
        > =
        retainedStates
            .map { retained ->
                retained.snapshot
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
                        Snapshot.Available
                )
                    ?.playback
            }
            .distinctUntilChanged()

    val queueCurrents:
        Flow<
            QueueSnapshot?
        > =
        states
            .map { current ->
                (
                    current as?
                        Snapshot.Available
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
                        Snapshot.Available
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
                        Snapshot.Available
                )
                    ?.currentMedia
            }
            .distinctUntilChanged()

    val current:
        Snapshot
        get() =
            retainedCurrent
                .snapshot

    val currentQueue:
        QueueSnapshot?
        get() =
            (
                current as?
                    Snapshot.Available
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
        Boolean {
        val after =
            admit(
                Update.ConnectionOpened(
                    connectionId =
                        connectionId,
                    observations =
                        observations,
                ),
            )

        return after.connectionId ==
            connectionId
    }

    internal fun receive(
        observations:
            List<
                VexStreamPlayerPartner.Connection.Observation
            >,
    ):
        Boolean {
        if (
            observations.isEmpty()
        ) {
            return false
        }

        val connectionId =
            observations
                .first()
                .connectionId
        val after =
            admit(
                Update.Observed(
                    observations,
                ),
            )

        return after.connectionId ==
            connectionId
    }

    internal fun connectionClosed(
        connectionId:
            VexStreamPlayerPartner.Connection.Id,
    ):
        Boolean {
        val before =
            retainedCurrent
        val after =
            admit(
                Update.ConnectionClosed(
                    connectionId,
                ),
            )

        return before.connectionId ==
            connectionId &&
            after.connectionId ==
                null
    }

    private fun converge(
        update:
            Update,
        current:
            Retained,
    ):
        Retained =
        when (
            update
        ) {
            is Update
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

                Retained(
                    latestConnectionOccurrence =
                        update.connectionId
                            .occurrence,
                    connectionId =
                        update.connectionId,
                    snapshot =
                        completeSnapshot(
                            update.observations,
                        ),
                )
            }

            is Update
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
                    current.snapshot as?
                        Snapshot.Available
                        ?: return current

                current.copy(
                    snapshot =
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

            is Update
                .ConnectionClosed ->
                if (
                    current.connectionId ==
                    update.connectionId
                ) {
                    current.copy(
                        connectionId =
                            null,
                        snapshot =
                            Snapshot.Unavailable,
                    )
                } else {
                    current
                }
        }

    private fun admit(
        update:
            Update,
    ):
        Retained =
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

    private fun completeSnapshot(
        observations:
            List<
                VexStreamPlayerPartner.Connection.Observation
            >,
    ):
        Snapshot.Available =
        Snapshot.Available(
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

private fun VexStreamPlayerStore.Snapshot.Available.withSignal(
    signal:
        VexStreamPlayerPartner.Signal,
):
    VexStreamPlayerStore.Snapshot.Available =
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
