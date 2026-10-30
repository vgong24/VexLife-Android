package com.vextreme.vexlife.android.relay

import com.vextreme.vexlife.android.humanvetting.VettedWithHuman

/**
 * Retained totality of one Compound Relay.
 *
 * [inputState] is the latest coherent input family retained from its dependencies.
 * [outputState] is the current derived meaning exposed to consumers.
 */
@VettedWithHuman("VexCompoundState", "2026-10-29T22:46:00-07:00", "Victor Approved")
data class VexCompoundState<
    InputState : VexInputState<InputState>,
    OutputState : VexOutputState<OutputState>,
>(
    @VettedWithHuman("VexCompoundState.inputState", "2026-10-29T22:46:00-07:00", "Victor Approved")
    val inputState: InputState,
    @VettedWithHuman("VexCompoundState.outputState", "2026-10-29T22:46:00-07:00", "Victor Approved")
    val outputState: OutputState,
) : VexState<VexCompoundState<InputState, OutputState>> {
    override fun deepCopy(): VexCompoundState<InputState, OutputState> =
        copy(
            inputState = inputState.deepCopy(),
            outputState = outputState.deepCopy(),
        )
}

// [VXG RealForever]
