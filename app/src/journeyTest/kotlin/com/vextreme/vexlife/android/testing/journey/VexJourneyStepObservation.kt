package com.vextreme.vexlife.android.testing.journey

/**
 * One observed coordinate from execution of the authored Journey walk.
 *
 * This value is shared test grammar: Android execution produces it and evidence consumers may
 * project from it. It does not own Journey meaning, runtime behavior, or evidence acceptance.
 */
data class VexJourneyStepObservation(
    val stepIndex: Int,
    val stepKind: String,
    val screenId: String,
    val semanticRef: String?,
)

// [VXG RealForever]
