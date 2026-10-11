package com.vextreme.vexlife.android.testing.journey

import com.vextreme.vexlife.android.elements.ActionElement
import com.vextreme.vexlife.android.elements.DialogElement
import com.vextreme.vexlife.android.elements.ElementIntent
import com.vextreme.vexlife.android.elements.ModalBottomSheetElement
import com.vextreme.vexlife.android.elements.ScreenElement
import com.vextreme.vexlife.android.elements.TextFieldElement
import com.vextreme.vexlife.android.elements.UIElement

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
