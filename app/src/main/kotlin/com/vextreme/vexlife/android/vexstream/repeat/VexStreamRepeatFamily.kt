package com.vextreme.vexlife.android.vexstream.repeat

import com.vextreme.vexlife.android.behavior.VexFamily

/**
 * Semantic owner for: "Is Repeat active here, and can the listener toggle it?"
 * Media3 remains provider truth; this Family owns Repeat meaning only.
 */
internal class VexStreamRepeatFamily(
    initialState:
        State,
) :
    VexFamily<
        VexStreamRepeatFamily,
        VexStreamRepeatFamily.State,
        VexStreamRepeatFamily.Control,
        VexStreamRepeatFamily.Effect
    >(
        initialState =
            initialState,
    ) {
    /** Provider-derived truth. Media3 Repeat-One and Repeat-All both mean Enabled here. */
    sealed interface State :
        VexFamily.State<
            VexStreamRepeatFamily
        > {
        val toggleAvailable:
            Boolean

        data class Disabled(
            override val toggleAvailable:
                Boolean,
        ) :
            State

        data class Enabled(
            override val toggleAvailable:
                Boolean,
        ) :
            State
    }

    enum class Control :
        VexFamily.Control<
            VexStreamRepeatFamily
        > {
        Toggle,
    }

    /** Requested Media3 transformation; resulting State still comes back through observation. */
    sealed interface Effect :
        VexFamily.Effect<
            VexStreamRepeatFamily
        > {
        data object RequestEnableAll :
            Effect

        data object RequestDisable :
            Effect
    }

    override fun act(
        currentState:
            State,
        control:
            Control,
    ):
        VexFamily.Act<
            VexStreamRepeatFamily,
            State,
            Effect
        >? =
        when (
            control
        ) {
            Control.Toggle ->
                when (
                    currentState
                ) {
                    is State.Disabled ->
                        if (
                            currentState.toggleAvailable
                        ) {
                            stay(
                                begin(
                                    Effect.RequestEnableAll,
                                ),
                            )
                        } else {
                            null
                        }

                    is State.Enabled ->
                        if (
                            currentState.toggleAvailable
                        ) {
                            stay(
                                begin(
                                    Effect.RequestDisable,
                                ),
                            )
                        } else {
                            null
                        }
                }
        }
}

// [VXG RealForever][VEXSTREAM][REPEAT][DIRECT-FAMILY]
