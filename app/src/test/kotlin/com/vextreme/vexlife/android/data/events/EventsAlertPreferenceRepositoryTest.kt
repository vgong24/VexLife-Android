package com.vextreme.vexlife.android.data.events

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EventsAlertPreferenceRepositoryTest {
    @Test
    fun acknowledgementDeferSuppressAndResetRemainSeparateFromEventTruth() {
        val preferences =
            RelayEventsAlertPreferenceRepository()

        preferences.acknowledge("event.a")
        preferences.defer("event.b")
        preferences.suspend("event.c")
        preferences.suppress("event.d")

        assertEquals(
            EventsAlertDisposition.ACKNOWLEDGED,
            preferences.current
                .dispositionByEventRef
                .getValue("event.a"),
        )
        assertEquals(
            EventsAlertDisposition.DEFERRED,
            preferences.current
                .dispositionByEventRef
                .getValue("event.b"),
        )
        assertEquals(
            EventsAlertDisposition.SUSPENDED,
            preferences.current
                .dispositionByEventRef
                .getValue("event.c"),
        )
        assertEquals(
            EventsAlertDisposition.SUPPRESSED,
            preferences.current
                .dispositionByEventRef
                .getValue("event.d"),
        )
        assertEquals(
            1L,
            preferences.current
                .deferredOrderByEventRef
                .getValue("event.b"),
        )
        assertEquals(
            2L,
            preferences.current
                .suspendedOrderByEventRef
                .getValue("event.c"),
        )

        preferences.reset("event.b")

        assertFalse(
            preferences.current
                .dispositionByEventRef
                .containsKey("event.b"),
        )
        assertFalse(
            preferences.current
                .deferredOrderByEventRef
                .containsKey("event.b"),
        )

        preferences.resetAll()

        assertTrue(
            preferences.current
                .dispositionByEventRef
                .isEmpty(),
        )
    }
}

// [VXG RealForever]
