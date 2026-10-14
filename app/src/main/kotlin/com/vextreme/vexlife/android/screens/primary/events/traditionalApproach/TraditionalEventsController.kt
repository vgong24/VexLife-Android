package com.vextreme.vexlife.android.screens.primary.events.traditionalApproach

import com.vextreme.vexlife.android.data.events.EventsAttentionDisposition
import com.vextreme.vexlife.android.data.events.EventsAttentionLabRepository
import com.vextreme.vexlife.android.data.events.EventsAttentionLabScenario
import com.vextreme.vexlife.android.data.events.EventsAttentionLabState
import com.vextreme.vexlife.android.data.events.EventsAttentionPreferenceRepository
import com.vextreme.vexlife.android.data.events.EventsAttentionPreferenceState
import com.vextreme.vexlife.android.data.events.EventsFeedState
import com.vextreme.vexlife.android.data.events.EventsRepository
import com.vextreme.vexlife.android.data.events.EventsSourceAvailabilityRepository
import com.vextreme.vexlife.android.data.events.EventsSourceAvailabilityState
import com.vextreme.vexlife.android.data.events.VexExternalEvent
import com.vextreme.vexlife.android.data.events.VexExternalEventKind
import com.vextreme.vexlife.android.data.userprofile.UserProfile
import com.vextreme.vexlife.android.data.userprofile.UserProfileRefs
import com.vextreme.vexlife.android.data.userprofile.UserProfileRepository
import com.vextreme.vexlife.android.screens.primary.events.EventsPresentationRefs
import com.vextreme.vexlife.android.screens.primary.events.EventsScreen
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Intentionally traditional comparison implementation for Events.
 *
 * This is not "bad code". It is a competent controller-style implementation:
 * - the controller manually subscribes to every dependency;
 * - mutable retained fields form the controller snapshot;
 * - every callback invokes one recomputation method;
 * - the controller exposes one UI StateFlow and imperative actions;
 * - lifecycle is explicit start/stop rather than framework-declared dependencies.
 *
 * Keep this package recognizable and removable. It exists to compare maintenance / continuity
 * against EventsConvergenceRelay, not to become a second production architecture.
 */
class TraditionalEventsController(
    private val eventsRepository: EventsRepository,
    private val attentionPreferences: EventsAttentionPreferenceRepository,
    private val attentionLab: EventsAttentionLabRepository,
    private val userProfile: UserProfileRepository,
    private val sourceAvailabilityRepository: EventsSourceAvailabilityRepository,
) {
    private val lock = Any()
    private var ownerJob: Job? = null

    private var feed: EventsFeedState? = null
    private var preferences: EventsAttentionPreferenceState? = null
    private var lab: EventsAttentionLabState? = null
    private var profileObserved = false
    private var profile: UserProfile? = null
    private var sourceAvailability: EventsSourceAvailabilityState? = null
    private var currentScreenId: String? = null
    private var transientBlocked: Boolean? = null
    private var visibleEventRefsBySurface: Map<String, Set<String>> = emptyMap()
    private var latestProfileNameChange: VexExternalEvent? = null

    private val mutableUiState =
        MutableStateFlow(TraditionalEventsUiState())

    val uiStates: StateFlow<TraditionalEventsUiState> =
        mutableUiState.asStateFlow()

    val current: TraditionalEventsUiState
        get() = mutableUiState.value.deepCopy()

    /**
     * The traditional controller owns collector orchestration explicitly.
     *
     * A developer adding a dependency must remember to add another child job, retained field,
     * update callback and recomputation participation here.
     */
    fun start(scope: CoroutineScope) {
        synchronized(lock) {
            check(ownerJob == null) {
                "TraditionalEventsController already started"
            }

            val parent =
                checkNotNull(scope.coroutineContext[Job]) {
                    "TraditionalEventsController requires a CoroutineScope with a Job"
                }
            check(parent.isActive) {
                "TraditionalEventsController cannot start in an inactive scope"
            }

            val job = SupervisorJob(parent)
            val controllerScope =
                CoroutineScope(scope.coroutineContext + job)
            ownerJob = job

            controllerScope.launch(CoroutineName("traditional-events/feed")) {
                eventsRepository.states.collect { observed ->
                    synchronized(lock) {
                        feed = observed.deepCopy()
                        recomputeLocked()
                    }
                }
            }

            controllerScope.launch(CoroutineName("traditional-events/preferences")) {
                attentionPreferences.states.collect { observed ->
                    synchronized(lock) {
                        preferences = observed.deepCopy()
                        recomputeLocked()
                    }
                }
            }

            controllerScope.launch(CoroutineName("traditional-events/lab")) {
                attentionLab.states.collect { observed ->
                    synchronized(lock) {
                        lab = observed.deepCopy()
                        recomputeLocked()
                    }
                }
            }

            controllerScope.launch(CoroutineName("traditional-events/profile")) {
                userProfile.observe(UserProfileRefs.local).collect { observed ->
                    synchronized(lock) {
                        updateProfileLocked(observed)
                        recomputeLocked()
                    }
                }
            }

            controllerScope.launch(CoroutineName("traditional-events/source-availability")) {
                sourceAvailabilityRepository.states.collect { observed ->
                    synchronized(lock) {
                        sourceAvailability = observed.deepCopy()
                        recomputeLocked()
                    }
                }
            }
        }
    }

    suspend fun stop() {
        val job =
            synchronized(lock) {
                ownerJob
            } ?: return

        job.cancelAndJoin()

        synchronized(lock) {
            if (ownerJob === job) {
                ownerJob = null
            }
        }
    }

    fun surfaceChanged(
        screenId: String,
        blockedByTransient: Boolean,
    ) {
        require(screenId.isNotBlank())
        synchronized(lock) {
            currentScreenId = screenId
            transientBlocked = blockedByTransient
            recomputeLocked()
        }
    }

    fun visibleTargetsChanged(
        surfaceRef: String,
        eventRefs: Set<String>,
    ) {
        require(surfaceRef.isNotBlank())
        synchronized(lock) {
            visibleEventRefsBySurface =
                visibleEventRefsBySurface +
                    (surfaceRef to eventRefs.toSet())
            recomputeLocked()
        }
    }

    fun visibleTargetsChanged(eventRefs: Set<String>) {
        visibleTargetsChanged(
            surfaceRef = EventsScreen.screenId,
            eventRefs = eventRefs,
        )
    }

    fun acknowledge(eventRef: String) =
        attentionPreferences.acknowledge(eventRef)

    fun later(eventRef: String) =
        attentionPreferences.defer(eventRef)

    fun suspend(eventRef: String) =
        attentionPreferences.suspend(eventRef)

    fun suppress(eventRef: String) =
        attentionPreferences.suppress(eventRef)

    fun completeCurrentScenario() =
        attentionLab.completeCurrent()

    private fun updateProfileLocked(incoming: UserProfile?) {
        if (profileObserved) {
            val before = profile
            val after = incoming

            if (
                before != null &&
                after != null &&
                before.profileRef == after.profileRef &&
                before.displayName != after.displayName
            ) {
                latestProfileNameChange =
                    VexExternalEvent(
                        eventRef =
                            "event.user-profile.display-name-change@" +
                                after.updatedAt.toString(),
                        sourceRef =
                            "source.user-profile." + after.profileRef,
                        kind = VexExternalEventKind.SYSTEM,
                        title = "Profile name changed",
                        detail = before.displayName + " → " + after.displayName,
                        occurredAtEpochMillis = after.updatedAt.toEpochMilli(),
                    )
            }
        }

        profileObserved = true
        profile = incoming?.copy()
    }

    /**
     * Traditional controller recomputation boundary.
     *
     * The controller must manually know every retained dependency and presentation consequence.
     * This is deliberately independent of EventsConvergenceRelay.processStates().
     */
    private fun recomputeLocked() {
        val events =
            (
                feed
                    ?.events
                    .orEmpty()
                    .map(VexExternalEvent::deepCopy) +
                    listOfNotNull(
                        latestProfileNameChange?.deepCopy(),
                    )
            ).also { combined ->
                check(
                    combined.map(VexExternalEvent::eventRef)
                        .distinct()
                        .size == combined.size,
                ) {
                    "traditional Events controller contains duplicate eventRef values"
                }
            }.sortedWith(
                compareByDescending<VexExternalEvent> {
                    it.occurredAtEpochMillis
                }.thenByDescending {
                    it.eventRef
                },
            )

        val listState =
            when {
                events.isNotEmpty() ->
                    TraditionalEventsListState.Content(
                        events.map(VexExternalEvent::deepCopy),
                    )

                feed == null ->
                    TraditionalEventsListState.Unobserved

                else ->
                    TraditionalEventsListState.Empty
            }

        val dispositions =
            preferences
                ?.dispositionByEventRef
                .orEmpty()

        val normalQueue =
            events
                .filter { event ->
                    event.eventRef !in dispositions
                }
                .asReversed()

        val suspendedQueue =
            events
                .filter { event ->
                    dispositions[event.eventRef] ==
                        EventsAttentionDisposition.SUSPENDED
                }
                .sortedBy { event ->
                    preferences
                        ?.suspendedOrderByEventRef
                        ?.get(event.eventRef)
                        ?: Long.MAX_VALUE
                }

        val deferredQueue =
            events
                .filter { event ->
                    dispositions[event.eventRef] ==
                        EventsAttentionDisposition.DEFERRED
                }
                .sortedBy { event ->
                    preferences
                        ?.deferredOrderByEventRef
                        ?.get(event.eventRef)
                        ?: Long.MAX_VALUE
                }

        val activePool =
            when {
                normalQueue.isNotEmpty() -> normalQueue
                suspendedQueue.isNotEmpty() -> suspendedQueue
                else -> deferredQueue
            }

        val previousCandidateRef =
            mutableUiState.value.candidate?.eventRef
        val labTargetRef =
            lab?.currentScenario?.targetEventRef

        val attentionEligiblePool =
            activePool.filter { event ->
                sourceAvailability
                    ?.isAvailable(event.sourceRef)
                    ?: false
            }

        val candidateEvent =
            if (
                feed != null &&
                preferences != null &&
                sourceAvailability != null
            ) {
                attentionEligiblePool.firstOrNull { event ->
                    event.eventRef == labTargetRef
                } ?: attentionEligiblePool.firstOrNull { event ->
                    event.eventRef == previousCandidateRef
                } ?: attentionEligiblePool.firstOrNull()
            } else {
                null
            }

        val candidate =
            candidateEvent?.let { event ->
                TraditionalEventsCandidate(
                    eventRef = event.eventRef,
                    targetElementRef =
                        EventsPresentationRefs.row(event.eventRef),
                )
            }

        val eventsSurfaceVisibility =
            visibleEventRefsBySurface[EventsScreen.screenId]

        val availability =
            when {
                feed == null ||
                    preferences == null ||
                    sourceAvailability == null ||
                    currentScreenId == null ||
                    transientBlocked == null ->
                    TraditionalEventsAvailability.UNOBSERVED

                candidate == null ->
                    TraditionalEventsAvailability.NONE

                currentScreenId != EventsScreen.screenId ->
                    TraditionalEventsAvailability.HELD_OTHER_SCREEN

                transientBlocked == true ->
                    TraditionalEventsAvailability.HELD_TRANSIENT_OCCUPIED

                eventsSurfaceVisibility == null ->
                    TraditionalEventsAvailability.UNOBSERVED

                candidate.eventRef !in eventsSurfaceVisibility ->
                    TraditionalEventsAvailability.HELD_TARGET_UNREACHABLE

                else ->
                    TraditionalEventsAvailability.READY
            }

        val currentQueueCount =
            normalQueue.size + suspendedQueue.size
        val nextSequenceCount =
            deferredQueue.size
        val unhandledCount =
            currentQueueCount + nextSequenceCount
        val activeQueueCount =
            when {
                normalQueue.isNotEmpty() -> currentQueueCount
                suspendedQueue.isNotEmpty() -> suspendedQueue.size
                else -> deferredQueue.size
            }

        mutableUiState.value =
            TraditionalEventsUiState(
                list = listState,
                candidate = candidate,
                availability = availability,
                dispositionByEventRef = dispositions.toMap(),
                scenario = lab?.currentScenario?.copy(),
                latestProfileNameChange = latestProfileNameChange?.deepCopy(),
                unhandledCount = unhandledCount,
                queuedAfterCurrentCount =
                    if (candidate == null) 0
                    else (activeQueueCount - 1).coerceAtLeast(0),
                acknowledgedCount =
                    dispositions.values.count {
                        it == EventsAttentionDisposition.ACKNOWLEDGED
                    },
                deferredCount = deferredQueue.size,
                suspendedCount = suspendedQueue.size,
                suppressedCount =
                    dispositions.values.count {
                        it == EventsAttentionDisposition.SUPPRESSED
                    },
                currentQueueCount = currentQueueCount,
                nextSequenceCount = nextSequenceCount,
            )
    }
}

enum class TraditionalEventsAvailability {
    UNOBSERVED,
    NONE,
    HELD_OTHER_SCREEN,
    HELD_TARGET_UNREACHABLE,
    HELD_TRANSIENT_OCCUPIED,
    READY,
}

sealed interface TraditionalEventsListState {
    data object Unobserved : TraditionalEventsListState
    data object Empty : TraditionalEventsListState

    data class Content(
        val events: List<VexExternalEvent>,
    ) : TraditionalEventsListState
}

data class TraditionalEventsCandidate(
    val eventRef: String,
    val targetElementRef: String,
)

data class TraditionalEventsUiState(
    val list: TraditionalEventsListState =
        TraditionalEventsListState.Unobserved,
    val candidate: TraditionalEventsCandidate? = null,
    val availability: TraditionalEventsAvailability =
        TraditionalEventsAvailability.UNOBSERVED,
    val dispositionByEventRef: Map<String, EventsAttentionDisposition> = emptyMap(),
    val scenario: EventsAttentionLabScenario? = null,
    val latestProfileNameChange: VexExternalEvent? = null,
    val unhandledCount: Int = 0,
    val queuedAfterCurrentCount: Int = 0,
    val acknowledgedCount: Int = 0,
    val deferredCount: Int = 0,
    val suspendedCount: Int = 0,
    val suppressedCount: Int = 0,
    val currentQueueCount: Int = 0,
    val nextSequenceCount: Int = 0,
) {
    val events: List<VexExternalEvent>
        get() =
            when (val current = list) {
                TraditionalEventsListState.Unobserved,
                TraditionalEventsListState.Empty,
                -> emptyList()

                is TraditionalEventsListState.Content ->
                    current.events.map(VexExternalEvent::deepCopy)
            }

    fun deepCopy(): TraditionalEventsUiState =
        copy(
            list =
                when (val current = list) {
                    TraditionalEventsListState.Unobserved ->
                        TraditionalEventsListState.Unobserved
                    TraditionalEventsListState.Empty ->
                        TraditionalEventsListState.Empty
                    is TraditionalEventsListState.Content ->
                        TraditionalEventsListState.Content(
                            current.events.map(VexExternalEvent::deepCopy),
                        )
                },
            candidate = candidate?.copy(),
            dispositionByEventRef = dispositionByEventRef.toMap(),
            scenario = scenario?.copy(),
            latestProfileNameChange = latestProfileNameChange?.deepCopy(),
        )
}

// [VXG RealForever][EVENTS][TRADITIONAL-APPROACH-COMPARISON]
