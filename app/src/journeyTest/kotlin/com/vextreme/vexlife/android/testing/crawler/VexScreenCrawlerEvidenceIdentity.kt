package com.vextreme.vexlife.android.testing.crawler

import com.vextreme.vexlife.android.testing.journey.VexJourneyStepObservation

/**
 * Stable semantic identity for one screenshot condition.
 *
 * [scenarioRef] and [evidenceRef] deliberately exclude locale/font configuration so the same frame
 * can be matched across English/Chinese, normal/large text, and future evidence conditions.
 */
data class VexCrawlerEvidenceIdentity(
    val scenarioRef: String,
    val evidenceRef: String,
    val screenId: String,
    val stepIndex: Int,
    val stepKind: String,
    val semanticRef: String?,
)

fun VexScreenCrawlerCase.evidenceIdentity(
    observation: VexJourneyStepObservation,
): VexCrawlerEvidenceIdentity {
    val scenarioRef =
        this.scenarioRef

    val stepRef =
        if (observation.stepIndex == Int.MAX_VALUE) {
            "final"
        } else {
            "step-" + observation.stepIndex.toString().padStart(3, '0')
        }

    val semanticPart =
        observation.semanticRef ?: "no-semantic-ref"

    return VexCrawlerEvidenceIdentity(
        scenarioRef = scenarioRef,
        evidenceRef =
            listOf(
                scenarioRef,
                observation.screenId,
                stepRef,
                observation.stepKind,
                semanticPart,
            ).joinToString("::"),
        screenId = observation.screenId,
        stepIndex = observation.stepIndex,
        stepKind = observation.stepKind,
        semanticRef = observation.semanticRef,
    )
}
