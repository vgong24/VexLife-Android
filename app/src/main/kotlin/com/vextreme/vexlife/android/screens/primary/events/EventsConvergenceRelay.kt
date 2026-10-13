package com.vextreme.vexlife.android.screens.primary.events

import com.vextreme.vexlife.android.data.events.EventsAttentionDisposition
import com.vextreme.vexlife.android.data.events.EventsAttentionLabRepository
import com.vextreme.vexlife.android.data.events.EventsAttentionLabScenario
import com.vextreme.vexlife.android.data.events.EventsAttentionLabState
import com.vextreme.vexlife.android.data.events.EventsAttentionPreferenceRepository
import com.vextreme.vexlife.android.data.events.EventsAttentionPreferenceState
import com.vextreme.vexlife.android.data.events.EventsFeedState
import com.vextreme.vexlife.android.data.events.EventsRepository
import com.vextreme.vexlife.android.data.events.VexExternalEvent
import com.vextreme.vexlife.android.data.events.VexExternalEventKind
import com.vextreme.vexlife.android.data.userprofile.UserProfile
import com.vextreme.vexlife.android.data.userprofile.UserProfileRefs
import com.vextreme.vexlife.android.data.userprofile.UserProfileRepository
import com.vextreme.vexlife.android.relay.VexCompoundRelay
import com.vextreme.vexlife.android.relay.VexCompoundState
import com.vextreme.vexlife.android.relay.VexDeepCopyable
import com.vextreme.vexlife.android.relay.VexInputState
import com.vextreme.vexlife.android.relay.VexOutputState
import com.vextreme.vexlife.android.relay.vexDependencies

/**
 * Observed profile value.
 *
 * The wrapper matters because the Compound patch itself uses null for "this dependency supplied no
 * new value". An observed null profile instead means "the profile source was observed and is empty".
 */
data class EventsProfileObservation(
    val profile: UserProfile?,
) : VexDeepCopyable<EventsProfileObservation> {
    override fun deepCopy(): EventsProfileObservation =
        copy(
            profile = profile?.copy(),
        )
}

data class EventsConvergenceInputState(
    val currentScreenId: String? = null,
    val feed: EventsFeedState? = null,
    val preferences: EventsAttentionPreferenceState? = null,
    val lab: EventsAttentionLabState? = null,
    val profile: EventsProfileObservation? = null,
    val visibleEventRefsBySurface: Map<String, Set<String>>? = null,
    val transientBlocked: Boolean? = null,
) : VexInputState<EventsConvergenceInputState> {
    override fun deepCopy(): EventsConvergenceInputState =
        copy(
            feed = feed?.deepCopy(),
            preferences = preferences?.deepCopy(),
            lab = lab?.deepCopy(),
            profile = profile?.deepCopy(),
            visibleEventRefsBySurface =
                visibleEventRefsBySurface?.mapValues { (_, refs) ->
                    refs.toSet()
                },
        )
}

data class EventsConvergenceCandidate(
    val eventRef: String,
    val targetElementRef: String,
)

enum class EventsConvergenceAvailability {
    /** One or more dependencies required by the current presentation have not been observed yet. */
    UNOBSERVED,

    /** Current context contains no unhandled event candidate. */
    NONE,

    /** Candidate exists but Events is not the current semantic Screen. */
    HELD_OTHER_SCREEN,

    /** Candidate exists and Events is current, but its exact row is not presently reachable. */
    HELD_TARGET_UNREACHABLE,

    /** Candidate exists, but another transient surface currently owns presentation attention. */
    HELD_TRANSIENT_OCCUPIED,

    /** Candidate exists and the exact target is currently eligible for anchored presentation. */
    READY,
}


sealed interface EventsConvergenceListState {
    /** The Relay has not yet observed canonical feed truth. */
    data object Unobserved : EventsConvergenceListState

    /** Canonical feed truth is observed and the converged current list has no events. */
    data object Empty : EventsConvergenceListState

    /** Canonical feed truth is observed and the current converged list contains events. */
    data class Content(
        val events: List<VexExternalEvent>,
    ) : EventsConvergenceListState
}

data class EventsConvergenceOutputState(
    /**
     * Current list meaning.
     *
     * Do not infer "loaded empty" from an empty List: [Unobserved] and [Empty] are intentionally
     * different states. Future partner sources may earn additional Loading/Unavailable distinctions.
     */
    val list: EventsConvergenceListState =
        EventsConvergenceListState.Unobserved,

    val candidate: EventsConvergenceCandidate? = null,
    val availability: EventsConvergenceAvailability =
        EventsConvergenceAvailability.UNOBSERVED,

    /** Current presentation facts already resolved by the Relay, not recomputed in Compose. */
    val dispositionByEventRef: Map<String, EventsAttentionDisposition> = emptyMap(),
    val scenario: EventsAttentionLabScenario? = null,

    /**
     * The latest observed local-profile display-name transition.
     *
     * This is intentionally a current bounded notice rather than an event ledger. The profile's
     * domain-owned updatedAt identifies the change; no artificial click/revision counter is created
     * merely to force StateFlow inequality.
     */
    val latestProfileNameChange: VexExternalEvent? = null,

    /** Human/debug-facing queue metadata derived from the same Relay decision. */
    val unhandledCount: Int = 0,
    val queuedAfterCurrentCount: Int = 0,
    val acknowledgedCount: Int = 0,
    val deferredCount: Int = 0,
    val suspendedCount: Int = 0,
    val suppressedCount: Int = 0,
    val currentQueueCount: Int = 0,
    val nextSequenceCount: Int = 0,
) : VexOutputState<EventsConvergenceOutputState> {
    val events: List<VexExternalEvent>
        get() =
            when (val currentList = list) {
                EventsConvergenceListState.Unobserved,
                EventsConvergenceListState.Empty,
                ->
                    emptyList()

                is EventsConvergenceListState.Content ->
                    currentList.events.map(VexExternalEvent::deepCopy)
            }

    override fun deepCopy(): EventsConvergenceOutputState =
        copy(
            list =
                when (val currentList = list) {
                    EventsConvergenceListState.Unobserved ->
                        EventsConvergenceListState.Unobserved

                    EventsConvergenceListState.Empty ->
                        EventsConvergenceListState.Empty

                    is EventsConvergenceListState.Content ->
                        EventsConvergenceListState.Content(
                            events =
                                currentList.events.map(
                                    VexExternalEvent::deepCopy,
                                ),
                        )
                },
            candidate = candidate?.copy(),
            dispositionByEventRef = dispositionByEventRef.toMap(),
            scenario = scenario?.copy(),
            latestProfileNameChange = latestProfileNameChange?.deepCopy(),
        )
}

/**
 * One reusable convergence relationship over current Events truth.
 *
 * Read this class in four places:
 *
 * dependencies()   -> what continuously changes around Events?
 * public methods   -> what intentions/surface facts may enter?
 * processStates()  -> what does the latest combination mean?
 * outputStates     -> what may Home/Events/other consumers observe?
 *
 * Dependency collectors are framework-owned and asynchronous. processStates remains a synchronous,
 * serialized reducer over already-observed facts.
 */
class EventsConvergenceRelay internal constructor(
    private val eventsRepository: EventsRepository?,
    private val attentionPreferences: EventsAttentionPreferenceRepository?,
    private val attentionLab: EventsAttentionLabRepository?,
    private val userProfile: UserProfileRepository?,
) : VexCompoundRelay<EventsConvergenceInputState, EventsConvergenceOutputState>(
    relayRef = "relay.events.convergence",
    initialInputState = EventsConvergenceInputState(),
    initialOutputState = EventsConvergenceOutputState(),
) {
    /** Pure reducer tests may drive partial inputs directly without starting dependency observers. */
    internal constructor() : this(
        eventsRepository = null,
        attentionPreferences = null,
        attentionLab = null,
        userProfile = null,
    )

    override fun dependencies() =
        vexDependencies<EventsConvergenceInputState> {
            eventsRepository?.let { repository ->
                observe(
                    dependencyRef = FEED_DEPENDENCY,
                    flow = repository.states,
                ) { feed ->
                    EventsConvergenceInputState(
                        feed = feed.deepCopy(),
                    )
                }
            }

            attentionPreferences?.let { repository ->
                observe(
                    dependencyRef = PREFERENCES_DEPENDENCY,
                    flow = repository.states,
                ) { preferences ->
                    EventsConvergenceInputState(
                        preferences = preferences.deepCopy(),
                    )
                }
            }

            attentionLab?.let { repository ->
                observe(
                    dependencyRef = LAB_DEPENDENCY,
                    flow = repository.states,
                ) { lab ->
                    EventsConvergenceInputState(
                        lab = lab.deepCopy(),
                    )
                }
            }

            userProfile?.let { repository ->
                observe(
                    dependencyRef = PROFILE_DEPENDENCY,
                    flow = repository.observe(UserProfileRefs.local),
                ) { profile ->
                    EventsConvergenceInputState(
                        profile =
                            EventsProfileObservation(
                                profile = profile?.copy(),
                            ),
                    )
                }
            }
        }

    /**
     * Current semantic Screen + global transient occupancy arrive from the UI/runtime boundary.
     *
     * This is one synchronous state admission, not a coroutine launch.
     */
    fun surfaceChanged(
        screenId: String,
        transientBlocked: Boolean,
    ) {
        require(screenId.isNotBlank())

        send(
            EventsConvergenceInputState(
                currentScreenId = screenId,
                transientBlocked = transientBlocked,
            ),
        )
    }

    fun visibleTargetsChanged(
        surfaceRef: String,
        eventRefs: Set<String>,
    ) {
        require(surfaceRef.isNotBlank())

        send(
            EventsConvergenceInputState(
                visibleEventRefsBySurface =
                    mapOf(
                        surfaceRef to eventRefs.toSet(),
                    ),
            ),
        )
    }

    /** Convenience for the current Events list surface. */
    fun visibleTargetsChanged(eventRefs: Set<String>) {
        visibleTargetsChanged(
            surfaceRef = EventsScreen.screenId,
            eventRefs = eventRefs,
        )
    }

    fun acknowledge(eventRef: String) {
        requireAttentionPreferences()
            .acknowledge(eventRef)
    }

    fun later(eventRef: String) {
        requireAttentionPreferences()
            .defer(eventRef)
    }

    fun suspend(eventRef: String) {
        requireAttentionPreferences()
            .suspend(eventRef)
    }

    fun suppress(eventRef: String) {
        requireAttentionPreferences()
            .suppress(eventRef)
    }

    fun completeCurrentScenario() {
        requireAttentionLab()
            .completeCurrent()
    }

    /*
     * Direct reducer seams remain internal for focused processStates tests. Production repository
     * observation enters through dependencies() instead of Compose forwarding.
     */
    internal fun screenChanged(screenId: String) {
        require(screenId.isNotBlank())
        send(
            EventsConvergenceInputState(
                currentScreenId = screenId,
            ),
        )
    }

    internal fun feedChanged(feed: EventsFeedState) {
        send(
            EventsConvergenceInputState(
                feed = feed.deepCopy(),
            ),
        )
    }

    internal fun preferencesChanged(
        preferences: EventsAttentionPreferenceState,
    ) {
        send(
            EventsConvergenceInputState(
                preferences = preferences.deepCopy(),
            ),
        )
    }

    internal fun labChanged(lab: EventsAttentionLabState) {
        send(
            EventsConvergenceInputState(
                lab = lab.deepCopy(),
            ),
        )
    }

    internal fun transientBlockedChanged(blocked: Boolean) {
        send(
            EventsConvergenceInputState(
                transientBlocked = blocked,
            ),
        )
    }

    override fun processStates(
        inputState: EventsConvergenceInputState,
        currentState: VexCompoundState<
            EventsConvergenceInputState,
            EventsConvergenceOutputState
        >,
    ): VexCompoundState<
        EventsConvergenceInputState,
        EventsConvergenceOutputState
    > {
        val previous =
            currentState.inputState

        val screenId =
            inputState.currentScreenId
                ?: previous.currentScreenId
        val feed =
            inputState.feed
                ?: previous.feed
        val preferences =
            inputState.preferences
                ?: previous.preferences
        val lab =
            inputState.lab
                ?: previous.lab
        val profile =
            inputState.profile
                ?: previous.profile
        val visibleEventRefsBySurface =
            mergeVisibleSurfacePatches(
                previous =
                    previous.visibleEventRefsBySurface,
                patch =
                    inputState.visibleEventRefsBySurface,
            )
        val transientBlocked =
            inputState.transientBlocked
                ?: previous.transientBlocked

        val latestProfileNameChange =
            profileNameChangeFromPatch(
                previousProfile = previous.profile,
                incomingProfile = inputState.profile,
            )
                ?: currentState.outputState
                    .latestProfileNameChange
                    ?.deepCopy()

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
                    "converged Events context contains duplicate eventRef values"
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
                    EventsConvergenceListState.Content(
                        events =
                            events.map(
                                VexExternalEvent::deepCopy,
                            ),
                    )

                feed == null ->
                    EventsConvergenceListState.Unobserved

                else ->
                    EventsConvergenceListState.Empty
            }

        val coherentInput =
            EventsConvergenceInputState(
                currentScreenId = screenId,
                feed = feed?.deepCopy(),
                preferences = preferences?.deepCopy(),
                lab = lab?.deepCopy(),
                profile = profile?.deepCopy(),
                visibleEventRefsBySurface =
                    visibleEventRefsBySurface?.mapValues { (_, refs) ->
                        refs.toSet()
                    },
                transientBlocked = transientBlocked,
            )

        val dispositions =
            preferences
                ?.dispositionByEventRef
                .orEmpty()

        /*
         * Queue law:
         *
         * NORMAL      -> current queue
         * SUSPENDED   -> back of current queue
         * DEFERRED    -> next sequence, only after current queue drains
         * ACK/SUPPRESS-> terminal for proactive attention this lab/session
         */
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
                normalQueue.isNotEmpty() ->
                    normalQueue

                suspendedQueue.isNotEmpty() ->
                    suspendedQueue

                else ->
                    deferredQueue
            }

        val candidateEvent =
            if (feed != null && preferences != null) {
                val labTargetRef =
                    lab
                        ?.currentScenario
                        ?.targetEventRef

                val retainedCandidateRef =
                    currentState.outputState
                        .candidate
                        ?.eventRef

                activePool.firstOrNull { event ->
                    event.eventRef == labTargetRef
                } ?: activePool.firstOrNull { event ->
                    event.eventRef == retainedCandidateRef
                } ?: activePool.firstOrNull()
            } else {
                null
            }

        val candidate =
            candidateEvent?.let { event ->
                EventsConvergenceCandidate(
                    eventRef = event.eventRef,
                    targetElementRef =
                        EventsPresentationRefs.row(
                            event.eventRef,
                        ),
                )
            }

        val eventsSurfaceVisibility =
            visibleEventRefsBySurface
                ?.get(EventsScreen.screenId)

        val availability =
            when {
                feed == null ||
                    preferences == null ||
                    screenId == null ||
                    transientBlocked == null ->
                    EventsConvergenceAvailability.UNOBSERVED

                candidate == null ->
                    EventsConvergenceAvailability.NONE

                screenId != EventsScreen.screenId ->
                    EventsConvergenceAvailability.HELD_OTHER_SCREEN

                transientBlocked ->
                    EventsConvergenceAvailability.HELD_TRANSIENT_OCCUPIED

                eventsSurfaceVisibility == null ->
                    EventsConvergenceAvailability.UNOBSERVED

                candidate.eventRef !in eventsSurfaceVisibility ->
                    EventsConvergenceAvailability.HELD_TARGET_UNREACHABLE

                else ->
                    EventsConvergenceAvailability.READY
            }

        val currentQueueCount =
            normalQueue.size +
                suspendedQueue.size
        val nextSequenceCount =
            deferredQueue.size
        val unhandledCount =
            currentQueueCount +
                nextSequenceCount

        val activeQueueCount =
            when {
                normalQueue.isNotEmpty() ->
                    currentQueueCount

                suspendedQueue.isNotEmpty() ->
                    suspendedQueue.size

                else ->
                    deferredQueue.size
            }

        return currentState.copy(
            inputState = coherentInput,
            outputState =
                EventsConvergenceOutputState(
                    list = listState,
                    candidate = candidate,
                    availability = availability,
                    dispositionByEventRef = dispositions.toMap(),
                    scenario =
                        lab
                            ?.currentScenario
                            ?.copy(),
                    latestProfileNameChange =
                        latestProfileNameChange
                            ?.deepCopy(),
                    unhandledCount = unhandledCount,
                    queuedAfterCurrentCount =
                        if (candidate == null) {
                            0
                        } else {
                            (activeQueueCount - 1)
                                .coerceAtLeast(0)
                        },
                    acknowledgedCount =
                        dispositions.values.count { disposition ->
                            disposition ==
                                EventsAttentionDisposition.ACKNOWLEDGED
                        },
                    deferredCount =
                        deferredQueue.size,
                    suspendedCount =
                        suspendedQueue.size,
                    suppressedCount =
                        dispositions.values.count { disposition ->
                            disposition ==
                                EventsAttentionDisposition.SUPPRESSED
                        },
                    currentQueueCount =
                        currentQueueCount,
                    nextSequenceCount =
                        nextSequenceCount,
                ),
        )
    }

    private fun requireAttentionPreferences(): EventsAttentionPreferenceRepository =
        checkNotNull(attentionPreferences) {
            "EventsConvergenceRelay action methods require an attention repository"
        }

    private fun requireAttentionLab(): EventsAttentionLabRepository =
        checkNotNull(attentionLab) {
            "EventsConvergenceRelay lab methods require a lab repository"
        }

    private fun mergeVisibleSurfacePatches(
        previous: Map<String, Set<String>>?,
        patch: Map<String, Set<String>>?,
    ): Map<String, Set<String>>? =
        when {
            patch == null ->
                previous?.mapValues { (_, refs) ->
                    refs.toSet()
                }

            previous == null ->
                patch.mapValues { (_, refs) ->
                    refs.toSet()
                }

            else ->
                previous
                    .mapValues { (_, refs) ->
                        refs.toSet()
                    } +
                    patch.mapValues { (_, refs) ->
                        refs.toSet()
                    }
        }

    private fun profileNameChangeFromPatch(
        previousProfile: EventsProfileObservation?,
        incomingProfile: EventsProfileObservation?,
    ): VexExternalEvent? {
        val before =
            previousProfile
                ?.profile
                ?: return null
        val after =
            incomingProfile
                ?.profile
                ?: return null

        if (
            before.profileRef != after.profileRef ||
            before.displayName == after.displayName
        ) {
            return null
        }

        return VexExternalEvent(
            eventRef =
                "event.user-profile.display-name-change@" +
                    after.updatedAt.toString(),
            sourceRef =
                "source.user-profile." +
                    after.profileRef,
            kind = VexExternalEventKind.SYSTEM,
            title = "Profile name changed",
            detail =
                before.displayName +
                    " → " +
                    after.displayName,
            occurredAtEpochMillis =
                after.updatedAt.toEpochMilli(),
        )
    }

    private companion object {
        const val FEED_DEPENDENCY =
            "events.feed"
        const val PREFERENCES_DEPENDENCY =
            "events.preferences"
        const val LAB_DEPENDENCY =
            "events.lab"
        const val PROFILE_DEPENDENCY =
            "user-profile.local"
    }
}

// [VXG RealForever]
