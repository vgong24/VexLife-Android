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
import com.vextreme.vexlife.android.elements.navigation.BackLayer

/**
 * Owns transient presentation that sits above the current page.
 *
 * Examples: an expanded FAB menu, dialog, or later a sheet. Opening one of these does NOT mean the
 * user navigated to another page, so it deliberately does not change [VexRuntime.currentScreen].
 *
 * Debugging:
 * - breakpoint [open], [close], or [closeTop] when a transient surface behaves incorrectly;
 * - inspect this object when the correct page is active but something is unexpectedly covering it.
 */
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

/**
 * Owns the semantic page position of the app.
 *
 * Primary pages are peers: selecting Home -> Events changes [primarySelection] and does not build a
 * back stack. Home is also the primary-root fallback: Back from Events/Settings returns to Home
 * before Android is allowed to close the app. Shared/detail pages are pushed into [pageStack], so
 * Debug opened from Events still pops to Events first.
 *
 * Debugging a navigation problem:
 * - [current] answers "which page is active right now?";
 * - [primarySelection] answers "which Home/Events/Settings peer is underneath?";
 * - [pageStack] answers "which pushed pages led from that primary page to the current page?".
 */
class AppNavigationState(
    initialPrimary: ScreenElement,
    primaryScreens: List<ScreenElement>,
) {
    private val primaryIds = primaryScreens.map { it.screenId }.toSet()
    private val primaryRoot = initialPrimary

    var primarySelection by mutableStateOf(initialPrimary)
        private set

    var pageStack by mutableStateOf<List<ScreenElement>>(emptyList())
        private set

    val current: ScreenElement
        get() = pageStack.lastOrNull() ?: primarySelection

    val canPopPage: Boolean
        get() = pageStack.isNotEmpty()

    val canReturnToPrimaryRoot: Boolean
        get() =
            pageStack.isEmpty() &&
                primarySelection.screenId != primaryRoot.screenId

    val canGoBack: Boolean
        get() = canPopPage || canReturnToPrimaryRoot

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

    fun goBack(): ScreenElement? {
        if (pageStack.isNotEmpty()) {
            return popPage()
        }

        if (canReturnToPrimaryRoot) {
            selectPrimary(primaryRoot)
            return current
        }

        return null
    }

    fun peekBackTarget(): ScreenElement? =
        when {
            pageStack.isNotEmpty() ->
                pageStack.dropLast(1).lastOrNull() ?: primarySelection

            canReturnToPrimaryRoot ->
                primaryRoot

            else ->
                null
        }
}

/**
 * Front-door coordinator between declared UI intentions and the current running app.
 *
 * This is the most useful first breakpoint when an interaction or navigation result is surprising.
 * It does not contain page UI; page structure lives under `pages/`. It translates an element's
 * declared intent into runtime state, logs the accepted action in semantic order, and lets Compose
 * re-render from the new state.
 *
 * Interaction sequence:
 *
 * element click/toggle/FAB
 * -> [activate]
 * -> snapshot [currentScreen] BEFORE changing navigation
 * -> record the action + before/after page meaning
 * -> [applyIntent]
 * -> navigation/presentation state changes
 * -> Compose observes that state and renders the new result
 *
 * Debugging map:
 * - "What page is active?" -> [currentScreen]
 * - "How did I get here?" -> [navigation]
 * - "Is a dialog/FAB layer open above it?" -> [presentation]
 * - "What did this button actually request?" -> breakpoint [activate] and inspect element.intent
 * - "Why did a horizontal swipe change pages?" -> [onPagerSettled]
 * - "Why did Back go here?" -> [goBack]
 */
class VexRuntime(
    val registry: UIBlueprintRegistry,
    val logger: VexLogger,
    initialScreen: ScreenElement,
    primaryScreens: List<ScreenElement>,
    private val onAppAction: (ActionKey, UIElement) -> Unit,
) : ElementActionHandler {
    private var appOpenedRecorded = false
    private var activeBackLayer: BackLayer? by mutableStateOf(null)

    val presentation = PresentationState(registry)
    val navigation = AppNavigationState(initialScreen, primaryScreens)

    /**
     * The single runtime answer to "which semantic page is active right now?".
     *
     * This is intentionally easy to inspect in the debugger. Do not infer the active page from
     * whichever Composable happens to be mounted or from Android lifecycle callbacks.
     */
    val currentScreen: ScreenElement
        get() = navigation.current

    val canGoBack: Boolean
        get() =
            presentation.hasOpenPresentation ||
                activeBackLayer?.canGoBack == true ||
                navigation.canGoBack

    val showToolbarBack: Boolean
        get() = activeBackLayer?.canGoBack == true || navigation.canPopPage

    fun isPrimary(screen: ScreenElement = currentScreen): Boolean =
        navigation.isPrimary(screen)

    /**
     * Makes one page-local navigation trail participate in the same Back chain as the app.
     *
     * A local layer is active only while its UI is mounted. Call [detachBackLayer] when that UI
     * leaves composition so Back can fall through to the pushed page underneath.
     */
    fun attachBackLayer(layer: BackLayer) {
        activeBackLayer = layer
    }

    fun detachBackLayer(layer: BackLayer) {
        if (activeBackLayer === layer) {
            activeBackLayer = null
        }
    }

    fun appOpened() {
        if (!appOpenedRecorded) {
            logger.appOpened(currentScreen)
            appOpenedRecorded = true
        }
    }

    /**
     * Entry point for action-bearing UI elements.
     *
     * Put a breakpoint here to observe every framework-routed button/FAB/toggle interaction before
     * it mutates navigation or presentation. [before] is captured first so logging cannot accidentally
     * attribute a click to the destination page after navigation has already changed.
     */
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

    /**
     * Commits a horizontal pager gesture only after the pager has settled on a primary page.
     *
     * Keeping this separate from raw scroll motion prevents half-finished swipes from becoming
     * semantic navigation history.
     */
    fun onPagerSettled(target: ScreenElement) {
        val before = currentScreen
        if (!isPrimary(before) || before.screenId == target.screenId) return

        logger.recordScreenGesture(before, target)
        navigation.selectPrimary(target)
    }

    /**
     * Resolves Back from the shallowest visible layer inward:
     *
     * 1. dismiss a transient presentation (dialog/FAB layer);
     * 2. otherwise pop the active page-local navigation layer;
     * 3. otherwise pop a pushed app page;
     * 4. otherwise return Events/Settings to the primary root (Home);
     * 5. return false only when already at Home with no shallower layer, so Android may own root Back.
     *
     * Toolbar Back and Android system Back both end up here.
     */
    fun goBack(logEvent: Boolean = true): Boolean {
        val before = currentScreen

        if (presentation.hasOpenPresentation) {
            if (logEvent) {
                logger.recordBack(screenBefore = before)
            }
            presentation.closeTop()
            return true
        }

        val localLayer = activeBackLayer
        if (localLayer?.canGoBack == true) {
            if (logEvent) {
                logger.recordBack(screenBefore = before)
            }
            return localLayer.goBack()
        }

        val target = navigation.peekBackTarget() ?: return false
        if (logEvent) {
            logger.recordBack(
                screenBefore = before,
                screenAfter = target,
            )
        }
        navigation.goBack()
        return true
    }

    private fun navigationTarget(intent: ElementIntent): ScreenElement? = when (intent) {
        is ElementIntent.NavigateTo -> intent.screen
        ElementIntent.NavigateBack ->
            if (presentation.hasOpenPresentation) null else navigation.peekBackTarget()
        is ElementIntent.Sequence -> intent.intents.mapNotNull(::navigationTarget).lastOrNull()
        else -> null
    }

    /**
     * The small interpreter for the sentence-like intent grammar declared by page elements.
     *
     * If an element's visible behavior is wrong but [activate] received the correct element, step
     * through this method next. It is the point where NavigateTo/Open/Close/Toggle/SwapTo/Invoke
     * become actual runtime changes.
     */
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
