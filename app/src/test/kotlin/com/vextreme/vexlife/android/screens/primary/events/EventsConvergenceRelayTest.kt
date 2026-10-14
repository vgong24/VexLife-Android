package com.vextreme.vexlife.android.screens.primary.events

import com.vextreme.vexlife.android.data.events.EventsAlertDisposition
import com.vextreme.vexlife.android.data.events.EventsDebugMenuAlertMode
import com.vextreme.vexlife.android.data.events.EventsDebugMenuAlertPlacement
import com.vextreme.vexlife.android.data.events.EventsDebugMenuAlertExpectedAction
import com.vextreme.vexlife.android.data.events.EventsDebugMenuAlertCase
import com.vextreme.vexlife.android.data.events.EventsDebugMenuAlertCaseType
import com.vextreme.vexlife.android.data.events.EventsDebugMenuAlertState
import com.vextreme.vexlife.android.data.events.EventsAlertPreferenceState
import com.vextreme.vexlife.android.data.events.EventsFeedState
import com.vextreme.vexlife.android.data.events.RelayEventsDebugMenuAlertRepository
import com.vextreme.vexlife.android.data.events.RelayEventsAlertPreferenceRepository
import com.vextreme.vexlife.android.data.events.RelayEventsRepository
import com.vextreme.vexlife.android.data.events.VexExternalEvent
import com.vextreme.vexlife.android.data.events.VexExternalEventKind
import com.vextreme.vexlife.android.data.events.VexExternalEventSourceSnapshot
import com.vextreme.vexlife.android.data.userprofile.InMemoryUserProfileRepository
import com.vextreme.vexlife.android.data.userprofile.UserProfile
import com.vextreme.vexlife.android.data.userprofile.UserProfileRefs
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EventsConvergenceRelayTest {
    private fun feed(): EventsFeedState {
        val source = "source.test"
        val first =
            VexExternalEvent(
                "event.first",
                source,
                VexExternalEventKind.MESSAGE,
                "First",
                "First",
                10L,
            )
        val second =
            VexExternalEvent(
                "event.second",
                source,
                VexExternalEventKind.CALL,
                "Second",
                "Second",
                20L,
            )

        return EventsFeedState(
            sourceSnapshots =
                mapOf(
                    source to
                        VexExternalEventSourceSnapshot(
                            source,
                            1L,
                            listOf(first, second),
                        ),
                ),
        )
    }

    @Test
    fun listProjectionDistinguishesUnobservedEmptyAndContent() {
        val relay =
            EventsConvergenceRelay()

        assertEquals(
            EventsConvergenceListState.Unobserved,
            relay.currentOutput.list,
        )

        relay.feedChanged(
            EventsFeedState(),
        )

        assertEquals(
            EventsConvergenceListState.Empty,
            relay.currentOutput.list,
        )

        relay.feedChanged(
            feed(),
        )

        val content =
            relay.currentOutput.list as
                EventsConvergenceListState.Content

        assertEquals(
            listOf(
                "event.second",
                "event.first",
            ),
            content.events.map(
                VexExternalEvent::eventRef,
            ),
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun outputProjectionSuppressesInputOnlyCompoundRevisions() = runTest {
        val relay = EventsConvergenceRelay()

        relay.feedChanged(feed())
        relay.alertPreferencesChanged(
            EventsAlertPreferenceState(),
        )
        relay.visibleTargetsChanged(
            setOf("event.first", "event.second"),
        )
        relay.transientBlockedChanged(false)
        relay.screenChanged("screen.home")

        val observed =
            mutableListOf<EventsConvergenceOutputState>()

        backgroundScope.launch {
            relay.outputStates.collect { output ->
                observed += output
            }
        }
        runCurrent()

        assertEquals(1, observed.size)
        assertEquals(
            EventsConvergenceAvailability.HELD_OTHER_SCREEN,
            observed.single().availability,
        )

        // This changes retained compound input/revision, but while another Screen is current
        // the presentation output is still HELD_OTHER_SCREEN and should not re-emit.
        relay.transientBlockedChanged(true)
        runCurrent()

        assertEquals(1, observed.size)
    }

    @Test
    fun candidateIdentitySurvivesScreenReachabilityAndTransientChanges() {
        val relay = EventsConvergenceRelay()

        assertNull(relay.current.outputState.candidate)
        assertEquals(
            EventsConvergenceAvailability.UNOBSERVED,
            relay.current.outputState.availability,
        )

        relay.feedChanged(feed())
        relay.alertPreferencesChanged(
            EventsAlertPreferenceState(),
        )
        relay.visibleTargetsChanged(
            setOf("event.first", "event.second"),
        )
        relay.transientBlockedChanged(false)
        relay.screenChanged("screen.home")

        assertEquals(
            "event.first",
            relay.current.outputState.candidate?.eventRef,
        )
        assertEquals(
            EventsConvergenceAvailability.HELD_OTHER_SCREEN,
            relay.current.outputState.availability,
        )

        relay.screenChanged(EventsScreen.screenId)

        assertEquals(
            "event.first",
            relay.current.outputState.candidate?.eventRef,
        )
        assertEquals(
            EventsConvergenceAvailability.READY,
            relay.current.outputState.availability,
        )
        assertEquals(
            2,
            relay.current.outputState.unhandledCount,
        )
        assertEquals(
            1,
            relay.current.outputState.queuedAfterCurrentCount,
        )

        relay.visibleTargetsChanged(
            setOf("event.second"),
        )

        assertEquals(
            "event.first",
            relay.current.outputState.candidate?.eventRef,
        )
        assertEquals(
            EventsConvergenceAvailability.HELD_TARGET_UNREACHABLE,
            relay.current.outputState.availability,
        )

        relay.visibleTargetsChanged(
            setOf("event.first", "event.second"),
        )
        relay.transientBlockedChanged(true)

        assertEquals(
            "event.first",
            relay.current.outputState.candidate?.eventRef,
        )
        assertEquals(
            EventsConvergenceAvailability.HELD_TRANSIENT_OCCUPIED,
            relay.current.outputState.availability,
        )

        relay.transientBlockedChanged(false)
        assertEquals(
            EventsConvergenceAvailability.READY,
            relay.current.outputState.availability,
        )

        relay.alertPreferencesChanged(
            EventsAlertPreferenceState(
                mapOf(
                    "event.first" to
                        EventsAlertDisposition.ACKNOWLEDGED,
                ),
            ),
        )

        assertEquals(
            "event.second",
            relay.current.outputState.candidate?.eventRef,
        )
        assertEquals(
            EventsConvergenceAvailability.READY,
            relay.current.outputState.availability,
        )
        assertEquals(
            1,
            relay.current.outputState.unhandledCount,
        )
        assertEquals(
            0,
            relay.current.outputState.queuedAfterCurrentCount,
        )
        assertEquals(
            1,
            relay.current.outputState.acknowledgedCount,
        )
        assertEquals(
            0,
            relay.current.outputState.deferredCount,
        )
        assertEquals(
            0,
            relay.current.outputState.suppressedCount,
        )
    }

    @Test
    fun suspendStaysInCurrentQueueWhileLaterMovesToNextSequence() {
        val source = "source.queue"
        val first =
            VexExternalEvent(
                "event.first",
                source,
                VexExternalEventKind.MESSAGE,
                "First",
                "First",
                10L,
            )
        val second =
            VexExternalEvent(
                "event.second",
                source,
                VexExternalEventKind.CALL,
                "Second",
                "Second",
                20L,
            )
        val third =
            VexExternalEvent(
                "event.third",
                source,
                VexExternalEventKind.MAP,
                "Third",
                "Third",
                30L,
            )

        val feed =
            EventsFeedState(
                sourceSnapshots =
                    mapOf(
                        source to
                            VexExternalEventSourceSnapshot(
                                source,
                                1L,
                                listOf(first, second, third),
                            ),
                    ),
            )

        val relay = EventsConvergenceRelay()
        relay.feedChanged(feed)
        relay.visibleTargetsChanged(
            setOf(
                first.eventRef,
                second.eventRef,
                third.eventRef,
            ),
        )
        relay.transientBlockedChanged(false)
        relay.screenChanged(EventsScreen.screenId)
        relay.alertPreferencesChanged(
            EventsAlertPreferenceState(),
        )

        assertEquals(
            first.eventRef,
            relay.current.outputState.candidate?.eventRef,
        )

        relay.alertPreferencesChanged(
            EventsAlertPreferenceState(
                mapOf(
                    first.eventRef to
                        EventsAlertDisposition.SUSPENDED,
                ),
            ),
        )

        assertEquals(
            second.eventRef,
            relay.current.outputState.candidate?.eventRef,
        )
        assertEquals(3, relay.current.outputState.currentQueueCount)
        assertEquals(0, relay.current.outputState.nextSequenceCount)
        assertEquals(1, relay.current.outputState.suspendedCount)

        relay.alertPreferencesChanged(
            EventsAlertPreferenceState(
                mapOf(
                    first.eventRef to
                        EventsAlertDisposition.SUSPENDED,
                    second.eventRef to
                        EventsAlertDisposition.DEFERRED,
                ),
            ),
        )

        assertEquals(
            third.eventRef,
            relay.current.outputState.candidate?.eventRef,
        )
        assertEquals(2, relay.current.outputState.currentQueueCount)
        assertEquals(1, relay.current.outputState.nextSequenceCount)

        relay.alertPreferencesChanged(
            EventsAlertPreferenceState(
                mapOf(
                    first.eventRef to
                        EventsAlertDisposition.SUSPENDED,
                    second.eventRef to
                        EventsAlertDisposition.DEFERRED,
                    third.eventRef to
                        EventsAlertDisposition.ACKNOWLEDGED,
                ),
            ),
        )

        assertEquals(
            first.eventRef,
            relay.current.outputState.candidate?.eventRef,
        )

        relay.alertPreferencesChanged(
            EventsAlertPreferenceState(
                mapOf(
                    first.eventRef to
                        EventsAlertDisposition.ACKNOWLEDGED,
                    second.eventRef to
                        EventsAlertDisposition.DEFERRED,
                    third.eventRef to
                        EventsAlertDisposition.ACKNOWLEDGED,
                ),
            ),
        )

        assertEquals(
            second.eventRef,
            relay.current.outputState.candidate?.eventRef,
        )
        assertEquals(0, relay.current.outputState.currentQueueCount)
        assertEquals(1, relay.current.outputState.nextSequenceCount)
    }

    @Test
    fun debugMenuAlertCaseTargetsExactStableEventRefInsteadOfVisualPositionGuessing() {
        val relay = EventsConvergenceRelay()
        val state = feed()

        relay.feedChanged(state)
        relay.alertPreferencesChanged(
            EventsAlertPreferenceState(),
        )
        relay.visibleTargetsChanged(
            setOf("event.first", "event.second"),
        )
        relay.transientBlockedChanged(false)
        relay.screenChanged(EventsScreen.screenId)

        relay.debugMenuAlertChanged(
            EventsDebugMenuAlertState(
                currentAlertCase =
                    EventsDebugMenuAlertCase(
                        alertCaseRef = "scenario.test.middle",
                        ordinal = 2L,
                        mode = EventsDebugMenuAlertMode.ROTATION,
                        seed = 1L,
                        type =
                            EventsDebugMenuAlertCaseType.ANCHORED_MIDDLE,
                        targetEventRef = "event.second",
                        targetPositionFromTop = 0,
                        listCountAtPlan = 2,
                        preferredPlacement =
                            EventsDebugMenuAlertPlacement.ABOVE,
                        expectedAction =
                            EventsDebugMenuAlertExpectedAction.LATER,
                        freezeProjection = false,
                        blockingAcknowledgeOnly = false,
                    ),
            ),
        )

        assertEquals(
            "event.second",
            relay.current.outputState.candidate?.eventRef,
        )
        assertEquals(
            EventsConvergenceAvailability.READY,
            relay.current.outputState.availability,
        )
    }

    @Test
    fun candidateIdentitySurvivesProviderInsertReorderAndRemoval() {
        val source = "source.reorder"
        val first =
            VexExternalEvent(
                "event.first",
                source,
                VexExternalEventKind.MESSAGE,
                "First",
                "First",
                10L,
            )
        val second =
            VexExternalEvent(
                "event.second",
                source,
                VexExternalEventKind.CALL,
                "Second",
                "Second",
                20L,
            )
        val insertedOlder =
            VexExternalEvent(
                "event.inserted-older",
                source,
                VexExternalEventKind.MAP,
                "Inserted older",
                "Inserted older",
                5L,
            )

        fun feed(
            revision: Long,
            events: List<VexExternalEvent>,
        ) =
            EventsFeedState(
                sourceSnapshots =
                    mapOf(
                        source to
                            VexExternalEventSourceSnapshot(
                                source,
                                revision,
                                events,
                            ),
                    ),
            )

        val relay = EventsConvergenceRelay()

        relay.feedChanged(feed(1L, listOf(first, second)))
        relay.alertPreferencesChanged(
            EventsAlertPreferenceState(),
        )
        relay.visibleTargetsChanged(
            setOf(
                first.eventRef,
                second.eventRef,
                insertedOlder.eventRef,
            ),
        )
        relay.transientBlockedChanged(false)
        relay.screenChanged(EventsScreen.screenId)

        assertEquals(
            first.eventRef,
            relay.current.outputState.candidate?.eventRef,
        )

        // A later provider revision inserts an even older item and reorders the list.
        // Existing candidate identity must remain stable while it is still present/unhandled.
        relay.feedChanged(
            feed(
                2L,
                listOf(
                    second.copy(
                        occurredAtEpochMillis = 30L,
                    ),
                    insertedOlder,
                    first,
                ),
            ),
        )

        assertEquals(
            first.eventRef,
            relay.current.outputState.candidate?.eventRef,
        )

        // If the provider removes the retained candidate from its current projection,
        // the Relay may advance to the next unhandled event.
        relay.feedChanged(
            feed(
                3L,
                listOf(
                    second,
                    insertedOlder,
                ),
            ),
        )

        assertEquals(
            insertedOlder.eventRef,
            relay.current.outputState.candidate?.eventRef,
        )
    }


    @Test
    fun declaredDependenciesObserveProfileNameChangeWithoutComposeForwarding() = runTest {
        val events =
            RelayEventsRepository()
        val alertPreferences =
            RelayEventsAlertPreferenceRepository()
        val debugMenuAlert =
            RelayEventsDebugMenuAlertRepository()
        val initialProfile =
            UserProfile(
                profileRef = UserProfileRefs.local,
                displayName = "Victor",
                updatedAt = Instant.parse("2026-10-12T18:00:00Z"),
            )
        val profiles =
            InMemoryUserProfileRepository(
                listOf(initialProfile),
            )
        val relay =
            EventsConvergenceRelay(
                eventsRepository = events,
                alertPreferences = preferences,
                debugMenuAlerts = lab,
                userProfile = profiles,
            )

        relay.activate(backgroundScope)
        relay.surfaceChanged(
            screenId = EventsScreen.screenId,
            transientBlocked = false,
        )

        withTimeout(1_000L) {
            relay.inputStates.first { input ->
                input.feed != null &&
                    input.preferences != null &&
                    input.profile?.profile?.displayName == "Victor"
            }
        }

        // Initial observation establishes current profile truth. It is not itself a name-change event.
        assertEquals(
            emptyList<VexExternalEvent>(),
            relay.currentOutput.events,
        )

        profiles.upsert(
            initialProfile.copy(
                displayName = "Vex",
                updatedAt = Instant.parse("2026-10-12T18:01:00Z"),
            ),
        )

        val changed =
            withTimeout(1_000L) {
                relay.outputStates.first { output ->
                    output.latestProfileNameChange != null
                }
            }
        val notice =
            checkNotNull(
                changed.latestProfileNameChange,
            )

        assertEquals(
            VexExternalEventKind.SYSTEM,
            notice.type,
        )
        assertEquals(
            "Profile name changed",
            notice.title,
        )
        assertEquals(
            "Victor → Vex",
            notice.detail,
        )
        assertEquals(
            notice,
            changed.events.single(),
        )

        relay.visibleTargetsChanged(
            setOf(notice.eventRef),
        )

        assertEquals(
            notice.eventRef,
            relay.currentOutput.candidate?.eventRef,
        )
        assertEquals(
            EventsConvergenceAvailability.READY,
            relay.currentOutput.availability,
        )
    }

    @Test
    fun handledFeedExposesNoneInsteadOfUnobserved() {
        val relay = EventsConvergenceRelay()

        relay.feedChanged(feed())
        relay.alertPreferencesChanged(
            EventsAlertPreferenceState(
                mapOf(
                    "event.first" to
                        EventsAlertDisposition.ACKNOWLEDGED,
                    "event.second" to
                        EventsAlertDisposition.SUPPRESSED,
                ),
            ),
        )
        relay.visibleTargetsChanged(
            setOf("event.first", "event.second"),
        )
        relay.transientBlockedChanged(false)
        relay.screenChanged(EventsScreen.screenId)

        assertNull(relay.current.outputState.candidate)
        assertEquals(
            EventsConvergenceAvailability.NONE,
            relay.current.outputState.availability,
        )
        assertEquals(0, relay.current.outputState.unhandledCount)
        assertEquals(0, relay.current.outputState.queuedAfterCurrentCount)
        assertEquals(1, relay.current.outputState.acknowledgedCount)
        assertEquals(0, relay.current.outputState.deferredCount)
        assertEquals(1, relay.current.outputState.suppressedCount)
    }
}

// [VXG RealForever]
