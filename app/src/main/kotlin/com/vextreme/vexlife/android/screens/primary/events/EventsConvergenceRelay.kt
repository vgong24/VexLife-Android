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

/**
 * Context needed to answer one product question:
 *
 * "Of the information that is currently true, what should the person be helped to notice now
 * without losing, reordering, or interrupting what they are already dealing with?"
 *
 * currentScreenId
 *     protects the person from an Events attention surface appearing while they are somewhere else;
 *
 * feed
 *     is the outside-world information that actually exists, independent of whether it deserves
 *     attention right now;
 *
 * preferences
 *     preserves what the person already chose to do with that information. Acknowledge, Later,
 *     Suspend and Suppress change attention treatment, not whether the event happened;
 *
 * lab
 *     names the exact lived condition being intentionally exercised so proof can target one stable
 *     event without changing ordinary Events meaning;
 *
 * profile
 *     lets a meaningful local identity change become visible as a bounded Events notice instead of
 *     disappearing merely because it came from inside VexLife;
 *
 * visibleEventRefsBySurface
 *     protects anchored attention from claiming a target the person cannot currently see/reach;
 *
 * transientBlocked
 *     protects the person from competing transient surfaces trying to own attention at once.
 *
 * These truths may change independently, but the person should experience one coherent answer.
 * [processStates] therefore re-evaluates the whole bounded Events relationship after any one changes.
 */
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

    /** At least one current event exists in the converged projection. */
    data class Content(
        val events: List<VexExternalEvent>,
    ) : EventsConvergenceListState
}

data class EventsConvergenceOutputState(
    /**
     * Current list meaning.
     *
     * Do not infer "loaded empty" from an empty List:
     * [EventsConvergenceListState.Unobserved] and [EventsConvergenceListState.Empty] are intentionally
     * different states. Future partner sources may earn additional Loading/Unavailable distinctions.
     */
    val list: EventsConvergenceListState =
        EventsConvergenceListState.Unobserved,

    /**
     * The one event the product is currently helping the person deal with next.
     *
     * Keep this identity stable while it is still lawful so a newer arrival, list reorder, Screen
     * change, or temporary visibility loss does not yank the person's attention to something else.
     */
    val candidate: EventsConvergenceCandidate? = null,

    /**
     * Whether [candidate] may responsibly ask for the person's attention right now.
     *
     * A candidate can remain the right event while being temporarily held because the person is on
     * another Screen, another transient already owns attention, or the exact row is not reachable.
     * Holding presentation must not silently replace the candidate or erase the event.
     */
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
 * User story:
 *
 * The person is on Home when a message/call/map-style event arrives. The event must remain true even
 * though Events is not current, so it is retained without interrupting them. Later they open Events:
 * the oldest still-unhandled event becomes the attention candidate. If that row is off-screen or a
 * different transient is already open, the same candidate is held rather than replaced. When the
 * row becomes reachable and attention is free, the candidate becomes READY. Pressing Later changes
 * only the person's attention preference, moves that event to the next sequence, and lets the next
 * current event become the candidate without deleting the deferred event.
 *
 * This Relay exists so those facts are resolved once from current truth instead of every Screen or
 * overlay separately deciding what deserves attention.
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
        /*
         * Decide from one coherent picture of the person's current Events situation.
         *
         * Feed, human choices, Screen context, row visibility and transient attention can change at
         * different times. Never let one fresh observation be interpreted against stale siblings
         * and accidentally change what the person is being shown or asked to deal with.
         */
        val previousInput =
            currentState.inputState
        val previousOutput =
            currentState.outputState

        val currentScreenId =
            inputState.currentScreenId
                ?: previousInput.currentScreenId
        val currentFeed =
            inputState.feed
                ?: previousInput.feed
        val currentPreferences =
            inputState.preferences
                ?: previousInput.preferences
        val currentLab =
            inputState.lab
                ?: previousInput.lab
        val currentProfile =
            inputState.profile
                ?: previousInput.profile
        val currentVisibleEventRefsBySurface =
            mergeVisibleSurfacePatches(
                previous =
                    previousInput.visibleEventRefsBySurface,
                patch =
                    inputState.visibleEventRefsBySurface,
            )
        val currentTransientBlocked =
            inputState.transientBlocked
                ?: previousInput.transientBlocked

        val currentInput =
            EventsConvergenceInputState(
                currentScreenId =
                    currentScreenId,
                feed =
                    currentFeed
                        ?.deepCopy(),
                preferences =
                    currentPreferences
                        ?.deepCopy(),
                lab =
                    currentLab
                        ?.deepCopy(),
                profile =
                    currentProfile
                        ?.deepCopy(),
                visibleEventRefsBySurface =
                    currentVisibleEventRefsBySurface
                        ?.mapValues { (_, refs) ->
                            refs.toSet()
                        },
                transientBlocked =
                    currentTransientBlocked,
            )

        /*
         * Form the information that Events should currently preserve for the person.
         *
         * Provider events remain factual whether or not they receive attention. A meaningful local
         * profile-name change joins that same human-facing information stream as a bounded notice,
         * rather than becoming a separate hidden notification path.
         */
        val latestProfileNameChange =
            profileNameChangeFromPatch(
                previousProfile =
                    previousInput.profile,
                incomingProfile =
                    inputState.profile,
            )
                ?: previousOutput
                    .latestProfileNameChange
                    ?.deepCopy()

        val events =
            (
                currentFeed
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

                currentFeed == null ->
                    EventsConvergenceListState.Unobserved

                else ->
                    EventsConvergenceListState.Empty
            }

        /*
         * Respect the person's prior attention choices without rewriting what actually happened.
         *
         * Acknowledge/Suppress finish proactive attention, Suspend keeps an item in the current
         * sequence but moves it behind current work, and Later moves it to the next sequence.
         */
        val dispositions =
            currentPreferences
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
                    currentPreferences
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
                    currentPreferences
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

        /*
         * Keep the person's current attention target stable unless there is a real reason to move on.
         *
         * A proof scenario may deliberately select one target. Otherwise preserve the candidate the
         * person is already dealing with while it remains lawful; only choose a new event when that
         * identity can no longer be retained.
         */
        val candidateEvent =
            if (
                currentFeed != null &&
                currentPreferences != null
            ) {
                val labTargetRef =
                    currentLab
                        ?.currentScenario
                        ?.targetEventRef
                val retainedCandidateRef =
                    previousOutput
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
                    eventRef =
                        event.eventRef,
                    targetElementRef =
                        EventsPresentationRefs.row(
                            event.eventRef,
                        ),
                )
            }

        val eventsSurfaceVisibility =
            currentVisibleEventRefsBySurface
                ?.get(
                    EventsScreen.screenId,
                )

        /*
         * Decide whether asking for attention now would fit the person's current context.
         *
         * Being on another Screen, already handling another transient, or lacking a reachable row
         * should pause presentation, not reinterpret which event matters.
         */
        val availability =
            when {
                currentFeed == null ||
                    currentPreferences == null ||
                    currentScreenId == null ||
                    currentTransientBlocked == null ->
                    EventsConvergenceAvailability.UNOBSERVED

                candidate == null ->
                    EventsConvergenceAvailability.NONE

                currentScreenId != EventsScreen.screenId ->
                    EventsConvergenceAvailability.HELD_OTHER_SCREEN

                currentTransientBlocked ->
                    EventsConvergenceAvailability.HELD_TRANSIENT_OCCUPIED

                /*
                 * At this point canonical feed/preferences/screen/transient truth is already
                 * observed and a concrete candidate exists. A missing LazyList visibility snapshot
                 * means only "target reachability has not reported yet", not "the Events
                 * relationship is unobserved". Classify it with the same bounded presentation hold
                 * as a known off-screen target so an already-composed row can bridge the one-layout
                 * observation gap without weakening other held states.
                 */
                eventsSurfaceVisibility == null ->
                    EventsConvergenceAvailability.HELD_TARGET_UNREACHABLE

                candidate.eventRef !in
                    eventsSurfaceVisibility ->
                    EventsConvergenceAvailability.HELD_TARGET_UNREACHABLE

                else ->
                    EventsConvergenceAvailability.READY
            }

        /*
         * Publish one self-consistent Events answer for every consumer.
         *
         * The Screen should receive already-resolved list, candidate, availability and queue meaning
         * together; it should not have to reconstruct product policy from raw inputs.
         */
        val deferredCount =
            deferredQueue.size
        val suspendedCount =
            suspendedQueue.size
        val currentQueueCount =
            normalQueue.size +
                suspendedCount
        val nextSequenceCount =
            deferredCount
        val unhandledCount =
            currentQueueCount +
                nextSequenceCount
        val activeQueueCount =
            when {
                normalQueue.isNotEmpty() ->
                    currentQueueCount

                suspendedQueue.isNotEmpty() ->
                    suspendedCount

                else ->
                    deferredCount
            }
        val queuedAfterCurrentCount =
            if (candidate == null) {
                0
            } else {
                (activeQueueCount - 1)
                    .coerceAtLeast(0)
            }
        val acknowledgedCount =
            dispositions.values.count { disposition ->
                disposition ==
                    EventsAttentionDisposition.ACKNOWLEDGED
            }
        val suppressedCount =
            dispositions.values.count { disposition ->
                disposition ==
                    EventsAttentionDisposition.SUPPRESSED
            }
        val currentDispositionByEventRef =
            dispositions.toMap()
        val currentScenario =
            currentLab
                ?.currentScenario
                ?.copy()
        val currentProfileNameChange =
            latestProfileNameChange
                ?.deepCopy()

        val currentOutput =
            EventsConvergenceOutputState(
                list =
                    listState,
                candidate =
                    candidate,
                availability =
                    availability,
                dispositionByEventRef =
                    currentDispositionByEventRef,
                scenario =
                    currentScenario,
                latestProfileNameChange =
                    currentProfileNameChange,
                unhandledCount =
                    unhandledCount,
                queuedAfterCurrentCount =
                    queuedAfterCurrentCount,
                acknowledgedCount =
                    acknowledgedCount,
                deferredCount =
                    deferredCount,
                suspendedCount =
                    suspendedCount,
                suppressedCount =
                    suppressedCount,
                currentQueueCount =
                    currentQueueCount,
                nextSequenceCount =
                    nextSequenceCount,
            )

        return currentState.copy(
            inputState =
                currentInput,
            outputState =
                currentOutput,
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
