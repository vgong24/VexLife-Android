package com.vextreme.vexlife.android.screens.shared.maps.distribution.deliveryroute

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class VexMapsDeliveryRouteConnection(
    initialState:
        VexMapsDeliveryRouteState =
        VexMapsDeliveryRouteState.Empty,
) : VexMapsDeliveryRouteConnectionFlow {
    private val behavior =
        vexMapsDeliveryRouteBehavior()
    private val controls =
        behavior.vexMapsDeliveryRouteControls()

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
        StateFlow<VexMapsDeliveryRouteProjection> =
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
                VexMapsDeliveryRouteState.Empty,
                is VexMapsDeliveryRouteState.Draft ->
                    if (stopRefs.isEmpty()) {
                        VexMapsDeliveryRouteState.Empty
                    } else {
                        VexMapsDeliveryRouteState.Draft(
                            source =
                                VexMapsDeliveryRouteDraftIdentity(
                                    revision =
                                        revision,
                                    stopRefs =
                                        stopRefs.toList(),
                                    returnToStart =
                                        returnToStart,
                                ),
                        )
                    }

                is VexMapsDeliveryRouteState.Preparing,
                is VexMapsDeliveryRouteState.Ready,
                is VexMapsDeliveryRouteState.Delivering,
                is VexMapsDeliveryRouteState.AtDelivery,
                is VexMapsDeliveryRouteState.ReturningToStart,
                is VexMapsDeliveryRouteState.Completed ->
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
                VexMapsDeliveryRouteState.Preparing
                ?: return false

        if (
            current.preparationRef !=
            preparationRef
        ) {
            return false
        }

        observe(
            VexMapsDeliveryRouteState.Ready(
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
                VexMapsDeliveryRouteState.Preparing
                ?: return false

        if (
            current.preparationRef !=
            preparationRef
        ) {
            return false
        }

        observe(
            VexMapsDeliveryRouteState.Draft(
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
                VexMapsDeliveryRouteState.Ready
                ?: return false

        if (
            current.planRef !=
            planRef
        ) {
            return false
        }

        observe(
            VexMapsDeliveryRouteState.Draft(
                source =
                    current.source,
            ),
        )
        return true
    }

    override fun activate(
        control:
            VexMapsDeliveryRouteControl,
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
                VexMapsDeliveryRouteEffect.BeginPreparation -> {
                    val current =
                        state as
                            VexMapsDeliveryRouteState.Draft

                    nextPreparationGeneration +=
                        1L

                    VexMapsDeliveryRouteState.Preparing(
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
                    )
                }

                VexMapsDeliveryRouteEffect.BeginDeliveryRoute -> {
                    val current =
                        state as
                            VexMapsDeliveryRouteState.Ready

                    VexMapsDeliveryRouteState.Delivering(
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

                VexMapsDeliveryRouteEffect.RecordDeliveryArrival -> {
                    val current =
                        state as
                            VexMapsDeliveryRouteState.Delivering

                    VexMapsDeliveryRouteState.AtDelivery(
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

                VexMapsDeliveryRouteEffect.AdvanceToNextDelivery -> {
                    val current =
                        state as
                            VexMapsDeliveryRouteState.AtDelivery

                    VexMapsDeliveryRouteState.Delivering(
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

                VexMapsDeliveryRouteEffect.BeginReturnToStart -> {
                    val current =
                        state as
                            VexMapsDeliveryRouteState.AtDelivery

                    VexMapsDeliveryRouteState.ReturningToStart(
                        planRef =
                            current.planRef,
                        source =
                            current.source,
                        orderedStopRefs =
                            current.orderedStopRefs,
                    )
                }

                VexMapsDeliveryRouteEffect.CompleteDeliveryRoute -> {
                    val current =
                        state

                    when (current) {
                        is VexMapsDeliveryRouteState.AtDelivery ->
                            VexMapsDeliveryRouteState.Completed(
                                planRef =
                                    current.planRef,
                                source =
                                    current.source,
                                orderedStopRefs =
                                    current.orderedStopRefs,
                            )

                        is VexMapsDeliveryRouteState.ReturningToStart ->
                            VexMapsDeliveryRouteState.Completed(
                                planRef =
                                    current.planRef,
                                source =
                                    current.source,
                                orderedStopRefs =
                                    current.orderedStopRefs,
                            )

                        else ->
                            error(
                                "CompleteDeliveryRoute is not lawful from $current",
                            )
                    }
                }

                VexMapsDeliveryRouteEffect.ClearDeliveryRoute ->
                    VexMapsDeliveryRouteState.Empty
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
            VexMapsDeliveryRouteState,
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
