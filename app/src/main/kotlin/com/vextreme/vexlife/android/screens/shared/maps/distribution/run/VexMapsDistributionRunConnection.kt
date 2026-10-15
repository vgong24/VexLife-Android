package com.vextreme.vexlife.android.screens.shared.maps.distribution.run

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class VexMapsDistributionRunConnection(
    initialState:
        VexMapsDistributionRunState =
        VexMapsDistributionRunState.Empty,
) : VexMapsDistributionRunConnectionFlow {
    private val behavior =
        vexMapsDistributionRunBehavior()
    private val controls =
        behavior.vexMapsDistributionRunControls()
    private var state =
        initialState
    private val mutableProjections =
        MutableStateFlow(
            controls.projection(
                state,
            ),
        )

    override val projections:
        StateFlow<VexMapsDistributionRunProjection> =
        mutableProjections.asStateFlow()

    fun observeDraft(
        stopCount: Int,
        returnToStart: Boolean,
    ): Boolean {
        require(stopCount >= 0)

        val admitted =
            when (state) {
                VexMapsDistributionRunState.Empty,
                is VexMapsDistributionRunState.Draft,
                is VexMapsDistributionRunState.Ready ->
                    if (stopCount == 0) {
                        VexMapsDistributionRunState.Empty
                    } else {
                        VexMapsDistributionRunState.Draft(
                            stopCount = stopCount,
                            returnToStart = returnToStart,
                        )
                    }

                is VexMapsDistributionRunState.Running,
                is VexMapsDistributionRunState.AtStop,
                is VexMapsDistributionRunState.Returning,
                is VexMapsDistributionRunState.Completed ->
                    return false
            }

        observe(admitted)
        return true
    }

    override fun activate(
        control: VexMapsDistributionRunControl,
    ) {
        val act =
            controls.resolve(
                current = state,
                control = control,
            )?.act
                ?: return
        val resolution =
            behavior.resolve(
                current = state,
                act = act,
            ) ?: return

        val admitted =
            when (resolution.effect) {
                VexMapsDistributionRunEffect.SealDraft -> {
                    val current =
                        state as VexMapsDistributionRunState.Draft
                    VexMapsDistributionRunState.Ready(
                        stopCount = current.stopCount,
                        returnToStart = current.returnToStart,
                    )
                }

                VexMapsDistributionRunEffect.BeginRun -> {
                    val current =
                        state as VexMapsDistributionRunState.Ready
                    VexMapsDistributionRunState.Running(
                        stopCount = current.stopCount,
                        currentStopIndex = 0,
                        returnToStart = current.returnToStart,
                    )
                }

                VexMapsDistributionRunEffect.MarkArrived -> {
                    val current =
                        state as VexMapsDistributionRunState.Running
                    VexMapsDistributionRunState.AtStop(
                        stopCount = current.stopCount,
                        currentStopIndex = current.currentStopIndex,
                        returnToStart = current.returnToStart,
                    )
                }

                VexMapsDistributionRunEffect.AdvanceToNextStop -> {
                    val current =
                        state as VexMapsDistributionRunState.AtStop
                    VexMapsDistributionRunState.Running(
                        stopCount = current.stopCount,
                        currentStopIndex = current.currentStopIndex + 1,
                        returnToStart = current.returnToStart,
                    )
                }

                VexMapsDistributionRunEffect.BeginReturn -> {
                    val current =
                        state as VexMapsDistributionRunState.AtStop
                    VexMapsDistributionRunState.Returning(
                        stopCount = current.stopCount,
                    )
                }

                VexMapsDistributionRunEffect.CompleteRun -> {
                    val stopCount =
                        when (val current = state) {
                            is VexMapsDistributionRunState.AtStop ->
                                current.stopCount
                            is VexMapsDistributionRunState.Returning ->
                                current.stopCount
                            else ->
                                error(
                                    "CompleteRun is not lawful from $current",
                                )
                        }

                    VexMapsDistributionRunState.Completed(
                        stopCount = stopCount,
                    )
                }

                VexMapsDistributionRunEffect.ClearRun ->
                    VexMapsDistributionRunState.Empty
            }

        check(
            resolution.admittedStateKind
                .isInstance(admitted),
        )
        observe(admitted)
    }

    private fun observe(
        admitted: VexMapsDistributionRunState,
    ) {
        state = admitted
        mutableProjections.value =
            controls.projection(
                admitted,
            )
    }
}

// [VXG RealForever]
