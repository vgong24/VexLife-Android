package com.vextreme.vexlife.android.screens.shared.maps.distribution

import com.vextreme.vexlife.android.data.maps.distribution.InMemoryVexMapsDistributionRepository
import com.vextreme.vexlife.android.data.maps.distribution.VexMapsDistributionDraft
import com.vextreme.vexlife.android.data.maps.distribution.VexMapsDistributionRepository
import com.vextreme.vexlife.android.data.maps.distribution.VexMapsDistributionStop
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import com.vextreme.vexlife.android.screens.shared.maps.VexMapCoordinate
import com.vextreme.vexlife.android.screens.shared.maps.VexMapsRegionPack
import com.vextreme.vexlife.android.screens.shared.maps.VexMapsTravelMode
import com.vextreme.vexlife.android.screens.shared.maps.VexNavigationRoute
import com.vextreme.vexlife.android.screens.shared.maps.VexNavigationSessionPhase
import com.vextreme.vexlife.android.screens.shared.maps.VexNavigationSessionState
import com.vextreme.vexlife.android.screens.shared.maps.VexOfflineRouteRequest
import com.vextreme.vexlife.android.screens.shared.maps.VexOfflineRouteResult
import com.vextreme.vexlife.android.screens.shared.maps.VexRouteGeometryPoint
import com.vextreme.vexlife.android.screens.shared.maps.distribution.convergence.VexMapsDistributionExpectedLegState
import com.vextreme.vexlife.android.screens.shared.maps.distribution.convergence.VexMapsDistributionLegAttempt
import com.vextreme.vexlife.android.screens.shared.maps.distribution.convergence.VexMapsDistributionLegTarget
import com.vextreme.vexlife.android.screens.shared.maps.distribution.mode.VexMapsDistributionModeState
import com.vextreme.vexlife.android.screens.shared.maps.distribution.planning.VexMapsDistributionPlan
import com.vextreme.vexlife.android.screens.shared.maps.distribution.planning.VexMapsDistributionPreparationSnapshot
import com.vextreme.vexlife.android.screens.shared.maps.distribution.run.VexMapsDistributionRunState
import com.vextreme.vexlife.android.screens.shared.maps.distribution.session.VexMapsDistributionNavigationPort
import com.vextreme.vexlife.android.screens.shared.maps.distribution.session.VexMapsDistributionRouting
import com.vextreme.vexlife.android.screens.shared.maps.distribution.session.VexMapsDistributionSession
import java.io.File
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VexMapsDistributionSessionTest {
    private val origin =
        VexMapCoordinate(
            latitude = 34.0,
            longitude = -118.0,
        )

    private val pack =
        VexMapsRegionPack(
            tileExtract =
                File(
                    "/tmp/vexmaps-distribution-test.tar",
                ),
            byteLength = 1L,
            lastModifiedMillis = 1L,
        )

    private fun stop(
        ref: String,
        longitude: Double,
        inputOrder: Int,
    ) =
        VexMapsDistributionStop(
            stopRef = ref,
            placeRef = ref,
            name = ref,
            subtitle = null,
            latitude = 34.0,
            longitude = longitude,
            inputOrder = inputOrder,
        )

    private fun initialDraft() =
        VexMapsDistributionDraft(
            revision = 8L,
            returnToStart = true,
            stops =
                listOf(
                    stop(
                        "alpha",
                        -118.1,
                        0,
                    ),
                    stop(
                        "beta",
                        -118.2,
                        1,
                    ),
                ),
        )

    @Test
    fun preparingSealsDraftAndExperienceRecreationDoesNotResetSession() =
        runBlocking {
            val repository =
                InMemoryVexMapsDistributionRepository(
                    initialDraft(),
                )
            val optimizeGate =
                CompletableDeferred<Unit>()
            val routing =
                FakeRouting(
                    optimizeGate,
                )
            val navigation =
                FakeNavigation()
            val owner =
                SupervisorJob(
                    coroutineContext[
                        Job
                    ],
                )
            val executionRoot =
                VexExecutionRoot(
                    dispatcher =
                        Dispatchers.Unconfined,
                )
            val session =
                VexMapsDistributionSession(
                    repository =
                        repository,
                    routing =
                        routing,
                    navigation =
                        navigation,
                    executionRoot =
                        executionRoot,
                    scope =
                        CoroutineScope(
                            coroutineContext +
                                owner,
                        ),
                )

            try {
                awaitDraft(
                    session,
                )

                session.prepare(
                    origin =
                        origin,
                    travelMode =
                        VexMapsTravelMode.DRIVE,
                    pack =
                        pack,
                )

                assertTrue(
                    session.currentRun
                        .state is
                        VexMapsDistributionRunState.Preparing,
                )

                session.addStop(
                    placeRef = "gamma",
                    name = "gamma",
                    subtitle = null,
                    latitude = 34.0,
                    longitude = -118.3,
                )

                assertEquals(
                    2,
                    repository
                        .loadDraft()
                        .stops
                        .size,
                )

                optimizeGate.complete(
                    Unit,
                )

                val ready =
                    awaitReady(
                        session,
                    )

                val experienceAfterRecreation =
                    VexMapsDistributionExperience(
                        session,
                    )
                assertEquals(
                    ready.state,
                    experienceAfterRecreation
                        .runProjection
                        .state,
                )
                assertEquals(
                    session.states.value
                        .plan
                        ?.planRef,
                    experienceAfterRecreation
                        .states
                        .value
                        .plan
                        ?.planRef,
                )

                session.planningInputChanged()

                awaitDraft(
                    session,
                )
                assertNull(
                    session.states.value
                        .plan,
                )
                assertEquals(
                    VexNavigationSessionPhase.IDLE,
                    navigation.current.phase,
                )
            } finally {
                executionRoot.joinStopAll(
                    "Distribution session test complete",
                )
                owner.cancelAndJoin()
            }
        }

    @Test
    fun failedNextLegRemainsRetryableAndRetryRestartsTheExactStop() =
        runBlocking {
            val repository =
                InMemoryVexMapsDistributionRepository(
                    initialDraft(),
                )
            val routing =
                FakeRouting(
                    CompletableDeferred(
                        Unit,
                    ),
                )
            val navigation =
                FakeNavigation()
            val owner =
                SupervisorJob(
                    coroutineContext[
                        Job
                    ],
                )
            val executionRoot =
                VexExecutionRoot(
                    dispatcher =
                        Dispatchers.Unconfined,
                )
            val session =
                VexMapsDistributionSession(
                    repository =
                        repository,
                    routing =
                        routing,
                    navigation =
                        navigation,
                    executionRoot =
                        executionRoot,
                    scope =
                        CoroutineScope(
                            coroutineContext +
                                owner,
                        ),
                )

            try {
                awaitDraft(
                    session,
                )
                prepareAndStart(
                    session =
                        session,
                    navigation =
                        navigation,
                )

                navigation.arrive()

                withTimeout(
                    1_000L,
                ) {
                    session.runProjections
                        .first {
                            it.state is
                                VexMapsDistributionRunState.AtStop
                        }
                }

                routing.remainingRouteFailures =
                    1
                session.completeCurrentStop()

                val failed =
                    withTimeout(
                        1_000L,
                    ) {
                        session.states
                            .first {
                                it.legAttempt is
                                    VexMapsDistributionLegAttempt.Failed
                            }
                    }

                assertEquals(
                    VexMapsDistributionLegTarget.Stop(
                        "alpha",
                    ),
                    (
                        failed.legAttempt as
                            VexMapsDistributionLegAttempt.Failed
                    ).target,
                )

                session.retryFailedLeg()

                withTimeout(
                    1_000L,
                ) {
                    navigation.states
                        .first {
                            it.phase ==
                                VexNavigationSessionPhase.ACTIVE &&
                                it.routeResult
                                    ?.route
                                    ?.routeRef
                                    ?.endsWith(
                                        ".stop.alpha",
                                    ) == true
                        }
                }

                assertEquals(
                    VexMapsDistributionLegAttempt.Idle,
                    session.states.value
                        .legAttempt,
                )
                assertTrue(
                    session.states.value
                        .expectedLeg is
                        VexMapsDistributionExpectedLegState.Expected,
                )
            } finally {
                executionRoot.joinStopAll(
                    "Distribution session test complete",
                )
                owner.cancelAndJoin()
            }
        }

    @Test
    fun failedNavigationStartStaysVisibleAndRetryUsesSamePreparedRoute() =
        runBlocking {
            val repository =
                InMemoryVexMapsDistributionRepository(
                    initialDraft(),
                )
            val routing =
                FakeRouting(
                    CompletableDeferred(
                        Unit,
                    ),
                )
            val navigation =
                FakeNavigation(
                    remainingStartFailures =
                        1,
                )
            val owner =
                SupervisorJob(
                    coroutineContext[
                        Job
                    ],
                )
            val executionRoot =
                VexExecutionRoot(
                    dispatcher =
                        Dispatchers.Unconfined,
                )
            val session =
                VexMapsDistributionSession(
                    repository =
                        repository,
                    routing =
                        routing,
                    navigation =
                        navigation,
                    executionRoot =
                        executionRoot,
                    scope =
                        CoroutineScope(
                            coroutineContext +
                                owner,
                        ),
                )

            try {
                awaitDraft(
                    session,
                )

                session.prepare(
                    origin =
                        origin,
                    travelMode =
                        VexMapsTravelMode.DRIVE,
                    pack =
                        pack,
                )
                awaitReady(
                    session,
                )

                val preparedRouteRef =
                    requireNotNull(
                        navigation.current
                            .routeResult
                            ?.route
                            ?.routeRef,
                    )

                session.startPreparedRun()

                val failed =
                    withTimeout(
                        1_000L,
                    ) {
                        session.states
                            .first {
                                it.legAttempt is
                                    VexMapsDistributionLegAttempt.Failed
                            }
                    }

                assertTrue(
                    session.currentRun
                        .state is
                        VexMapsDistributionRunState.Running,
                )
                assertEquals(
                    VexNavigationSessionPhase.READY,
                    navigation.current.phase,
                )
                assertNotNull(
                    failed.expectedLeg as?
                        VexMapsDistributionExpectedLegState.Expected,
                )

                session.retryFailedLeg()

                withTimeout(
                    1_000L,
                ) {
                    navigation.states
                        .first {
                            it.phase ==
                                VexNavigationSessionPhase.ACTIVE
                        }
                }

                assertEquals(
                    preparedRouteRef,
                    navigation.current
                        .routeResult
                        ?.route
                        ?.routeRef,
                )
                assertEquals(
                    VexMapsDistributionLegAttempt.Idle,
                    session.states.value
                        .legAttempt,
                )
            } finally {
                executionRoot.joinStopAll(
                    "Distribution session test complete",
                )
                owner.cancelAndJoin()
            }
        }

    @Test
    fun failedDurableClearPreservesDraftAndRunTruth() =
        runBlocking {
            val repository =
                FailingClearRepository(
                    initialDraft(),
                )
            val navigation =
                FakeNavigation()
            val owner =
                SupervisorJob(
                    coroutineContext[
                        Job
                    ],
                )
            val executionRoot =
                VexExecutionRoot(
                    dispatcher =
                        Dispatchers.Unconfined,
                )
            val session =
                VexMapsDistributionSession(
                    repository =
                        repository,
                    routing =
                        FakeRouting(
                            CompletableDeferred(
                                Unit,
                            ),
                        ),
                    navigation =
                        navigation,
                    executionRoot =
                        executionRoot,
                    scope =
                        CoroutineScope(
                            coroutineContext +
                                owner,
                        ),
                )

            try {
                awaitDraft(
                    session,
                )

                session.clearAll()

                val failed =
                    withTimeout(
                        1_000L,
                    ) {
                        session.states
                            .first {
                                !it.draftOperationInFlight &&
                                    it.draftFailure !=
                                    null
                            }
                    }

                assertEquals(
                    listOf(
                        "alpha",
                        "beta",
                    ),
                    failed.draft
                        .stops
                        .map {
                            it.stopRef
                        },
                )
                assertTrue(
                    session.currentRun
                        .state is
                        VexMapsDistributionRunState.Draft,
                )
                assertEquals(
                    2,
                    repository
                        .loadDraft()
                        .stops
                        .size,
                )

                repository.failClear =
                    false
                session.clearAll()

                withTimeout(
                    1_000L,
                ) {
                    session.runProjections
                        .first {
                            it.state is
                                VexMapsDistributionRunState.Empty
                        }
                }

                assertTrue(
                    session.states.value
                        .draft
                        .stops
                        .isEmpty(),
                )
            } finally {
                executionRoot.joinStopAll(
                    "Distribution session test complete",
                )
                owner.cancelAndJoin()
            }
        }

    @Test
    fun routingRegionReplacementInvalidatesPreparedRealityButNotActiveRun() =
        runBlocking {
            val repository =
                InMemoryVexMapsDistributionRepository(
                    initialDraft(),
                )
            val navigation =
                FakeNavigation()
            val owner =
                SupervisorJob(
                    coroutineContext[
                        Job
                    ],
                )
            val executionRoot =
                VexExecutionRoot(
                    dispatcher =
                        Dispatchers.Unconfined,
                )
            val session =
                VexMapsDistributionSession(
                    repository =
                        repository,
                    routing =
                        FakeRouting(
                            CompletableDeferred(
                                Unit,
                            ),
                        ),
                    navigation =
                        navigation,
                    executionRoot =
                        executionRoot,
                    scope =
                        CoroutineScope(
                            coroutineContext +
                                owner,
                        ),
                )

            try {
                awaitDraft(
                    session,
                )

                session.prepare(
                    origin =
                        origin,
                    travelMode =
                        VexMapsTravelMode.DRIVE,
                    pack =
                        pack,
                )
                awaitReady(
                    session,
                )

                assertTrue(
                    session.routingRegionReplacementRequested(),
                )
                awaitDraft(
                    session,
                )
                assertNull(
                    session.states.value
                        .plan,
                )
                assertEquals(
                    VexNavigationSessionPhase.IDLE,
                    navigation.current.phase,
                )

                prepareAndStart(
                    session =
                        session,
                    navigation =
                        navigation,
                )

                assertFalse(
                    session.canReplaceRoutingRegion(),
                )
                assertFalse(
                    session.routingRegionReplacementRequested(),
                )
                assertTrue(
                    session.currentRun
                        .state is
                        VexMapsDistributionRunState.Running,
                )
                assertNotNull(
                    session.states.value
                        .plan,
                )
            } finally {
                executionRoot.joinStopAll(
                    "Distribution session test complete",
                )
                owner.cancelAndJoin()
            }
        }

    private suspend fun awaitDraft(
        session:
            VexMapsDistributionSession,
    ) {
        withTimeout(
            1_000L,
        ) {
            session.runProjections
                .first {
                    it.state is
                        VexMapsDistributionRunState.Draft
                }
        }
        withTimeout(
            1_000L,
        ) {
            session.modeProjections
                .first {
                    it.state is
                        VexMapsDistributionModeState.Distribution
                }
        }
    }

    private suspend fun awaitReady(
        session:
            VexMapsDistributionSession,
    ) =
        withTimeout(
            1_000L,
        ) {
            session.runProjections
                .first {
                    it.state is
                        VexMapsDistributionRunState.Ready
                }
        }

    private suspend fun prepareAndStart(
        session:
            VexMapsDistributionSession,
        navigation:
            FakeNavigation,
    ) {
        session.prepare(
            origin =
                origin,
            travelMode =
                VexMapsTravelMode.DRIVE,
            pack =
                pack,
        )
        awaitReady(
            session,
        )
        session.startPreparedRun()

        withTimeout(
            1_000L,
        ) {
            navigation.states
                .first {
                    it.phase ==
                        VexNavigationSessionPhase.ACTIVE
                }
        }
    }

    private class FakeRouting(
        private val optimizeGate:
            CompletableDeferred<Unit>,
    ) :
        VexMapsDistributionRouting {
        var remainingRouteFailures =
            0

        override suspend fun optimize(
            draft:
                VexMapsDistributionDraft,
            preparation:
                VexMapsDistributionPreparationSnapshot,
            pack:
                VexMapsRegionPack,
        ): VexMapsDistributionPlan {
            optimizeGate.await()

            return VexMapsDistributionPlan(
                planRef =
                    "plan." +
                        preparation.preparationRef,
                preparation =
                    preparation,
                orderedStops =
                    draft.stops
                        .asReversed(),
                estimatedDurationSeconds =
                    120L,
            )
        }

        override suspend fun route(
            pack:
                VexMapsRegionPack,
            request:
                VexOfflineRouteRequest,
        ): VexOfflineRouteResult {
            if (
                remainingRouteFailures >
                0
            ) {
                remainingRouteFailures -=
                    1
                error(
                    "synthetic route failure",
                )
            }

            return routeResult(
                request,
            )
        }
    }

    private class FakeNavigation(
        var remainingStartFailures:
            Int =
            0,
    ) :
        VexMapsDistributionNavigationPort {
        private val mutableStates =
            MutableStateFlow(
                VexNavigationSessionState(),
            )

        override val states:
            StateFlow<VexNavigationSessionState> =
            mutableStates

        override val current:
            VexNavigationSessionState
            get() =
                mutableStates.value

        override fun prepare(
            routeResult:
                VexOfflineRouteResult,
            routingPack:
                VexMapsRegionPack,
        ) {
            mutableStates.value =
                VexNavigationSessionState(
                    phase =
                        VexNavigationSessionPhase.READY,
                    routeResult =
                        routeResult,
                )
        }

        override fun startPrepared() {
            if (
                remainingStartFailures >
                0
            ) {
                remainingStartFailures -=
                    1
                error(
                    "synthetic navigation start failure",
                )
            }

            check(
                mutableStates.value
                    .phase ==
                    VexNavigationSessionPhase.READY,
            )
            mutableStates.value =
                mutableStates.value.copy(
                    phase =
                        VexNavigationSessionPhase.ACTIVE,
                )
        }

        override fun stop() {
            mutableStates.value =
                VexNavigationSessionState()
        }

        override fun dismissArrival() =
            Unit

        fun arrive() {
            mutableStates.value =
                mutableStates.value.copy(
                    phase =
                        VexNavigationSessionPhase.ARRIVED,
                )
        }
    }

    private class FailingClearRepository(
        initialDraft:
            VexMapsDistributionDraft,
    ) :
        VexMapsDistributionRepository {
        private val delegate =
            InMemoryVexMapsDistributionRepository(
                initialDraft,
            )

        var failClear =
            true

        override suspend fun loadDraft():
            VexMapsDistributionDraft =
            delegate.loadDraft()

        override suspend fun addStop(
            placeRef: String?,
            name: String,
            subtitle: String?,
            latitude: Double,
            longitude: Double,
        ): VexMapsDistributionDraft =
            delegate.addStop(
                placeRef =
                    placeRef,
                name =
                    name,
                subtitle =
                    subtitle,
                latitude =
                    latitude,
                longitude =
                    longitude,
            )

        override suspend fun removeStop(
            stopRef: String,
        ): VexMapsDistributionDraft =
            delegate.removeStop(
                stopRef,
            )

        override suspend fun setReturnToStart(
            enabled: Boolean,
        ): VexMapsDistributionDraft =
            delegate.setReturnToStart(
                enabled,
            )

        override suspend fun clearDraft():
            VexMapsDistributionDraft {
            if (failClear) {
                error(
                    "synthetic clear failure",
                )
            }
            return delegate.clearDraft()
        }
    }

}

private fun routeResult(
    request:
        VexOfflineRouteRequest,
): VexOfflineRouteResult =
    VexOfflineRouteResult(
        route =
            VexNavigationRoute(
                routeRef =
                    request.routeRef,
                routeRevision =
                    request.routeRevision,
                destinationLabel =
                    request.destinationLabel,
                totalDistanceMeters =
                    100,
                maneuvers =
                    emptyList(),
            ),
        geometry =
            listOf(
                VexRouteGeometryPoint(
                    latitude =
                        request.origin
                            .latitude,
                    longitude =
                        request.origin
                            .longitude,
                ),
                VexRouteGeometryPoint(
                    latitude =
                        request.destination
                            .latitude,
                    longitude =
                        request.destination
                            .longitude,
                ),
            ),
        travelMode =
            request.travelMode,
        estimatedDurationSeconds =
            60,
    )

// [VXG RealForever]
