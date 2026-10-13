package com.vextreme.vexlife.android.screens.primary.events.traditionalApproach

import com.vextreme.vexlife.android.data.events.EventsAttentionDisposition
import com.vextreme.vexlife.android.data.events.RelayEventsAttentionLabRepository
import com.vextreme.vexlife.android.data.events.RelayEventsAttentionPreferenceRepository
import com.vextreme.vexlife.android.data.events.RelayEventsRepository
import com.vextreme.vexlife.android.data.events.VexExternalEvent
import com.vextreme.vexlife.android.data.events.VexExternalEventKind
import com.vextreme.vexlife.android.data.events.VexExternalEventSourceSnapshot
import com.vextreme.vexlife.android.data.userprofile.InMemoryUserProfileRepository
import com.vextreme.vexlife.android.data.userprofile.UserProfile
import com.vextreme.vexlife.android.data.userprofile.UserProfileRefs
import com.vextreme.vexlife.android.screens.primary.events.EventsConvergenceListState
import com.vextreme.vexlife.android.screens.primary.events.EventsConvergenceOutputState
import com.vextreme.vexlife.android.screens.primary.events.EventsConvergenceRelay
import com.vextreme.vexlife.android.screens.primary.events.EventsScreen
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Candidate parity proof for the architecture comparison.
 *
 * IMPORTANT: equality here means "same selected human projection for the same inputs". It does not
 * make the implementations architecturally equivalent and it does not let either implementation
 * reuse the other's reducer.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EventsArchitectureParityTest {
    @Test
    fun currentRelayAndTraditionalControllerAgreeOnCoreHumanProjection() = runTest {
        val events = RelayEventsRepository()
        val preferences = RelayEventsAttentionPreferenceRepository()
        val lab = RelayEventsAttentionLabRepository()
        val profiles =
            InMemoryUserProfileRepository(
                listOf(
                    UserProfile(
                        profileRef = UserProfileRefs.local,
                        displayName = "Victor",
                        updatedAt = Instant.parse("2026-10-13T00:00:00Z"),
                    ),
                ),
            )

        val current =
            EventsConvergenceRelay(
                eventsRepository = events,
                attentionPreferences = preferences,
                attentionLab = lab,
                userProfile = profiles,
            )
        val traditional =
            TraditionalEventsController(
                eventsRepository = events,
                attentionPreferences = preferences,
                attentionLab = lab,
                userProfile = profiles,
            )

        current.activate(backgroundScope)
        traditional.start(backgroundScope)

        current.surfaceChanged(
            screenId = EventsScreen.screenId,
            transientBlocked = false,
        )
        traditional.surfaceChanged(
            screenId = EventsScreen.screenId,
            blockedByTransient = false,
        )

        val sourceRef = "source.architecture-comparison"
        events.accept(
            VexExternalEventSourceSnapshot(
                sourceRef = sourceRef,
                sourceRevision = 1L,
                events =
                    listOf(
                        VexExternalEvent(
                            eventRef = "event.compare.first",
                            sourceRef = sourceRef,
                            kind = VexExternalEventKind.MESSAGE,
                            title = "First",
                            detail = "First",
                            occurredAtEpochMillis = 10L,
                        ),
                        VexExternalEvent(
                            eventRef = "event.compare.second",
                            sourceRef = sourceRef,
                            kind = VexExternalEventKind.CALL,
                            title = "Second",
                            detail = "Second",
                            occurredAtEpochMillis = 20L,
                        ),
                    ),
            ),
        )

        val visible =
            setOf(
                "event.compare.first",
                "event.compare.second",
            )
        current.visibleTargetsChanged(visible)
        traditional.visibleTargetsChanged(visible)
        runCurrent()

        assertEquals(
            current.currentOutput.comparable(),
            traditional.current.comparable(),
        )

        preferences.suspend("event.compare.first")
        runCurrent()
        assertEquals(
            current.currentOutput.comparable(),
            traditional.current.comparable(),
        )

        preferences.defer("event.compare.second")
        runCurrent()
        assertEquals(
            current.currentOutput.comparable(),
            traditional.current.comparable(),
        )

        profiles.updateDisplayName(
            profileRef = UserProfileRefs.local,
            displayName = "Vex",
            updatedAt = Instant.parse("2026-10-13T00:01:00Z"),
        )
        runCurrent()
        assertEquals(
            current.currentOutput.comparable(),
            traditional.current.comparable(),
        )
    }
}

private data class ComparableEventsProjection(
    val listMeaning: String,
    val eventRefs: List<String>,
    val candidateRef: String?,
    val availability: String,
    val dispositions: Map<String, EventsAttentionDisposition>,
    val scenarioRef: String?,
    val latestProfileEventRef: String?,
    val latestProfileTitle: String?,
    val latestProfileDetail: String?,
    val unhandledCount: Int,
    val queuedAfterCurrentCount: Int,
    val acknowledgedCount: Int,
    val deferredCount: Int,
    val suspendedCount: Int,
    val suppressedCount: Int,
    val currentQueueCount: Int,
    val nextSequenceCount: Int,
)

private fun EventsConvergenceOutputState.comparable(): ComparableEventsProjection =
    ComparableEventsProjection(
        listMeaning =
            when (list) {
                EventsConvergenceListState.Unobserved -> "UNOBSERVED"
                EventsConvergenceListState.Empty -> "EMPTY"
                is EventsConvergenceListState.Content -> "CONTENT"
            },
        eventRefs = events.map(VexExternalEvent::eventRef),
        candidateRef = candidate?.eventRef,
        availability = availability.name,
        dispositions = dispositionByEventRef,
        scenarioRef = scenario?.scenarioRef,
        latestProfileEventRef = latestProfileNameChange?.eventRef,
        latestProfileTitle = latestProfileNameChange?.title,
        latestProfileDetail = latestProfileNameChange?.detail,
        unhandledCount = unhandledCount,
        queuedAfterCurrentCount = queuedAfterCurrentCount,
        acknowledgedCount = acknowledgedCount,
        deferredCount = deferredCount,
        suspendedCount = suspendedCount,
        suppressedCount = suppressedCount,
        currentQueueCount = currentQueueCount,
        nextSequenceCount = nextSequenceCount,
    )

private fun TraditionalEventsUiState.comparable(): ComparableEventsProjection =
    ComparableEventsProjection(
        listMeaning =
            when (list) {
                TraditionalEventsListState.Unobserved -> "UNOBSERVED"
                TraditionalEventsListState.Empty -> "EMPTY"
                is TraditionalEventsListState.Content -> "CONTENT"
            },
        eventRefs = events.map(VexExternalEvent::eventRef),
        candidateRef = candidate?.eventRef,
        availability = availability.name,
        dispositions = dispositionByEventRef,
        scenarioRef = scenario?.scenarioRef,
        latestProfileEventRef = latestProfileNameChange?.eventRef,
        latestProfileTitle = latestProfileNameChange?.title,
        latestProfileDetail = latestProfileNameChange?.detail,
        unhandledCount = unhandledCount,
        queuedAfterCurrentCount = queuedAfterCurrentCount,
        acknowledgedCount = acknowledgedCount,
        deferredCount = deferredCount,
        suspendedCount = suspendedCount,
        suppressedCount = suppressedCount,
        currentQueueCount = currentQueueCount,
        nextSequenceCount = nextSequenceCount,
    )

// [VXG RealForever][EVENTS][ARCHITECTURE-PARITY]
