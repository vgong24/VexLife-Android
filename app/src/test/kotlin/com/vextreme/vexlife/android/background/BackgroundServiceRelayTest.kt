package com.vextreme.vexlife.android.background

import com.vextreme.vexlife.android.execution.VexExecution
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackgroundServiceRelayTest {
    private class Sources {
        val playback =
            MutableStateFlow(
                false,
            )
        val navigation =
            MutableStateFlow(
                false,
            )
    }

    @Test
    fun processExecutionOwnsRelayDependencyObservation() =
        runBlocking {
            val root =
                VexExecutionRoot()
            val sources =
                Sources()
            val relay =
                BackgroundServiceRelay()

            try {
                val playbackJob =
                    VexExecution.observe(
                        executionRoot =
                            root,
                        owner =
                            relay,
                        source =
                            sources::playback,
                    ) { active ->
                        relay.observePlayback(
                            active,
                        )
                    }
                val navigationJob =
                    VexExecution.observe(
                        executionRoot =
                            root,
                        owner =
                            relay,
                        source =
                            sources::navigation,
                    ) { active ->
                        relay.observeNavigation(
                            active,
                        )
                    }

                sources.playback.value =
                    true
                sources.navigation.value =
                    true

                withTimeout(
                    2_000L,
                ) {
                    while (
                        relay.currentOutput
                            .activeRelationships !=
                        setOf(
                            BackgroundServiceRelationship.Playback,
                            BackgroundServiceRelationship.Navigation,
                        )
                    ) {
                        kotlinx.coroutines.yield()
                    }
                }

                assertTrue(
                    playbackJob.isActive,
                )
                assertTrue(
                    navigationJob.isActive,
                )
            } finally {
                root.joinStopAll(
                    "test complete",
                )
            }
        }

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
