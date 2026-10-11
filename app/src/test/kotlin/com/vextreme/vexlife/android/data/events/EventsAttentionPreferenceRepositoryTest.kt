package com.vextreme.vexlife.android.data.events

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class EventsAttentionPreferenceRepositoryTest {
    @Test
    fun acknowledgementDeferSuppressAndResetRemainSeparateFromEventTruth() {
        val preferences =
            RelayEventsAttentionPreferenceRepository()

        preferences.acknowledge("event.a")
        preferences.defer("event.b")
        preferences.suppress("event.c")

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
            EventsAttentionDisposition.SUPPRESSED,
            preferences.current
                .dispositionByEventRef
                .getValue("event.c"),
        )

        preferences.reset("event.b")

        assertFalse(
            preferences.current
                .dispositionByEventRef
                .containsKey("event.b"),
        )
    }
}

// [VXG RealForever]
