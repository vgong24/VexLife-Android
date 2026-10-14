package com.vextreme.vexlife.android.screens.primary.events.traditionalApproach

import com.vextreme.vexlife.android.data.events.EventsAttentionDisposition
import com.vextreme.vexlife.android.data.events.RelayEventsAttentionLabRepository
import com.vextreme.vexlife.android.data.events.RelayEventsAttentionPreferenceRepository
import com.vextreme.vexlife.android.data.events.RelayEventsRepository
import com.vextreme.vexlife.android.data.events.RelayEventsSourceAvailabilityRepository
import com.vextreme.vexlife.android.data.events.VexExternalEvent
import com.vextreme.vexlife.android.data.events.VexExternalEventKind
import com.vextreme.vexlife.android.data.events.VexExternalEventSourceSnapshot
import com.vextreme.vexlife.android.screens.primary.events.EventsConvergenceListState
import com.vextreme.vexlife.android.screens.primary.events.EventsConvergenceOutputState
import com.vextreme.vexlife.android.screens.primary.events.EventsConvergenceRelay
import com.vextreme.vexlife.android.screens.primary.events.EventsScreen
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
        val sourceAvailability = RelayEventsSourceAvailabilityRepository()
        val current =
            EventsConvergenceRelay(
                eventsRepository = events,
                attentionPreferences = preferences,
                attentionLab = lab,
                sourceAvailability = sourceAvailability,
            )
        val traditional =
            TraditionalEventsController(
                eventsRepository = events,
                attentionPreferences = preferences,
                attentionLab = lab,
                sourceAvailabilityRepository = sourceAvailability,
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

    @Test
    fun currentRelayAndTraditionalControllerAgreeAcrossSurfaceAndProviderPressure() = runTest {
        val events = RelayEventsRepository()
        val preferences = RelayEventsAttentionPreferenceRepository()
        val lab = RelayEventsAttentionLabRepository()
        val sourceAvailability = RelayEventsSourceAvailabilityRepository()

        val current =
            EventsConvergenceRelay(
                eventsRepository = events,
                attentionPreferences = preferences,
                attentionLab = lab,
                sourceAvailability = sourceAvailability,
            )
        val traditional =
            TraditionalEventsController(
                eventsRepository = events,
                attentionPreferences = preferences,
                attentionLab = lab,
                sourceAvailabilityRepository = sourceAvailability,
            )

        current.activate(backgroundScope)
        traditional.start(backgroundScope)
        runCurrent()

        current.surfaceChanged(
            screenId = "screen.home",
            transientBlocked = false,
        )
        traditional.surfaceChanged(
            screenId = "screen.home",
            blockedByTransient = false,
        )

        val sourceRef = "source.architecture-surface-pressure"
        val first =
            VexExternalEvent(
                eventRef = "event.compare.surface.first",
                sourceRef = sourceRef,
                kind = VexExternalEventKind.MESSAGE,
                title = "First",
                detail = "First",
                occurredAtEpochMillis = 10L,
            )
        val second =
            VexExternalEvent(
                eventRef = "event.compare.surface.second",
                sourceRef = sourceRef,
                kind = VexExternalEventKind.CALL,
                title = "Second",
                detail = "Second",
                occurredAtEpochMillis = 20L,
            )
        val historical =
            VexExternalEvent(
                eventRef = "event.compare.surface.historical",
                sourceRef = sourceRef,
                kind = VexExternalEventKind.SYSTEM,
                title = "Historical",
                detail = "Historical",
                occurredAtEpochMillis = 5L,
            )

        events.accept(
            VexExternalEventSourceSnapshot(
                sourceRef = sourceRef,
                sourceRevision = 1L,
                events = listOf(first, second),
            ),
        )

        val allVisible =
            setOf(
                first.eventRef,
                second.eventRef,
                historical.eventRef,
            )
        current.visibleTargetsChanged(allVisible)
        traditional.visibleTargetsChanged(allVisible)
        runCurrent()

        assertEquals(
            current.currentOutput.comparable(),
            traditional.current.comparable(),
        )
        assertEquals(
            "HELD_OTHER_SCREEN",
            current.currentOutput.comparable().availability,
        )
        assertEquals(
            first.eventRef,
            current.currentOutput.candidate?.eventRef,
        )

        current.surfaceChanged(
            screenId = EventsScreen.screenId,
            transientBlocked = false,
        )
        traditional.surfaceChanged(
            screenId = EventsScreen.screenId,
            blockedByTransient = false,
        )
        runCurrent()

        assertEquals(
            current.currentOutput.comparable(),
            traditional.current.comparable(),
        )
        assertEquals(
            "READY",
            current.currentOutput.comparable().availability,
        )

        current.visibleTargetsChanged(setOf(second.eventRef))
        traditional.visibleTargetsChanged(setOf(second.eventRef))
        runCurrent()

        assertEquals(
            current.currentOutput.comparable(),
            traditional.current.comparable(),
        )
        assertEquals(
            "HELD_TARGET_UNREACHABLE",
            current.currentOutput.comparable().availability,
        )

        current.visibleTargetsChanged(allVisible)
        traditional.visibleTargetsChanged(allVisible)
        current.surfaceChanged(
            screenId = EventsScreen.screenId,
            transientBlocked = true,
        )
        traditional.surfaceChanged(
            screenId = EventsScreen.screenId,
            blockedByTransient = true,
        )
        runCurrent()

        assertEquals(
            current.currentOutput.comparable(),
            traditional.current.comparable(),
        )
        assertEquals(
            "HELD_TRANSIENT_OCCUPIED",
            current.currentOutput.comparable().availability,
        )

        current.surfaceChanged(
            screenId = EventsScreen.screenId,
            transientBlocked = false,
        )
        traditional.surfaceChanged(
            screenId = EventsScreen.screenId,
            blockedByTransient = false,
        )

        events.accept(
            VexExternalEventSourceSnapshot(
                sourceRef = sourceRef,
                sourceRevision = 2L,
                events =
                    listOf(
                        second.copy(
                            occurredAtEpochMillis = 30L,
                        ),
                        historical,
                        first,
                    ),
            ),
        )
        runCurrent()

        assertEquals(
            current.currentOutput.comparable(),
            traditional.current.comparable(),
        )
        assertEquals(
            first.eventRef,
            current.currentOutput.candidate?.eventRef,
        )

        events.accept(
            VexExternalEventSourceSnapshot(
                sourceRef = sourceRef,
                sourceRevision = 3L,
                events = listOf(second, historical),
            ),
        )
        runCurrent()

        assertEquals(
            current.currentOutput.comparable(),
            traditional.current.comparable(),
        )
        assertEquals(
            historical.eventRef,
            current.currentOutput.candidate?.eventRef,
        )
    }
    @Test
    fun currentRelayAndTraditionalControllerAgreeWhenSourceAvailabilityChanges() = runTest {
        val events = RelayEventsRepository()
        val preferences = RelayEventsAttentionPreferenceRepository()
        val lab = RelayEventsAttentionLabRepository()
        val sourceAvailability = RelayEventsSourceAvailabilityRepository()

        val current =
            EventsConvergenceRelay(
                eventsRepository = events,
                attentionPreferences = preferences,
                attentionLab = lab,
                sourceAvailability = sourceAvailability,
            )
        val traditional =
            TraditionalEventsController(
                eventsRepository = events,
                attentionPreferences = preferences,
                attentionLab = lab,
                sourceAvailabilityRepository = sourceAvailability,
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

        val unavailableSourceRef =
            "source.architecture-availability.unavailable"
        val availableSourceRef =
            "source.architecture-availability.available"
        val unavailableEvent =
            VexExternalEvent(
                eventRef = "event.compare.availability.unavailable",
                sourceRef = unavailableSourceRef,
                kind = VexExternalEventKind.MESSAGE,
                title = "Unavailable source",
                detail = "Visible in Events, not eligible for attention.",
                occurredAtEpochMillis = 10L,
            )
        val availableEvent =
            VexExternalEvent(
                eventRef = "event.compare.availability.available",
                sourceRef = availableSourceRef,
                kind = VexExternalEventKind.CALL,
                title = "Available source",
                detail = "Eligible for attention.",
                occurredAtEpochMillis = 20L,
            )

        events.accept(
            VexExternalEventSourceSnapshot(
                sourceRef = unavailableSourceRef,
                sourceRevision = 1L,
                events = listOf(unavailableEvent),
            ),
        )
        events.accept(
            VexExternalEventSourceSnapshot(
                sourceRef = availableSourceRef,
                sourceRevision = 1L,
                events = listOf(availableEvent),
            ),
        )

        val visible =
            setOf(
                unavailableEvent.eventRef,
                availableEvent.eventRef,
            )
        current.visibleTargetsChanged(visible)
        traditional.visibleTargetsChanged(visible)
        runCurrent()

        assertEquals(
            current.currentOutput.comparable(),
            traditional.current.comparable(),
        )
        assertEquals(
            unavailableEvent.eventRef,
            current.currentOutput.candidate?.eventRef,
        )

        sourceAvailability.setAvailable(
            sourceRef = unavailableSourceRef,
            available = false,
        )
        runCurrent()

        assertEquals(
            current.currentOutput.comparable(),
            traditional.current.comparable(),
        )
        assertEquals(
            listOf(
                availableEvent.eventRef,
                unavailableEvent.eventRef,
            ),
            current.currentOutput.events
                .map(VexExternalEvent::eventRef),
        )
        assertEquals(
            availableEvent.eventRef,
            current.currentOutput.candidate?.eventRef,
        )
        assertEquals(
            "READY",
            current.currentOutput.comparable().availability,
        )

        sourceAvailability.setAvailable(
            sourceRef = availableSourceRef,
            available = false,
        )
        runCurrent()

        assertEquals(
            current.currentOutput.comparable(),
            traditional.current.comparable(),
        )
        assertEquals(
            null,
            current.currentOutput.candidate?.eventRef,
        )
        assertEquals(
            "NONE",
            current.currentOutput.comparable().availability,
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
