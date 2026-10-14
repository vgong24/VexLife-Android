package com.vextreme.vexlife.android.data.events

import com.vextreme.vexlife.android.relay.VexAction
import com.vextreme.vexlife.android.relay.VexActionRelay
import com.vextreme.vexlife.android.relay.VexState
import kotlin.random.Random
import kotlinx.coroutines.flow.Flow

enum class EventsDebugMenuAlertMode {
    ROTATION,
    SEEDED_RANDOM,
}

/**
 * Feature-local variants for the Events developer alert case.
 *
 * This is not a shared AlertCase framework. It exists only so the Events Debug-menu alert cases can name
 * which overlay-placement/interaction case it is asking a developer or test to perform.
 */
enum class EventsDebugMenuAlertCaseType {
    ANCHORED_TOP,
    ANCHORED_MIDDLE,
    ANCHORED_BOTTOM,
    BLOCKING_FREEZE_TOP,
}

enum class EventsDebugMenuAlertPlacement {
    ABOVE,
    BELOW,
}

enum class EventsDebugMenuAlertExpectedAction {
    ACKNOWLEDGE,
    LATER,
    SUSPEND,
    SUPPRESS,
}

/**
 * One planned alert-overlay case used only by the Events Debug menu.
 *
 * IMPORTANT:
 *
 *     EventsDebugMenuAlertCase
 *     != Relay infrastructure
 *     != generic app alert-case infrastructure
 *     != canonical Events/provider truth
 *
 * It is a feature-local developer/test fixture that answers:
 *
 * - which current Events row should receive the anchored alert overlay;
 * - where that overlay prefers to appear;
 * - which human response this case expects;
 * - whether presentation should freeze while the case is active.
 *
 * Why this value exists:
 *
 * The Debug menu needs one immutable plan that can travel through the normal feature path without making
 * Compose invent test meaning locally:
 *
 *     developer asks for next alert case
 *     -> EventsDebugMenuAlertAction.PrepareNextAlertCase
 *     -> EventsDebugMenuAlertRelay.nextStateFor(...)
 *     -> buildEventsDebugMenuAlertCase(...)
 *     -> EventsDebugMenuAlertState.currentAlertCase
 *     -> EventsConvergenceRelay observes Debug-menu alert state
 *     -> EventsScreen renders the requested anchored case
 *
 * During ordinary Events use, [EventsDebugMenuAlertState.currentAlertCase] is null and none of this
 * case metadata changes canonical event truth.
 *
 * The name "AlertCase" is retained here for current repository compatibility. Treat it as
 * "one Events Debug-menu alert case", not as a reusable AlertCase subsystem.
 */
data class EventsDebugMenuAlertCase(
    val alertCaseRef: String,
    val ordinal: Long,
    val mode: EventsDebugMenuAlertMode,
    val seed: Long,
    val type: EventsDebugMenuAlertCaseType,
    val targetEventRef: String?,
    val targetPositionFromTop: Int?,
    val listCountAtPlan: Int,
    val preferredPlacement: EventsDebugMenuAlertPlacement,
    val expectedAction: EventsDebugMenuAlertExpectedAction,
    val freezeProjection: Boolean,
    val blockingAcknowledgeOnly: Boolean,
) {
    init {
        require(alertCaseRef.isNotBlank())
        require(ordinal > 0L)
        require(listCountAtPlan >= 0)
        require(
            targetPositionFromTop == null ||
                targetPositionFromTop in 0 until listCountAtPlan,
        )
    }
}

data class EventsDebugMenuAlertState(
    val mode: EventsDebugMenuAlertMode =
        EventsDebugMenuAlertMode.ROTATION,
    val seed: Long = DEFAULT_SEED,
    val nextOrdinal: Long = 1L,

    /**
     * Active Debug-menu case only.
     *
     * null means no Debug-menu alert case is currently imposed on Events presentation.
     * This is not the current Event, current candidate, or generic app alertCase.
     */
    val currentAlertCase: EventsDebugMenuAlertCase? = null,
) : VexState<EventsDebugMenuAlertState> {
    companion object {
        const val DEFAULT_SEED = 24_101_126L
    }
}

interface EventsDebugMenuAlertRepository {
    val states: Flow<EventsDebugMenuAlertState>
    val current: EventsDebugMenuAlertState

    fun useRotation()
    fun useSeededRandom(seed: Long)

    /**
     * Plan and commit one next Events Debug-menu alert case against current event truth.
     *
     * The return value is the exact case committed by this call. Returning that feature-local
     * value is a convenience for Debug orchestration that may need to react to case metadata
     * (for example freezeProjection); it is not a generic VexActionRelay result contract.
     */
    fun prepareNextAlertCase(events: List<VexExternalEvent>): EventsDebugMenuAlertCase

    fun completeCurrentAlertCase()
    fun reset()
}

class RelayEventsDebugMenuAlertRepository :
    EventsDebugMenuAlertRepository {
    private val relay =
        EventsDebugMenuAlertRelay()

    override val states: Flow<EventsDebugMenuAlertState>
        get() = relay.states

    override val current: EventsDebugMenuAlertState
        get() = relay.current

    override fun useRotation() {
        relay.updateMode(
            mode = EventsDebugMenuAlertMode.ROTATION,
            seed = EventsDebugMenuAlertState.DEFAULT_SEED,
        )
    }

    override fun useSeededRandom(seed: Long) {
        relay.updateMode(
            mode = EventsDebugMenuAlertMode.SEEDED_RANDOM,
            seed = seed,
        )
    }

    override fun prepareNextAlertCase(
        events: List<VexExternalEvent>,
    ): EventsDebugMenuAlertCase =
        relay.prepareNextAlertCase(events)

    override fun completeCurrentAlertCase() {
        relay.completeCurrentAlertCase()
    }

    override fun reset() {
        relay.reset()
    }
}

private sealed interface EventsDebugMenuAlertAction :
    VexAction {
    data class UseMode(
        val mode: EventsDebugMenuAlertMode,
        val seed: Long,
    ) : EventsDebugMenuAlertAction

    data class PrepareNextAlertCase(
        val events: List<VexExternalEvent>,
    ) : EventsDebugMenuAlertAction

    data object CompleteCurrentAlertCase :
        EventsDebugMenuAlertAction

    data object Reset :
        EventsDebugMenuAlertAction
}

/**
 * Action-driven owner of Events Debug-menu alert state.
 *
 * Every state-changing intention enters as one [EventsDebugMenuAlertAction] and reaches exactly one
 * reducer: [nextStateFor]. Public methods below only name/form the action; they never read/copy/mutate
 * current state themselves.
 */
private class EventsDebugMenuAlertRelay :
    VexActionRelay<
        EventsDebugMenuAlertAction,
        EventsDebugMenuAlertState
    >(
        relayRef = "relay.events.debug-menu-alert",
        initialState = EventsDebugMenuAlertState(),
    ) {
    fun updateMode(
        mode: EventsDebugMenuAlertMode,
        seed: Long,
    ) {
        sendAction(
            EventsDebugMenuAlertAction.UseMode(
                mode = mode,
                seed = seed,
            ),
        )
    }

    /**
     * Ask the Events Debug menu to form and retain one next overlay case.
     *
     * Callers provide current event truth only; they do not construct AlertCase state themselves.
     * The named PrepareNextAlertCase action is the only entry, and [nextStateFor] is the only state transition.
     */
    fun prepareNextAlertCase(
        events: List<VexExternalEvent>,
    ): EventsDebugMenuAlertCase {
        val committedState =
            sendAction(
                EventsDebugMenuAlertAction.PrepareNextAlertCase(
                    // The action is immutable by contract; detach the caller-owned list once here.
                    events =
                        events.map(
                            VexExternalEvent::deepCopy,
                        ),
                ),
            )

        /*
         * sendAction() returns the post-transition State. PrepareNextAlertCase's reducer contract always stores
         * the case it just planned in currentAlertCase, so this method projects that one
         * domain-specific result back to its caller.
         *
         * Higher layers should call prepareNextAlertCase(); they should not inspect Relay State to recover this.
         */
        return checkNotNull(
            committedState.currentAlertCase,
        ) {
            "PrepareNextAlertCase must commit one current Events Debug-menu alert case"
        }
    }

    fun completeCurrentAlertCase() {
        sendAction(
            EventsDebugMenuAlertAction.CompleteCurrentAlertCase,
        )
    }

    fun reset() {
        sendAction(
            EventsDebugMenuAlertAction.Reset,
        )
    }

    /**
     * The only action -> next-state path for this Relay.
     *
     * No caller supplies "the next State" and no caller receives an anonymous current-state lambda.
     * If the Debug menu gains a new behavior, add one action above and one exhaustive branch here.
     */
    override fun nextStateFor(
        action: EventsDebugMenuAlertAction,
        currentState: EventsDebugMenuAlertState,
    ): EventsDebugMenuAlertState =
        when (action) {
            is EventsDebugMenuAlertAction.UseMode ->
                currentState.copy(
                    mode = action.mode,
                    seed = action.seed,
                    nextOrdinal = 1L,
                    currentAlertCase = null,
                )

            is EventsDebugMenuAlertAction.PrepareNextAlertCase -> {
                val alertCase =
                    buildEventsDebugMenuAlertCase(
                        mode = currentState.mode,
                        seed = currentState.seed,
                        ordinal = currentState.nextOrdinal,
                        events = action.events,
                    )

                currentState.copy(
                    nextOrdinal = currentState.nextOrdinal + 1L,
                    currentAlertCase = alertCase,
                )
            }

            EventsDebugMenuAlertAction.CompleteCurrentAlertCase ->
                currentState.copy(
                    currentAlertCase = null,
                )

            EventsDebugMenuAlertAction.Reset ->
                EventsDebugMenuAlertState(
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

private data class EventsDebugMenuAlertCaseTemplate(
    val type: EventsDebugMenuAlertCaseType,
    val placement: EventsDebugMenuAlertPlacement,
    val expectedAction: EventsDebugMenuAlertExpectedAction,
)

/**
 * Stable Debug-menu alert-case vocabulary.
 *
 * Keep the declarative choices together so the planner below reads as "select -> derive -> project"
 * rather than rebuilding configuration while also reasoning about one alert case.
 */
private val eventsDebugMenuAlertCaseTemplates =
    listOf(
        EventsDebugMenuAlertCaseTemplate(
            type = EventsDebugMenuAlertCaseType.ANCHORED_TOP,
            placement = EventsDebugMenuAlertPlacement.BELOW,
            expectedAction = EventsDebugMenuAlertExpectedAction.ACKNOWLEDGE,
        ),
        EventsDebugMenuAlertCaseTemplate(
            type = EventsDebugMenuAlertCaseType.ANCHORED_MIDDLE,
            placement = EventsDebugMenuAlertPlacement.ABOVE,
            expectedAction = EventsDebugMenuAlertExpectedAction.LATER,
        ),
        EventsDebugMenuAlertCaseTemplate(
            type = EventsDebugMenuAlertCaseType.ANCHORED_BOTTOM,
            placement = EventsDebugMenuAlertPlacement.ABOVE,
            expectedAction = EventsDebugMenuAlertExpectedAction.SUSPEND,
        ),
        EventsDebugMenuAlertCaseTemplate(
            type = EventsDebugMenuAlertCaseType.ANCHORED_MIDDLE,
            placement = EventsDebugMenuAlertPlacement.BELOW,
            expectedAction = EventsDebugMenuAlertExpectedAction.SUPPRESS,
        ),
        EventsDebugMenuAlertCaseTemplate(
            type = EventsDebugMenuAlertCaseType.BLOCKING_FREEZE_TOP,
            placement = EventsDebugMenuAlertPlacement.BELOW,
            expectedAction = EventsDebugMenuAlertExpectedAction.ACKNOWLEDGE,
        ),
    )

/**
 * Pure planner for one Events alert-lab overlay case.
 *
 * This is feature-local calculation, not shared infrastructure. It does not read Relay current state
 * and does not mutate Relay state. EventsDebugMenuAlertRelay.prepareNextAlertCase() supplies one exact current
 * mode/seed/ordinal from one exact current State inside the action reducer, then atomically retains
 * the returned case in EventsDebugMenuAlertState.currentAlertCase.
 *
 * Read in order:
 *
 * 1. VALIDATE -> reject impossible planning inputs;
 * 2. SELECT   -> choose one stable alert-case template;
 * 3. DERIVE   -> resolve target position/target from current events;
 * 4. PROJECT  -> construct one complete immutable alert case;
 * 5. RETURN   -> hand the value back to the atomic Relay transition.
 */
internal fun buildEventsDebugMenuAlertCase(
    mode: EventsDebugMenuAlertMode,
    seed: Long,
    ordinal: Long,
    events: List<VexExternalEvent>,
): EventsDebugMenuAlertCase {
    // 1. VALIDATE
    require(ordinal > 0L)

    // 2. SELECT
    val template =
        when (mode) {
            EventsDebugMenuAlertMode.ROTATION ->
                eventsDebugMenuAlertCaseTemplates[
                    ((ordinal - 1L) % eventsDebugMenuAlertCaseTemplates.size)
                        .toInt()
                ]

            EventsDebugMenuAlertMode.SEEDED_RANDOM ->
                eventsDebugMenuAlertCaseTemplates[
                    Random(
                        foldedRandomSeed(
                            seed = seed,
                            ordinal = ordinal,
                        ),
                    ).nextInt(eventsDebugMenuAlertCaseTemplates.size)
                ]
        }

    // 3. DERIVE
    val type =
        template.type
    val targetPosition =
        if (events.isEmpty()) {
            null
        } else {
            when (type) {
                EventsDebugMenuAlertCaseType.ANCHORED_TOP,
                EventsDebugMenuAlertCaseType.BLOCKING_FREEZE_TOP,
                -> 0

                EventsDebugMenuAlertCaseType.ANCHORED_MIDDLE ->
                    events.lastIndex / 2

                EventsDebugMenuAlertCaseType.ANCHORED_BOTTOM ->
                    events.lastIndex
            }
        }
    val target =
        targetPosition?.let(events::get)

    // 4. PROJECT
    val alertCase =
        EventsDebugMenuAlertCase(
            alertCaseRef =
                buildString {
                    append("alert-case.events.debug-menu.")
                    append(mode.name.lowercase())
                    if (mode == EventsDebugMenuAlertMode.SEEDED_RANDOM) {
                        append(".seed-")
                        append(seed)
                    }
                    append(".")
                    append(ordinal.toString().padStart(3, '0'))
                    append(".")
                    append(type.name.lowercase())
                },
            ordinal = ordinal,
            mode = mode,
            seed = seed,
            type = type,
            targetEventRef = target?.eventRef,
            targetPositionFromTop = targetPosition,
            listCountAtPlan = events.size,
            preferredPlacement = template.placement,
            expectedAction = template.expectedAction,
            freezeProjection =
                type ==
                    EventsDebugMenuAlertCaseType.BLOCKING_FREEZE_TOP,
            blockingAcknowledgeOnly =
                type ==
                    EventsDebugMenuAlertCaseType.BLOCKING_FREEZE_TOP,
        )

    // 5. RETURN
    return alertCase
}

// [VXG RealForever]
