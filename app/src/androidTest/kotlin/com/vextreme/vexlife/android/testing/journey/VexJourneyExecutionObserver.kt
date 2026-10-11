package com.vextreme.vexlife.android.testing.journey

data class VexJourneyStepObservation(
    val stepIndex: Int,
    val stepKind: String,
    val screenId: String,
    val semanticRef: String?,
)

interface VexJourneyExecutionObserver {
    fun afterStep(observation: VexJourneyStepObservation)

    fun afterJourney(
        caseLabel: String,
        finalScreenId: String,
    )

    companion object {
        val None: VexJourneyExecutionObserver =
            object : VexJourneyExecutionObserver {
                override fun afterStep(
                    observation: VexJourneyStepObservation,
                ) = Unit

                override fun afterJourney(
                    caseLabel: String,
                    finalScreenId: String,
                ) = Unit
            }
    }
}
