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
import com.vextreme.vexlife.android.screens.shared.maps.distribution.convergence.VexMapsDistributionExpectedLeg
import com.vextreme.vexlife.android.screens.shared.maps.distribution.convergence.VexMapsDistributionExpectedLegState
import com.vextreme.vexlife.android.screens.shared.maps.distribution.convergence.VexMapsDistributionLegAttempt
import com.vextreme.vexlife.android.screens.shared.maps.distribution.convergence.VexMapsDistributionLegFailureStage
import com.vextreme.vexlife.android.screens.shared.maps.distribution.convergence.VexMapsDistributionLegTarget
import com.vextreme.vexlife.android.screens.shared.maps.distribution.convergence.VexMapsDistributionModeRunConvergenceRelay
import com.vextreme.vexlife.android.screens.shared.maps.distribution.convergence.VexMapsDistributionNavigationConvergenceRelay
import com.vextreme.vexlife.android.screens.shared.maps.distribution.convergence.VexMapsDistributionNavigationOutput
import com.vextreme.vexlife.android.screens.shared.maps.distribution.convergence.VexMapsDistributionNavigationRequest
import com.vextreme.vexlife.android.screens.shared.maps.distribution.convergence.VexMapsDistributionRouteIdentity
import com.vextreme.vexlife.android.screens.shared.maps.distribution.convergence.vexMapsDistributionNavigationRealizesExpectedLeg
import com.vextreme.vexlife.android.screens.shared.maps.distribution.mode.CurrentVexMapsDistributionModeExperience
import com.vextreme.vexlife.android.screens.shared.maps.distribution.mode.VexMapsDistributionModeControl
import com.vextreme.vexlife.android.screens.shared.maps.distribution.mode.VexMapsDistributionModeExperience
import com.vextreme.vexlife.android.screens.shared.maps.distribution.mode.VexMapsDistributionModeProjection
import com.vextreme.vexlife.android.screens.shared.maps.distribution.planning.VexMapsDistributionPlan
import com.vextreme.vexlife.android.screens.shared.maps.distribution.planning.VexMapsDistributionPreparationSnapshot
import com.vextreme.vexlife.android.screens.shared.maps.distribution.run.CurrentVexMapsDistributionRunExperience
import com.vextreme.vexlife.android.screens.shared.maps.distribution.run.VexMapsDistributionRunControl
import com.vextreme.vexlife.android.screens.shared.maps.distribution.run.VexMapsDistributionRunExperience
import com.vextreme.vexlife.android.screens.shared.maps.distribution.run.VexMapsDistributionRunProjection
import com.vextreme.vexlife.android.screens.shared.maps.distribution.run.VexMapsDistributionRunState
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
        VexMapsDistributionLegAttempt =
        VexMapsDistributionLegAttempt.Idle,
    val expectedLeg:
        VexMapsDistributionExpectedLegState =
        VexMapsDistributionExpectedLegState.None,
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
        VexMapsDistributionLegTarget,
    val plan:
        VexMapsDistributionPlan,
    val pack:
        VexMapsRegionPack,
)

/**
 * Process-lifetime owner of one lived Distribution session.
 *
 * Repository, Mode, Run, Optimizer and Navigation remain independent owners. This class owns their
 * active relationship: the admitted preparation snapshot, derived Plan, expected Navigation leg,
 * async attempt/failure state and the bounded Control feedback needed to advance the run.
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
        VexMapsDistributionModeExperience =
        CurrentVexMapsDistributionModeExperience(),
    private val run:
        VexMapsDistributionRunExperience =
        CurrentVexMapsDistributionRunExperience(),
) {
    private val mutableExpectedLeg =
        MutableStateFlow<
            VexMapsDistributionExpectedLegState
        >(
            VexMapsDistributionExpectedLegState.None,
        )
    private val mutableLegAttempt =
        MutableStateFlow<
            VexMapsDistributionLegAttempt
        >(
            VexMapsDistributionLegAttempt.Idle,
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

    val runProjections:
        StateFlow<VexMapsDistributionRunProjection>
        get() =
            run.projections

    val currentMode:
        VexMapsDistributionModeProjection
        get() =
            mode.currentProjection

    val currentRun:
        VexMapsDistributionRunProjection
        get() =
            run.currentProjection

    private val modeRunRelay =
        VexMapsDistributionModeRunConvergenceRelay(
            modeProjections =
                mode.projections,
            runProjections =
                run.projections,
        )

    private val navigationRelay =
        VexMapsDistributionNavigationConvergenceRelay(
            runProjections =
                run.projections,
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
        Flow<VexMapsDistributionNavigationOutput> =
        navigationRelay.outputStates

    val currentNavigation:
        VexMapsDistributionNavigationOutput
        get() =
            navigationRelay.currentOutput

    private var activePack:
        VexMapsRegionPack? =
        null

    init {
        modeRunRelay.activate(
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
        modeRunRelay.deactivate()
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

    fun prepare(
        origin: VexMapCoordinate,
        travelMode: VexMapsTravelMode,
        pack: VexMapsRegionPack,
    ) {
        if (
            mutableState.value
                .draftOperationInFlight ||
            !run.currentProjection[
                VexMapsDistributionRunControl.Prepare
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

        run.activate(
            VexMapsDistributionRunControl.Prepare,
        )

        val preparing =
            run.currentProjection
                .state as?
                VexMapsDistributionRunState.Preparing
                ?: return

        execution.cancelPreparation()
        execution.cancelLeg()
        clearExpectedLeg()
        setLegAttempt(
            VexMapsDistributionLegAttempt.Idle,
        )

        val cleanupFailure =
            runCatching {
                navigation.stop()
            }.exceptionOrNull()

        if (cleanupFailure != null) {
            run.preparationFailed(
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
                VexMapsDistributionExpectedLeg(
                    planRef =
                        plan.planRef,
                    target =
                        VexMapsDistributionLegTarget
                            .Stop(
                                firstStopRef,
                            ),
                    route =
                        route.identity(),
                ),
            )

            if (
                !run.preparationSucceeded(
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
            run.preparationFailed(
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
     * method. Once a run is executing, its admitted Plan remains sealed.
     */
    fun canReplaceRoutingRegion():
        Boolean =
        when (
            run.currentProjection
                .state
        ) {
            is VexMapsDistributionRunState.Running,
            is VexMapsDistributionRunState.AtStop,
            is VexMapsDistributionRunState.Returning ->
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
                run.currentProjection
                    .state
        ) {
            is VexMapsDistributionRunState.Preparing -> {
                execution.cancelPreparation()
                run.preparationFailed(
                    state.preparationRef,
                )
                invalidatePreparedReality()
            }

            is VexMapsDistributionRunState.Ready -> {
                run.invalidateReadyPlan(
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
    fun startPreparedRun() {
        val ready =
            run.currentProjection
                .state as?
                VexMapsDistributionRunState.Ready
                ?: return
        val expected =
            (
                mutableExpectedLeg.value as?
                    VexMapsDistributionExpectedLegState.Expected
            )
                ?.leg
                ?: return

        if (
            expected.planRef !=
                ready.planRef ||
            !vexMapsDistributionNavigationRealizesExpectedLeg(
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

        run.activate(
            VexMapsDistributionRunControl.Start,
        )
    }

    fun completeCurrentStop() {
        if (
            !run.currentProjection[
                VexMapsDistributionRunControl.CompleteStop
            ].isAvailable
        ) {
            return
        }

        val nextTarget =
            nextLegTargetAfterCurrentStop()

        setLegAttempt(
            VexMapsDistributionLegAttempt
                .CleaningUpPreviousLeg(
                    nextTarget =
                        nextTarget,
                ),
        )

        run.activate(
            VexMapsDistributionRunControl.CompleteStop,
        )

        val cleanupFailure =
            cleanupArrivedNavigation()

        clearExpectedLeg()

        if (cleanupFailure != null) {
            setLegAttempt(
                VexMapsDistributionLegAttempt.Failed(
                    stage =
                        VexMapsDistributionLegFailureStage
                            .PreviousLegCleanup,
                    target =
                        nextTarget,
                    reason =
                        cleanupFailure,
                ),
            )
        } else {
            setLegAttempt(
                VexMapsDistributionLegAttempt.Idle,
            )
        }
    }

    fun retryFailedLeg() {
        val failed =
            mutableLegAttempt.value as?
                VexMapsDistributionLegAttempt.Failed
                ?: return

        if (
            failed.stage ==
            VexMapsDistributionLegFailureStage
                .PreviousLegCleanup
        ) {
            setLegAttempt(
                VexMapsDistributionLegAttempt
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
                    VexMapsDistributionLegAttempt.Idle,
                )
            }
            return
        }

        setLegAttempt(
            VexMapsDistributionLegAttempt.Idle,
        )
    }

    fun cancelExecutionPreservingDraft() {
        if (
            run.currentProjection[
                VexMapsDistributionRunControl.Reset
            ].isAvailable
        ) {
            run.activate(
                VexMapsDistributionRunControl.Reset,
            )
        }

        invalidatePreparedReality()

        publishDraft(
            mutableState.value
                .draft,
        )
    }

    fun clearAll() {
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

        val currentRunState =
            run.currentProjection
                .state

        if (
            currentRunState !is
                VexMapsDistributionRunState.Empty &&
            currentRunState !is
                VexMapsDistributionRunState.Draft &&
            run.currentProjection[
                VexMapsDistributionRunControl.Reset
            ].isAvailable
        ) {
            run.activate(
                VexMapsDistributionRunControl.Reset,
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
            run.observeDraft(
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
                run.currentProjection
                    .state
            ) {
                VexMapsDistributionRunState.Empty,
                is VexMapsDistributionRunState.Draft ->
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
            VexMapsDistributionExpectedLeg,
    ) {
        mutableExpectedLeg.value =
            VexMapsDistributionExpectedLegState
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
            VexMapsDistributionExpectedLegState.None
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
            VexMapsDistributionLegAttempt,
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
            VexMapsDistributionLegAttempt.Idle,
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
            run.currentProjection
                .state as?
                VexMapsDistributionRunState.Preparing
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

    private fun nextLegTargetAfterCurrentStop():
        VexMapsDistributionLegTarget? {
        val state =
            run.currentProjection
                .state as?
                VexMapsDistributionRunState.AtStop
                ?: return null

        return state.nextStopRef
            ?.let {
                VexMapsDistributionLegTarget.Stop(
                    it,
                )
            }
            ?: if (state.source.returnToStart) {
                VexMapsDistributionLegTarget.ReturnToStart
            } else {
                null
            }
    }

    private fun currentLegTargetOrNull():
        VexMapsDistributionLegTarget? =
        when (
            val state =
                run.currentProjection
                    .state
        ) {
            is VexMapsDistributionRunState.Ready ->
                VexMapsDistributionLegTarget.Stop(
                    state.orderedStopRefs.first(),
                )

            is VexMapsDistributionRunState.Running ->
                VexMapsDistributionLegTarget.Stop(
                    state.currentStopRef,
                )

            is VexMapsDistributionRunState.Returning ->
                VexMapsDistributionLegTarget.ReturnToStart

            else ->
                null
        }

    internal fun modeRunOutputsForExecution():
        Flow<com.vextreme.vexlife.android.screens.shared.maps.distribution.convergence.VexMapsDistributionModeRunOutput> =
        modeRunRelay.outputStates

    internal fun navigationOutputsForExecution():
        Flow<VexMapsDistributionNavigationOutput> =
        navigationRelay.outputStates

    internal fun acceptModeRunOutput(
        output:
            com.vextreme.vexlife.android.screens.shared.maps.distribution.convergence.VexMapsDistributionModeRunOutput,
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
            VexMapsDistributionNavigationOutput,
    ) {
        if (
            output.layer ==
                com.vextreme.vexlife.android.screens.shared.maps.distribution.convergence
                    .VexMapsDistributionNavigationLayer.Navigating &&
            mutableLegAttempt.value is
                VexMapsDistributionLegAttempt.Preparing
        ) {
            setLegAttempt(
                VexMapsDistributionLegAttempt.Idle,
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
            VexMapsDistributionNavigationRequest,
    ) {
        when (
            request
        ) {
            is VexMapsDistributionNavigationRequest.PrepareLeg ->
                prepareLeg(
                    VexMapsDistributionLegTarget.Stop(
                        request.stopRef,
                    ),
                )

            VexMapsDistributionNavigationRequest.PrepareReturn ->
                prepareLeg(
                    VexMapsDistributionLegTarget.ReturnToStart,
                )

            VexMapsDistributionNavigationRequest.StartPreparedNavigation -> {
                val target =
                    currentLegTargetOrNull()
                        ?: return

                setLegAttempt(
                    VexMapsDistributionLegAttempt.Preparing(
                        target,
                    ),
                )

                runCatching {
                    navigation.startPrepared()
                }.onFailure { error ->
                    setLegAttempt(
                        VexMapsDistributionLegAttempt.Failed(
                            stage =
                                VexMapsDistributionLegFailureStage
                                    .NavigationStart,
                            target =
                                target,
                            reason =
                                error.failureMeaning(),
                        ),
                    )
                }
            }

            is VexMapsDistributionNavigationRequest.RunControl -> {
                val completingReturn =
                    request.control ==
                        VexMapsDistributionRunControl.Arrive &&
                        run.currentProjection
                            .state is
                        VexMapsDistributionRunState.Returning

                if (completingReturn) {
                    setLegAttempt(
                        VexMapsDistributionLegAttempt
                            .CleaningUpPreviousLeg(
                                nextTarget =
                                    null,
                            ),
                    )
                }

                run.activate(
                    request.control,
                )

                if (
                    completingReturn &&
                    run.currentProjection
                        .state is
                    VexMapsDistributionRunState.Completed
                ) {
                    /*
                     * Run is already semantically complete. Terminal Android cleanup must not
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
                            VexMapsDistributionLegAttempt.Failed(
                                stage =
                                    VexMapsDistributionLegFailureStage
                                        .PreviousLegCleanup,
                                target =
                                    null,
                                reason =
                                    cleanupFailure,
                            )
                        } else {
                            VexMapsDistributionLegAttempt.Idle
                        },
                    )
                }
            }
        }
    }

    private fun prepareLeg(
        target:
            VexMapsDistributionLegTarget,
    ) {
        if (
            mutableLegAttempt.value !is
            VexMapsDistributionLegAttempt.Idle
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

        val runPlanRef =
            when (
                val current =
                    run.currentProjection
                        .state
            ) {
                is VexMapsDistributionRunState.Ready ->
                    current.planRef

                is VexMapsDistributionRunState.Running ->
                    current.planRef

                is VexMapsDistributionRunState.Returning ->
                    current.planRef

                else ->
                    return
            }

        if (
            runPlanRef !=
            plan.planRef
        ) {
            return
        }

        val alreadyPrepared =
            (
                mutableExpectedLeg.value as?
                    VexMapsDistributionExpectedLegState.Expected
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
            vexMapsDistributionNavigationRealizesExpectedLeg(
                navigation =
                    navigation.current,
                expected =
                    alreadyPrepared,
            )
        ) {
            return
        }

        setLegAttempt(
            VexMapsDistributionLegAttempt.Preparing(
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
                    is VexMapsDistributionLegTarget.Stop ->
                        routeToStop(
                            plan =
                                plan,
                            stopRef =
                                target.stopRef,
                            pack =
                                pack,
                        )

                    VexMapsDistributionLegTarget.ReturnToStart ->
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
                VexMapsDistributionExpectedLeg(
                    planRef =
                        plan.planRef,
                    target =
                        target,
                    route =
                        route.identity(),
                ),
            )
            setLegAttempt(
                VexMapsDistributionLegAttempt.Idle,
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
                VexMapsDistributionLegAttempt.Failed(
                    stage =
                        VexMapsDistributionLegFailureStage
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
    VexMapsDistributionRouteIdentity =
    VexMapsDistributionRouteIdentity(
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
