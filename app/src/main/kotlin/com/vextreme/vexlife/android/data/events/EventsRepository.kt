package com.vextreme.vexlife.android.data.events

import com.vextreme.vexlife.android.relay.VexRelay
import kotlinx.coroutines.flow.Flow

interface EventsRepository {
    val states: Flow<EventsFeedState>
    val current: EventsFeedState

    fun accept(snapshot: VexExternalEventSourceSnapshot): EventsFeedState
}

class RelayEventsRepository : EventsRepository {
    private val relay =
        EventsFeedRelay()

    override val states: Flow<EventsFeedState>
        get() = relay.states

    override val current: EventsFeedState
        get() = relay.current

    override fun accept(
        snapshot: VexExternalEventSourceSnapshot,
    ): EventsFeedState =
        relay.sourceSnapshotArrived(snapshot)
}

internal class EventsFeedRelay :
    VexRelay<EventsFeedState>(
        relayRef = "relay.events.external-feed",
        initialState = EventsFeedState(),
    ) {
    override fun processState(
        inputState: EventsFeedState,
        currentState: EventsFeedState,
    ): EventsFeedState {
        val next =
            currentState.sourceSnapshots
                .mapValues { (_, snapshot) -> snapshot.deepCopy() }
                .toMutableMap()

        inputState.sourceSnapshots.forEach { (sourceRef, incoming) ->
            val retained = next[sourceRef]

            when {
                retained == null ->
                    next[sourceRef] = incoming.deepCopy()

                incoming.sourceRevision > retained.sourceRevision ->
                    next[sourceRef] = incoming.deepCopy()

                incoming.sourceRevision == retained.sourceRevision &&
                    incoming != retained ->
                    error(
                        "external source reused revision " +
                            incoming.sourceRevision +
                            " with different content: " +
                            sourceRef,
                    )

                else ->
                    Unit
            }
        }

        return EventsFeedState(sourceSnapshots = next.toMap())
    }

    fun sourceSnapshotArrived(
        snapshot: VexExternalEventSourceSnapshot,
    ): EventsFeedState =
        send(
            EventsFeedState(
                sourceSnapshots =
                    mapOf(
                        snapshot.sourceRef to snapshot.deepCopy(),
                    ),
            ),
        )
}

// [VXG RealForever]
