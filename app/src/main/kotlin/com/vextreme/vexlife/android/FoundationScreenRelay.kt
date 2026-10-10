package com.vextreme.vexlife.android

import com.vextreme.vex.android.architecture.relay.VexRelay
import com.vextreme.vex.android.architecture.relay.VexState

data class FoundationScreenVexState(
    val screenPresence: ScreenPresence = ScreenPresence.EXITED,
    val revision: Long = 0L,
) : VexState<FoundationScreenVexState> {
    enum class ScreenPresence {
        ENTERED,
        EXITED,
    }

    override fun deepCopy(): FoundationScreenVexState =
        copy()
}

/**
 * Smallest lived Relay: one state type, default replacement processing, named partial updates.
 */
class FoundationScreenRelay :
    VexRelay<FoundationScreenVexState>(
        initialState = FoundationScreenVexState(),
    ) {
    fun screenEntered() {
        update { current ->
            if (current.screenPresence == FoundationScreenVexState.ScreenPresence.ENTERED) {
                current
            } else {
                current.copy(
                    screenPresence = FoundationScreenVexState.ScreenPresence.ENTERED,
                    revision = current.revision + 1L,
                )
            }
        }
    }

    fun screenExited() {
        update { current ->
            if (current.screenPresence == FoundationScreenVexState.ScreenPresence.EXITED) {
                current
            } else {
                current.copy(
                    screenPresence = FoundationScreenVexState.ScreenPresence.EXITED,
                    revision = current.revision + 1L,
                )
            }
        }
    }
}

// [VXG RealForever]
