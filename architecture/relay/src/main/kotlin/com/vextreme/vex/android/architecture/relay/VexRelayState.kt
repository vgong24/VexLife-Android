package com.vextreme.vex.android.architecture.relay

/** Marker for the aggregate input family owned by one VexRelay component. */
interface VexInputState

/** Marker for the aggregate output family owned by one VexRelay component. */
interface VexOutputState

/**
 * Explicit snapshot contract.
 *
 * A Relay must never retain or expose caller-owned mutable objects by reference. The component
 * supplies the copy rule because only the component knows which nested values require copying.
 */
fun interface VexStateSnapshotter<State> {
    fun snapshot(state: State): State
}

/**
 * One current Relay relationship.
 *
 * inputRevision advances when accepted input meaning changes.
 * outputRevision advances only when the reconciled output meaning changes.
 */
data class VexRelayState<InputState : VexInputState, OutputState : VexOutputState>(
    val inputState: InputState,
    val outputState: OutputState,
    val inputRevision: Long,
    val outputRevision: Long,
) {
    init {
        require(inputRevision >= 0L) { "inputRevision must be non-negative" }
        require(outputRevision >= 0L) { "outputRevision must be non-negative" }
    }
}

// [VXG RealForever]
