package com.vextreme.vexlife.android

import com.vextreme.vex.android.architecture.observability.VexLogRef
import com.vextreme.vex.android.architecture.observability.VexLogSink
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

class FoundationScreenRelay(
    logSink: VexLogSink = VexLogSink.None,
) : VexRelay<FoundationScreenVexState>(
    relayRef = VexLogRef("relay.foundation.screen"),
    initialState = FoundationScreenVexState(),
    logSink = logSink,
) {
    fun screenEntered() {
        update(
            originRef = VexLogRef("android.activity.onStart"),
        ) { current ->
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
        update(
            originRef = VexLogRef("android.activity.onStop"),
        ) { current ->
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
