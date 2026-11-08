package com.vextreme.vexlife.android.background

import com.vextreme.vexlife.android.execution.VexExecutionRoot
import com.vextreme.vexlife.android.relay.VexCompoundRelay
import com.vextreme.vexlife.android.relay.VexCompoundState
import com.vextreme.vexlife.android.relay.VexInputState
import com.vextreme.vexlife.android.relay.VexOutputState
import com.vextreme.vexlife.android.relay.vexObserveDependencies
import com.vextreme.vexlife.android.screens.shared.maps.VexNavigationGuidanceCue
import com.vextreme.vexlife.android.screens.shared.maps.VexNavigationLocationDisposition
import com.vextreme.vexlife.android.screens.shared.maps.VexNavigationSessionPhase
import com.vextreme.vexlife.android.screens.shared.maps.VexNavigationSessionState
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackState
import kotlinx.coroutines.flow.StateFlow

/**
 * Partial app-scope updates from independently owned current truths.
 *
 * Null means "this patch does not update that owner", not absence of the owner's truth.
 */
internal data class BackgroundServiceNavigationProjection(
    val phase:
        VexNavigationSessionPhase,
    val routeRef:
        String?,
    val routeRevision:
        Long?,
    val destinationLabel:
        String?,
    val locationDisposition:
        VexNavigationLocationDisposition,
    val pendingRerouteRef:
        String?,
    val latestIssuedCue:
        VexNavigationGuidanceCue?,
) {
    fun deepCopy():
        BackgroundServiceNavigationProjection =
        copy(
            latestIssuedCue =
                latestIssuedCue
                    ?.deepCopy(),
        )
}

internal data class BackgroundServiceInputState(
    val playback:
        VexStreamPlaybackState? =
        null,
    val navigation:
        BackgroundServiceNavigationProjection? =
        null,
) :
    VexInputState<
        BackgroundServiceInputState
    > {
    override fun deepCopy():
        BackgroundServiceInputState =
        copy(
            playback =
                playback
                    ?.deepCopyForBackgroundService(),
            navigation =
                navigation
                    ?.deepCopy(),
        )
}

/**
 * Coherent app-scope current truth available to sustained/background realizations.
 *
 * This is intentionally not an active-Service registry. Playback and Navigation remain rightful
 * owners of their own truth; this Relay keeps the small background-relevant projections of their
 * latest values coherent in one live process-scoped relationship so another background realization
 * can reason across them without Service-to-Service references or a Room/database message bus.
 *
 * Navigation deliberately contributes semantic/session coordinates rather than full route geometry.
 * Future Health / Companion / Call fields belong here only after those rightful owners exist.
 */
internal data class BackgroundServiceOutputState(
    val playback:
        VexStreamPlaybackState,
    val navigation:
        BackgroundServiceNavigationProjection,
) :
    VexOutputState<
        BackgroundServiceOutputState
    > {
    override fun deepCopy():
        BackgroundServiceOutputState =
        copy(
            playback =
                playback
                    .deepCopyForBackgroundService(),
            navigation =
                navigation
                    .deepCopy(),
        )
}

/**
 * Process-lived convergence over the latest background-relevant app truth.
 *
 * Human question:
 *
 *     "What is CURRENT across the independent truths that sustained Vex realizations may need to
 *      coordinate around while UI is absent?"
 *
 * The Relay does not start/stop Android Services, own provider reality, manufacture Service IDs, or
 * persist a second truth map. VexApplication establishes this exact instance at process lifetime.
 * Concrete Services may observe/filter the fields that matter to their own realization when a real
 * cross-domain decision requires it.
 */
internal class BackgroundServiceRelay(
    executionRoot:
        VexExecutionRoot,
    playbackStates:
        StateFlow<
            VexStreamPlaybackState
        >,
    navigationStates:
        StateFlow<
            VexNavigationSessionState
        >,
) :
    VexCompoundRelay<
        BackgroundServiceInputState,
        BackgroundServiceOutputState
    >(
        relayRef =
            "relay.vexlife.background-service",
        initialInputState =
            BackgroundServiceInputState(
                playback =
                    playbackStates
                        .value
                        .deepCopyForBackgroundService(),
                navigation =
                    navigationStates
                        .value
                        .toBackgroundServiceNavigationProjection(),
            ),
        initialOutputState =
            BackgroundServiceOutputState(
                playback =
                    playbackStates
                        .value
                        .deepCopyForBackgroundService(),
                navigation =
                    navigationStates
                        .value
                        .toBackgroundServiceNavigationProjection(),
            ),
        executionRoot =
            executionRoot,
        observedDependencies =
            vexObserveDependencies<
                BackgroundServiceInputState
            > {
                observe(
                    dependencyRef =
                        "playback.current",
                    flow =
                        playbackStates,
                ) { state ->
                    BackgroundServiceInputState(
                        playback =
                            state
                                .deepCopyForBackgroundService(),
                    )
                }

                observe(
                    dependencyRef =
                        "navigation.current",
                    flow =
                        navigationStates,
                ) { state ->
                    BackgroundServiceInputState(
                        navigation =
                            state
                                .toBackgroundServiceNavigationProjection(),
                    )
                }
            },
    ) {
    init {
        startObservedDependencies()
    }

    override fun processStates(
        inputState:
            BackgroundServiceInputState,
        currentState:
            VexCompoundState<
                BackgroundServiceInputState,
                BackgroundServiceOutputState
            >,
    ):
        VexCompoundState<
            BackgroundServiceInputState,
            BackgroundServiceOutputState
        > {
        val previous =
            currentState.inputState

        val playback =
            (
                inputState.playback
                    ?: previous.playback
                    ?: currentState.outputState.playback
            )
                .deepCopyForBackgroundService()
        val navigation =
            (
                inputState.navigation
                    ?: previous.navigation
                    ?: currentState.outputState.navigation
            )
                .deepCopy()

        return currentState.copy(
            inputState =
                BackgroundServiceInputState(
                    playback =
                        playback,
                    navigation =
                        navigation,
                ),
            outputState =
                BackgroundServiceOutputState(
                    playback =
                        playback,
                    navigation =
                        navigation,
                ),
        )
    }
}

private fun VexStreamPlaybackState.deepCopyForBackgroundService():
    VexStreamPlaybackState =
    when (
        this
    ) {
        VexStreamPlaybackState.NoMedia ->
            VexStreamPlaybackState.NoMedia

        is VexStreamPlaybackState.Media ->
            copy()

        is VexStreamPlaybackState.Ended ->
            copy()
    }

private fun VexNavigationSessionState.toBackgroundServiceNavigationProjection():
    BackgroundServiceNavigationProjection {
    val route =
        routeResult
            ?.route

    return BackgroundServiceNavigationProjection(
        phase =
            phase,
        routeRef =
            route
                ?.routeRef,
        routeRevision =
            route
                ?.routeRevision,
        destinationLabel =
            route
                ?.destinationLabel,
        locationDisposition =
            locationDisposition,
        pendingRerouteRef =
            pendingReroute
                ?.requestRef,
        latestIssuedCue =
            convergence
                ?.latestIssuedCue
                ?.deepCopy(),
    )
}

// [VXG RealForever][VEXARCHITECTURE][BACKGROUND-SERVICE-RELAY]
