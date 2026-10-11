package com.vextreme.vexlife.android.testing.journey

import com.vextreme.vexlife.android.elements.ActionElement
import com.vextreme.vexlife.android.elements.DialogElement
import com.vextreme.vexlife.android.elements.ModalBottomSheetElement
import com.vextreme.vexlife.android.elements.TextFieldElement
import com.vextreme.vexlife.android.elements.UIElement
import com.vextreme.vexlife.android.screens.AppScreens
import org.junit.Assert.assertTrue
import org.junit.Test

class VexJourneyCoverageTest {
    @Test
    fun everyRegisteredSemanticSurfaceIsNamedByAJourney() {
        val recorder = VexJourneyCoverageRecorder()
        VexJourneyCatalog.all.forEach { journey ->
            // Validate the declared state/grammar sequence as part of the fast gate too.
            journey.walk(VexExpectedWalk())
            journey.walk(recorder)
        }
        val covered = recorder.snapshot()

        val productionScreens =
            AppScreens.all.map { it.screenId }.toSet()

        val productionActions =
            AppScreens.registry.allElements
                .filterIsInstance<ActionElement>()
                .map { (it as UIElement).elementId }
                .toSet()

        val productionEditableFields =
            AppScreens.registry.allElements
                .filterIsInstance<TextFieldElement>()
                .filterNot { it.readOnly }
                .map { it.elementId }
                .toSet()

        val productionPresentations =
            AppScreens.registry.allElements
                .filter { it is DialogElement || it is ModalBottomSheetElement }
                .map { it.elementId }
                .toSet()

        assertExactCoverage(
            label = "Screen",
            production = productionScreens,
            covered = covered.screenIds,
        )
        assertExactCoverage(
            label = "ActionElement",
            production = productionActions,
            covered = covered.actionElementIds,
        )
        assertExactCoverage(
            label = "editable TextFieldElement",
            production = productionEditableFields,
            covered = covered.editableFieldIds,
        )
        assertExactCoverage(
            label = "Dialog/ModalBottomSheet",
            production = productionPresentations,
            covered = covered.presentationElementIds,
        )
    }
}

private fun assertExactCoverage(
    label: String,
    production: Set<String>,
    covered: Set<String>,
) {
    val missing = (production - covered).sorted()
    val unknown = (covered - production).sorted()

    assertTrue(
        buildString {
            appendLine("$label journey coverage mismatch.")
            if (missing.isNotEmpty()) {
                appendLine("Missing production semantics:")
                missing.forEach { appendLine("  - $it") }
            }
            if (unknown.isNotEmpty()) {
                appendLine("Journey references not present in production registry:")
                unknown.forEach { appendLine("  - $it") }
            }
        },
        missing.isEmpty() && unknown.isEmpty(),
    )
}
