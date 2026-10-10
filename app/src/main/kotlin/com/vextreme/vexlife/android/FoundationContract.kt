package com.vextreme.vexlife.android

import com.vextreme.vex.android.architecture.relay.VexRelay

enum class FoundationScreenEvent {
    ENTER,
    EXIT,
}

enum class FoundationScreenPresence {
    ENTERED,
    EXITED,
}

object FoundationContract {
    const val REF = "foundation.vexlife.android.standalone-build/v1"
    const val STATUS = "FOUNDATION_INCOMPLETE"

    fun screenPresenceRelay(): VexRelay<FoundationScreenEvent, FoundationScreenPresence> =
        VexRelay(
            initialInput = FoundationScreenEvent.EXIT,
        ) { event ->
            when (event) {
                FoundationScreenEvent.ENTER -> FoundationScreenPresence.ENTERED
                FoundationScreenEvent.EXIT -> FoundationScreenPresence.EXITED
            }
        }
}
