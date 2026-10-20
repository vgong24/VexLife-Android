package com.vextreme.vexlife.android.data.events

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EventsAttentionPreferenceRepositoryTest {
    @Test
    fun acknowledgementDeferSuppressAndResetRemainSeparateFromEventTruth() {
        val preferences =
            RelayEventsAttentionPreferenceRepository()

        preferences.acknowledge("event.a")
        preferences.defer("event.b")
        preferences.suspend("event.c")
        preferences.suppress("event.d")

        assertEquals(
            EventsAttentionDisposition.ACKNOWLEDGED,
            preferences.current
                .dispositionByEventRef
                .getValue("event.a"),
        )
        assertEquals(
            EventsAttentionDisposition.DEFERRED,
            preferences.current
                .dispositionByEventRef
                .getValue("event.b"),
        )
        assertEquals(
            EventsAttentionDisposition.SUSPENDED,
            preferences.current
                .dispositionByEventRef
                .getValue("event.c"),
        )
        assertEquals(
            EventsAttentionDisposition.SUPPRESSED,
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
