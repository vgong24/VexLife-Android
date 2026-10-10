package com.vextreme.vex.android.architecture.relay

/**
 * The retained totality of one compound Relay.
 *
 * InputState contains the latest resolved input family.
 * OutputState contains the latest derived output family.
 *
 * Output fields do not need to duplicate input fields: consumers that need both can read both from
 * this compound state.
 *
 * [revision] is owned and advanced by VexCompoundRelay after a semantic input/output change.
 * processStates(...) may derive input/output only; it must return the current revision unchanged.
 */
data class VexCompoundState<
    InputState : VexInputState<InputState>,
    OutputState : VexOutputState<OutputState>,
>(
    val inputState: InputState,
    val outputState: OutputState,
    val revision: Long = 0L,
) : VexState<VexCompoundState<InputState, OutputState>> {
    init {
        require(revision >= 0L) { "revision must be non-negative" }
    }

    override fun deepCopy(): VexCompoundState<InputState, OutputState> =
        copy(
            inputState = inputState.deepCopy(),
            outputState = outputState.deepCopy(),
        )
}

// [VXG RealForever]
