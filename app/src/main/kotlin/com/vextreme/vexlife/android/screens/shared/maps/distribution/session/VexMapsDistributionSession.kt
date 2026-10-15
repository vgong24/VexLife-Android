package com.vextreme.vexlife.android.screens.shared.maps.distribution.session

import com.vextreme.vexlife.android.data.maps.distribution.VexMapsDistributionDraft
import com.vextreme.vexlife.android.data.maps.distribution.VexMapsDistributionRepository
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
import com.vextreme.vexlife.android.screens.shared.maps.distribution.convergence.VexMapsDistributionLegTarget
import com.vextreme.vexlife.android.screens.shared.maps.distribution.convergence.VexMapsDistributionModeRunConvergenceRelay
import com.vextreme.vexlife.android.screens.shared.maps.distribution.convergence.VexMapsDistributionNavigationConvergenceRelay
import com.vextreme.vexlife.android.screens.shared.maps.distribution.convergence.VexMapsDistributionNavigationOutput
import com.vextreme.vexlife.android.screens.shared.maps.distribution.convergence.VexMapsDistributionNavigationRequest
import com.vextreme.vexlife.android.screens.shared.maps.distribution.convergence.VexMapsDistributionRouteIdentity
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
    val legAttempt:
        VexMapsDistributionLegAttempt =
        VexMapsDistributionLegAttempt.Idle,
    val expectedLeg:
        VexMapsDistributionExpectedLegState =
        VexMapsDistributionExpectedLegState.None,
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
    private val scope:
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

    val navigationOutputs:
        Flow<VexMapsDistributionNavigationOutput> =
        navigationRelay.outputStates

    val currentNavigation:
        VexMapsDistributionNavigationOutput
        get() =
            navigationRelay.currentOutput

    private var preparationJob:
        Job? =
        null
    private var legJob:
        Job? =
        null
    private var activePack:
        VexMapsRegionPack? =
        null

    init {
        modeRunRelay.activate(
            scope,
        )
        navigationRelay.activate(
            scope,
        )

        scope.launch {
            modeRunRelay
                .outputStates
                .collect { output ->
                    output
                        .requestedModeControl
                        ?.let { control ->
                            mode.activate(
                                control,
                            )
                        }
                }
        }

        scope.launch {
            navigationRelay
                .outputStates
                .collect { output ->
                    output.request
                        ?.let {
                            realizeNavigationRequest(
                                it,
                            )
                        }
                }
        }

        scope.launch {
            restoreDraft()
        }
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

        scope.launch {
            runCatching {
                repository.addStop(
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
    }

    fun removeStop(
        stopRef: String,
    ) {
        if (!beginDraftOperation()) {
            return
        }

        scope.launch {
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
    }

    fun setReturnToStart(
        enabled: Boolean,
    ) {
        if (!beginDraftOperation()) {
            return
        }

        scope.launch {
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

        preparationJob?.cancel()
        legJob?.cancel()
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
        )

        preparationJob =
            scope.launch {
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
                        return@launch
                    }

                    publishSession(
                        plan =
                            plan,
                        preparationFailure =
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
                    navigation.stop()
                    publishSession(
                        plan =
                            null,
                        preparationFailure =
                            error.failureMeaning(),
                    )
                }
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

        planningInputChanged()
        return true
    }

    fun planningInputChanged() {
        when (
            val state =
                run.currentProjection
                    .state
        ) {
            is VexMapsDistributionRunState.Preparing -> {
                preparationJob?.cancel()
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
            !navigation.matches(
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

        run.activate(
            VexMapsDistributionRunControl.CompleteStop,
        )

        val cleanupFailure =
            runCatching {
                navigation.dismissArrival()
                navigation.stop()
            }.exceptionOrNull()

        clearExpectedLeg()

        val nextTarget =
            currentLegTargetOrNull()

        if (
            cleanupFailure != null &&
            nextTarget != null
        ) {
            setLegAttempt(
                VexMapsDistributionLegAttempt.Failed(
                    target =
                        nextTarget,
                    reason =
                        cleanupFailure
                            .failureMeaning(),
                ),
            )
        } else {
            setLegAttempt(
                VexMapsDistributionLegAttempt.Idle,
            )
        }
    }

    fun retryFailedLeg() {
        if (
            mutableLegAttempt.value !is
            VexMapsDistributionLegAttempt.Failed
        ) {
            return
        }

        setLegAttempt(
            VexMapsDistributionLegAttempt.Idle,
        )
    }

    fun cancelExecutionPreservingDraft() {
        preparationJob?.cancel()
        legJob?.cancel()

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

        preparationJob?.cancel()
        legJob?.cancel()

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

        scope.launch {
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
    }

    private suspend fun restoreDraft() {
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
    ) {
        mutableState.value =
            mutableState.value
                .copy(
                    plan =
                        plan,
                    preparationFailure =
                        preparationFailure,
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
        preparationJob?.cancel()
        legJob?.cancel()
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

            VexMapsDistributionNavigationRequest.StartPreparedNavigation ->
                navigation.startPrepared()

            is VexMapsDistributionNavigationRequest.RunControl -> {
                val completingReturn =
                    request.control ==
                        VexMapsDistributionRunControl.Arrive &&
                        run.currentProjection
                            .state is
                        VexMapsDistributionRunState.Returning

                run.activate(
                    request.control,
                )

                if (
                    completingReturn &&
                    run.currentProjection
                        .state is
                    VexMapsDistributionRunState.Completed
                ) {
                    navigation.dismissArrival()
                    navigation.stop()
                    clearExpectedLeg()
                }
            }
        }
    }

    private fun prepareLeg(
        target:
            VexMapsDistributionLegTarget,
    ) {
        if (
            mutableLegAttempt.value is
            VexMapsDistributionLegAttempt.Preparing
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

        legJob?.cancel()
        setLegAttempt(
            VexMapsDistributionLegAttempt.Preparing(
                target,
            ),
        )
        clearExpectedLeg()

        legJob =
            scope.launch {
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
                        return@launch
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
                            target =
                                target,
                            reason =
                                error.failureMeaning(),
                        ),
                    )
                }
            }
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

private fun VexMapsDistributionNavigationPort.matches(
    expected:
        VexMapsDistributionExpectedLeg,
): Boolean {
    val route =
        current
            .routeResult
            ?.route
            ?: return false

    return route.routeRef ==
        expected.route.routeRef &&
        route.routeRevision ==
        expected.route.routeRevision
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
