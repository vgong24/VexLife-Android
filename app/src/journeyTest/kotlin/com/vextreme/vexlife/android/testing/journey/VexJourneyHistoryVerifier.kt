package com.vextreme.vexlife.android.testing.journey

import com.vextreme.vexlife.android.logging.VexLogRecord

enum class VexJourneyHistoryMismatchKind {
    DIFFERENT_RECORD,
    MISSING_ACTUAL_RECORD,
    UNEXPECTED_ACTUAL_RECORD,
}

data class VexJourneyHistoryMismatch(
    val index: Int,
    val kind: VexJourneyHistoryMismatchKind,
    val expected: VexExpectedRecord?,
    val actual: VexExpectedRecord?,
)

data class VexJourneyHistoryComparison(
    val expected: List<VexExpectedRecord>,
    val actual: List<VexExpectedRecord>,
    val mismatch: VexJourneyHistoryMismatch?,
) {
    val matches: Boolean
        get() = mismatch == null

    fun render(caseLabel: String): String =
        buildString {
            appendLine("Vex journey semantic history mismatch")
            appendLine("case: ${caseLabel}")
            appendLine("expected records: ${expected.size}")
            appendLine("actual records: ${actual.size}")

            val problem = mismatch
            if (problem == null) {
                appendLine("result: MATCH")
                return@buildString
            }

            appendLine("result: ${problem.kind}")
            appendLine("first divergent sequence index: ${problem.index}")
            appendLine("expected: ${problem.expected ?: "<none>"}")
            appendLine("actual:   ${problem.actual ?: "<none>"}")
            appendLine()
            appendLine("Sequence neighborhood:")

            val from = (problem.index - 2).coerceAtLeast(0)
            val to = (problem.index + 2)
                .coerceAtMost(maxOf(expected.lastIndex, actual.lastIndex))

            if (to < from) {
                appendLine("  <empty>")
            } else {
                for (index in from..to) {
                    appendLine(
                        "  [${index}] expected=${expected.getOrNull(index) ?: "<none>"}",
                    )
                    appendLine(
                        "      actual=${actual.getOrNull(index) ?: "<none>"}",
                    )
                }
            }
        }
}

object VexJourneyHistoryVerifier {
    fun compare(
        expected: List<VexExpectedRecord>,
        actual: List<VexExpectedRecord>,
    ): VexJourneyHistoryComparison {
        val count = maxOf(expected.size, actual.size)

        for (index in 0 until count) {
            val wanted = expected.getOrNull(index)
            val observed = actual.getOrNull(index)

            if (wanted == observed) continue

            val kind = when {
                wanted == null ->
                    VexJourneyHistoryMismatchKind.UNEXPECTED_ACTUAL_RECORD

                observed == null ->
                    VexJourneyHistoryMismatchKind.MISSING_ACTUAL_RECORD

                else ->
                    VexJourneyHistoryMismatchKind.DIFFERENT_RECORD
            }

            return VexJourneyHistoryComparison(
                expected = expected,
                actual = actual,
                mismatch = VexJourneyHistoryMismatch(
                    index = index,
                    kind = kind,
                    expected = wanted,
                    actual = observed,
                ),
            )
        }

        return VexJourneyHistoryComparison(
            expected = expected,
            actual = actual,
            mismatch = null,
        )
    }
}

/**
 * Canonical instrumentation projection.
 *
 * sequence/timestamp/duration are deliberately excluded. List position is the ordering contract.
 */
fun VexLogRecord.toJourneyRecord(): VexExpectedRecord =
    VexExpectedRecord(
        kind = kind,
        subjectRef = subjectRef,
        action = action,
        screenBeforeId = screenBeforeId,
        screenAfterId = screenAfterId,
        detailsJson = detailsJson,
    )
