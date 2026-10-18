package com.vextreme.vexlife.android.screens.shared.maps.distribution.mode

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Reality boundary for the Distribution operating-mode family.
 *
 * No provider API is required yet. The Connection still owns realization so Behavior remains
 * provider/UI/lifetime independent.
 */
internal class VexMapsDistributionModeConnection(
    initialState:
        VexMapsDistributionModeState =
        VexMapsDistributionModeState.Regular,
) : VexMapsDistributionModeConnectionFlow {
    private val behavior =
        vexMapsDistributionModeBehavior()

    private val controls =
        behavior.vexMapsDistributionModeControls()

    private var state =
        initialState

    private val mutableProjections =
        MutableStateFlow(
            controls.projection(
                state,
            ),
        )

    override val projections:
        StateFlow<VexMapsDistributionModeProjection> =
        mutableProjections.asStateFlow()

    override fun activate(
        control:
            VexMapsDistributionModeControl,
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
                VexMapsDistributionModeEffect.UseDistributionMode ->
                    VexMapsDistributionModeState.Distribution

                VexMapsDistributionModeEffect.UseRegularMode ->
                    VexMapsDistributionModeState.Regular
            }

        check(
            resolution.admittedStateKind
                .isInstance(
                    admitted,
                ),
        )

        state =
            admitted
        mutableProjections.value =
            controls.projection(
                admitted,
            )
    }
}

// [VXG RealForever]
