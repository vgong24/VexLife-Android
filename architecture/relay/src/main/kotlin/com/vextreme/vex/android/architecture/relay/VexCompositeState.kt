package com.vextreme.vex.android.architecture.relay

/**
 * The current relationship owned by one [VexRelay].
 *
 * InputState and OutputState belong to the component using the Relay. They should use names that
 * explain that component's relationship, for example ScreenEvent -> ScreenPresence or
 * TooltipInputState -> TooltipOutputState.
 */
data class VexCompositeState<InputState, OutputState>(
    val input: InputState,
    val output: OutputState,
    val revision: Long = 0L,
) {
    init {
        require(revision >= 0L) { "revision must be non-negative" }
    }
}

// [VXG RealForever]
