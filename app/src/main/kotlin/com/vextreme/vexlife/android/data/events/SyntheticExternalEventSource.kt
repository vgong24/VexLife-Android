package com.vextreme.vexlife.android.data.events

import kotlinx.coroutines.delay

class SyntheticExternalEventSource(
    private val sink: (VexExternalEventSourceSnapshot) -> Unit,
    private val intervalMillis: Long = DEFAULT_INTERVAL_MILLIS,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    init {
        require(intervalMillis > 0L) {
            "synthetic external event interval must be positive"
        }
    }

    private var sourceRevision = 0L
    private val emitted = mutableListOf<VexExternalEvent>()

    suspend fun runForever() {
        while (true) {
            delay(intervalMillis)

            sourceRevision += 1L
            val template =
                templates[
                    ((sourceRevision - 1L) % templates.size).toInt()
                ]

            emitted +=
                VexExternalEvent(
                    eventRef =
                        "event.synthetic.external." +
                            sourceRevision.toString().padStart(4, '0'),
                    sourceRef = SOURCE_REF,
                    kind = template.kind,
                    title = template.title,
                    detail = template.detail,
                    occurredAtEpochMillis = clock(),
                )

            sink(
                VexExternalEventSourceSnapshot(
                    sourceRef = SOURCE_REF,
                    sourceRevision = sourceRevision,
                    events = emitted.toList(),
                ),
            )
        }
    }

    private data class Template(
        val kind: VexExternalEventKind,
        val title: String,
        val detail: String,
    )

    companion object {
        const val SOURCE_REF = "source.synthetic.external-information"
        const val DEFAULT_INTERVAL_MILLIS = 5_000L

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
