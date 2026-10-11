package com.vextreme.vexlife.android.runtime

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vextreme.vexlife.android.elements.ActionElement
import com.vextreme.vexlife.android.elements.ActionKey
import com.vextreme.vexlife.android.elements.ElementActionHandler
import com.vextreme.vexlife.android.elements.ElementIntent
import com.vextreme.vexlife.android.elements.FormSubmission
import com.vextreme.vexlife.android.elements.ScreenElement
import com.vextreme.vexlife.android.elements.SwappableContainerElement
import com.vextreme.vexlife.android.elements.UIBlueprintRegistry
import com.vextreme.vexlife.android.elements.UIElement
import com.vextreme.vexlife.android.logging.VexLogger
import com.vextreme.vexlife.android.elements.navigation.BackLayer

/**
 * Owns transient presentation that sits above the current screen.
 *
 * Examples: an expanded FAB menu, dialog, or later a sheet. Opening one of these does NOT mean the
 * user navigated to another screen, so it deliberately does not change [VexRuntime.currentScreen].
 *
 * Debugging:
 * - breakpoint [open], [close], or [closeTop] when a transient surface behaves incorrectly;
 * - inspect this object when the correct screen is active but something is unexpectedly covering it.
 */
class PresentationState(
    private val registry: UIBlueprintRegistry,
) {
    /*
     * Session memory kept by semantic identity, not by retaining Composable trees:
     *
     * openElementIds
     *   = what transient presentation is open right now (FAB options/dialogs).
     *   It behaves like shallow presentation depth and is consumed by Back resolution.
     *
     * selectedByContainer
     *   = the last selected child of a swappable container.
     *   Example: Gallery.Content -> Gallery.AppGraphPanel. This is why returning to Gallery during
     *   the same VexRuntime session can reopen the last selected Gallery tab.
     *
     * Both live only with this runtime instance. They are NOT database/DataStore persistence and
     * should not silently become durable user preference/state.
     */
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
 * Owns the semantic screen position of the app.
 *
 * Primary screens are peers: selecting Home -> Events changes [primarySelection] and does not build a
 * back stack. Home is also the primary-root fallback: Back from Events/Settings returns to Home
 * before Android is allowed to close the app. Shared/detail screens are pushed into [screenStack], so
 * Debug opened from Events still pops to Events first.
 *
 * Debugging a navigation problem:
 * - [current] answers "which screen is active right now?";
 * - [primarySelection] answers "which Home/Events/Settings peer is underneath?";
 * - [screenStack] answers "which pushed screens led from that primary screen to the current screen?".
 */
class AppNavigationState(
    initialPrimary: ScreenElement,
    primaryScreens: List<ScreenElement>,
) {
    private val primaryIds = primaryScreens.map { it.screenId }.toSet()
    private val primaryRoot = initialPrimary

    var primarySelection by mutableStateOf(initialPrimary)
        private set

    var screenStack by mutableStateOf<List<ScreenElement>>(emptyList())
        private set

    val current: ScreenElement
        get() = screenStack.lastOrNull() ?: primarySelection

    val nextBack: AppNavigationBack?
        get() = when {
            screenStack.isNotEmpty() ->
                AppNavigationBack.PopScreen(
                    target = screenStack.dropLast(1).lastOrNull() ?: primarySelection,
                )

            primarySelection.screenId != primaryRoot.screenId ->
                AppNavigationBack.ReturnToPrimaryRoot(
                    target = primaryRoot,
                )

            else ->
                null
        }

    fun isPrimary(screen: ScreenElement = current): Boolean =
        screen.screenId in primaryIds && screenStack.isEmpty()

    fun selectPrimary(screen: ScreenElement) {
        require(screen.screenId in primaryIds) {
            "not a registered primary screen: ${screen.screenId}"
        }

        primarySelection = screen
        screenStack = emptyList()
    }

    fun push(screen: ScreenElement) {
        if (screen.screenId in primaryIds) {
            selectPrimary(screen)
            return
        }

        if (current.screenId == screen.screenId) return
        screenStack = screenStack + screen
    }

    fun popScreen(): ScreenElement? {
        if (screenStack.isEmpty()) return null
        screenStack = screenStack.dropLast(1)
        return current
    }

    fun applyBack(step: AppNavigationBack): ScreenElement =
        when (step) {
            is AppNavigationBack.PopScreen -> {
                screenStack = screenStack.dropLast(1)
                current
            }

            is AppNavigationBack.ReturnToPrimaryRoot -> {
                selectPrimary(primaryRoot)
                current
            }
        }
}

/**
 * One semantic application action after VexRuntime has accepted/logged the source interaction.
 *
 * [submission] exists only for explicit submit-style actions. Draft field changes never create
 * requests, so the action boundary sees one immutable snapshot instead of a keystroke stream.
 */
data class AppActionRequest(
    val action: ActionKey,
    val source: UIElement,
    val submission: FormSubmission? = null,
)

/**
 * Front-door coordinator between declared UI intentions and the current running app.
 *
 * This is the most useful first breakpoint when an interaction or navigation result is surprising.
 * It does not contain screen UI; screen structure lives under `screens/`. It translates an element's
 * declared intent into runtime state, logs the accepted action in semantic order, and lets Compose
 * re-render from the new state.
 *
 * Interaction sequence:
 *
 * element click/toggle/FAB
 * -> [activate]
 * -> snapshot [currentScreen] BEFORE changing navigation
 * -> record the action + before/after screen meaning
 * -> [applyIntent]
 * -> navigation/presentation state changes OR Invoke is handed to [appActions]
 * -> exactly one feature-local AppActionRegistration owns the effect
 * -> Compose observes resulting state and renders the new result
 *
 * Debugging map:
 * - "What screen is active?" -> [currentScreen]
 * - "How did I get here?" -> [navigation]
 * - "Is a dialog/FAB layer open above it?" -> [presentation]
 * - "What did this button actually request?" -> breakpoint [activate] and inspect element.intent
 * - "Why did a horizontal swipe change screens?" -> [onPagerSettled]
 * - "Why did Back go here?" -> [goBack]
 */
class VexRuntime(
    val registry: UIBlueprintRegistry,
    val logger: VexLogger,
    initialScreen: ScreenElement,
    primaryScreens: List<ScreenElement>,
    private val currentAppActions: () -> AppActionRegistrations = {
        AppActionRegistrations.Empty
    },
) : ElementActionHandler {
    private var appOpenedRecorded = false
    private var activeBackLayer: BackLayer? by mutableStateOf(null)

    val presentation = PresentationState(registry)
    val navigation = AppNavigationState(initialScreen, primaryScreens)

    /**
     * Current immutable app-action wiring snapshot.
     *
     * Screens/tools may inspect this by ActionKey or ActionElement. Registration means "handler is
     * wired", not "this action is currently available/authorized".
     */
    val appActions: AppActionRegistrations
        get() = currentAppActions()

    /**
     * The single runtime answer to "which semantic screen is active right now?".
     *
     * This is intentionally easy to inspect in the debugger. Do not infer the active screen from
     * whichever Composable happens to be mounted or from Android lifecycle callbacks.
     */
    val currentScreen: ScreenElement
        get() = navigation.current

    /**
     * Single resolved answer for the entire app Back chain.
     *
     * This is deliberately not exposed as several independent booleans. If Back behavior changes,
     * inspect this value first: it tells you exactly which layer currently owns Back.
     */
    val nextBack: BackResolution?
        get() = resolveBack()

    fun isPrimary(screen: ScreenElement = currentScreen): Boolean =
        navigation.isPrimary(screen)

    private fun resolveBack(): BackResolution? {
        if (presentation.hasOpenPresentation) {
            return BackResolution.DismissPresentation
        }

        activeBackLayer
            ?.takeIf { it.canGoBack }
            ?.let { return BackResolution.PopLocalLayer(it) }

        return navigation.nextBack?.let { BackResolution.AppNavigation(it) }
    }

    /**
     * Makes one screen-local navigation trail participate in the same Back chain as the app.
     *
     * A local layer is active only while its UI is mounted. Call [detachBackLayer] when that UI
     * leaves composition so Back can fall through to the pushed screen underneath.
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
     * attribute a click to the destination screen after navigation has already changed.
     */
    override fun activate(element: ActionElement) {
        activate(
            element = element,
            submission = null,
        )
    }

    /**
     * Submit-style activation for forms.
     *
     * The interaction is logged exactly once at the semantic action boundary (for example Save).
     * [submission] is forwarded to the invoked app action but its field values are not written to
     * VexLogger.
     */
    fun submit(
        element: ActionElement,
        submission: FormSubmission,
    ) {
        activate(
            element = element,
            submission = submission,
        )
    }

    private fun activate(
        element: ActionElement,
        submission: FormSubmission?,
    ) {
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

        applyIntent(
            intent = element.intent,
            source = uiElement,
            submission = submission,
        )
    }

    /**
     * Commits a horizontal pager gesture only after the pager has settled on a primary screen.
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
     * Executes [nextBack], the single resolved Back plan for the current state.
     *
     * Resolution priority remains:
     * 1. transient presentation;
     * 2. active screen-local trail;
     * 3. pushed app screen;
     * 4. primary peer fallback to Home;
     * 5. null at Home/root, allowing Android to own app exit.
     *
     * Toolbar Back and Android system Back both consume this same resolved value.
     */
    fun goBack(logEvent: Boolean = true): Boolean {
        val resolution = nextBack ?: return false
        return goBack(resolution, logEvent)
    }

    /**
     * Executes an already-resolved Back plan.
     *
     * AppFrame uses this overload so the same [BackResolution] instance that enabled native Back is
     * the one that executes when Android dispatches it. This prevents enablement and execution from
     * being derived from different condition checks.
     */
    fun goBack(
        resolution: BackResolution,
        logEvent: Boolean = true,
    ): Boolean {
        val before = currentScreen

        if (logEvent) {
            logger.recordBack(
                screenBefore = before,
                screenAfter = resolution.screenAfter,
            )
        }

        return when (resolution) {
            BackResolution.DismissPresentation ->
                presentation.closeTop() != null

            is BackResolution.PopLocalLayer ->
                resolution.layer.goBack()

            is BackResolution.AppNavigation -> {
                navigation.applyBack(resolution.step)
                true
            }
        }
    }

    private fun navigationTarget(intent: ElementIntent): ScreenElement? = when (intent) {
        is ElementIntent.NavigateTo -> intent.screen
        ElementIntent.NavigateBack ->
            nextBack?.screenAfter
        is ElementIntent.Sequence -> intent.intents.mapNotNull(::navigationTarget).lastOrNull()
        else -> null
    }

    /**
     * The small interpreter for the sentence-like intent grammar declared by screen elements.
     *
     * If an element's visible behavior is wrong but [activate] received the correct element, step
     * through this method next. It is the point where NavigateTo/Open/Close/Toggle/SwapTo become
     * runtime changes and Invoke is resolved through the current [AppActionRegistrations] snapshot.
     */
    private fun applyIntent(
        intent: ElementIntent,
        source: UIElement,
        submission: FormSubmission? = null,
    ) {
        when (intent) {
            is ElementIntent.NavigateTo -> navigation.push(intent.screen)
            ElementIntent.NavigateBack -> goBack(logEvent = false)
            is ElementIntent.Open -> presentation.open(intent.element)
            is ElementIntent.Close -> presentation.close(intent.element)
            is ElementIntent.Toggle -> presentation.toggle(intent.element)
            is ElementIntent.SwapTo -> presentation.swapTo(intent.element)
            is ElementIntent.Invoke -> appActions.dispatch(
                AppActionRequest(
                    action = intent.action,
                    source = source,
                    submission = submission,
                ),
            )

            is ElementIntent.Sequence -> intent.intents.forEach {
                applyIntent(
                    intent = it,
                    source = source,
                    submission = submission,
                )
            }
        }
    }
}
