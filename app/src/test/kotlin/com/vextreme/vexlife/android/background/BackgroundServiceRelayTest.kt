package com.vextreme.vexlife.android.background

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackgroundServiceRelayTest {
    @Test
    fun oneProcessRelayTracksBackgroundRelationshipsAsAlreadyOwnedTruthChanges() {
        val relay =
            BackgroundServiceRelay()

        assertEquals(
            emptySet<
                BackgroundServiceRelationship
            >(),
            relay.currentOutput
                .activeRelationships,
        )
        assertFalse(
            relay.currentOutput
                .backgroundContinuityRequired,
        )

        relay.observePlayback(
            true,
        )

        assertEquals(
            setOf(
                BackgroundServiceRelationship
                    .Playback,
            ),
            relay.currentOutput
                .activeRelationships,
        )
        assertFalse(
            relay.currentOutput
                .concurrentBackgroundRelationships,
        )

        relay.observeNavigation(
            true,
        )

        assertEquals(
            setOf(
                BackgroundServiceRelationship
                    .Playback,
                BackgroundServiceRelationship
                    .Navigation,
            ),
            relay.currentOutput
                .activeRelationships,
        )
        assertTrue(
            relay.currentOutput
                .concurrentBackgroundRelationships,
        )

        relay.observePlayback(
            false,
        )

        assertEquals(
            setOf(
                BackgroundServiceRelationship
                    .Navigation,
            ),
            relay.currentOutput
                .activeRelationships,
        )
    }
}

// [VXG RealForever][VEXARCHITECTURE][BACKGROUND-SERVICE-RELAY]
