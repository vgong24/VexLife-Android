package com.vextreme.vexlife.android.relay

/**
 * Retained totality of one Compound Relay.
 *
 * [inputState] is the latest coherent input family retained from its dependencies.
 * [outputState] is the current derived meaning exposed to consumers.
 * [revision] advances only when either retained input meaning or derived output meaning changes.
 *
 * This revision is current-state lineage, not a substitute for a durable event ledger.
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
