package com.vextreme.vexlife.android.data.events

import com.vextreme.vexlife.android.relay.VexRelay
import com.vextreme.vexlife.android.relay.VexState
import kotlin.random.Random
import kotlinx.coroutines.flow.Flow

enum class EventsAttentionLabMode {
    ROTATION,
    SEEDED_RANDOM,
}

enum class EventsAttentionLabScenarioKind {
    ANCHORED_TOP,
    ANCHORED_MIDDLE,
    ANCHORED_BOTTOM,
    BLOCKING_FREEZE_TOP,
}

enum class EventsAttentionLabPlacement {
    ABOVE,
    BELOW,
}

enum class EventsAttentionLabExpectedAction {
    ACKNOWLEDGE,
    LATER,
    SUSPEND,
    SUPPRESS,
}

data class EventsAttentionLabScenario(
    val scenarioRef: String,
    val ordinal: Long,
    val mode: EventsAttentionLabMode,
    val seed: Long,
    val kind: EventsAttentionLabScenarioKind,
    val targetEventRef: String?,
    val targetPositionFromTop: Int?,
    val listCountAtPlan: Int,
    val preferredPlacement: EventsAttentionLabPlacement,
    val expectedAction: EventsAttentionLabExpectedAction,
    val freezeProjection: Boolean,
    val blockingAcknowledgeOnly: Boolean,
) {
    init {
        require(scenarioRef.isNotBlank())
        require(ordinal > 0L)
        require(listCountAtPlan >= 0)
        require(
            targetPositionFromTop == null ||
                targetPositionFromTop in 0 until listCountAtPlan,
        )
    }
}

data class EventsAttentionLabState(
    val mode: EventsAttentionLabMode =
        EventsAttentionLabMode.ROTATION,
    val seed: Long = DEFAULT_SEED,
    val nextOrdinal: Long = 1L,
    val currentScenario: EventsAttentionLabScenario? = null,
) : VexState<EventsAttentionLabState> {
    companion object {
        const val DEFAULT_SEED = 24_101_126L
    }
}

interface EventsAttentionLabRepository {
    val states: Flow<EventsAttentionLabState>
    val current: EventsAttentionLabState

    fun useRotation()
    fun useSeededRandom(seed: Long)
    fun planNext(events: List<VexExternalEvent>): EventsAttentionLabScenario
    fun completeCurrent()
    fun reset()
}

class RelayEventsAttentionLabRepository :
    EventsAttentionLabRepository {
    private val relay =
        EventsAttentionLabRelay()

    override val states: Flow<EventsAttentionLabState>
        get() = relay.states

    override val current: EventsAttentionLabState
        get() = relay.current

    override fun useRotation() {
        relay.updateMode(
            mode = EventsAttentionLabMode.ROTATION,
            seed = EventsAttentionLabState.DEFAULT_SEED,
        )
    }

    override fun useSeededRandom(seed: Long) {
        relay.updateMode(
            mode = EventsAttentionLabMode.SEEDED_RANDOM,
            seed = seed,
        )
    }

    override fun planNext(
        events: List<VexExternalEvent>,
    ): EventsAttentionLabScenario =
        relay.planNext(events)

    override fun completeCurrent() {
        relay.completeCurrent()
    }

    override fun reset() {
        relay.reset()
    }
}

private class EventsAttentionLabRelay :
    VexRelay<EventsAttentionLabState>(
        relayRef = "relay.events.attention-lab",
        initialState = EventsAttentionLabState(),
    ) {
    fun updateMode(
        mode: EventsAttentionLabMode,
        seed: Long,
    ) {
        updateCurrent { current ->
            current.copy(
                mode = mode,
                seed = seed,
                nextOrdinal = 1L,
                currentScenario = null,
            )
        }
    }

    fun planNext(
        events: List<VexExternalEvent>,
    ): EventsAttentionLabScenario {
        /*
         * Plan + advance are one atomic current -> next transition.
         *
         * Do not read relay.current before updateCurrent when that read decides the next ordinal:
         * two concurrent callers could otherwise both plan from the same stale ordinal and only
         * serialize the later writes.
         */
        val nextState =
            updateCurrent { current ->
                val scenario =
                    planEventsAttentionLabScenario(
                        mode = current.mode,
                        seed = current.seed,
                        ordinal = current.nextOrdinal,
                        events = events,
                    )

                current.copy(
                    nextOrdinal = current.nextOrdinal + 1L,
                    currentScenario = scenario,
                )
            }

        return checkNotNull(
            nextState.currentScenario,
        )
    }

    fun completeCurrent() {
        updateCurrent { current ->
            current.copy(
                currentScenario = null,
            )
        }
    }

    fun reset() {
        updateCurrent { current ->
            EventsAttentionLabState(
                mode = current.mode,
                seed = current.seed,
            )
        }
    }
}

private fun foldedRandomSeed(
    seed: Long,
    ordinal: Long,
): Int {
    val mixed =
        seed xor
            (ordinal * 0x5DEECE66DL)

    return (
        mixed xor
            (mixed ushr 32)
        ).toInt()
}

private data class EventsAttentionLabTemplate(
    val kind: EventsAttentionLabScenarioKind,
    val placement: EventsAttentionLabPlacement,
    val expectedAction: EventsAttentionLabExpectedAction,
)

internal fun planEventsAttentionLabScenario(
    mode: EventsAttentionLabMode,
    seed: Long,
    ordinal: Long,
    events: List<VexExternalEvent>,
): EventsAttentionLabScenario {
    require(ordinal > 0L)

    val templates =
        listOf(
            EventsAttentionLabTemplate(
                kind = EventsAttentionLabScenarioKind.ANCHORED_TOP,
                placement = EventsAttentionLabPlacement.BELOW,
                expectedAction = EventsAttentionLabExpectedAction.ACKNOWLEDGE,
            ),
            EventsAttentionLabTemplate(
                kind = EventsAttentionLabScenarioKind.ANCHORED_MIDDLE,
                placement = EventsAttentionLabPlacement.ABOVE,
                expectedAction = EventsAttentionLabExpectedAction.LATER,
            ),
            EventsAttentionLabTemplate(
                kind = EventsAttentionLabScenarioKind.ANCHORED_BOTTOM,
                placement = EventsAttentionLabPlacement.ABOVE,
                expectedAction = EventsAttentionLabExpectedAction.SUSPEND,
            ),
            EventsAttentionLabTemplate(
                kind = EventsAttentionLabScenarioKind.ANCHORED_MIDDLE,
                placement = EventsAttentionLabPlacement.BELOW,
                expectedAction = EventsAttentionLabExpectedAction.SUPPRESS,
            ),
            EventsAttentionLabTemplate(
                kind = EventsAttentionLabScenarioKind.BLOCKING_FREEZE_TOP,
                placement = EventsAttentionLabPlacement.BELOW,
                expectedAction = EventsAttentionLabExpectedAction.ACKNOWLEDGE,
            ),
        )

    val template =
        when (mode) {
            EventsAttentionLabMode.ROTATION ->
                templates[
                    ((ordinal - 1L) % templates.size)
                        .toInt()
                ]

            EventsAttentionLabMode.SEEDED_RANDOM ->
                templates[
                    Random(
                        foldedRandomSeed(
                            seed = seed,
                            ordinal = ordinal,
                        ),
                    ).nextInt(templates.size)
                ]
        }

    val kind =
        template.kind

    val targetPosition =
        if (events.isEmpty()) {
            null
        } else {
            when (kind) {
                EventsAttentionLabScenarioKind.ANCHORED_TOP,
                EventsAttentionLabScenarioKind.BLOCKING_FREEZE_TOP,
                -> 0

                EventsAttentionLabScenarioKind.ANCHORED_MIDDLE ->
                    (events.lastIndex / 2)

                EventsAttentionLabScenarioKind.ANCHORED_BOTTOM ->
                    events.lastIndex
            }
        }

    val placement =
        template.placement

    val target =
        targetPosition?.let(events::get)

    return EventsAttentionLabScenario(
        scenarioRef =
            buildString {
                append("scenario.events.attention.")
                append(mode.name.lowercase())
                if (mode == EventsAttentionLabMode.SEEDED_RANDOM) {
                    append(".seed-")
                    append(seed)
                }
                append(".")
                append(ordinal.toString().padStart(3, '0'))
                append(".")
                append(kind.name.lowercase())
            },
        ordinal = ordinal,
        mode = mode,
        seed = seed,
        kind = kind,
        targetEventRef = target?.eventRef,
        targetPositionFromTop = targetPosition,
        listCountAtPlan = events.size,
        preferredPlacement = placement,
        expectedAction = template.expectedAction,
        freezeProjection =
            kind ==
                EventsAttentionLabScenarioKind.BLOCKING_FREEZE_TOP,
        blockingAcknowledgeOnly =
            kind ==
                EventsAttentionLabScenarioKind.BLOCKING_FREEZE_TOP,
    )
}

// [VXG RealForever]
