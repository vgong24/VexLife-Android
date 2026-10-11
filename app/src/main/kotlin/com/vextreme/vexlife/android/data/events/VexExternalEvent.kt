package com.vextreme.vexlife.android.data.events

import com.vextreme.vexlife.android.relay.VexDeepCopyable
import com.vextreme.vexlife.android.relay.VexState

enum class VexExternalEventKind {
    MESSAGE,
    CALL,
    MAP,
    MEDIA,
    SYSTEM,
}

data class VexExternalEvent(
    val eventRef: String,
    val sourceRef: String,
    val kind: VexExternalEventKind,
    val title: String,
    val detail: String,
    val occurredAtEpochMillis: Long,
) : VexDeepCopyable<VexExternalEvent> {
    init {
        requireSemanticRef(eventRef, "eventRef")
        requireSemanticRef(sourceRef, "sourceRef")
        require(title.isNotBlank()) { "external event title must not be blank" }
        require(occurredAtEpochMillis >= 0L) {
            "external event occurredAtEpochMillis must be non-negative"
        }
    }

    override fun deepCopy(): VexExternalEvent = copy()
}

data class VexExternalEventSourceSnapshot(
    val sourceRef: String,
    val sourceRevision: Long,
    val events: List<VexExternalEvent>,
) : VexDeepCopyable<VexExternalEventSourceSnapshot> {
    init {
        requireSemanticRef(sourceRef, "sourceRef")
        require(sourceRevision >= 0L) { "external source revision must be non-negative" }
        require(events.all { event -> event.sourceRef == sourceRef }) {
            "every event in a source snapshot must carry the same sourceRef"
        }
        require(events.map(VexExternalEvent::eventRef).distinct().size == events.size) {
            "external source snapshot contains duplicate eventRef values"
        }
    }

    override fun deepCopy(): VexExternalEventSourceSnapshot =
        copy(events = events.map(VexExternalEvent::deepCopy))
}

data class EventsFeedState(
    val sourceSnapshots: Map<String, VexExternalEventSourceSnapshot> = emptyMap(),
) : VexState<EventsFeedState> {
    val events: List<VexExternalEvent>
        get() =
            sourceSnapshots.values
                .flatMap { snapshot -> snapshot.events }
                .sortedWith(
                    compareByDescending<VexExternalEvent> {
                        it.occurredAtEpochMillis
                    }.thenByDescending {
                        it.eventRef
                    },
                )

    init {
        require(sourceSnapshots.all { (sourceRef, snapshot) ->
            sourceRef == snapshot.sourceRef
        }) {
            "event feed source map key must equal snapshot sourceRef"
        }

        val refs =
            sourceSnapshots.values
                .flatMap { snapshot -> snapshot.events }
                .map(VexExternalEvent::eventRef)

        require(refs.distinct().size == refs.size) {
            "eventRef must be globally unique across current external sources"
        }
    }

    override fun deepCopy(): EventsFeedState =
        copy(
            sourceSnapshots =
                sourceSnapshots.mapValues { (_, snapshot) ->
                    snapshot.deepCopy()
                }.toMap(),
        )
}

internal fun requireSemanticRef(value: String, name: String) {
    require(value.matches(Regex("[A-Za-z0-9][A-Za-z0-9._:/#@+\\-]{0,511}"))) {
        "$name is not a valid semantic ref: $value"
    }
}

// [VXG RealForever]
