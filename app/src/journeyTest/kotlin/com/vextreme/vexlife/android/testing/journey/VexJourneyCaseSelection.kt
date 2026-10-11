package com.vextreme.vexlife.android.testing.journey

enum class VexJourneyMatrixMode {
    BASELINE_ONLY,
    ALL_DECLARED,
}

data class VexJourneyRunSelection(
    val matrixMode: VexJourneyMatrixMode =
        VexJourneyMatrixMode.BASELINE_ONLY,
    val exactCaseRef: String? = null,
    val shardId: String? = null,
    val environmentId: String? = null,
)

object VexJourneyCaseSelector {
    fun select(
        selection: VexJourneyRunSelection,
    ): List<VexJourneyCase> {
        val initial = when {
            selection.exactCaseRef != null ->
                listOf(
                    VexJourneyCatalog.requireCase(
                        selection.exactCaseRef,
                    ),
                )

            selection.environmentId != null ->
                VexJourneyCatalog.cases.filter { case ->
                    case.environment.id == selection.environmentId
                }

            selection.matrixMode ==
                VexJourneyMatrixMode.ALL_DECLARED ->
                VexJourneyCatalog.cases

            else ->
                VexJourneyCatalog.baselineCases
        }

        val selected = initial.filter { case ->
            selection.shardId == null ||
                case.shardId == selection.shardId
        }

        require(selected.isNotEmpty()) {
            buildString {
                append("journey selection produced zero cases")
                selection.exactCaseRef?.let {
                    append("; case=$it")
                }
                selection.shardId?.let {
                    append("; shard=$it")
                }
                selection.environmentId?.let {
                    append("; environment=$it")
                }
                append("; matrix=${selection.matrixMode}")
            }
        }

        return selected
    }
}
