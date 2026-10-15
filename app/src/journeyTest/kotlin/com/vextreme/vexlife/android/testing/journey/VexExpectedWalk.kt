package com.vextreme.vexlife.android.testing.journey

import com.vextreme.vexlife.android.elements.ActionElement
import com.vextreme.vexlife.android.elements.ElementIntent
import com.vextreme.vexlife.android.elements.ScreenElement
import com.vextreme.vexlife.android.elements.TextFieldElement
import com.vextreme.vexlife.android.elements.UIElement
import com.vextreme.vexlife.android.logging.VexLogKind
import com.vextreme.vexlife.android.runtime.AppActions
import com.vextreme.vexlife.android.screens.AppScreens
import com.vextreme.vexlife.android.screens.primary.home.HomeScreen

/**
 * Stable semantic projection used for journey comparison.
 *
 * Timestamps, duration and logger sequence are deliberately excluded. List order is the journey
 * order; timing belongs to separate logger/readable-history tests.
 */
data class VexExpectedRecord(
    val kind: VexLogKind,
    val subjectRef: String,
    val action: String,
    val screenBeforeId: String?,
    val screenAfterId: String?,
    val detailsJson: String? = null,
)

/**
 * Independent test-side model of the small runtime semantics a journey needs.
 *
 * This intentionally does not call VexRuntime or VexLogger. It interprets the public element grammar
 * so an instrumentation run can compare "what the declared walk means" with what the real app
 * actually logged.
 */
class VexExpectedWalk : VexJourneyScope {
    private val expected = mutableListOf<VexExpectedRecord>()
    private val openPresentation = mutableListOf<UIElement>()
    private val typedValues = linkedMapOf<String, String>()
    private val primaryIds = AppScreens.primary.map { it.screenId }.toSet()

    private var started = false
    private var primarySelection: ScreenElement = HomeScreen
    private var screenStack = mutableListOf<ScreenElement>()

    val records: List<VexExpectedRecord>
        get() = expected.toList()

    val currentScreen: ScreenElement
        get() = screenStack.lastOrNull() ?: primarySelection

    override fun start(screen: ScreenElement) {
        check(!started) { "journey already started" }
        check(screen.screenId in primaryIds) {
            "instrumented journey start must be a registered primary Screen: ${screen.screenId}"
        }

        started = true
        primarySelection = screen
        screenStack.clear()
        openPresentation.clear()
        typedValues.clear()

        expected += VexExpectedRecord(
            kind = VexLogKind.AppOpened,
            subjectRef = "app",
            action = "App Opened",
            screenBeforeId = null,
            screenAfterId = screen.screenId,
        )
    }

    override fun signal(signal: VexJourneySignal) = Unit

    override fun tapDynamic(element: VexDynamicElementRef) {
        requireStarted()

        expected += VexExpectedRecord(
            kind = VexLogKind.Interaction,
            subjectRef = element.elementId,
            action = "Clicked",
            screenBeforeId = currentScreen.screenId,
            screenAfterId = null,
        )
    }

    override fun expectVisible(element: VexDynamicElementRef) {
        requireStarted()
    }

    override fun tap(element: ActionElement) {
        requireStarted()
        val source = element as? UIElement
            ?: error("ActionElement must also be a UIElement")

        val before = currentScreen
        val target = navigationTarget(element.intent)
            ?.takeIf { it.screenId != before.screenId }

        expected += VexExpectedRecord(
            kind = VexLogKind.Interaction,
            subjectRef = source.elementId,
            action = "Clicked",
            screenBeforeId = before.screenId,
            screenAfterId = target?.screenId,
        )

        applyIntent(element.intent)

        if (invokes(element.intent, AppActions.ClearLogHistory.actionId)) {
            // The actual Reset registration clears VexLogger and the ordered persistence queue after
            // the Reset interaction is formed. Final observable history therefore starts fresh.
            expected.clear()
        }
    }

    override fun type(
        field: TextFieldElement,
        value: String,
    ) {
        requireStarted()
        check(!field.readOnly) {
            "cannot type into read-only field ${field.elementId}"
        }
        typedValues[field.elementId] = value
    }

    override fun submit(element: ActionElement) {
        requireStarted()
        val source = element as? UIElement
            ?: error("ActionElement must also be a UIElement")
        val before = currentScreen

        val submitted = typedValues
            .filterKeys { elementId ->
                val field = AppScreens.registry.element(elementId)
                field != null &&
                    AppScreens.registry.ownerScreenOf(field)?.screenId == before.screenId
            }
            .toSortedMap()

        expected += VexExpectedRecord(
            kind = VexLogKind.Interaction,
            subjectRef = source.elementId,
            action = "Submitted",
            screenBeforeId = before.screenId,
            screenAfterId = navigationTarget(element.intent)
                ?.takeIf { it.screenId != before.screenId }
                ?.screenId,
            detailsJson = submitted.toDiagnosticJson(),
        )

        applyIntent(element.intent)
    }

    override fun dismiss(element: UIElement) {
        requireStarted()
        check(openPresentation.any { it.elementId == element.elementId }) {
            "cannot dismiss presentation that is not open: ${element.elementId}"
        }

        expected += VexExpectedRecord(
            kind = VexLogKind.Interaction,
            subjectRef = element.elementId,
            action = "Dismissed",
            screenBeforeId = currentScreen.screenId,
            screenAfterId = null,
        )
        openPresentation.removeAll { it.elementId == element.elementId }
    }

    override fun back() {
        requireStarted()
        val before = currentScreen
        val after = nextBackScreen()

        expected += VexExpectedRecord(
            kind = VexLogKind.Gesture,
            subjectRef = "system.back",
            action = "Back",
            screenBeforeId = before.screenId,
            screenAfterId = after?.screenId,
        )

        applyBack()
    }

    private fun navigationTarget(intent: ElementIntent): ScreenElement? =
        when (intent) {
            is ElementIntent.NavigateTo -> intent.screen
            ElementIntent.NavigateBack -> nextBackScreen()
            is ElementIntent.Sequence ->
                intent.intents.mapNotNull(::navigationTarget).lastOrNull()
            else -> null
        }

    private fun applyIntent(intent: ElementIntent) {
        when (intent) {
            is ElementIntent.NavigateTo -> navigateTo(intent.screen)
            ElementIntent.NavigateBack -> applyBack()
            is ElementIntent.Open -> open(intent.element)
            is ElementIntent.Close ->
                openPresentation.removeAll { it.elementId == intent.element.elementId }
            is ElementIntent.Toggle -> {
                if (openPresentation.any { it.elementId == intent.element.elementId }) {
                    openPresentation.removeAll { it.elementId == intent.element.elementId }
                } else {
                    open(intent.element)
                }
            }
            is ElementIntent.SwapTo -> Unit
            ElementIntent.LocalEffect -> Unit
            is ElementIntent.Invoke -> Unit
            is ElementIntent.Sequence -> intent.intents.forEach(::applyIntent)
        }
    }

    private fun navigateTo(screen: ScreenElement) {
        if (screen.screenId in primaryIds) {
            primarySelection = screen
            screenStack.clear()
            return
        }

        if (currentScreen.screenId != screen.screenId) {
            screenStack += screen
        }
    }

    private fun open(element: UIElement) {
        openPresentation.removeAll { it.elementId == element.elementId }
        openPresentation += element
    }

    private fun nextBackScreen(): ScreenElement? =
        when {
            openPresentation.isNotEmpty() -> null
            screenStack.isNotEmpty() ->
                screenStack.dropLast(1).lastOrNull() ?: primarySelection
            primarySelection.screenId != HomeScreen.screenId -> HomeScreen
            else -> error("journey attempted Android Back at app root")
        }

    private fun applyBack() {
        when {
            openPresentation.isNotEmpty() ->
                openPresentation.removeAt(openPresentation.lastIndex)

            screenStack.isNotEmpty() ->
                screenStack.removeAt(screenStack.lastIndex)

            primarySelection.screenId != HomeScreen.screenId -> {
                primarySelection = HomeScreen
                screenStack.clear()
            }

            else ->
                error("journey attempted Android Back at app root")
        }
    }

    private fun requireStarted() {
        check(started) { "journey must call start(...) first" }
    }
}

private fun invokes(
    intent: ElementIntent,
    actionId: String,
): Boolean =
    when (intent) {
        is ElementIntent.Invoke -> intent.action.actionId == actionId
        is ElementIntent.Sequence -> intent.intents.any { invokes(it, actionId) }
        else -> false
    }

private fun Map<String, String>.toDiagnosticJson(): String =
    entries.joinToString(
        prefix = "{\n",
        postfix = "\n}",
        separator = ",\n",
    ) { (key, value) ->
        "  \"${key.jsonEscape()}\": \"${value.jsonEscape()}\""
    }

private fun String.jsonEscape(): String =
    buildString(length) {
        this@jsonEscape.forEach { ch ->
            when (ch) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\b' -> append("\\b")
                '\u000C' -> append("\\f")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> {
                    if (ch.code < 0x20) {
                        append("\\u")
                        append(ch.code.toString(16).padStart(4, '0'))
                    } else {
                        append(ch)
                    }
                }
            }
        }
    }
