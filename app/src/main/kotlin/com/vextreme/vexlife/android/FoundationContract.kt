package com.vextreme.vexlife.android

import com.vextreme.vex.android.state.VexObservation
import com.vextreme.vex.android.state.VexStateProjection
import com.vextreme.vex.android.state.VexStateSnapshot

object FoundationContract {
    const val REF = "foundation.vexlife.android.standalone-build/v1"
    const val STATUS = "FOUNDATION_INCOMPLETE"
    const val STATUS_STATE_REF = "state.vexlife.android.foundation.status"

    fun statusProjection(): VexStateProjection<String> =
        VexStateProjection(
            initial = VexStateSnapshot(
                stateRef = STATUS_STATE_REF,
                instanceRef = "instance.vexlife.android.foundation.001",
                revision = 0L,
                transitionRefOrNull = null,
                observation = VexObservation.PRESENT,
                semanticHash = STATUS,
                valueOrNull = STATUS,
            ),
            copyValue = { value -> value },
        )
}
