package com.vextreme.vexlife.android.data.events

import kotlinx.coroutines.delay

/**
 * Deterministic outside-world fixture used to exercise Events without coupling the feature to one
 * real SDK/service.
 *
 * The timed loop is only one control surface. Manual methods below let Debug/Design-Lab work provoke
 * specific conditions immediately:
 *
 * - [emitNext] -> one ordinary new provider event;
 * - [emitBurst] -> enough rows to exercise scrolling/visibility;
 * - [insertHistorical] -> insert an older event without changing existing event identity;
 * - [removeOldest] -> provider-removal condition for candidate advancement;
 * - [resetCurrent] -> clear the provider projection while preserving monotonic source/event identity.
 *
 * All mutations share one source revision and one lock, so timed and manual play cannot fork the
 * provider into contradictory histories.
 */
class SyntheticExternalEventSource(
    private val sink: (VexExternalEventSourceSnapshot) -> Unit,
    private val intervalMillis: Long = DEFAULT_INTERVAL_MILLIS,
    private val clock: () -> Long = System::currentTimeMillis,
    private val maxEvents: Int = DEFAULT_MAX_EVENTS,
) {
    init {
        require(intervalMillis > 0L) {
            "synthetic external event interval must be positive"
        }
        require(maxEvents > 0) {
            "synthetic external event maxEvents must be positive"
        }
    }

    private val lock = Any()
    private var timedEmissionEnabled = false
    private var sourceRevision = 0L
    private var eventOrdinal = 0L
    private var templateCursor = 0
    private val emitted = mutableListOf<VexExternalEvent>()

    suspend fun runForever() {
        while (true) {
            delay(intervalMillis)

            val snapshot =
                synchronized(lock) {
                    if (!timedEmissionEnabled) {
                        return
                    }

                    nextSnapshotLocked()
                }

            publish(snapshot)
        }
    }

    fun setTimedEmissionEnabled(enabled: Boolean) {
        synchronized(lock) {
            timedEmissionEnabled = enabled
        }
    }

    fun emitNext(): VexExternalEventSourceSnapshot =
        publish(
            synchronized(lock) {
                nextSnapshotLocked()
            },
        )

    private fun nextSnapshotLocked(): VexExternalEventSourceSnapshot {
        sourceRevision += 1L
        eventOrdinal += 1L

        val template =
            templates[
                templateCursor % templates.size
            ]
        templateCursor += 1

        emitted +=
            VexExternalEvent(
                eventRef =
                    "event.synthetic.external." +
                        eventOrdinal.toString().padStart(4, '0'),
                sourceRef = SOURCE_REF,
                kind = template.kind,
                title = template.title,
                detail = template.detail,
                occurredAtEpochMillis = clock(),
            )

        trimToBoundLocked()
        return snapshotLocked()
    }

    fun emitBurst(
        count: Int = DEFAULT_BURST_COUNT,
    ): VexExternalEventSourceSnapshot {
        require(count > 0) {
            "synthetic event burst count must be positive"
        }

        var latest: VexExternalEventSourceSnapshot? = null
        repeat(count) {
            latest = emitNext()
        }
        return requireNotNull(latest)
    }

    /**
     * Adds a newly discovered provider item whose occurrence time predates the current list.
     *
     * This is intentionally not "reorder the UI". The provider supplies a newer snapshot containing
     * a stable new event with an older occurrence time; Events sorting naturally inserts it into an
     * earlier chronological position. Existing event refs remain unchanged.
     */
    fun insertHistorical(): VexExternalEventSourceSnapshot =
        publish(
            synchronized(lock) {
                sourceRevision += 1L
                eventOrdinal += 1L

                val oldestTime =
                    emitted.minOfOrNull {
                        it.occurredAtEpochMillis
                    } ?: clock()

                emitted +=
                    VexExternalEvent(
                        eventRef =
                            "event.synthetic.external.historical." +
                                eventOrdinal.toString().padStart(4, '0'),
                        sourceRef = SOURCE_REF,
                        kind = VexExternalEventKind.SYSTEM,
                        title = "Historical provider insert",
                        detail =
                            "A newer provider snapshot discovered an older event. Existing rows " +
                                "must keep their semantic identity while list order changes.",
                        occurredAtEpochMillis =
                            (oldestTime - 1_000L)
                                .coerceAtLeast(0L),
                    )

                trimToBoundLocked()
                snapshotLocked()
            },
        )

    /**
     * Removes whichever current event has the oldest occurrence time.
     *
     * A removal advances sourceRevision because the provider's current projection changed. The
     * returned null means the source had no current events and therefore emitted no new snapshot.
     */
    fun removeOldest(): VexExternalEventSourceSnapshot? {
        val snapshot =
            synchronized(lock) {
                val oldestIndex =
                    emitted.indices.minByOrNull { index ->
                        emitted[index].occurredAtEpochMillis
                    } ?: return@synchronized null

                sourceRevision += 1L
                emitted.removeAt(oldestIndex)
                snapshotLocked()
            }

        return snapshot?.let(::publish)
    }

    /**
     * Clears only this provider's current projection.
     *
     * Revision/event ordinals remain monotonic so Reset Lab is repeatable without teaching a real
     * adapter that reconnect/reset means "pretend the provider went back to revision 1".
     */
    fun resetCurrent(): VexExternalEventSourceSnapshot =
        publish(
            synchronized(lock) {
                sourceRevision += 1L
                emitted.clear()
                snapshotLocked()
            },
        )

    private fun trimToBoundLocked() {
        while (emitted.size > maxEvents) {
            // Bound by admission order rather than occurrence time so a deliberate historical insert
            // remains visible long enough to test list reordering.
            emitted.removeAt(0)
        }
    }

    private fun snapshotLocked(): VexExternalEventSourceSnapshot =
        VexExternalEventSourceSnapshot(
            sourceRef = SOURCE_REF,
            sourceRevision = sourceRevision,
            events = emitted.toList(),
        )

    private fun publish(
        snapshot: VexExternalEventSourceSnapshot,
    ): VexExternalEventSourceSnapshot {
        sink(snapshot)
        return snapshot
    }

    private data class Template(
        val kind: VexExternalEventKind,
        val title: String,
        val detail: String,
    )

    companion object {
        const val SOURCE_REF = "source.synthetic.external-information"
        const val DEFAULT_INTERVAL_MILLIS = 5_000L
        const val DEFAULT_MAX_EVENTS = 24
        const val DEFAULT_BURST_COUNT = 12

        private val templates =
            listOf(
                Template(
                    VexExternalEventKind.MESSAGE,
                    "Synthetic message",
                    "A message-like external update arrived outside the current Screen.",
                ),
                Template(
                    VexExternalEventKind.CALL,
                    "Synthetic call",
                    "A call-like external update arrived outside the current Screen.",
                ),
                Template(
                    VexExternalEventKind.MAP,
                    "Synthetic map update",
                    "A map-like route/context update arrived from an external source.",
                ),
                Template(
                    VexExternalEventKind.MEDIA,
                    "Synthetic media update",
                    "A media-like external state change arrived from another service.",
                ),
            )
    }
}

// [VXG RealForever]
