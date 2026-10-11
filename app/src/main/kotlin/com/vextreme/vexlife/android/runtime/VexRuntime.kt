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
    private var openElementIds by mutableStateOf<List<String>>(emptyList())
    private var selectedByContainer by mutableStateOf<Map<String, String>>(emptyMap())

    val hasOpenPresentation: Boolean
        get() = openElementIds.isNotEmpty()

    fun isOpen(element: UIElement): Boolean =
        element.elementId in openElementIds

    fun open(element: UIElement) {
        openElementIds = openElementIds
            .filterNot { it == element.elementId } + element.elementId
    }

    fun close(element: UIElement) {
        openElementIds = openElementIds.filterNot { it == element.elementId }
    }

    fun closeTop(): UIElement? {
        val topId = openElementIds.lastOrNull() ?: return null
        openElementIds = openElementIds.dropLast(1)
        return registry.element(topId)
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
        val selectedId = selectedByContainer[container.elementId]
            ?: container.defaultChild.elementId

        return registry.childrenOf(container)
            .firstOrNull { it.elementId == selectedId }
            ?: container.defaultChild
    }
}

class AppNavigationState(
    initialPrimary: ScreenElement,
    primaryScreens: List<ScreenElement>,
) {
    private val primaryIds = primaryScreens.map { it.screenId }.toSet()

    var primarySelection by mutableStateOf(initialPrimary)
        private set

    var pageStack by mutableStateOf<List<ScreenElement>>(emptyList())
        private set

    val current: ScreenElement
        get() = pageStack.lastOrNull() ?: primarySelection

    val canPopPage: Boolean
        get() = pageStack.isNotEmpty()

    fun isPrimary(screen: ScreenElement = current): Boolean =
        screen.screenId in primaryIds && pageStack.isEmpty()

    fun selectPrimary(screen: ScreenElement) {
        require(screen.screenId in primaryIds) {
            "not a registered primary screen: ${screen.screenId}"
        }

        primarySelection = screen
        pageStack = emptyList()
    }

    fun push(screen: ScreenElement) {
        if (screen.screenId in primaryIds) {
            selectPrimary(screen)
            return
        }

        if (current.screenId == screen.screenId) return
        pageStack = pageStack + screen
    }

    fun popPage(): ScreenElement? {
        if (pageStack.isEmpty()) return null
        pageStack = pageStack.dropLast(1)
        return current
    }

    fun peekBackTarget(): ScreenElement? =
        if (pageStack.isNotEmpty()) {
            pageStack.dropLast(1).lastOrNull() ?: primarySelection
        } else {
            null
        }
}

class VexRuntime(
    val registry: UIBlueprintRegistry,
    val logger: VexLogger,
    initialScreen: ScreenElement,
    primaryScreens: List<ScreenElement>,
    private val onAppAction: (ActionKey, UIElement) -> Unit,
) : ElementActionHandler {
    private var appOpenedRecorded = false

    val presentation = PresentationState(registry)
    val navigation = AppNavigationState(initialScreen, primaryScreens)

    val currentScreen: ScreenElement
        get() = navigation.current

    val canGoBack: Boolean
        get() = presentation.hasOpenPresentation || navigation.canPopPage

    fun isPrimary(screen: ScreenElement = currentScreen): Boolean =
        navigation.isPrimary(screen)

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
        navigation.selectPrimary(target)
    }

    fun goBack(logEvent: Boolean = true): Boolean {
        val before = currentScreen

        if (presentation.hasOpenPresentation) {
            if (logEvent) {
                logger.recordBack(screenBefore = before)
            }
            presentation.closeTop()
            return true
        }

        val target = navigation.peekBackTarget() ?: return false
        if (logEvent) {
            logger.recordBack(
                screenBefore = before,
                screenAfter = target,
            )
        }
        navigation.popPage()
        return true
    }

    private fun navigationTarget(intent: ElementIntent): ScreenElement? = when (intent) {
        is ElementIntent.NavigateTo -> intent.screen
        ElementIntent.NavigateBack ->
            if (presentation.hasOpenPresentation) null else navigation.peekBackTarget()
        is ElementIntent.Sequence -> intent.intents.mapNotNull(::navigationTarget).lastOrNull()
        else -> null
    }

    private fun applyIntent(
        intent: ElementIntent,
        source: UIElement,
    ) {
        when (intent) {
            is ElementIntent.NavigateTo -> navigation.push(intent.screen)
            ElementIntent.NavigateBack -> goBack(logEvent = false)
            is ElementIntent.Open -> presentation.open(intent.element)
            is ElementIntent.Close -> presentation.close(intent.element)
            is ElementIntent.Toggle -> presentation.toggle(intent.element)
            is ElementIntent.SwapTo -> presentation.swapTo(intent.element)
            is ElementIntent.Invoke -> onAppAction(intent.action, source)
            is ElementIntent.Sequence -> intent.intents.forEach { applyIntent(it, source) }
        }
    }
}
