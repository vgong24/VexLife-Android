package com.vextreme.vexlife.android.screens.primary.events.traditionalApproach

import com.vextreme.vexlife.android.data.events.PartnerTransitAdvisoryBatch
import com.vextreme.vexlife.android.data.events.PartnerTransitAdvisoryRecord
import com.vextreme.vexlife.android.data.events.PartnerTransitAdvisoryStatus
import com.vextreme.vexlife.android.data.events.RelayEventsAttentionLabRepository
import com.vextreme.vexlife.android.data.events.RelayEventsAttentionPreferenceRepository
import com.vextreme.vexlife.android.data.events.RelayEventsRepository
import com.vextreme.vexlife.android.data.events.RelayEventsSourceAvailabilityRepository
import com.vextreme.vexlife.android.data.events.toVexEvents
import com.vextreme.vexlife.android.screens.primary.events.EventsConvergenceRelay
import com.vextreme.vexlife.android.screens.primary.events.EventsScreen
import com.vextreme.vexlife.android.screens.primary.events.toPresentationState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PartnerTransitAdvisoryArchitectureParityTest {
    @Test
    fun normalizedPartnerSnapshotNeedsNoPartnerLogicInEitherArchitecture() = runTest {
        val events = RelayEventsRepository()
        val preferences = RelayEventsAttentionPreferenceRepository()
        val lab = RelayEventsAttentionLabRepository()
        val sourceAvailability =
            RelayEventsSourceAvailabilityRepository()

        val relay =
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

        relay.activate(backgroundScope)
        traditional.start(backgroundScope)

        relay.surfaceChanged(
            screenId = EventsScreen.screenId,
            transientBlocked = false,
        )
        traditional.surfaceChanged(
            screenId = EventsScreen.screenId,
            blockedByTransient = false,
        )

        val converted =
            PartnerTransitAdvisoryBatch(
                partnerAccountId = "partner account? west",
                opaqueCursor = "cursor?opaque=17",
                fetchedAtEpochSeconds = 200L,
                records =
                    listOf(
                        PartnerTransitAdvisoryRecord(
                            remoteId = "delay/A?1",
                            status =
                                PartnerTransitAdvisoryStatus.ACTIVE,
                            severityCode = 4,
                            headline = "Airport route delay",
                            description = "Signal issue",
                            affectedRouteNames =
                                listOf("Airport"),
                            startsAtEpochSeconds = 210L,
                        ),
                        PartnerTransitAdvisoryRecord(
                            remoteId = "cancelled/B",
                            status =
                                PartnerTransitAdvisoryStatus.CANCELLED,
                            severityCode = 1,
                            headline = "Resolved",
                            description = "Should not enter Events",
                            affectedRouteNames =
                                listOf("Downtown"),
                            startsAtEpochSeconds = 190L,
                        ),
                    ),
            ).toVexEvents(
                canonicalRevision = 1L,
            )

        events.accept(converted.snapshot)

        val visibleRefs =
            converted.snapshot.events
                .map { event ->
                    event.eventRef
                }
                .toSet()

        relay.visibleTargetsChanged(visibleRefs)
        traditional.visibleTargetsChanged(visibleRefs)
        runCurrent()

        assertEquals(
            relay.currentOutput.toPresentationState(),
            traditional.current.toPresentationState(),
        )
        assertEquals(
            listOf("Airport route delay"),
            relay.currentOutput.events
                .map { event ->
                    event.title
                },
        )
        assertTrue(
            relay.currentOutput.events
                .all { event ->
                    event.sourceRef ==
                        converted.snapshot.sourceRef
                },
        )
    }
}

// [VXG RealForever][EVENTS][INCOMPATIBLE-PARTNER-ARCHITECTURE-PARITY]
