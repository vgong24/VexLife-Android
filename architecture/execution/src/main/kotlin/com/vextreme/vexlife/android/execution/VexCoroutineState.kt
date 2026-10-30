package com.vextreme.vexlife.android.execution

internal fun VexExecution.Status.isTerminal():
    Boolean =
    this ==
        VexExecution.Status.COMPLETED ||
        this ==
        VexExecution.Status.CANCELLED ||
        this ==
        VexExecution.Status.FAILED

// [VXG RealForever]
