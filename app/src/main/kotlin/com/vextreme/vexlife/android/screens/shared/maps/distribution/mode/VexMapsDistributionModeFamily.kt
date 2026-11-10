package com.vextreme.vexlife.android.screens.shared.maps.distribution.mode

import com.vextreme.vexlife.android.behavior.VexFamily

/**
 * Direct semantic owner for the VexMaps Distribution operating-mode question.
 *
 * Question:
 *     Is Maps operating as ordinary Maps or as a Distribution workspace?
 *
 * There is no provider or runtime Effect in this relationship. Accepting a lawful Control changes
 * semantic State directly. Delivery Route, Session, Relay, UI and execution remain independent
 * owners of their own questions.
 */
internal class VexMapsDistributionModeFamily(
    initialState:
        State =
        State.Regular,
) :
    VexFamily<
        VexMapsDistributionModeFamily,
        VexMapsDistributionModeFamily.State,
        VexMapsDistributionModeFamily.Control,
        VexMapsDistributionModeFamily.Effect
    >(
        initialState =
            initialState,
    ) {
    sealed interface State :
        VexFamily.State<
            VexMapsDistributionModeFamily
        > {
        data object Regular :
            State

        data object Distribution :
            State
    }

    enum class Control :
        VexFamily.Control<
            VexMapsDistributionModeFamily
        > {
        ToggleDistribution,
        UseDistribution,
    }

    /**
     * No runtime Effect is currently earned by Distribution Mode.
     *
     * The type remains only because VexFamily closes one exact State / Control / Effect vocabulary.
     * A future provider/runtime consequence must earn a concrete Effect instead of inventing one for
     * symmetry.
     */
    sealed interface Effect :
        VexFamily.Effect<
            VexMapsDistributionModeFamily
        >

    override fun act(
        currentState:
            State,
        control:
            Control,
    ):
        VexFamily.Act<
            VexMapsDistributionModeFamily,
            State,
            Effect
        >? =
        when (
            currentState
        ) {
            State.Regular ->
                when (
                    control
                ) {
                    Control.ToggleDistribution,
                    Control.UseDistribution,
                    ->
                        admit(
                            State.Distribution,
                        )
                }

            State.Distribution ->
                when (
                    control
                ) {
                    Control.ToggleDistribution ->
                        admit(
                            State.Regular,
                        )

                    Control.UseDistribution ->
                        null
                }
        }
}

// [VXG RealForever][VEXMAPS][DISTRIBUTION][MODE][DIRECT-FAMILY]
