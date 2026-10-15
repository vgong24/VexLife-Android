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

    private var nextPreparationGeneration =
        0L
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
        revision: Long,
        stopRefs: List<String>,
        returnToStart: Boolean,
    ): Boolean {
        require(revision >= 0L)
        require(stopRefs.all { it.isNotBlank() })

        val admitted =
            when (state) {
                VexMapsDistributionRunState.Empty,
                is VexMapsDistributionRunState.Draft ->
                    if (stopRefs.isEmpty()) {
                        VexMapsDistributionRunState.Empty
                    } else {
                        VexMapsDistributionRunState.Draft(
                            source =
                                VexMapsDistributionRunDraftIdentity(
                                    revision =
                                        revision,
                                    stopRefs =
                                        stopRefs.toList(),
                                    returnToStart =
                                        returnToStart,
                                ),
                        )
                    }

                is VexMapsDistributionRunState.Preparing,
                is VexMapsDistributionRunState.Ready,
                is VexMapsDistributionRunState.Running,
                is VexMapsDistributionRunState.AtStop,
                is VexMapsDistributionRunState.Returning,
                is VexMapsDistributionRunState.Completed ->
                    return false
            }

        observe(
            admitted,
        )
        return true
    }

    fun preparationSucceeded(
        preparationRef: String,
        planRef: String,
        orderedStopRefs: List<String>,
    ): Boolean {
        val current =
            state as?
                VexMapsDistributionRunState.Preparing
                ?: return false

        if (
            current.preparationRef !=
            preparationRef
        ) {
            return false
        }

        observe(
            VexMapsDistributionRunState.Ready(
                planRef =
                    planRef,
                source =
                    current.source,
                orderedStopRefs =
                    orderedStopRefs.toList(),
            ),
        )
        return true
    }

    fun preparationFailed(
        preparationRef: String,
    ): Boolean {
        val current =
            state as?
                VexMapsDistributionRunState.Preparing
                ?: return false

        if (
            current.preparationRef !=
            preparationRef
        ) {
            return false
        }

        observe(
            VexMapsDistributionRunState.Draft(
                source =
                    current.source,
            ),
        )
        return true
    }

    fun invalidateReadyPlan(
        planRef: String,
    ): Boolean {
        val current =
            state as?
                VexMapsDistributionRunState.Ready
                ?: return false

        if (
            current.planRef !=
            planRef
        ) {
            return false
        }

        observe(
            VexMapsDistributionRunState.Draft(
                source =
                    current.source,
            ),
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

                    nextPreparationGeneration +=
                        1L

                    VexMapsDistributionRunState.Preparing(
                        preparationRef =
                            buildString {
                                append(
                                    "vexmaps.distribution.preparation.",
                                )
                                append(
                                    current.source.revision,
                                )
                                append('.')
                                append(
                                    nextPreparationGeneration,
                                )
                            },
                        source =
                            current.source,
                        orderedStopRefs =
                            current.orderedStopRefs,
                    )
                }

                VexMapsDistributionRunEffect.BeginRun -> {
                    val current =
                        state as
                            VexMapsDistributionRunState.Ready

                    VexMapsDistributionRunState.Running(
                        planRef =
                            current.planRef,
                        source =
                            current.source,
                        orderedStopRefs =
                            current.orderedStopRefs,
                        currentStopRef =
                            current.orderedStopRefs
                                .first(),
                    )
                }

                VexMapsDistributionRunEffect.MarkArrived -> {
                    val current =
                        state as
                            VexMapsDistributionRunState.Running

                    VexMapsDistributionRunState.AtStop(
                        planRef =
                            current.planRef,
                        source =
                            current.source,
                        orderedStopRefs =
                            current.orderedStopRefs,
                        currentStopRef =
                            current.currentStopRef,
                    )
                }

                VexMapsDistributionRunEffect.AdvanceToNextStop -> {
                    val current =
                        state as
                            VexMapsDistributionRunState.AtStop

                    VexMapsDistributionRunState.Running(
                        planRef =
                            current.planRef,
                        source =
                            current.source,
                        orderedStopRefs =
                            current.orderedStopRefs,
                        currentStopRef =
                            requireNotNull(
                                current.nextStopRef,
                            ),
                    )
                }

                VexMapsDistributionRunEffect.BeginReturn -> {
                    val current =
                        state as
                            VexMapsDistributionRunState.AtStop

                    VexMapsDistributionRunState.Returning(
                        planRef =
                            current.planRef,
                        source =
                            current.source,
                    )
                }

                VexMapsDistributionRunEffect.CompleteRun -> {
                    val current =
                        state

                    when (current) {
                        is VexMapsDistributionRunState.AtStop ->
                            VexMapsDistributionRunState.Completed(
                                planRef =
                                    current.planRef,
                                source =
                                    current.source,
                                orderedStopRefs =
                                    current.orderedStopRefs,
                            )

                        is VexMapsDistributionRunState.Returning ->
                            VexMapsDistributionRunState.Completed(
                                planRef =
                                    current.planRef,
                                source =
                                    current.source,
                                orderedStopRefs =
                                    current.orderedStopRefs,
                            )

                        else ->
                            error(
                                "CompleteRun is not lawful from $current",
                            )
                    }
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
