package com.vextreme.vexlife.android.background

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackgroundServiceRelayTest {
    @Test
    fun oneProcessRelayTracksBackgroundRelationshipsAsTheyBecomeCurrentTogether() {
        val playback =
            MutableStateFlow(
                false,
            )
        val navigation =
            MutableStateFlow(
                false,
            )
        val scope =
            CoroutineScope(
                SupervisorJob() +
                    Dispatchers.Unconfined,
            )
        val relay =
            BackgroundServiceRelay(
                playbackStates =
                    playback,
                navigationStates =
                    navigation,
            )

        try {
            relay.activate(
                scope,
            )

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

            playback.value =
                true

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

            navigation.value =
                true

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

            playback.value =
                false

            assertEquals(
                setOf(
                    BackgroundServiceRelationship
                        .Navigation,
                ),
                relay.currentOutput
                    .activeRelationships,
            )
        } finally {
            scope.cancel()
        }
    }
}

// [VXG RealForever][VEXARCHITECTURE][BACKGROUND-SERVICE-RELAY]
