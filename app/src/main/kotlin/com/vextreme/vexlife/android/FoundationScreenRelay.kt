package com.vextreme.vexlife.android

import com.vextreme.vex.android.architecture.relay.VexInputState
import com.vextreme.vex.android.architecture.relay.VexOutputState
import com.vextreme.vex.android.architecture.relay.VexRelay
import com.vextreme.vex.android.architecture.relay.VexStateSnapshotter

/**
 * Smallest lived Relay family: the screen owns one input family and one output family.
 *
 * The public API speaks in screen meaning. Callers never replace the full InputState.
 */
class FoundationScreenRelay :
    VexRelay<FoundationScreenRelay.InputState, FoundationScreenRelay.OutputState>(
        initialInputState = InputState(),
        inputSnapshotter = VexStateSnapshotter { state -> state.copy() },
        outputSnapshotter = VexStateSnapshotter { state -> state.copy() },
    ) {
    data class InputState(
        val screenPresence: ScreenPresence = ScreenPresence.EXITED,
    ) : VexInputState

    data class OutputState(
        val screenPresence: ScreenPresence,
    ) : VexOutputState

    enum class ScreenPresence {
        ENTERED,
        EXITED,
    }

    override fun processLatestState(
        inputState: InputState,
    ): OutputState =
        OutputState(
            screenPresence = inputState.screenPresence,
        )

    fun screenEntered() {
        send { inputState ->
            inputState.copy(
                screenPresence = ScreenPresence.ENTERED,
            )
        }
    }

    fun screenExited() {
        send { inputState ->
            inputState.copy(
                screenPresence = ScreenPresence.EXITED,
            )
        }
    }
}

// [VXG RealForever]
