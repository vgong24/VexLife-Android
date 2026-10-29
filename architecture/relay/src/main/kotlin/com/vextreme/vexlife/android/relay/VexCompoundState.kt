package com.vextreme.vexlife.android.relay

/**
 * Retained totality of one Compound Relay.
 *
 * [inputState] is the latest coherent input family retained from its dependencies.
 * [outputState] is the current derived meaning exposed to consumers.
 */
data class VexCompoundState<
    InputState : VexInputState<InputState>,
    OutputState : VexOutputState<OutputState>,
>(
    val inputState: InputState,
    val outputState: OutputState,
) : VexState<VexCompoundState<InputState, OutputState>> {
    override fun deepCopy(): VexCompoundState<InputState, OutputState> =
        copy(
            inputState = inputState.deepCopy(),
            outputState = outputState.deepCopy(),
        )
}

// [VXG RealForever]
