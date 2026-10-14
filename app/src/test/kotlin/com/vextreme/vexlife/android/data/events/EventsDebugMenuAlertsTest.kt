package com.vextreme.vexlife.android.data.events

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EventsDebugMenuAlertsTest {
    private fun events(count: Int): List<VexExternalEvent> =
        (1..count).map { ordinal ->
            VexExternalEvent(
                eventRef =
                    "event.debug-menu-alert." +
                        ordinal.toString().padStart(2, '0'),
                sourceRef = "source.debug-menu-alert",
                kind = VexExternalEventKind.SYSTEM,
                title = "Debug alert $ordinal",
                detail = "fixture",
                occurredAtEpochMillis = ordinal.toLong(),
            )
        }.reversed()

    @Test
    fun rotationCoversEveryAlertCaseInAStableOrder() {
        val kinds =
            (1L..5L).map { ordinal ->
                planEventsDebugMenuAlertCase(
                    mode = EventsDebugMenuAlertMode.ROTATION,
                    seed = 7L,
                    ordinal = ordinal,
                    events = events(9),
                ).kind
            }

        assertEquals(
            listOf(
                EventsDebugMenuAlertCaseType.ANCHORED_TOP,
                EventsDebugMenuAlertCaseType.ANCHORED_MIDDLE,
                EventsDebugMenuAlertCaseType.ANCHORED_BOTTOM,
                EventsDebugMenuAlertCaseType.ANCHORED_MIDDLE,
                EventsDebugMenuAlertCaseType.BLOCKING_FREEZE_TOP,
            ),
            kinds,
        )
    }

    @Test
    fun rotationAlsoNamesOneExpectedActionPerAlertCase() {
        val alertCases =
            (1L..5L).map { ordinal ->
                planEventsDebugMenuAlertCase(
                    mode = EventsDebugMenuAlertMode.ROTATION,
                    seed = 7L,
                    ordinal = ordinal,
                    events = events(9),
                )
            }

        assertEquals(
            listOf(
                EventsDebugMenuAlertExpectedAction.ACKNOWLEDGE,
                EventsDebugMenuAlertExpectedAction.LATER,
                EventsDebugMenuAlertExpectedAction.SUSPEND,
                EventsDebugMenuAlertExpectedAction.SUPPRESS,
                EventsDebugMenuAlertExpectedAction.ACKNOWLEDGE,
            ),
            alertCases.map(EventsDebugMenuAlertCase::expectedAction),
        )
    }

    @Test
    fun seededRandomIsRepeatableAndSourceAddressable() {
        val first =
            (1L..8L).map { ordinal ->
                planEventsDebugMenuAlertCase(
                    mode = EventsDebugMenuAlertMode.SEEDED_RANDOM,
                    seed = 42L,
                    ordinal = ordinal,
                    events = events(12),
                )
            }
        val replay =
            (1L..8L).map { ordinal ->
                planEventsDebugMenuAlertCase(
                    mode = EventsDebugMenuAlertMode.SEEDED_RANDOM,
                    seed = 42L,
                    ordinal = ordinal,
                    events = events(12),
                )
            }

        assertEquals(
            first.map(EventsDebugMenuAlertCase::type),
            replay.map(EventsDebugMenuAlertCase::type),
        )
        assertEquals(
            first.map(EventsDebugMenuAlertCase::targetEventRef),
            replay.map(EventsDebugMenuAlertCase::targetEventRef),
        )
        assertTrue(
            first.all { alertCase ->
                alertCase.alertCaseRef.contains("seeded_random") &&
                    alertCase.alertCaseRef.contains("seed-42")
            },
        )
    }

    @Test
    fun listCountDeterminesStableTargetPosition() {
        val nine = events(9)

        val top =
            planEventsDebugMenuAlertCase(
                EventsDebugMenuAlertMode.ROTATION,
                seed = 1L,
                ordinal = 1L,
                events = nine,
            )
        val middle =
            planEventsDebugMenuAlertCase(
                EventsDebugMenuAlertMode.ROTATION,
                seed = 1L,
                ordinal = 2L,
                events = nine,
            )
        val bottom =
            planEventsDebugMenuAlertCase(
                EventsDebugMenuAlertMode.ROTATION,
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
    fun concurrentPlanNextCallsUseUniqueAtomicOrdinals() {
        val repository =
            RelayEventsDebugMenuAlertRepository()
        val fixtureEvents =
            events(9)
        val count =
            24
        val ready =
            CountDownLatch(count)
        val go =
            CountDownLatch(1)
        val executor =
            Executors.newFixedThreadPool(count)

        try {
            val futures =
                (0 until count).map {
                    executor.submit<EventsDebugMenuAlertCase> {
                        ready.countDown()
                        go.await()

                        repository.prepareNextAlertCase(
                            fixtureEvents,
                        )
                    }
                }

            assertTrue(
                ready.await(
                    2L,
                    TimeUnit.SECONDS,
                ),
            )
            go.countDown()

            val alertCases =
                futures.map { future ->
                    future.get(
                        3L,
                        TimeUnit.SECONDS,
                    )
                }

            assertEquals(
                (1L..count.toLong()).toSet(),
                alertCases
                    .map(EventsDebugMenuAlertCase::ordinal)
                    .toSet(),
            )
            assertEquals(
                count,
                alertCases
                    .map(EventsDebugMenuAlertCase::alertCaseRef)
                    .toSet()
                    .size,
            )
            assertEquals(
                count.toLong() + 1L,
                repository.current.nextOrdinal,
            )
            assertEquals(
                count.toLong(),
                repository.current.currentAlertCase?.ordinal,
            )
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun blockingAlertCaseFreezesProjectionAndRequiresAcknowledge() {
        val alertCase =
            planEventsDebugMenuAlertCase(
                EventsDebugMenuAlertMode.ROTATION,
                seed = 1L,
                ordinal = 5L,
                events = events(12),
            )

        assertEquals(
            EventsDebugMenuAlertCaseType.BLOCKING_FREEZE_TOP,
            alertCase.type,
        )
        assertTrue(alertCase.freezeProjection)
        assertTrue(alertCase.blockingAcknowledgeOnly)
        assertEquals(
            EventsDebugMenuAlertExpectedAction.ACKNOWLEDGE,
            alertCase.expectedAction,
        )
        assertEquals(0, alertCase.targetPositionFromTop)
        assertEquals(
            EventsDebugMenuAlertPlacement.BELOW,
            alertCase.preferredPlacement,
        )
    }
}

// [VXG RealForever]
