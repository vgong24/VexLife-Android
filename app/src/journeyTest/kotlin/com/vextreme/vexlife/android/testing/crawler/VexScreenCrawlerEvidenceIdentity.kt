package com.vextreme.vexlife.android.testing.crawler

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
    screenId: String,
    stepIndex: Int,
    stepKind: String,
    semanticRef: String?,
): VexCrawlerEvidenceIdentity {
    val scenarioRef =
        this.scenarioRef

    val stepRef =
        if (stepIndex == Int.MAX_VALUE) {
            "final"
        } else {
            "step-" + stepIndex.toString().padStart(3, '0')
        }

    val semanticPart =
        semanticRef ?: "no-semantic-ref"

    return VexCrawlerEvidenceIdentity(
        scenarioRef = scenarioRef,
        evidenceRef =
            listOf(
                scenarioRef,
                screenId,
                stepRef,
                stepKind,
                semanticPart,
            ).joinToString("::"),
        screenId = screenId,
        stepIndex = stepIndex,
        stepKind = stepKind,
        semanticRef = semanticRef,
    )
}
