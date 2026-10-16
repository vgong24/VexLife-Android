package com.vextreme.vexlife.android.screens.shared.maps.distribution

import com.vextreme.vexlife.android.screens.shared.maps.VexMapsTravelMode
import com.vextreme.vexlife.android.screens.shared.maps.VexNavigationRoute
import com.vextreme.vexlife.android.screens.shared.maps.VexNavigationSessionPhase
import com.vextreme.vexlife.android.screens.shared.maps.VexNavigationSessionState
import com.vextreme.vexlife.android.screens.shared.maps.VexOfflineRouteResult
import com.vextreme.vexlife.android.screens.shared.maps.VexRouteGeometryPoint
import com.vextreme.vexlife.android.screens.shared.maps.distribution.convergence.VexMapsDeliveryRouteExpectedLeg
import com.vextreme.vexlife.android.screens.shared.maps.distribution.convergence.VexMapsDeliveryRouteExpectedLegState
import com.vextreme.vexlife.android.screens.shared.maps.distribution.convergence.VexMapsDeliveryRouteLegAttempt
import com.vextreme.vexlife.android.screens.shared.maps.distribution.convergence.VexMapsDeliveryRouteLegFailureStage
import com.vextreme.vexlife.android.screens.shared.maps.distribution.convergence.VexMapsDeliveryRouteLegTarget
import com.vextreme.vexlife.android.screens.shared.maps.distribution.convergence.VexMapsDeliveryRouteNavigationConvergenceRelay
import com.vextreme.vexlife.android.screens.shared.maps.distribution.convergence.VexMapsDeliveryRouteNavigationLayer
import com.vextreme.vexlife.android.screens.shared.maps.distribution.convergence.VexMapsDeliveryRouteNavigationRequest
import com.vextreme.vexlife.android.screens.shared.maps.distribution.convergence.VexMapsDeliveryRouteNavigationRouteIdentity
import com.vextreme.vexlife.android.screens.shared.maps.distribution.deliveryroute.VexMapsDeliveryRouteConnection
import com.vextreme.vexlife.android.screens.shared.maps.distribution.deliveryroute.VexMapsDeliveryRouteControl
import com.vextreme.vexlife.android.screens.shared.maps.distribution.deliveryroute.VexMapsDeliveryRouteState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VexMapsDeliveryRouteNavigationConvergenceRelayTest {
    private fun readyConnection():
        VexMapsDeliveryRouteConnection =
        VexMapsDeliveryRouteConnection()
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
                    VexMapsDeliveryRouteControl.Prepare,
                )
                val preparing =
                    connection.currentProjection
                        .state as
                        VexMapsDeliveryRouteState.Preparing
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
            val deliveryRoute =
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
                    VexMapsDeliveryRouteExpectedLegState
                >(
                    VexMapsDeliveryRouteExpectedLegState.Expected(
                        VexMapsDeliveryRouteExpectedLeg(
                            planRef =
                                "plan.4",
                            target =
                                VexMapsDeliveryRouteLegTarget.Stop(
                                    "beta",
                                ),
                            route =
                                VexMapsDeliveryRouteNavigationRouteIdentity(
                                    "distribution.beta",
                                    7L,
                                ),
                        ),
                    ),
                )
            val attempts =
                MutableStateFlow<
                    VexMapsDeliveryRouteLegAttempt
                >(
                    VexMapsDeliveryRouteLegAttempt.Idle,
                )
            val relay =
                VexMapsDeliveryRouteNavigationConvergenceRelay(
                    deliveryRouteProjections =
                        deliveryRoute.projections,
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
                                    VexMapsDeliveryRouteNavigationLayer
                                        .ReadyToStart
                            }
                    }
                assertNull(
                    ready.request,
                )

                expected.value =
                    VexMapsDeliveryRouteExpectedLegState.Expected(
                        VexMapsDeliveryRouteExpectedLeg(
                            planRef =
                                "old.plan",
                            target =
                                VexMapsDeliveryRouteLegTarget.Stop(
                                    "beta",
                                ),
                            route =
                                VexMapsDeliveryRouteNavigationRouteIdentity(
                                    "distribution.beta",
                                    7L,
                                ),
                        ),
                    )

                withTimeout(
                    1_000L,
                ) {
                    relay.outputStates
                        .first {
                            it.layer ==
                                VexMapsDeliveryRouteNavigationLayer
                                    .ForeignNavigation
                        }
                }

                expected.value =
                    VexMapsDeliveryRouteExpectedLegState.Expected(
                        VexMapsDeliveryRouteExpectedLeg(
                            planRef =
                                "plan.4",
                            target =
                                VexMapsDeliveryRouteLegTarget.Stop(
                                    "beta",
                                ),
                            route =
                                VexMapsDeliveryRouteNavigationRouteIdentity(
                                    "distribution.beta",
                                    7L,
                                ),
                        ),
                    )

                withTimeout(
                    1_000L,
                ) {
                    relay.outputStates
                        .first {
                            it.layer ==
                                VexMapsDeliveryRouteNavigationLayer
                                    .ReadyToStart
                        }
                }

                navigation.value =
                    navigation.value.copy(
                        routeResult =
                            route(
                                "distribution.beta",
                                8L,
                            ),
                    )

                withTimeout(
                    1_000L,
                ) {
                    relay.outputStates
                        .first {
                            it.layer ==
                                VexMapsDeliveryRouteNavigationLayer
                                    .ForeignNavigation
                        }
                }

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
                                    VexMapsDeliveryRouteNavigationLayer
                                        .ForeignNavigation
                            }
                    }

                assertNull(
                    mismatch.request,
                )
            } finally {
                relay.deactivate()
            }
        }

    @Test
    fun activeNavigationRerouteKeepsSameDistributionLegThroughArrival() =
        runBlocking {
            val deliveryRoute =
                readyConnection()
                    .also {
                        it.activate(
                            VexMapsDeliveryRouteControl.Start,
                        )
                    }
            val target =
                VexMapsDeliveryRouteLegTarget.Stop(
                    "beta",
                )
            val navigation =
                MutableStateFlow(
                    VexNavigationSessionState(
                        phase =
                            VexNavigationSessionPhase.ACTIVE,
                        routeResult =
                            route(
                                "distribution.beta",
                                7L,
                            ),
                    ),
                )
            val expected =
                MutableStateFlow<
                    VexMapsDeliveryRouteExpectedLegState
                >(
                    VexMapsDeliveryRouteExpectedLegState.Expected(
                        VexMapsDeliveryRouteExpectedLeg(
                            planRef =
                                "plan.4",
                            target =
                                target,
                            route =
                                VexMapsDeliveryRouteNavigationRouteIdentity(
                                    "distribution.beta",
                                    7L,
                                ),
                        ),
                    ),
                )
            val attempts =
                MutableStateFlow<
                    VexMapsDeliveryRouteLegAttempt
                >(
                    VexMapsDeliveryRouteLegAttempt.Idle,
                )
            val relay =
                VexMapsDeliveryRouteNavigationConvergenceRelay(
                    deliveryRouteProjections =
                        deliveryRoute.projections,
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
                                VexMapsDeliveryRouteNavigationLayer
                                    .Navigating
                        }
                }

                navigation.value =
                    navigation.value.copy(
                        routeResult =
                            route(
                                "distribution.beta",
                                8L,
                            ),
                    )
                yield()

                assertEquals(
                    VexMapsDeliveryRouteNavigationLayer.Navigating,
                    relay.currentOutput.layer,
                )
                assertNull(
                    relay.currentOutput.request,
                )

                navigation.value =
                    navigation.value.copy(
                        phase =
                            VexNavigationSessionPhase.ARRIVED,
                    )

                val arrived =
                    withTimeout(
                        1_000L,
                    ) {
                        relay.outputStates
                            .first {
                                it.request ==
                                    VexMapsDeliveryRouteNavigationRequest
                                        .DeliveryRouteControl(
                                            VexMapsDeliveryRouteControl
                                                .Arrive,
                                        )
                            }
                    }

                assertEquals(
                    VexMapsDeliveryRouteNavigationLayer.AtStop,
                    arrived.layer,
                )

                navigation.value =
                    VexNavigationSessionState(
                        phase =
                            VexNavigationSessionPhase.ACTIVE,
                        routeResult =
                            route(
                                "ordinary.route",
                                9L,
                            ),
                    )

                withTimeout(
                    1_000L,
                ) {
                    relay.outputStates
                        .first {
                            it.layer ==
                                VexMapsDeliveryRouteNavigationLayer
                                    .ForeignNavigation
                        }
                }
                Unit
            } finally {
                relay.deactivate()
            }
        }

    @Test
    fun failedLegIsStableUntilAnExplicitRetryChangesAttemptTruth() =
        runBlocking {
            val deliveryRoute =
                readyConnection()
                    .also {
                        it.activate(
                            VexMapsDeliveryRouteControl.Start,
                        )
                    }
            val target =
                VexMapsDeliveryRouteLegTarget.Stop(
                    "beta",
                )
            val navigation =
                MutableStateFlow(
                    VexNavigationSessionState(),
                )
            val expected =
                MutableStateFlow<
                    VexMapsDeliveryRouteExpectedLegState
                >(
                    VexMapsDeliveryRouteExpectedLegState.None,
                )
            val attempts =
                MutableStateFlow<
                    VexMapsDeliveryRouteLegAttempt
                >(
                    VexMapsDeliveryRouteLegAttempt.Failed(
                        stage =
                            VexMapsDeliveryRouteLegFailureStage
                                .RoutePreparation,
                        target =
                            target,
                        reason =
                            "offline route unavailable",
                    ),
                )
            val relay =
                VexMapsDeliveryRouteNavigationConvergenceRelay(
                    deliveryRouteProjections =
                        deliveryRoute.projections,
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
                                VexMapsDeliveryRouteNavigationLayer
                                    .LegFailed
                        }
                }

                attempts.value =
                    VexMapsDeliveryRouteLegAttempt.Idle

                val retry =
                    withTimeout(
                        1_000L,
                    ) {
                        relay.outputStates
                            .first {
                                it.layer ==
                                    VexMapsDeliveryRouteNavigationLayer
                                        .NeedsLeg
                            }
                    }

                assertEquals(
                    VexMapsDeliveryRouteNavigationRequest.PrepareLeg(
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
