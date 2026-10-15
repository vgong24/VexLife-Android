package com.vextreme.vexlife.android.screens.shared.maps.distribution

import com.vextreme.vexlife.android.screens.shared.maps.VexMapsTravelMode
import com.vextreme.vexlife.android.screens.shared.maps.VexNavigationRoute
import com.vextreme.vexlife.android.screens.shared.maps.VexNavigationSessionPhase
import com.vextreme.vexlife.android.screens.shared.maps.VexNavigationSessionState
import com.vextreme.vexlife.android.screens.shared.maps.VexOfflineRouteResult
import com.vextreme.vexlife.android.screens.shared.maps.VexRouteGeometryPoint
import com.vextreme.vexlife.android.screens.shared.maps.distribution.convergence.VexMapsDistributionExpectedLeg
import com.vextreme.vexlife.android.screens.shared.maps.distribution.convergence.VexMapsDistributionExpectedLegState
import com.vextreme.vexlife.android.screens.shared.maps.distribution.convergence.VexMapsDistributionLegAttempt
import com.vextreme.vexlife.android.screens.shared.maps.distribution.convergence.VexMapsDistributionLegTarget
import com.vextreme.vexlife.android.screens.shared.maps.distribution.convergence.VexMapsDistributionNavigationConvergenceRelay
import com.vextreme.vexlife.android.screens.shared.maps.distribution.convergence.VexMapsDistributionNavigationLayer
import com.vextreme.vexlife.android.screens.shared.maps.distribution.convergence.VexMapsDistributionNavigationRequest
import com.vextreme.vexlife.android.screens.shared.maps.distribution.convergence.VexMapsDistributionRouteIdentity
import com.vextreme.vexlife.android.screens.shared.maps.distribution.run.VexMapsDistributionRunConnection
import com.vextreme.vexlife.android.screens.shared.maps.distribution.run.VexMapsDistributionRunControl
import com.vextreme.vexlife.android.screens.shared.maps.distribution.run.VexMapsDistributionRunState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VexMapsDistributionNavigationConvergenceRelayTest {
    private fun readyConnection():
        VexMapsDistributionRunConnection =
        VexMapsDistributionRunConnection()
            .also { connection ->
                connection.observeDraft(
                    revision = 4L,
                    stopRefs =
                        listOf(
                            "alpha",
                            "beta",
                        ),
                    returnToStart =
                        true,
                )
                connection.activate(
                    VexMapsDistributionRunControl.Prepare,
                )
                val preparing =
                    connection.currentProjection
                        .state as
                        VexMapsDistributionRunState.Preparing
                connection.preparationSucceeded(
                    preparationRef =
                        preparing.preparationRef,
                    planRef =
                        "plan.4",
                    orderedStopRefs =
                        listOf(
                            "beta",
                            "alpha",
                        ),
                )
            }

    private fun route(
        routeRef: String,
        revision: Long,
    ) =
        VexOfflineRouteResult(
            route =
                VexNavigationRoute(
                    routeRef =
                        routeRef,
                    routeRevision =
                        revision,
                    destinationLabel =
                        "Destination",
                    totalDistanceMeters =
                        100,
                    maneuvers =
                        emptyList(),
                ),
            geometry =
                listOf(
                    VexRouteGeometryPoint(
                        latitude = 0.0,
                        longitude = 0.0,
                    ),
                    VexRouteGeometryPoint(
                        latitude = 0.0,
                        longitude = 0.001,
                    ),
                ),
            travelMode =
                VexMapsTravelMode.DRIVE,
        )

    @Test
    fun readyNavigationRequiresExactDistributionRouteIdentity() =
        runBlocking {
            val run =
                readyConnection()
            val navigation =
                MutableStateFlow(
                    VexNavigationSessionState(
                        phase =
                            VexNavigationSessionPhase.READY,
                        routeResult =
                            route(
                                "distribution.beta",
                                7L,
                            ),
                    ),
                )
            val expected =
                MutableStateFlow<
                    VexMapsDistributionExpectedLegState
                >(
                    VexMapsDistributionExpectedLegState.Expected(
                        VexMapsDistributionExpectedLeg(
                            planRef =
                                "plan.4",
                            target =
                                VexMapsDistributionLegTarget.Stop(
                                    "beta",
                                ),
                            route =
                                VexMapsDistributionRouteIdentity(
                                    "distribution.beta",
                                    7L,
                                ),
                        ),
                    ),
                )
            val attempts =
                MutableStateFlow<
                    VexMapsDistributionLegAttempt
                >(
                    VexMapsDistributionLegAttempt.Idle,
                )
            val relay =
                VexMapsDistributionNavigationConvergenceRelay(
                    runProjections =
                        run.projections,
                    navigationStates =
                        navigation,
                    expectedLegStates =
                        expected,
                    legAttempts =
                        attempts,
                )

            try {
                relay.activate(
                    this,
                )

                val ready =
                    withTimeout(
                        1_000L,
                    ) {
                        relay.outputStates
                            .first {
                                it.layer ==
                                    VexMapsDistributionNavigationLayer
                                        .ReadyToStart
                            }
                    }
                assertNull(
                    ready.request,
                )

                navigation.value =
                    navigation.value.copy(
                        routeResult =
                            route(
                                "ordinary.route",
                                8L,
                            ),
                    )

                val mismatch =
                    withTimeout(
                        1_000L,
                    ) {
                        relay.outputStates
                            .first {
                                it.layer ==
                                    VexMapsDistributionNavigationLayer
                                        .NeedsLeg
                            }
                    }

                assertEquals(
                    VexMapsDistributionNavigationRequest.PrepareLeg(
                        "beta",
                    ),
                    mismatch.request,
                )
            } finally {
                relay.deactivate()
            }
        }

    @Test
    fun failedLegIsStableUntilAnExplicitRetryChangesAttemptTruth() =
        runBlocking {
            val run =
                readyConnection()
                    .also {
                        it.activate(
                            VexMapsDistributionRunControl.Start,
                        )
                    }
            val target =
                VexMapsDistributionLegTarget.Stop(
                    "beta",
                )
            val navigation =
                MutableStateFlow(
                    VexNavigationSessionState(),
                )
            val expected =
                MutableStateFlow<
                    VexMapsDistributionExpectedLegState
                >(
                    VexMapsDistributionExpectedLegState.None,
                )
            val attempts =
                MutableStateFlow<
                    VexMapsDistributionLegAttempt
                >(
                    VexMapsDistributionLegAttempt.Failed(
                        target =
                            target,
                        reason =
                            "offline route unavailable",
                    ),
                )
            val relay =
                VexMapsDistributionNavigationConvergenceRelay(
                    runProjections =
                        run.projections,
                    navigationStates =
                        navigation,
                    expectedLegStates =
                        expected,
                    legAttempts =
                        attempts,
                )

            try {
                relay.activate(
                    this,
                )

                withTimeout(
                    1_000L,
                ) {
                    relay.outputStates
                        .first {
                            it.layer ==
                                VexMapsDistributionNavigationLayer
                                    .LegFailed
                        }
                }

                attempts.value =
                    VexMapsDistributionLegAttempt.Idle

                val retry =
                    withTimeout(
                        1_000L,
                    ) {
                        relay.outputStates
                            .first {
                                it.layer ==
                                    VexMapsDistributionNavigationLayer
                                        .NeedsLeg
                            }
                    }

                assertEquals(
                    VexMapsDistributionNavigationRequest.PrepareLeg(
                        "beta",
                    ),
                    retry.request,
                )
            } finally {
                relay.deactivate()
            }
        }
}

// [VXG RealForever]
