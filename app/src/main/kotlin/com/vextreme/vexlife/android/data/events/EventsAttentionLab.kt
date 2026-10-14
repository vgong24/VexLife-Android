package com.vextreme.vexlife.android.data.events

import com.vextreme.vexlife.android.relay.VexAction
import com.vextreme.vexlife.android.relay.VexActionRelay
import com.vextreme.vexlife.android.relay.VexState
import kotlin.random.Random
import kotlinx.coroutines.flow.Flow

enum class EventsAttentionLabMode {
    ROTATION,
    SEEDED_RANDOM,
}

/**
 * Feature-local variants for the Events developer attention exercise.
 *
 * This is not a shared Scenario framework. It exists only so the Events attention lab can name
 * which overlay-placement/interaction exercise it is asking a developer or test to perform.
 */
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

/**
 * One planned attention-overlay exercise used only by the Events developer lab.
 *
 * IMPORTANT:
 *
 *     EventsAttentionLabScenario
 *     != Relay infrastructure
 *     != generic app Scenario infrastructure
 *     != canonical Events/provider truth
 *
 * It is a feature-local developer/test fixture that answers:
 *
 * - which current Events row should receive the anchored attention overlay;
 * - where that overlay prefers to appear;
 * - which human response this exercise expects;
 * - whether presentation should freeze while the exercise is active.
 *
 * Why this value exists:
 *
 * The lab needs one immutable plan that can travel through the normal feature path without making
 * Compose invent test meaning locally:
 *
 *     developer asks for next lab exercise
 *     -> EventsAttentionLabAction.PlanNext
 *     -> EventsAttentionLabRelay.nextStateFor(...)
 *     -> planEventsAttentionLabScenario(...)
 *     -> EventsAttentionLabState.currentScenario
 *     -> EventsConvergenceRelay observes lab state
 *     -> EventsScreen renders the requested anchored exercise
 *
 * During ordinary Events use, [EventsAttentionLabState.currentScenario] is null and none of this
 * exercise metadata changes canonical event truth.
 *
 * The name "Scenario" is retained here for current repository compatibility. Treat it as
 * "one Events attention-lab exercise", not as a reusable Scenario subsystem.
 */
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

    /**
     * Active developer-lab exercise only.
     *
     * null means no attention-lab exercise is currently imposed on Events presentation.
     * This is not the current Event, current candidate, or generic app scenario.
     */
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

    /**
     * Plan and commit one next Events attention-lab exercise against current event truth.
     *
     * The return value is the exact exercise committed by this call. Returning that feature-local
     * value is a convenience for Debug orchestration that may need to react to exercise metadata
     * (for example freezeProjection); it is not a generic VexActionRelay result contract.
     */
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

private sealed interface EventsAttentionLabAction :
    VexAction {
    data class UseMode(
        val mode: EventsAttentionLabMode,
        val seed: Long,
    ) : EventsAttentionLabAction

    data class PlanNext(
        val events: List<VexExternalEvent>,
    ) : EventsAttentionLabAction

    data object CompleteCurrent :
        EventsAttentionLabAction

    data object Reset :
        EventsAttentionLabAction
}

/**
 * Action-driven owner of Events attention-lab state.
 *
 * Every state-changing intention enters as one [EventsAttentionLabAction] and reaches exactly one
 * reducer: [nextStateFor]. Public methods below only name/form the action; they never read/copy/mutate
 * current state themselves.
 */
private class EventsAttentionLabRelay :
    VexActionRelay<
        EventsAttentionLabAction,
        EventsAttentionLabState
    >(
        relayRef = "relay.events.attention-lab",
        initialState = EventsAttentionLabState(),
    ) {
    fun updateMode(
        mode: EventsAttentionLabMode,
        seed: Long,
    ) {
        sendAction(
            EventsAttentionLabAction.UseMode(
                mode = mode,
                seed = seed,
            ),
        )
    }

    /**
     * Ask the Events developer lab to form and retain one next overlay exercise.
     *
     * Callers provide current event truth only; they do not construct Scenario state themselves.
     * The named PlanNext action is the only entry, and [nextStateFor] is the only state transition.
     */
    fun planNext(
        events: List<VexExternalEvent>,
    ): EventsAttentionLabScenario {
        val committedState =
            sendAction(
                EventsAttentionLabAction.PlanNext(
                    // The action is immutable by contract; detach the caller-owned list once here.
                    events =
                        events.map(
                            VexExternalEvent::deepCopy,
                        ),
                ),
            )

        /*
         * sendAction() returns the post-transition State. PlanNext's reducer contract always stores
         * the exercise it just planned in currentScenario, so this method projects that one
         * domain-specific result back to its caller.
         *
         * Higher layers should call planNext(); they should not inspect Relay State to recover this.
         */
        return checkNotNull(
            committedState.currentScenario,
        ) {
            "PlanNext must commit one current Events attention-lab exercise"
        }
    }

    fun completeCurrent() {
        sendAction(
            EventsAttentionLabAction.CompleteCurrent,
        )
    }

    fun reset() {
        sendAction(
            EventsAttentionLabAction.Reset,
        )
    }

    /**
     * The only action -> next-state path for this Relay.
     *
     * No caller supplies "the next State" and no caller receives an anonymous current-state lambda.
     * If the lab gains a new behavior, add one action above and one exhaustive branch here.
     */
    override fun nextStateFor(
        action: EventsAttentionLabAction,
        currentState: EventsAttentionLabState,
    ): EventsAttentionLabState =
        when (action) {
            is EventsAttentionLabAction.UseMode ->
                currentState.copy(
                    mode = action.mode,
                    seed = action.seed,
                    nextOrdinal = 1L,
                    currentScenario = null,
                )

            is EventsAttentionLabAction.PlanNext -> {
                val scenario =
                    planEventsAttentionLabScenario(
                        mode = currentState.mode,
                        seed = currentState.seed,
                        ordinal = currentState.nextOrdinal,
                        events = action.events,
                    )

                currentState.copy(
                    nextOrdinal = currentState.nextOrdinal + 1L,
                    currentScenario = scenario,
                )
            }

            EventsAttentionLabAction.CompleteCurrent ->
                currentState.copy(
                    currentScenario = null,
                )

            EventsAttentionLabAction.Reset ->
                EventsAttentionLabState(
                    mode = currentState.mode,
                    seed = currentState.seed,
                )
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

/**
 * Stable scenario vocabulary.
 *
 * Keep the declarative choices together so the planner below reads as "select -> derive -> project"
 * rather than rebuilding configuration while also reasoning about one scenario.
 */
private val eventsAttentionLabTemplates =
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

/**
 * Pure planner for one Events attention-lab overlay exercise.
 *
 * This is feature-local calculation, not shared infrastructure. It does not read Relay current state
 * and does not mutate Relay state. EventsAttentionLabRelay.planNext() supplies one exact current
 * mode/seed/ordinal from one exact current State inside the action reducer, then atomically retains
 * the returned exercise in EventsAttentionLabState.currentScenario.
 *
 * Read in order:
 *
 * 1. VALIDATE -> reject impossible planning inputs;
 * 2. SELECT   -> choose one stable scenario template;
 * 3. DERIVE   -> resolve target position/target from current events;
 * 4. PROJECT  -> construct one complete immutable scenario;
 * 5. RETURN   -> hand the value back to the atomic Relay transition.
 */
internal fun planEventsAttentionLabScenario(
    mode: EventsAttentionLabMode,
    seed: Long,
    ordinal: Long,
    events: List<VexExternalEvent>,
): EventsAttentionLabScenario {
    // 1. VALIDATE
    require(ordinal > 0L)

    // 2. SELECT
    val template =
        when (mode) {
            EventsAttentionLabMode.ROTATION ->
                eventsAttentionLabTemplates[
                    ((ordinal - 1L) % eventsAttentionLabTemplates.size)
                        .toInt()
                ]

            EventsAttentionLabMode.SEEDED_RANDOM ->
                eventsAttentionLabTemplates[
                    Random(
                        foldedRandomSeed(
                            seed = seed,
                            ordinal = ordinal,
                        ),
                    ).nextInt(eventsAttentionLabTemplates.size)
                ]
        }

    // 3. DERIVE
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
                    events.lastIndex / 2

                EventsAttentionLabScenarioKind.ANCHORED_BOTTOM ->
                    events.lastIndex
            }
        }
    val target =
        targetPosition?.let(events::get)

    // 4. PROJECT
    val scenario =
        EventsAttentionLabScenario(
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
            preferredPlacement = template.placement,
            expectedAction = template.expectedAction,
            freezeProjection =
                kind ==
                    EventsAttentionLabScenarioKind.BLOCKING_FREEZE_TOP,
            blockingAcknowledgeOnly =
                kind ==
                    EventsAttentionLabScenarioKind.BLOCKING_FREEZE_TOP,
        )

    // 5. RETURN
    return scenario
}

// [VXG RealForever]
