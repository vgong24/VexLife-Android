package com.vextreme.vexlife.android.testing.journey

import com.vextreme.vexlife.android.elements.ActionElement
import com.vextreme.vexlife.android.elements.DialogElement
import com.vextreme.vexlife.android.elements.ElementIntent
import com.vextreme.vexlife.android.elements.ModalBottomSheetElement
import com.vextreme.vexlife.android.elements.ScreenElement
import com.vextreme.vexlife.android.elements.TextFieldElement
import com.vextreme.vexlife.android.elements.UIElement
import com.vextreme.vexlife.android.screens.AppScreens

data class VexJourneyCoverage(
    val screenIds: Set<String>,
    val actionElementIds: Set<String>,
    val editableFieldIds: Set<String>,
    val presentationElementIds: Set<String>,
)

/**
 * Executes journey declarations without Android UI and records the production semantics they name.
 *
 * This is the fast "did we forget a spot?" side of the foundation. It does not prove rendering or
 * click behavior; instrumentation executes the same declarations for that proof.
 */
class VexJourneyCoverageRecorder : VexJourneyScope {
    private val screens = linkedSetOf<String>()
    private val actions = linkedSetOf<String>()
    private val editableFields = linkedSetOf<String>()
    private val presentations = linkedSetOf<String>()

    fun snapshot(): VexJourneyCoverage =
        VexJourneyCoverage(
            screenIds = screens.toSet(),
            actionElementIds = actions.toSet(),
            editableFieldIds = editableFields.toSet(),
            presentationElementIds = presentations.toSet(),
        )

    override fun start(screen: ScreenElement) {
        screens += screen.screenId
    }

    override fun signal(signal: VexJourneySignal) = Unit

    override fun tapDynamic(element: VexDynamicElementRef) = Unit

    override fun expectVisible(element: VexDynamicElementRef) = Unit

    override fun tap(element: ActionElement) {
        val uiElement = element as? UIElement
            ?: error("ActionElement must also be a UIElement")
        actions += uiElement.elementId
        recordIntent(element.intent)
    }

    override fun type(
        field: TextFieldElement,
        value: String,
    ) {
        if (!field.readOnly) {
            editableFields += field.elementId
        }
    }

    override fun submit(element: ActionElement) {
        tap(element)
    }

    override fun dismiss(element: UIElement) {
        recordPresentation(element)
    }

    override fun back() = Unit

    private fun recordIntent(intent: ElementIntent) {
        when (intent) {
            is ElementIntent.NavigateTo ->
                screens += intent.screen.screenId

            ElementIntent.NavigateBack ->
                Unit

            is ElementIntent.Open ->
                recordPresentation(intent.element)

            is ElementIntent.Close ->
                recordPresentation(intent.element)

            is ElementIntent.Toggle ->
                recordPresentation(intent.element)

            is ElementIntent.SwapTo ->
                Unit

            ElementIntent.LocalEffect ->
                Unit

            is ElementIntent.Invoke ->
                Unit

            is ElementIntent.Sequence ->
                intent.intents.forEach(::recordIntent)
        }
    }

    private fun recordPresentation(element: UIElement) {
        if (element is DialogElement || element is ModalBottomSheetElement) {
            presentations += element.elementId
        }
    }
}


data class VexJourneyCoverageGap(
    val kind: String,
    val semanticRef: String,
    val ownerScreenId: String?,
    val problem: String,
)

data class VexJourneyShardCoverageSummary(
    val shardId: String,
    val journeyCount: Int,
    val environmentCaseCount: Int,
    val screenCount: Int,
    val actionCount: Int,
    val editableFieldCount: Int,
    val presentationCount: Int,
)

data class VexJourneyCoverageReport(
    val productionScreenCount: Int,
    val productionActionCount: Int,
    val productionEditableFieldCount: Int,
    val productionPresentationCount: Int,
    val coveredScreenCount: Int,
    val coveredActionCount: Int,
    val coveredEditableFieldCount: Int,
    val coveredPresentationCount: Int,
    val gaps: List<VexJourneyCoverageGap>,
    val shardSummaries: List<VexJourneyShardCoverageSummary>,
) {
    val hasDebt: Boolean
        get() = gaps.isNotEmpty()

    fun render(): String =
        buildString {
            appendLine("Vex semantic journey coverage")
            appendLine("============================")
            appendLine(
                "Screens: $coveredScreenCount / $productionScreenCount",
            )
            appendLine(
                "ActionElements: $coveredActionCount / $productionActionCount",
            )
            appendLine(
                "Editable fields: $coveredEditableFieldCount / $productionEditableFieldCount",
            )
            appendLine(
                "Modal surfaces: $coveredPresentationCount / $productionPresentationCount",
            )
            appendLine()
            appendLine("Shards:")
            shardSummaries.forEach { shard ->
                appendLine(
                    "  ${shard.shardId}: " +
                        "${shard.journeyCount} journeys / " +
                        "${shard.environmentCaseCount} Android cases / " +
                        "screens=${shard.screenCount}, " +
                        "actions=${shard.actionCount}, " +
                        "fields=${shard.editableFieldCount}, " +
                        "modals=${shard.presentationCount}",
                )
            }

            if (gaps.isNotEmpty()) {
                appendLine()
                appendLine("JOURNEY TECH DEBT:")
                gaps.forEach { gap ->
                    append("  - [${gap.kind}] ${gap.semanticRef}")
                    gap.ownerScreenId?.let { owner ->
                        append(" owner=$owner")
                    }
                    appendLine(" :: ${gap.problem}")
                }
            } else {
                appendLine()
                appendLine("No semantic journey coverage debt.")
            }
        }
}

fun buildJourneyCoverageReport(): VexJourneyCoverageReport {
    val shardCoverage = VexJourneyCatalog.shards.map { shard ->
        val recorder = VexJourneyCoverageRecorder()
        shard.journeys.forEach { journey ->
            journey.walk(recorder)
        }
        shard to recorder.snapshot()
    }

    val allCovered = VexJourneyCoverage(
        screenIds = shardCoverage.flatMap { it.second.screenIds }.toSet(),
        actionElementIds = shardCoverage.flatMap { it.second.actionElementIds }.toSet(),
        editableFieldIds = shardCoverage.flatMap { it.second.editableFieldIds }.toSet(),
        presentationElementIds = shardCoverage.flatMap {
            it.second.presentationElementIds
        }.toSet(),
    )

    val productionScreens = AppScreens.all.map { it.screenId }.toSet()
    val productionActions = AppScreens.registry.allElements
        .filterIsInstance<ActionElement>()
        .map { (it as UIElement).elementId }
        .toSet()
    val productionEditableFields = AppScreens.registry.allElements
        .filterIsInstance<TextFieldElement>()
        .filterNot { it.readOnly }
        .map { it.elementId }
        .toSet()
    val productionPresentations = AppScreens.registry.allElements
        .filter { it is DialogElement || it is ModalBottomSheetElement }
        .map { it.elementId }
        .toSet()

    val gaps = buildList {
        addCoverageGaps(
            kind = "Screen",
            production = productionScreens,
            covered = allCovered.screenIds,
            ownerOf = { it },
        )
        addCoverageGaps(
            kind = "ActionElement",
            production = productionActions,
            covered = allCovered.actionElementIds,
            ownerOf = ::ownerScreenId,
        )
        addCoverageGaps(
            kind = "EditableTextField",
            production = productionEditableFields,
            covered = allCovered.editableFieldIds,
            ownerOf = ::ownerScreenId,
        )
        addCoverageGaps(
            kind = "ModalPresentation",
            production = productionPresentations,
            covered = allCovered.presentationElementIds,
            ownerOf = ::ownerScreenId,
        )
    }.sortedWith(
        compareBy(
            VexJourneyCoverageGap::kind,
            VexJourneyCoverageGap::ownerScreenId,
            VexJourneyCoverageGap::semanticRef,
        ),
    )

    val shardSummaries = shardCoverage.map { (shard, coverage) ->
        VexJourneyShardCoverageSummary(
            shardId = shard.shardId,
            journeyCount = shard.journeys.size,
            environmentCaseCount = shard.journeys.sumOf { it.environments.size },
            screenCount = coverage.screenIds.size,
            actionCount = coverage.actionElementIds.size,
            editableFieldCount = coverage.editableFieldIds.size,
            presentationCount = coverage.presentationElementIds.size,
        )
    }

    return VexJourneyCoverageReport(
        productionScreenCount = productionScreens.size,
        productionActionCount = productionActions.size,
        productionEditableFieldCount = productionEditableFields.size,
        productionPresentationCount = productionPresentations.size,
        coveredScreenCount = allCovered.screenIds.intersect(productionScreens).size,
        coveredActionCount = allCovered.actionElementIds.intersect(productionActions).size,
        coveredEditableFieldCount =
            allCovered.editableFieldIds.intersect(productionEditableFields).size,
        coveredPresentationCount =
            allCovered.presentationElementIds.intersect(productionPresentations).size,
        gaps = gaps,
        shardSummaries = shardSummaries,
    )
}

private fun MutableList<VexJourneyCoverageGap>.addCoverageGaps(
    kind: String,
    production: Set<String>,
    covered: Set<String>,
    ownerOf: (String) -> String?,
) {
    (production - covered).forEach { semanticRef ->
        add(
            VexJourneyCoverageGap(
                kind = kind,
                semanticRef = semanticRef,
                ownerScreenId = ownerOf(semanticRef),
                problem = "production semantic surface is not named by any journey",
            ),
        )
    }

    (covered - production).forEach { semanticRef ->
        add(
            VexJourneyCoverageGap(
                kind = kind,
                semanticRef = semanticRef,
                ownerScreenId = ownerOf(semanticRef),
                problem = "journey references a semantic surface absent from production registry",
            ),
        )
    }
}

private fun ownerScreenId(elementId: String): String? =
    AppScreens.registry.element(elementId)
        ?.let(AppScreens.registry::ownerScreenOf)
        ?.screenId
