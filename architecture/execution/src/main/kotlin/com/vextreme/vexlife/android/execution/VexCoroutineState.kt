package com.vextreme.vexlife.android.execution

@Deprecated(
    "Use VexExecution.SourceRef",
)
typealias VexCoroutineRef =
    VexExecution.SourceRef

@Deprecated(
    "Use VexExecution.Status",
)
typealias VexCoroutineStatus =
    VexExecution.Status

@Deprecated(
    "Use VexExecution.State",
)
typealias VexCoroutineState =
    VexExecution.State

@Deprecated(
    "Use VexExecution.Handle",
)
typealias VexCoroutineHandle =
    VexExecution.Handle

internal fun VexExecution.Status.isTerminal():
    Boolean =
    this ==
        VexExecution.Status.COMPLETED ||
        this ==
        VexExecution.Status.CANCELLED ||
        this ==
        VexExecution.Status.FAILED

// [VXG RealForever]
