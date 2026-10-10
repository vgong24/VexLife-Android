package com.vextreme.vexlife.android

import com.vextreme.vex.android.architecture.observability.VexLogRef
import com.vextreme.vex.android.architecture.observability.VexLogSink
import com.vextreme.vex.android.architecture.relay.VexRelay
import com.vextreme.vex.android.architecture.relay.VexRelayRef
import com.vextreme.vex.android.architecture.relay.VexState

data class FoundationScreenVexState(
    val screenPresence: ScreenPresence = ScreenPresence.EXITED,
    val activeScreenCount: Int = 0,
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
    relayRef = VexRelayRef("relay.foundation.screen"),
    initialState = FoundationScreenVexState(),
    logSink = logSink,
) {
    fun screenEntered() {
        update(
            originRef = VexLogRef("android.activity.onStart"),
        ) { current ->
            val nextCount =
                current.activeScreenCount + 1

            current.copy(
                screenPresence = FoundationScreenVexState.ScreenPresence.ENTERED,
                activeScreenCount = nextCount,
                revision = current.revision + 1L,
            )
        }
    }

    fun screenExited() {
        update(
            originRef = VexLogRef("android.activity.onStop"),
        ) { current ->
            if (current.activeScreenCount == 0) {
                current
            } else {
                val nextCount =
                    current.activeScreenCount - 1

                current.copy(
                    screenPresence =
                        if (nextCount > 0) {
                            FoundationScreenVexState.ScreenPresence.ENTERED
                        } else {
                            FoundationScreenVexState.ScreenPresence.EXITED
                        },
                    activeScreenCount = nextCount,
                    revision = current.revision + 1L,
                )
            }
        }
    }
}

// [VXG RealForever]
