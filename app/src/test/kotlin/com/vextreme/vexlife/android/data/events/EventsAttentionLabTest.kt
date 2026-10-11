package com.vextreme.vexlife.android.data.events

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EventsAttentionLabTest {
    private fun events(count: Int): List<VexExternalEvent> =
        (1..count).map { ordinal ->
            VexExternalEvent(
                eventRef =
                    "event.lab." +
                        ordinal.toString().padStart(2, '0'),
                sourceRef = "source.lab",
                kind = VexExternalEventKind.SYSTEM,
                title = "Lab $ordinal",
                detail = "fixture",
                occurredAtEpochMillis = ordinal.toLong(),
            )
        }.reversed()

    @Test
    fun rotationCoversEveryScenarioInAStableOrder() {
        val kinds =
            (1L..5L).map { ordinal ->
                planEventsAttentionLabScenario(
                    mode = EventsAttentionLabMode.ROTATION,
                    seed = 7L,
                    ordinal = ordinal,
                    events = events(9),
                ).kind
            }

        assertEquals(
            listOf(
                EventsAttentionLabScenarioKind.ANCHORED_TOP,
                EventsAttentionLabScenarioKind.ANCHORED_MIDDLE,
                EventsAttentionLabScenarioKind.ANCHORED_BOTTOM,
                EventsAttentionLabScenarioKind.ANCHORED_MIDDLE,
                EventsAttentionLabScenarioKind.BLOCKING_FREEZE_TOP,
            ),
            kinds,
        )
    }

    @Test
    fun rotationAlsoNamesOneExpectedActionPerScenario() {
        val scenarios =
            (1L..5L).map { ordinal ->
                planEventsAttentionLabScenario(
                    mode = EventsAttentionLabMode.ROTATION,
                    seed = 7L,
                    ordinal = ordinal,
                    events = events(9),
                )
            }

        assertEquals(
            listOf(
                EventsAttentionLabExpectedAction.ACKNOWLEDGE,
                EventsAttentionLabExpectedAction.LATER,
                EventsAttentionLabExpectedAction.SUSPEND,
                EventsAttentionLabExpectedAction.SUPPRESS,
                EventsAttentionLabExpectedAction.ACKNOWLEDGE,
            ),
            scenarios.map(EventsAttentionLabScenario::expectedAction),
        )
    }

    @Test
    fun seededRandomIsRepeatableAndSourceAddressable() {
        val first =
            (1L..8L).map { ordinal ->
                planEventsAttentionLabScenario(
                    mode = EventsAttentionLabMode.SEEDED_RANDOM,
                    seed = 42L,
                    ordinal = ordinal,
                    events = events(12),
                )
            }
        val replay =
            (1L..8L).map { ordinal ->
                planEventsAttentionLabScenario(
                    mode = EventsAttentionLabMode.SEEDED_RANDOM,
                    seed = 42L,
                    ordinal = ordinal,
                    events = events(12),
                )
            }

        assertEquals(
            first.map(EventsAttentionLabScenario::kind),
            replay.map(EventsAttentionLabScenario::kind),
        )
        assertEquals(
            first.map(EventsAttentionLabScenario::targetEventRef),
            replay.map(EventsAttentionLabScenario::targetEventRef),
        )
        assertTrue(
            first.all { scenario ->
                scenario.scenarioRef.contains("seeded_random") &&
                    scenario.scenarioRef.contains("seed-42")
            },
        )
    }

    @Test
    fun listCountDeterminesStableTargetPosition() {
        val nine = events(9)

        val top =
            planEventsAttentionLabScenario(
                EventsAttentionLabMode.ROTATION,
                seed = 1L,
                ordinal = 1L,
                events = nine,
            )
        val middle =
            planEventsAttentionLabScenario(
                EventsAttentionLabMode.ROTATION,
                seed = 1L,
                ordinal = 2L,
                events = nine,
            )
        val bottom =
            planEventsAttentionLabScenario(
                EventsAttentionLabMode.ROTATION,
                seed = 1L,
                ordinal = 3L,
                events = nine,
            )

        assertEquals(0, top.targetPositionFromTop)
        assertEquals(4, middle.targetPositionFromTop)
        assertEquals(8, bottom.targetPositionFromTop)
        assertNotEquals(
            top.targetEventRef,
            middle.targetEventRef,
        )
        assertNotEquals(
            middle.targetEventRef,
            bottom.targetEventRef,
        )
    }

    @Test
    fun blockingScenarioFreezesProjectionAndRequiresAcknowledge() {
        val scenario =
            planEventsAttentionLabScenario(
                EventsAttentionLabMode.ROTATION,
                seed = 1L,
                ordinal = 5L,
                events = events(12),
            )

        assertEquals(
            EventsAttentionLabScenarioKind.BLOCKING_FREEZE_TOP,
            scenario.kind,
        )
        assertTrue(scenario.freezeProjection)
        assertTrue(scenario.blockingAcknowledgeOnly)
        assertEquals(
            EventsAttentionLabExpectedAction.ACKNOWLEDGE,
            scenario.expectedAction,
        )
        assertEquals(0, scenario.targetPositionFromTop)
        assertEquals(
            EventsAttentionLabPlacement.BELOW,
            scenario.preferredPlacement,
        )
    }
}

// [VXG RealForever]
