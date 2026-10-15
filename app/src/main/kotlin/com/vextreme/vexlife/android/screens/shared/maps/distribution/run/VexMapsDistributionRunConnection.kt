package com.vextreme.vexlife.android.screens.shared.maps.distribution.run

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Reality boundary for the finite Distribution Run family.
 *
 * Dynamic stop payload editing remains outside VexPort. [observeDraftStopCount] is the explicit seam
 * through which a future durable draft owner can publish payload-derived truth without pretending
 * AddStop(address) is a payload-free Control.
 */
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

    /**
     * Publish the current durable draft cardinality.
     *
     * Returns false while an executing/completed run owns its admitted stop set.
     */
    fun observeDraftStopCount(
        stopCount: Int,
    ): Boolean {
        require(stopCount >= 0)

        val admitted =
            when (
                state
            ) {
                VexMapsDistributionRunState.Empty,
                is VexMapsDistributionRunState.Draft,
                is VexMapsDistributionRunState.Ready ->
                    if (stopCount == 0) {
                        VexMapsDistributionRunState.Empty
                    } else {
                        VexMapsDistributionRunState.Draft(
                            stopCount = stopCount,
                        )
                    }

                is VexMapsDistributionRunState.Running,
                is VexMapsDistributionRunState.AtStop,
                is VexMapsDistributionRunState.Completed ->
                    return false
            }

        observe(
            admitted,
        )
        return true
    }

    override fun activate(
        control:
            VexMapsDistributionRunControl,
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
            )
                ?: return

        val admitted =
            when (
                resolution.effect
            ) {
                VexMapsDistributionRunEffect.SealDraft -> {
                    val current =
                        state as
                            VexMapsDistributionRunState.Draft

                    VexMapsDistributionRunState.Ready(
                        stopCount =
                            current.stopCount,
                    )
                }

                VexMapsDistributionRunEffect.BeginRun -> {
                    val current =
                        state as
                            VexMapsDistributionRunState.Ready

                    VexMapsDistributionRunState.Running(
                        stopCount =
                            current.stopCount,
                        currentStopIndex = 0,
                    )
                }

                VexMapsDistributionRunEffect.MarkArrived -> {
                    val current =
                        state as
                            VexMapsDistributionRunState.Running

                    VexMapsDistributionRunState.AtStop(
                        stopCount =
                            current.stopCount,
                        currentStopIndex =
                            current.currentStopIndex,
                    )
                }

                VexMapsDistributionRunEffect.AdvanceToNextStop -> {
                    val current =
                        state as
                            VexMapsDistributionRunState.AtStop

                    VexMapsDistributionRunState.Running(
                        stopCount =
                            current.stopCount,
                        currentStopIndex =
                            current.currentStopIndex + 1,
                    )
                }

                VexMapsDistributionRunEffect.CompleteRun -> {
                    val current =
                        state as
                            VexMapsDistributionRunState.AtStop

                    VexMapsDistributionRunState.Completed(
                        stopCount =
                            current.stopCount,
                    )
                }

                VexMapsDistributionRunEffect.ClearRun ->
                    VexMapsDistributionRunState.Empty
            }

        check(
            resolution.admittedStateKind
                .isInstance(
                    admitted,
                ),
        )

        observe(
            admitted,
        )
    }

    private fun observe(
        admitted:
            VexMapsDistributionRunState,
    ) {
        state =
            admitted
        mutableProjections.value =
            controls.projection(
                admitted,
            )
    }
}

// [VXG RealForever]
