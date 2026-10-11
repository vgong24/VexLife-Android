package com.vextreme.vexlife.android.runtime

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vextreme.vexlife.android.elements.ActionElement
import com.vextreme.vexlife.android.elements.ActionKey
import com.vextreme.vexlife.android.elements.ElementActionHandler
import com.vextreme.vexlife.android.elements.ElementIntent
import com.vextreme.vexlife.android.elements.ScreenElement
import com.vextreme.vexlife.android.elements.SwappableContainerElement
import com.vextreme.vexlife.android.elements.UIBlueprintRegistry
import com.vextreme.vexlife.android.elements.UIElement
import com.vextreme.vexlife.android.logging.VexLogger

class PresentationState(
    private val registry: UIBlueprintRegistry,
) {
    private var openElementIds by mutableStateOf<Set<String>>(emptySet())
    private var selectedByContainer by mutableStateOf<Map<String, String>>(emptyMap())

    fun isOpen(element: UIElement): Boolean = element.elementId in openElementIds

    fun open(element: UIElement) {
        openElementIds = openElementIds + element.elementId
    }

    fun close(element: UIElement) {
        openElementIds = openElementIds - element.elementId
    }

    fun toggle(element: UIElement) {
        if (isOpen(element)) close(element) else open(element)
    }

    fun swapTo(element: UIElement) {
        val container = registry.parentOf(element) as? SwappableContainerElement
            ?: error("swap target must have a swappable parent: ${element.elementId}")
        selectedByContainer = selectedByContainer + (container.elementId to element.elementId)
    }

    fun selected(container: SwappableContainerElement): UIElement {
        val selectedId = selectedByContainer[container.elementId] ?: container.defaultChild.elementId
        return registry.childrenOf(container)
            .firstOrNull { it.elementId == selectedId }
            ?: container.defaultChild
    }
}

class ScreenRelay(
    initialScreen: ScreenElement,
    private val primaryScreenIds: Set<String>,
) {
    var current by mutableStateOf(initialScreen)
        private set

    var lastPrimary by mutableStateOf(initialScreen)
        private set

    fun transitionTo(screen: ScreenElement) {
        current = screen
        if (screen.screenId in primaryScreenIds) {
            lastPrimary = screen
        }
    }

    fun backTarget(): ScreenElement = lastPrimary
}

class VexRuntime(
    val registry: UIBlueprintRegistry,
    val logger: VexLogger,
    initialScreen: ScreenElement,
    primaryScreens: List<ScreenElement>,
    private val onAppAction: (ActionKey, UIElement) -> Unit,
) : ElementActionHandler {
    private val primaryScreenIds = primaryScreens.map { it.screenId }.toSet()
    private var appOpenedRecorded = false

    val presentation = PresentationState(registry)
    val screens = ScreenRelay(initialScreen, primaryScreenIds)

    val currentScreen: ScreenElement
        get() = screens.current

    fun isPrimary(screen: ScreenElement = currentScreen): Boolean =
        screen.screenId in primaryScreenIds

    fun appOpened() {
        if (!appOpenedRecorded) {
            logger.appOpened(currentScreen)
            appOpenedRecorded = true
        }
    }

    override fun activate(element: ActionElement) {
        val uiElement = element as? UIElement
            ?: error("ActionElement must also be a UIElement")

        val before = currentScreen
        val target = navigationTarget(element.intent)
            ?.takeIf { it.screenId != before.screenId }

        logger.recordInteraction(
            element = uiElement,
            screenBefore = before,
            screenAfter = target,
        )

        applyIntent(element.intent, uiElement)
    }

    fun onPagerSettled(target: ScreenElement) {
        val before = currentScreen
        if (!isPrimary(before) || before.screenId == target.screenId) return

        logger.recordScreenGesture(before, target)
        screens.transitionTo(target)
    }

    private fun navigationTarget(intent: ElementIntent): ScreenElement? = when (intent) {
        is ElementIntent.NavigateTo -> intent.screen
        ElementIntent.NavigateBack -> screens.backTarget()
        is ElementIntent.Sequence -> intent.intents.mapNotNull(::navigationTarget).lastOrNull()
        else -> null
    }

    private fun applyIntent(
        intent: ElementIntent,
        source: UIElement,
    ) {
        when (intent) {
            is ElementIntent.NavigateTo -> screens.transitionTo(intent.screen)
            ElementIntent.NavigateBack -> screens.transitionTo(screens.backTarget())
            is ElementIntent.Open -> presentation.open(intent.element)
            is ElementIntent.Close -> presentation.close(intent.element)
            is ElementIntent.Toggle -> presentation.toggle(intent.element)
            is ElementIntent.SwapTo -> presentation.swapTo(intent.element)
            is ElementIntent.Invoke -> onAppAction(intent.action, source)
            is ElementIntent.Sequence -> intent.intents.forEach { applyIntent(it, source) }
        }
    }
}
