package com.vextreme.vexlife.android.data.events

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Session-scoped Debug fixture controller.
 *
 * Start/stop owns only the synthetic producer coroutine. Manual controls intentionally use the same
 * source instance, revision stream, and repository ingress as the timed producer so live play does
 * not exercise a second fake architecture.
 *
 * Useful human/debug conditions:
 *
 * - [emitNext] -> immediate single external arrival;
 * - [emitBurst] -> rapidly fill enough rows to exercise Lazy list reachability;
 * - [insertHistorical] -> list insertion/reorder while an attention candidate is retained;
 * - [removeOldest] -> provider-removal candidate advancement;
 * - [start]/[stop] -> asynchronous outside-world arrival while another Screen is current.
 */
class SyntheticExternalEventController(
    repository: EventsRepository,
    private val scope: CoroutineScope,
    sourceFactory: (
        sink: (VexExternalEventSourceSnapshot) -> Unit,
    ) -> SyntheticExternalEventSource = { sink ->
        SyntheticExternalEventSource(sink = sink)
    },
) {
    private val lock = Any()
    private val source =
        sourceFactory(repository::accept)

    private var job: Job? = null

    fun start() {
        synchronized(lock) {
            if (job != null) return

            val launched =
                scope.launch {
                    source.runForever()
                }

            job = launched

            launched.invokeOnCompletion {
                synchronized(lock) {
                    if (job === launched) {
                        job = null
                    }
                }
            }
        }
    }

    fun stop() {
        synchronized(lock) {
            job?.cancel()
        }
    }

    fun emitNext(): VexExternalEventSourceSnapshot =
        source.emitNext()

    fun emitBurst(
        count: Int = SyntheticExternalEventSource.DEFAULT_BURST_COUNT,
    ): VexExternalEventSourceSnapshot =
        source.emitBurst(count)

    fun insertHistorical(): VexExternalEventSourceSnapshot =
        source.insertHistorical()

    fun removeOldest(): VexExternalEventSourceSnapshot? =
        source.removeOldest()
}

// [VXG RealForever]
