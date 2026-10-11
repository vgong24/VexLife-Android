package com.vextreme.vexlife.android.data.events

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Session-scoped Debug fixture controller.
 *
 * Start/stop owns only the synthetic producer coroutine. The real normalized Events repository keeps
 * already-admitted event truth. Restarting this controller reuses the same source instance so its
 * source revision continues instead of replaying revision 1 over newer retained truth.
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
}

// [VXG RealForever]
