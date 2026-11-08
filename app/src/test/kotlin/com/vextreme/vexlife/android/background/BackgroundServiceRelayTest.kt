package com.vextreme.vexlife.android.background

import com.vextreme.vexlife.android.execution.VexExecutionRoot
import com.vextreme.vexlife.android.screens.shared.maps.VexNavigationLocationDisposition
import com.vextreme.vexlife.android.screens.shared.maps.VexNavigationSessionPhase
import com.vextreme.vexlife.android.screens.shared.maps.VexNavigationSessionState
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackPhase
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test

class BackgroundServiceRelayTest {
    @Test
    fun appScopeRelayKeepsLatestPlaybackAndNavigationCoordinationTruthTogether() =
        runBlocking {
            val executionRoot =
                VexExecutionRoot(
                    dispatcher =
                        Dispatchers.Unconfined,
                )
            val playback =
                MutableStateFlow<
                    VexStreamPlaybackState
                >(
                    VexStreamPlaybackState.NoMedia,
                )
            val navigation =
                MutableStateFlow(
                    VexNavigationSessionState(),
                )
            val relay =
                BackgroundServiceRelay(
                    executionRoot =
                        executionRoot,
                    playbackStates =
                        playback,
                    navigationStates =
                        navigation,
                )

            try {
                assertEquals(
                    VexStreamPlaybackState.NoMedia,
                    relay.currentOutput.playback,
                )
                assertEquals(
                    VexNavigationSessionPhase.IDLE,
                    relay.currentOutput
                        .navigation
                        .phase,
                )

                val playing =
                    VexStreamPlaybackState.Media(
                        playRequested =
                            true,
                        phase =
                            VexStreamPlaybackPhase.Ready,
                        playPauseAvailable =
                            true,
                    )
                playback.value =
                    playing

                val playbackCurrent =
                    withTimeout(
                        1_000L,
                    ) {
                        relay.outputStates
                            .first { state ->
                                state.playback ==
                                    playing
                            }
                    }

                assertEquals(
                    VexNavigationSessionPhase.IDLE,
                    playbackCurrent
                        .navigation
                        .phase,
                )

                navigation.value =
                    VexNavigationSessionState(
                        phase =
                            VexNavigationSessionPhase.ACTIVE,
                        locationDisposition =
                            VexNavigationLocationDisposition.MATCHED,
                    )

                val together =
                    withTimeout(
                        1_000L,
                    ) {
                        relay.outputStates
                            .first { state ->
                                state.playback ==
                                    playing &&
                                    state.navigation.phase ==
                                    VexNavigationSessionPhase.ACTIVE
                            }
                    }

                assertEquals(
                    playing,
                    together.playback,
                )
                assertEquals(
                    VexNavigationSessionPhase.ACTIVE,
                    together
                        .navigation
                        .phase,
                )
                assertEquals(
                    VexNavigationLocationDisposition.MATCHED,
                    together
                        .navigation
                        .locationDisposition,
                )
            } finally {
                executionRoot.joinStopAll(
                    "Background Service Relay test complete",
                )
            }
        }
}

// [VXG RealForever][VEXARCHITECTURE][BACKGROUND-SERVICE-RELAY]
