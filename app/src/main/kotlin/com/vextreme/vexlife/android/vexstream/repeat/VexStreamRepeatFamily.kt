package com.vextreme.vexlife.android.vexstream.repeat

import com.vextreme.vexlife.android.behavior.VexFamily

/**
 * Semantic owner for the VexStream Repeat question.
 *
 * Question:
 *     Is Repeat active for the current playback context, and can the listener toggle it?
 *
 * Media3 remains the provider truth owner. This Family owns only the product meaning projected from
 * that truth and the requested runtime transformation produced by a lawful Toggle.
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
    /**
     * Provider-derived Repeat truth used by the current one-toggle product experience.
     *
     * Media3 REPEAT_MODE_ONE and REPEAT_MODE_ALL both mean Enabled because the current product asks
     * only whether Repeat is active. Provider command availability remains visible as semantic
     * availability evidence for Toggle.
     */
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

    /**
     * Requested provider transformation, not resulting Repeat truth.
     *
     * The Media3 Connection realizes this synchronously. Family State remains provider-derived and
     * changes only when Media3 observation is reconciled back through [observe].
     */
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
