package com.vextreme.vexlife.android.screens.shared.maps.distribution.session

import com.vextreme.vexlife.android.data.maps.distribution.VexMapsDistributionDraft
import com.vextreme.vexlife.android.data.maps.distribution.VexMapsDistributionRepository
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import com.vextreme.vexlife.android.screens.shared.maps.VexMapCoordinate
import com.vextreme.vexlife.android.screens.shared.maps.VexMapsRegionPack
import com.vextreme.vexlife.android.screens.shared.maps.VexMapsRouteIdentity
import com.vextreme.vexlife.android.screens.shared.maps.VexMapsTravelMode
import com.vextreme.vexlife.android.screens.shared.maps.VexNavigationSessionPhase
import com.vextreme.vexlife.android.screens.shared.maps.VexNavigationSessionState
import com.vextreme.vexlife.android.screens.shared.maps.VexOfflineRouteRequest
import com.vextreme.vexlife.android.screens.shared.maps.VexOfflineRouteResult
import com.vextreme.vexlife.android.screens.shared.maps.distribution.convergence.VexMapsDeliveryRouteExpectedLeg
import com.vextreme.vexlife.android.screens.shared.maps.distribution.convergence.VexMapsDeliveryRouteExpectedLegState
import com.vextreme.vexlife.android.screens.shared.maps.distribution.convergence.VexMapsDeliveryRouteLegAttempt
import com.vextreme.vexlife.android.screens.shared.maps.distribution.convergence.VexMapsDeliveryRouteLegFailureStage
import com.vextreme.vexlife.android.screens.shared.maps.distribution.convergence.VexMapsDeliveryRouteLegTarget
import com.vextreme.vexlife.android.screens.shared.maps.distribution.convergence.VexMapsDistributionModeDeliveryRouteConvergenceRelay
import com.vextreme.vexlife.android.screens.shared.maps.distribution.convergence.VexMapsDeliveryRouteNavigationConvergenceRelay
import com.vextreme.vexlife.android.screens.shared.maps.distribution.convergence.VexMapsDeliveryRouteNavigationOutput
import com.vextreme.vexlife.android.screens.shared.maps.distribution.convergence.VexMapsDeliveryRouteNavigationRequest
import com.vextreme.vexlife.android.screens.shared.maps.distribution.convergence.VexMapsDeliveryRouteNavigationRouteIdentity
import com.vextreme.vexlife.android.screens.shared.maps.distribution.convergence.vexMapsDeliveryRouteNavigationRealizesExpectedLeg
import com.vextreme.vexlife.android.screens.shared.maps.distribution.mode.VexMapsDistributionModeConnection
import com.vextreme.vexlife.android.screens.shared.maps.distribution.mode.VexMapsDistributionModeControl
import com.vextreme.vexlife.android.screens.shared.maps.distribution.mode.VexMapsDistributionModeProjection
import com.vextreme.vexlife.android.screens.shared.maps.distribution.planning.VexMapsDistributionPlan
import com.vextreme.vexlife.android.screens.shared.maps.distribution.planning.VexMapsDistributionPreparationSnapshot
import com.vextreme.vexlife.android.screens.shared.maps.distribution.deliveryroute.VexMapsDeliveryRouteConnection
import com.vextreme.vexlife.android.screens.shared.maps.distribution.deliveryroute.VexMapsDeliveryRouteControl
import com.vextreme.vexlife.android.screens.shared.maps.distribution.deliveryroute.VexMapsDeliveryRouteProjection
import com.vextreme.vexlife.android.screens.shared.maps.distribution.deliveryroute.VexMapsDeliveryRouteState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal interface VexMapsDistributionRouting {
    suspend fun optimize(
        draft: VexMapsDistributionDraft,
        preparation:
            VexMapsDistributionPreparationSnapshot,
        pack: VexMapsRegionPack,
    ): VexMapsDistributionPlan

    suspend fun route(
        pack: VexMapsRegionPack,
        request: VexOfflineRouteRequest,
    ): VexOfflineRouteResult
}

internal interface VexMapsDistributionNavigationPort {
    val states:
        StateFlow<VexNavigationSessionState>

    val current:
        VexNavigationSessionState

    fun prepare(
        routeResult: VexOfflineRouteResult,
        routingPack: VexMapsRegionPack,
    )

    fun startPrepared()

    fun stop()

    fun dismissArrival()
}

internal data class VexMapsDistributionSessionState(
    val draft:
        VexMapsDistributionDraft =
        VexMapsDistributionDraft(),
    val draftOperationInFlight:
        Boolean =
        true,
    val draftFailure:
        String? =
        null,
    val plan:
        VexMapsDistributionPlan? =
        null,
    val preparationFailure:
        String? =
        null,
    val preparationCleanupFailure:
        String? =
        null,
    val legAttempt:
        VexMapsDeliveryRouteLegAttempt =
        VexMapsDeliveryRouteLegAttempt.Idle,
    val expectedLeg:
        VexMapsDeliveryRouteExpectedLegState =
        VexMapsDeliveryRouteExpectedLegState.None,
)

internal data class VexMapsDistributionAddStopWork(
    val placeRef: String?,
    val name: String,
    val subtitle: String?,
    val latitude: Double,
    val longitude: Double,
)

internal data class VexMapsDistributionPreparationWork(
    val draft:
        VexMapsDistributionDraft,
    val preparation:
        VexMapsDistributionPreparationSnapshot,
    val pack:
        VexMapsRegionPack,
)

internal data class VexMapsDistributionLegWork(
    val target:
        VexMapsDeliveryRouteLegTarget,
    val plan:
        VexMapsDistributionPlan,
    val pack:
        VexMapsRegionPack,
)

/**
 * Process-lifetime owner of one lived Distribution session.
 *
 * Repository, Distribution Mode, Delivery Route, Optimizer and Navigation remain independent owners. This class owns their
 * active relationship: the admitted preparation snapshot, derived Plan, expected Navigation leg,
 * async attempt/failure state and the bounded Control feedback needed to advance the delivery route.
 */
internal class VexMapsDistributionSession(
    private val repository:
        VexMapsDistributionRepository,
    private val routing:
        VexMapsDistributionRouting,
    private val navigation:
        VexMapsDistributionNavigationPort,
    executionRoot:
        VexExecutionRoot,
    private val relayScope:
        CoroutineScope,
    private val mode:
        VexMapsDistributionModeConnection =
        VexMapsDistributionModeConnection(),
    private val deliveryRoute:
        VexMapsDeliveryRouteConnection =
        VexMapsDeliveryRouteConnection(),
) {
    private val mutableExpectedLeg =
        MutableStateFlow<
            VexMapsDeliveryRouteExpectedLegState
        >(
            VexMapsDeliveryRouteExpectedLegState.None,
        )
    private val mutableLegAttempt =
        MutableStateFlow<
            VexMapsDeliveryRouteLegAttempt
        >(
            VexMapsDeliveryRouteLegAttempt.Idle,
        )
    private val mutableState =
        MutableStateFlow(
            VexMapsDistributionSessionState(),
        )

    val states:
        StateFlow<VexMapsDistributionSessionState> =
        mutableState.asStateFlow()

    val modeProjections:
        StateFlow<VexMapsDistributionModeProjection>
        get() =
            mode.projections

    val deliveryRouteProjections:
        StateFlow<VexMapsDeliveryRouteProjection>
        get() =
            deliveryRoute.projections

    val currentMode:
        VexMapsDistributionModeProjection
        get() =
            mode.currentProjection

    val currentDeliveryRoute:
        VexMapsDeliveryRouteProjection
        get() =
            deliveryRoute.currentProjection

    private val modeDeliveryRouteRelay =
        VexMapsDistributionModeDeliveryRouteConvergenceRelay(
            modeProjections =
                mode.projections,
            deliveryRouteProjections =
                deliveryRoute.projections,
        )

    private val navigationRelay =
        VexMapsDeliveryRouteNavigationConvergenceRelay(
            deliveryRouteProjections =
                deliveryRoute.projections,
            navigationStates =
                navigation.states,
            expectedLegStates =
                mutableExpectedLeg,
            legAttempts =
                mutableLegAttempt,
        )

    private val execution =
        VexMapsDistributionExecution(
            root =
                executionRoot,
            session =
                this,
        )

    val navigationOutputs:
        Flow<VexMapsDeliveryRouteNavigationOutput> =
        navigationRelay.outputStates

    val currentNavigation:
        VexMapsDeliveryRouteNavigationOutput
        get() =
            navigationRelay.currentOutput

    private var activePack:
        VexMapsRegionPack? =
        null

    init {
        modeDeliveryRouteRelay.activate(
            relayScope,
        )
        navigationRelay.activate(
            relayScope,
        )

        execution.start()
        execution.restoreDraft()
    }

    internal suspend fun shutdown() {
        execution.shutdown()
        modeDeliveryRouteRelay.deactivate()
        navigationRelay.deactivate()
    }

    fun toggleMode() {
        mode.activate(
            VexMapsDistributionModeControl
                .ToggleDistribution,
        )
    }

    fun addStop(
        placeRef: String?,
        name: String,
        subtitle: String?,
        latitude: Double,
        longitude: Double,
    ) {
        if (!beginDraftOperation()) {
            return
        }

        execution.addStop(
            VexMapsDistributionAddStopWork(
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
            ),
        )
    }

    internal suspend fun addStopForExecution(
        work:
            VexMapsDistributionAddStopWork,
    ) {
        runCatching {
            repository.addStop(
                placeRef =
                    work.placeRef,
                name =
                    work.name,
                subtitle =
                    work.subtitle,
                latitude =
                    work.latitude,
                longitude =
                    work.longitude,
            )
        }.onSuccess {
            finishDraftOperation(
                draft =
                    it,
            )
        }.onFailure {
            finishDraftOperation(
                error =
                    it,
            )
        }
    }

    fun removeStop(
        stopRef: String,
    ) {
        if (!beginDraftOperation()) {
            return
        }

        execution.removeStop(
            stopRef,
        )
    }

    internal suspend fun removeStopForExecution(
        stopRef: String,
    ) {
        runCatching {
            repository.removeStop(
                stopRef,
            )
        }.onSuccess {
            finishDraftOperation(
                draft =
                    it,
            )
        }.onFailure {
            finishDraftOperation(
                error =
                    it,
            )
        }
    }

    fun setReturnToStart(
        enabled: Boolean,
    ) {
        if (!beginDraftOperation()) {
            return
        }

        execution.setReturnToStart(
            enabled,
        )
    }

    internal suspend fun setReturnToStartForExecution(
        enabled: Boolean,
    ) {
        runCatching {
            repository.setReturnToStart(
                enabled,
            )
        }.onSuccess {
            finishDraftOperation(
                draft =
                    it,
            )
        }.onFailure {
            finishDraftOperation(
                error =
                    it,
            )
        }
    }

    fun prepareDeliveryRoute(
        origin: VexMapCoordinate,
        travelMode: VexMapsTravelMode,
        pack: VexMapsRegionPack,
    ) {
        if (
            mutableState.value
                .draftOperationInFlight ||
            !deliveryRoute.currentProjection[
                VexMapsDeliveryRouteControl.Prepare
            ].isAvailable
        ) {
            return
        }

        val draft =
            mutableState.value
                .draft
        if (draft.stops.isEmpty()) {
            return
        }

        deliveryRoute.activate(
            VexMapsDeliveryRouteControl.Prepare,
        )

        val preparing =
            deliveryRoute.currentProjection
                .state as?
                VexMapsDeliveryRouteState.Preparing
                ?: return

        execution.cancelPreparation()
        execution.cancelLeg()
        clearExpectedLeg()
        setLegAttempt(
            VexMapsDeliveryRouteLegAttempt.Idle,
        )

        val cleanupFailure =
            runCatching {
                navigation.stop()
            }.exceptionOrNull()

        if (cleanupFailure != null) {
            deliveryRoute.preparationFailed(
                preparing.preparationRef,
            )
            publishSession(
                plan =
                    null,
                preparationFailure =
                    cleanupFailure
                        .failureMeaning(),
                preparationCleanupFailure =
                    null,
            )
            return
        }

        val preparation =
            VexMapsDistributionPreparationSnapshot(
                preparationRef =
                    preparing.preparationRef,
                draftRevision =
                    preparing.source.revision,
                stopRefs =
                    preparing.source.stopRefs,
                returnToStart =
                    preparing.source.returnToStart,
                origin =
                    origin,
                travelMode =
                    travelMode,
                regionPackIdentity =
                    pack.identity,
            )

        activePack =
            pack
        publishSession(
            plan =
                null,
            preparationFailure =
                null,
            preparationCleanupFailure =
                null,
        )

        execution.startPreparation(
            VexMapsDistributionPreparationWork(
                draft =
                    draft,
                preparation =
                    preparation,
                pack =
                    pack,
            ),
        )
    }

    internal suspend fun prepareForExecution(
        work:
            VexMapsDistributionPreparationWork,
    ) {
        val draft =
            work.draft
        val preparation =
            work.preparation
        val pack =
            work.pack

        try {
            val plan =
                routing.optimize(
                    draft =
                        draft,
                    preparation =
                        preparation,
                    pack =
                        pack,
                )

            ensurePreparationStillCurrent(
                preparation,
            )

            val firstStopRef =
                plan.orderedStops
                    .first()
                    .stopRef
            val route =
                routeToStop(
                    plan =
                        plan,
                    stopRef =
                        firstStopRef,
                    pack =
                        pack,
                )

            ensurePreparationStillCurrent(
                preparation,
            )

            navigation.prepare(
                routeResult =
                    route,
                routingPack =
                    pack,
            )
            setExpectedLeg(
                VexMapsDeliveryRouteExpectedLeg(
                    planRef =
                        plan.planRef,
                    target =
                        VexMapsDeliveryRouteLegTarget
                            .Stop(
                                firstStopRef,
                            ),
                    route =
                        route.identity(),
                ),
            )

            if (
                !deliveryRoute.preparationSucceeded(
                    preparationRef =
                        preparation.preparationRef,
                    planRef =
                        plan.planRef,
                    orderedStopRefs =
                        plan.orderedStops
                            .map {
                                it.stopRef
                            },
                )
            ) {
                return
            }

            publishSession(
                plan =
                    plan,
                preparationFailure =
                    null,
                preparationCleanupFailure =
                    null,
            )
        } catch (
            cancelled:
                CancellationException,
        ) {
            throw cancelled
        } catch (
            error:
                Throwable,
        ) {
            deliveryRoute.preparationFailed(
                preparation.preparationRef,
            )
            activePack =
                null
            clearExpectedLeg()

            val cleanupFailure =
                runCatching {
                    navigation.stop()
                }.exceptionOrNull()

            publishSession(
                plan =
                    null,
                preparationFailure =
                    error.failureMeaning(),
                preparationCleanupFailure =
                    cleanupFailure
                        ?.failureMeaning(),
            )
        }
    }

    /**
     * Call only for a real human change to a material preparation input.
     *
     * Transient Compose restoration/default values are not preparation truth and must not use this
     * method. Once a delivery route is executing, its admitted Plan remains sealed.
     */
    fun canReplaceRoutingRegion():
        Boolean =
        when (
            deliveryRoute.currentProjection
                .state
        ) {
            is VexMapsDeliveryRouteState.Delivering,
            is VexMapsDeliveryRouteState.AtDelivery,
            is VexMapsDeliveryRouteState.ReturningToStart ->
                false

            else ->
                true
        }

    fun routingRegionReplacementRequested():
        Boolean {
        if (!canReplaceRoutingRegion()) {
            return false
        }

        humanPlanningInputChanged()
        return true
    }

    /**
     * Invalidates Preparing/Ready reality only for an explicit human change to a material planning
     * input. A Ready Plan seals the origin admitted at Prepare time; later live device fixes are
     * Navigation observations and do not silently redefine that Plan.
     */
    fun humanPlanningInputChanged() {
        when (
            val state =
                deliveryRoute.currentProjection
                    .state
        ) {
            is VexMapsDeliveryRouteState.Preparing -> {
                execution.cancelPreparation()
                deliveryRoute.preparationFailed(
                    state.preparationRef,
                )
                invalidatePreparedReality()
            }

            is VexMapsDeliveryRouteState.Ready -> {
                deliveryRoute.invalidateReadyPlan(
                    state.planRef,
                )
                invalidatePreparedReality()
            }

            else ->
                Unit
        }
    }

    /**
     * Starts the exact Ready Plan. Its preparation origin is sealed; the fresh precise-location read
     * performed by VexMapsMapSurface is Navigation start preflight, not a new Distribution origin.
     */
    fun startPreparedDeliveryRoute() {
        val ready =
            deliveryRoute.currentProjection
                .state as?
                VexMapsDeliveryRouteState.Ready
                ?: return
        val expected =
            (
                mutableExpectedLeg.value as?
                    VexMapsDeliveryRouteExpectedLegState.Expected
            )
                ?.leg
                ?: return

        if (
            expected.planRef !=
                ready.planRef ||
            !vexMapsDeliveryRouteNavigationRealizesExpectedLeg(
                navigation =
                    navigation.current,
                expected =
                    expected,
            ) ||
            navigation.current.phase !=
                VexNavigationSessionPhase.READY
        ) {
            return
        }

        deliveryRoute.activate(
            VexMapsDeliveryRouteControl.Start,
        )
    }

    fun markCurrentDeliveryComplete() {
        if (
            !deliveryRoute.currentProjection[
                VexMapsDeliveryRouteControl.MarkDelivered
            ].isAvailable
        ) {
            return
        }

        val nextTarget =
            nextLegTargetAfterCurrentDelivery()

        setLegAttempt(
            VexMapsDeliveryRouteLegAttempt
                .CleaningUpPreviousLeg(
                    nextTarget =
                        nextTarget,
                ),
        )

        deliveryRoute.activate(
            VexMapsDeliveryRouteControl.MarkDelivered,
        )

        val cleanupFailure =
            cleanupArrivedNavigation()

        clearExpectedLeg()

        if (cleanupFailure != null) {
            setLegAttempt(
                VexMapsDeliveryRouteLegAttempt.Failed(
                    stage =
                        VexMapsDeliveryRouteLegFailureStage
                            .PreviousLegCleanup,
                    target =
                        nextTarget,
                    reason =
                        cleanupFailure,
                ),
            )
        } else {
            setLegAttempt(
                VexMapsDeliveryRouteLegAttempt.Idle,
            )
        }
    }

    fun retryFailedLeg() {
        val failed =
            mutableLegAttempt.value as?
                VexMapsDeliveryRouteLegAttempt.Failed
                ?: return

        if (
            failed.stage ==
            VexMapsDeliveryRouteLegFailureStage
                .PreviousLegCleanup
        ) {
            setLegAttempt(
                VexMapsDeliveryRouteLegAttempt
                    .CleaningUpPreviousLeg(
                        nextTarget =
                            failed.target,
                    ),
            )

            val cleanupFailure =
                cleanupArrivedNavigation()

            if (cleanupFailure != null) {
                setLegAttempt(
                    failed.copy(
                        reason =
                            cleanupFailure,
                    ),
                )
            } else {
                setLegAttempt(
                    VexMapsDeliveryRouteLegAttempt.Idle,
                )
            }
            return
        }

        setLegAttempt(
            VexMapsDeliveryRouteLegAttempt.Idle,
        )
    }

    fun cancelDeliveryRoutePreservingDraft() {
        if (
            deliveryRoute.currentProjection[
                VexMapsDeliveryRouteControl.Clear
            ].isAvailable
        ) {
            deliveryRoute.activate(
                VexMapsDeliveryRouteControl.Clear,
            )
        }

        invalidatePreparedReality()

        publishDraft(
            mutableState.value
                .draft,
        )
    }

    fun clearDeliveryRoute() {
        if (
            mutableState.value
                .draftOperationInFlight
        ) {
            return
        }

        val retainedDraft =
            mutableState.value
                .draft

        execution.cancelPreparation()
        execution.cancelLeg()

        val currentDeliveryRouteState =
            deliveryRoute.currentProjection
                .state

        if (
            currentDeliveryRouteState !is
                VexMapsDeliveryRouteState.Empty &&
            currentDeliveryRouteState !is
                VexMapsDeliveryRouteState.Draft &&
            deliveryRoute.currentProjection[
                VexMapsDeliveryRouteControl.Clear
            ].isAvailable
        ) {
            deliveryRoute.activate(
                VexMapsDeliveryRouteControl.Clear,
            )
            invalidatePreparedReality()
            publishDraft(
                retainedDraft,
            )
        }

        mutableState.value =
            mutableState.value
                .copy(
                    draftOperationInFlight =
                        true,
                    draftFailure =
                        null,
                )

        execution.clearDraft()
    }

    internal suspend fun clearDraftForExecution() {
        runCatching {
            repository.clearDraft()
        }.onSuccess {
            finishDraftOperation(
                draft =
                    it,
            )
        }.onFailure {
            finishDraftOperation(
                error =
                    it,
            )
        }
    }

    internal suspend fun restoreDraftForExecution() {
        runCatching {
            repository.loadDraft()
        }.onSuccess {
            finishDraftOperation(
                draft =
                    it,
            )
        }.onFailure {
            finishDraftOperation(
                error =
                    it,
            )
        }
    }

    private fun publishDraft(
        draft:
            VexMapsDistributionDraft,
    ) {
        val admitted =
            deliveryRoute.observeDraft(
                revision =
                    draft.revision,
                stopRefs =
                    draft.stops
                        .sortedBy {
                            it.inputOrder
                        }
                        .map {
                            it.stopRef
                        },
                returnToStart =
                    draft.returnToStart,
            )

        if (admitted) {
            mutableState.value =
                mutableState.value
                    .copy(
                        draft =
                            draft,
                        plan =
                            null,
                        preparationFailure =
                            null,
                        preparationCleanupFailure =
                            null,
                    )
        }
    }

    private fun draftEditable():
        Boolean =
        !mutableState.value
            .draftOperationInFlight &&
            when (
                deliveryRoute.currentProjection
                    .state
            ) {
                VexMapsDeliveryRouteState.Empty,
                is VexMapsDeliveryRouteState.Draft ->
                    true

                else ->
                    false
            }

    private fun beginDraftOperation():
        Boolean {
        if (!draftEditable()) {
            return false
        }

        mutableState.value =
            mutableState.value
                .copy(
                    draftOperationInFlight =
                        true,
                    draftFailure =
                        null,
                )
        return true
    }

    private fun finishDraftOperation(
        draft:
            VexMapsDistributionDraft? =
            null,
        error:
            Throwable? =
            null,
    ) {
        if (draft != null) {
            publishDraft(
                draft,
            )
        }

        mutableState.value =
            mutableState.value
                .copy(
                    draftOperationInFlight =
                        false,
                    draftFailure =
                        error
                            ?.failureMeaning(),
                )
    }

    private fun publishSession(
        plan:
            VexMapsDistributionPlan?,
        preparationFailure:
            String?,
        preparationCleanupFailure:
            String? =
            mutableState.value
                .preparationCleanupFailure,
    ) {
        mutableState.value =
            mutableState.value
                .copy(
                    plan =
                        plan,
                    preparationFailure =
                        preparationFailure,
                    preparationCleanupFailure =
                        preparationCleanupFailure,
                    legAttempt =
                        mutableLegAttempt.value,
                    expectedLeg =
                        mutableExpectedLeg.value,
                )
    }

    private fun setExpectedLeg(
        expected:
            VexMapsDeliveryRouteExpectedLeg,
    ) {
        mutableExpectedLeg.value =
            VexMapsDeliveryRouteExpectedLegState
                .Expected(
                    expected,
                )
        publishSession(
            plan =
                mutableState.value
                    .plan,
            preparationFailure =
                mutableState.value
                    .preparationFailure,
        )
    }

    private fun clearExpectedLeg() {
        mutableExpectedLeg.value =
            VexMapsDeliveryRouteExpectedLegState.None
        publishSession(
            plan =
                mutableState.value
                    .plan,
            preparationFailure =
                mutableState.value
                    .preparationFailure,
        )
    }

    private fun setLegAttempt(
        attempt:
            VexMapsDeliveryRouteLegAttempt,
    ) {
        mutableLegAttempt.value =
            attempt
        publishSession(
            plan =
                mutableState.value
                    .plan,
            preparationFailure =
                mutableState.value
                    .preparationFailure,
        )
    }

    private fun invalidatePreparedReality() {
        execution.cancelPreparation()
        execution.cancelLeg()
        runCatching {
            navigation.stop()
        }
        activePack =
            null
        clearExpectedLeg()
        setLegAttempt(
            VexMapsDeliveryRouteLegAttempt.Idle,
        )
        publishSession(
            plan =
                null,
            preparationFailure =
                null,
            preparationCleanupFailure =
                null,
        )
    }

    private fun ensurePreparationStillCurrent(
        preparation:
            VexMapsDistributionPreparationSnapshot,
    ) {
        val current =
            deliveryRoute.currentProjection
                .state as?
                VexMapsDeliveryRouteState.Preparing
                ?: throw CancellationException(
                    "Distribution preparation is no longer admitted",
                )

        if (
            current.preparationRef !=
            preparation.preparationRef ||
            current.source.revision !=
            preparation.draftRevision ||
            current.source.stopRefs !=
            preparation.stopRefs ||
            current.source.returnToStart !=
            preparation.returnToStart
        ) {
            throw CancellationException(
                "Distribution preparation identity changed",
            )
        }
    }

    private fun nextLegTargetAfterCurrentDelivery():
        VexMapsDeliveryRouteLegTarget? {
        val state =
            deliveryRoute.currentProjection
                .state as?
                VexMapsDeliveryRouteState.AtDelivery
                ?: return null

        return state.nextStopRef
            ?.let {
                VexMapsDeliveryRouteLegTarget.Stop(
                    it,
                )
            }
            ?: if (state.source.returnToStart) {
                VexMapsDeliveryRouteLegTarget.ReturnToStart
            } else {
                null
            }
    }

    private fun currentLegTargetOrNull():
        VexMapsDeliveryRouteLegTarget? =
        when (
            val state =
                deliveryRoute.currentProjection
                    .state
        ) {
            is VexMapsDeliveryRouteState.Ready ->
                VexMapsDeliveryRouteLegTarget.Stop(
                    state.orderedStopRefs.first(),
                )

            is VexMapsDeliveryRouteState.Delivering ->
                VexMapsDeliveryRouteLegTarget.Stop(
                    state.currentStopRef,
                )

            is VexMapsDeliveryRouteState.ReturningToStart ->
                VexMapsDeliveryRouteLegTarget.ReturnToStart

            else ->
                null
        }

    internal fun modeDeliveryRouteOutputsForExecution():
        Flow<com.vextreme.vexlife.android.screens.shared.maps.distribution.convergence.VexMapsDistributionModeDeliveryRouteOutput> =
        modeDeliveryRouteRelay.outputStates

    internal fun navigationOutputsForExecution():
        Flow<VexMapsDeliveryRouteNavigationOutput> =
        navigationRelay.outputStates

    internal fun acceptModeDeliveryRouteOutput(
        output:
            com.vextreme.vexlife.android.screens.shared.maps.distribution.convergence.VexMapsDistributionModeDeliveryRouteOutput,
    ) {
        output.requestedModeControl
            ?.let { control ->
                mode.activate(
                    control,
                )
            }
    }

    internal fun acceptNavigationOutput(
        output:
            VexMapsDeliveryRouteNavigationOutput,
    ) {
        if (
            output.layer ==
                com.vextreme.vexlife.android.screens.shared.maps.distribution.convergence
                    .VexMapsDeliveryRouteNavigationLayer.Navigating &&
            mutableLegAttempt.value is
                VexMapsDeliveryRouteLegAttempt.Preparing
        ) {
            setLegAttempt(
                VexMapsDeliveryRouteLegAttempt.Idle,
            )
        }

        output.request
            ?.let {
                realizeNavigationRequest(
                    it,
                )
            }
    }

    private fun realizeNavigationRequest(
        request:
            VexMapsDeliveryRouteNavigationRequest,
    ) {
        when (
            request
        ) {
            is VexMapsDeliveryRouteNavigationRequest.PrepareLeg ->
                prepareLeg(
                    VexMapsDeliveryRouteLegTarget.Stop(
                        request.stopRef,
                    ),
                )

            VexMapsDeliveryRouteNavigationRequest.PrepareReturn ->
                prepareLeg(
                    VexMapsDeliveryRouteLegTarget.ReturnToStart,
                )

            VexMapsDeliveryRouteNavigationRequest.StartPreparedNavigation -> {
                val target =
                    currentLegTargetOrNull()
                        ?: return

                setLegAttempt(
                    VexMapsDeliveryRouteLegAttempt.Preparing(
                        target,
                    ),
                )

                runCatching {
                    navigation.startPrepared()
                }.onFailure { error ->
                    setLegAttempt(
                        VexMapsDeliveryRouteLegAttempt.Failed(
                            stage =
                                VexMapsDeliveryRouteLegFailureStage
                                    .NavigationStart,
                            target =
                                target,
                            reason =
                                error.failureMeaning(),
                        ),
                    )
                }
            }

            is VexMapsDeliveryRouteNavigationRequest.DeliveryRouteControl -> {
                val completingReturn =
                    request.control ==
                        VexMapsDeliveryRouteControl.Arrive &&
                        deliveryRoute.currentProjection
                            .state is
                        VexMapsDeliveryRouteState.ReturningToStart

                if (completingReturn) {
                    setLegAttempt(
                        VexMapsDeliveryRouteLegAttempt
                            .CleaningUpPreviousLeg(
                                nextTarget =
                                    null,
                            ),
                    )
                }

                deliveryRoute.activate(
                    request.control,
                )

                if (
                    completingReturn &&
                    deliveryRoute.currentProjection
                        .state is
                    VexMapsDeliveryRouteState.Completed
                ) {
                    /*
                     * The Delivery Route is already semantically complete. Terminal Android cleanup must not
                     * escape through VexExecution's onEach boundary and permanently kill the
                     * maintained Navigation convergence collector.
                     *
                     * Stop first: the production Navigation adapter clears process-local
                     * Navigation truth even when the best-effort service-stop request fails.
                     * Arrival-notification dismissal is an independent cleanup effect.
                     */
                    val cleanupFailure =
                        cleanupArrivedNavigation()
                    clearExpectedLeg()
                    setLegAttempt(
                        if (cleanupFailure != null) {
                            VexMapsDeliveryRouteLegAttempt.Failed(
                                stage =
                                    VexMapsDeliveryRouteLegFailureStage
                                        .PreviousLegCleanup,
                                target =
                                    null,
                                reason =
                                    cleanupFailure,
                            )
                        } else {
                            VexMapsDeliveryRouteLegAttempt.Idle
                        },
                    )
                }
            }
        }
    }

    private fun prepareLeg(
        target:
            VexMapsDeliveryRouteLegTarget,
    ) {
        if (
            mutableLegAttempt.value !is
            VexMapsDeliveryRouteLegAttempt.Idle
        ) {
            return
        }

        val plan =
            mutableState.value
                .plan
                ?: return
        val pack =
            activePack
                ?: return

        val deliveryRoutePlanRef =
            when (
                val current =
                    deliveryRoute.currentProjection
                        .state
            ) {
                is VexMapsDeliveryRouteState.Ready ->
                    current.planRef

                is VexMapsDeliveryRouteState.Delivering ->
                    current.planRef

                is VexMapsDeliveryRouteState.ReturningToStart ->
                    current.planRef

                else ->
                    return
            }

        if (
            deliveryRoutePlanRef !=
            plan.planRef
        ) {
            return
        }

        val alreadyPrepared =
            (
                mutableExpectedLeg.value as?
                    VexMapsDeliveryRouteExpectedLegState.Expected
            )
                ?.leg
                ?.takeIf {
                    it.planRef ==
                        plan.planRef &&
                        it.target ==
                        target
                }

        if (
            alreadyPrepared != null &&
            vexMapsDeliveryRouteNavigationRealizesExpectedLeg(
                navigation =
                    navigation.current,
                expected =
                    alreadyPrepared,
            )
        ) {
            return
        }

        setLegAttempt(
            VexMapsDeliveryRouteLegAttempt.Preparing(
                target,
            ),
        )
        clearExpectedLeg()

        execution.startLeg(
            VexMapsDistributionLegWork(
                target =
                    target,
                plan =
                    plan,
                pack =
                    pack,
            ),
        )
    }

    internal suspend fun prepareLegForExecution(
        work:
            VexMapsDistributionLegWork,
    ) {
        val target =
            work.target
        val plan =
            work.plan
        val pack =
            work.pack

        try {
            val route =
                when (
                    target
                ) {
                    is VexMapsDeliveryRouteLegTarget.Stop ->
                        routeToStop(
                            plan =
                                plan,
                            stopRef =
                                target.stopRef,
                            pack =
                                pack,
                        )

                    VexMapsDeliveryRouteLegTarget.ReturnToStart ->
                        routeReturn(
                            plan =
                                plan,
                            pack =
                                pack,
                        )
                }

            if (
                mutableState.value
                    .plan
                    ?.planRef !=
                plan.planRef
            ) {
                return
            }

            navigation.prepare(
                routeResult =
                    route,
                routingPack =
                    pack,
            )
            setExpectedLeg(
                VexMapsDeliveryRouteExpectedLeg(
                    planRef =
                        plan.planRef,
                    target =
                        target,
                    route =
                        route.identity(),
                ),
            )
            setLegAttempt(
                VexMapsDeliveryRouteLegAttempt.Idle,
            )
        } catch (
            cancelled:
                CancellationException,
        ) {
            throw cancelled
        } catch (
            error:
                Throwable,
        ) {
            setLegAttempt(
                VexMapsDeliveryRouteLegAttempt.Failed(
                    stage =
                        VexMapsDeliveryRouteLegFailureStage
                            .RoutePreparation,
                    target =
                        target,
                    reason =
                        error.failureMeaning(),
                ),
            )
        }
    }

    /**
     * Stop Navigation semantic truth first, then attempt arrival-notification cleanup independently.
     * The returned diagnostic blocks a next leg under PreviousLegCleanup rather than mislabeling it
     * as a route-preparation/provider failure.
     */
    private fun cleanupArrivedNavigation():
        String? {
        val failures =
            buildList {
                runCatching {
                    navigation.stop()
                }.exceptionOrNull()
                    ?.let { failure ->
                        add(
                            "stop: " +
                                failure
                                    .failureMeaning(),
                        )
                    }

                runCatching {
                    navigation.dismissArrival()
                }.exceptionOrNull()
                    ?.let { failure ->
                        add(
                            "dismiss-arrival: " +
                                failure
                                    .failureMeaning(),
                        )
                    }
            }

        return failures
            .takeIf {
                it.isNotEmpty()
            }
            ?.joinToString(
                separator = "; ",
            )
    }

    private suspend fun routeToStop(
        plan:
            VexMapsDistributionPlan,
        stopRef: String,
        pack:
            VexMapsRegionPack,
    ): VexOfflineRouteResult {
        val index =
            plan.orderedStops
                .indexOfFirst {
                    it.stopRef ==
                        stopRef
                }
        require(index >= 0) {
            "Distribution stop is not in current Plan"
        }

        val stop =
            plan.orderedStops[
                index
            ]
        val origin =
            if (index == 0) {
                plan.origin
            } else {
                plan.orderedStops[
                    index - 1
                ].coordinate()
            }

        return routing.route(
            pack =
                pack,
            request =
                VexOfflineRouteRequest(
                    routeRef =
                        plan.planRef +
                            ".stop." +
                            stop.stopRef,
                    routeRevision =
                        VexMapsRouteIdentity
                            .nextRevision(),
                    origin =
                        origin,
                    destination =
                        stop.coordinate(),
                    destinationLabel =
                        stop.name,
                    travelMode =
                        plan.travelMode,
                ),
        )
    }

    private suspend fun routeReturn(
        plan:
            VexMapsDistributionPlan,
        pack:
            VexMapsRegionPack,
    ): VexOfflineRouteResult {
        val last =
            plan.orderedStops
                .last()

        return routing.route(
            pack =
                pack,
            request =
                VexOfflineRouteRequest(
                    routeRef =
                        plan.planRef +
                            ".return",
                    routeRevision =
                        VexMapsRouteIdentity
                            .nextRevision(),
                    origin =
                        last.coordinate(),
                    destination =
                        plan.origin,
                    destinationLabel =
                        "Starting point",
                    travelMode =
                        plan.travelMode,
                ),
        )
    }
}

private fun VexOfflineRouteResult.identity():
    VexMapsDeliveryRouteNavigationRouteIdentity =
    VexMapsDeliveryRouteNavigationRouteIdentity(
        routeRef =
            route.routeRef,
        routeRevision =
            route.routeRevision,
    )

private fun com.vextreme.vexlife.android.data.maps.distribution.VexMapsDistributionStop.coordinate():
    VexMapCoordinate =
    VexMapCoordinate(
        latitude =
            latitude,
        longitude =
            longitude,
    )

private fun Throwable.failureMeaning():
    String =
    message
        ?.takeIf {
            it.isNotBlank()
        }
        ?: this::class
            .simpleName
        ?: "Distribution operation failed"

// [VXG RealForever]
