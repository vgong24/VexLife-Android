package com.vextreme.vexlife.android.elements

/**
 * One element at one declared structural position, plus its placed children/style override.
 *
 * UIElement owns semantic identity; ElementPlacement owns where that identity sits in a Screen/shell
 * tree. Keeping those concerns separate lets one registry answer parent/child/owner questions.
 */
data class ElementPlacement(
    val element: UIElement,
    val styleOverride: ElementStyleKey? = null,
    val children: List<ElementPlacement> = emptyList(),
)

/**
 * Small shell/legacy placement DSL used when elements already have explicit identities.
 *
 * Screen authors normally use ScreenLayoutScope; AppShell uses this scope because its identities are
 * app-wide rather than generated from one Screen key.
 */
class PlacementScope internal constructor() {
    private val placements = mutableListOf<ElementPlacement>()

    internal fun result(): List<ElementPlacement> = placements.toList()

    fun element(
        item: UIElement,
        style: ElementStyleKey? = null,
    ) {
        placements += ElementPlacement(item, style)
    }

    fun button(
        item: ButtonElement,
        style: ElementStyleKey? = null,
    ) = element(item, style)

    fun fab(
        item: FabElement,
        style: ElementStyleKey? = null,
    ) = element(item, style)

    fun toggle(
        item: ToggleElement,
        style: ElementStyleKey? = null,
    ) = element(item, style)

    fun text(
        item: TextElement,
        style: ElementStyleKey? = null,
    ) = element(item, style)

    fun image(
        item: ImageElement,
        style: ElementStyleKey? = null,
    ) = element(item, style)

    fun column(
        item: ColumnElement,
        style: ElementStyleKey? = null,
        block: PlacementScope.() -> Unit,
    ) = container(item, style, block)

    fun row(
        item: RowElement,
        style: ElementStyleKey? = null,
        block: PlacementScope.() -> Unit,
    ) = container(item, style, block)

    fun stack(
        item: StackElement,
        style: ElementStyleKey? = null,
        block: PlacementScope.() -> Unit,
    ) = container(item, style, block)

    fun scrollableColumn(
        item: ScrollableColumnElement,
        style: ElementStyleKey? = null,
        block: PlacementScope.() -> Unit,
    ) = container(item, style, block)

    fun dialog(
        item: DialogElement,
        style: ElementStyleKey? = null,
        block: PlacementScope.() -> Unit,
    ) = container(item, style, block)

    fun swappable(
        item: SwappableContainerElement,
        style: ElementStyleKey? = null,
        block: PlacementScope.() -> Unit,
    ) = container(item, style, block)

    private fun container(
        item: UIElement,
        style: ElementStyleKey?,
        block: PlacementScope.() -> Unit,
    ) {
        val childScope = PlacementScope().apply(block)
        placements += ElementPlacement(
            element = item,
            styleOverride = style,
            children = childScope.result(),
        )
    }
}

private fun rootPlacement(
    item: UIElement,
    style: ElementStyleKey? = null,
    block: PlacementScope.() -> Unit,
): ElementPlacement {
    val scope = PlacementScope().apply(block)
    return ElementPlacement(item, style, scope.result())
}

fun column(
    item: ColumnElement,
    style: ElementStyleKey? = null,
    block: PlacementScope.() -> Unit,
): ElementPlacement = rootPlacement(item, style, block)

fun row(
    item: RowElement,
    style: ElementStyleKey? = null,
    block: PlacementScope.() -> Unit,
): ElementPlacement = rootPlacement(item, style, block)

fun stack(
    item: StackElement,
    style: ElementStyleKey? = null,
    block: PlacementScope.() -> Unit,
): ElementPlacement = rootPlacement(item, style, block)

fun scrollableColumn(
    item: ScrollableColumnElement,
    style: ElementStyleKey? = null,
    block: PlacementScope.() -> Unit,
): ElementPlacement = rootPlacement(item, style, block)

/**
 * Registry projection of one screen and its root placement.
 *
 * Screen authors normally work in ScreenBlueprint; this compact pair is what AppBlueprint indexes.
 */
data class ScreenRegistration(
    val screen: ScreenElement,
    val root: ElementPlacement,
)

/**
 * Complete static UI registration input: one app-wide shell tree plus all Screen registrations.
 *
 * This is construction data for UIBlueprintRegistry, not mutable runtime navigation state.
 */
data class AppBlueprint(
    val shell: ElementPlacement,
    val screens: List<ScreenRegistration>,
)

/**
 * Indexed read model of the declared shell/Screen element graph.
 *
 * It answers stable structural questions (owner Screen, parent, children, style, navigation targets)
 * without becoming runtime navigation/presentation truth. Build-time validation also fails closed on
 * duplicate identities and intents that point at unregistered targets.
 */
class UIBlueprintRegistry(
    app: AppBlueprint,
) {
    private val screenById = linkedMapOf<String, ScreenElement>()
    private val placementByElementId = linkedMapOf<String, ElementPlacement>()
    private val parentByElementId = linkedMapOf<String, UIElement?>()
    private val ownerScreenByElementId = linkedMapOf<String, ScreenElement?>()
    private val screenRootById = linkedMapOf<String, ElementPlacement>()
    private val shellIds = linkedSetOf<String>()

    val allScreens: List<ScreenElement>
        get() = screenById.values.toList()

    val allElements: List<UIElement>
        get() = placementByElementId.values.map { it.element }

    val shellElements: List<UIElement>
        get() = shellIds.mapNotNull { placementByElementId[it]?.element }

    init {
        require(app.screens.isNotEmpty()) { "app blueprint must register at least one screen" }

        app.screens.forEach { blueprint ->
            check(screenById.putIfAbsent(blueprint.screen.screenId, blueprint.screen) == null) {
                "duplicate screenId ${blueprint.screen.screenId}"
            }
            screenRootById[blueprint.screen.screenId] = blueprint.root
        }

        indexPlacement(
            placement = app.shell,
            parent = null,
            ownerScreen = null,
            shell = true,
        )

        app.screens.forEach { blueprint ->
            indexPlacement(
                placement = blueprint.root,
                parent = null,
                ownerScreen = blueprint.screen,
                shell = false,
            )
        }

        placementByElementId.values.forEach { placement ->
            val source = placement.element
            val action = source as? ActionElement ?: return@forEach
            validateIntent(
                source = source,
                intent = action.intent,
            )
        }

        placementByElementId.values.forEach { placement ->
            val swappable = placement.element as? SwappableContainerElement ?: return@forEach
            val childIds = placement.children.map { it.element.elementId }.toSet()
            check(swappable.defaultChild.elementId in childIds) {
                "${swappable.elementId} default child ${swappable.defaultChild.elementId} is not a direct child"
            }
        }
    }

    private fun indexPlacement(
        placement: ElementPlacement,
        parent: UIElement?,
        ownerScreen: ScreenElement?,
        shell: Boolean,
    ) {
        val id = placement.element.elementId
        check(placementByElementId.putIfAbsent(id, placement) == null) {
            "duplicate elementId $id"
        }
        parentByElementId[id] = parent
        ownerScreenByElementId[id] = ownerScreen
        if (shell) shellIds += id

        placement.children.forEach { child ->
            indexPlacement(
                placement = child,
                parent = placement.element,
                ownerScreen = ownerScreen,
                shell = shell,
            )
        }
    }

    private fun validateIntent(
        source: UIElement,
        intent: ElementIntent,
    ) {
        intent.referencedScreens().forEach { screen ->
            check(screen.screenId in screenById) {
                "intent references unregistered screen ${screen.screenId}"
            }
        }

        when (intent) {
            is ElementIntent.Open -> {
                requireRegistered(intent.element)
                requireSameScope(source, intent.element)
            }

            is ElementIntent.Close -> {
                requireRegistered(intent.element)
                requireSameScope(source, intent.element)
            }

            is ElementIntent.Toggle -> {
                requireRegistered(intent.element)
                requireSameScope(source, intent.element)
            }

            is ElementIntent.SwapTo -> {
                requireRegistered(intent.container)
                requireRegistered(intent.child)

                val parent = parentByElementId[intent.child.elementId]
                check(parent?.elementId == intent.container.elementId) {
                    "swap child ${intent.child.elementId} must be a direct child of " +
                        intent.container.elementId
                }

                requireSameScope(source, intent.container)
                requireSameScope(intent.container, intent.child)
            }

            is ElementIntent.Sequence ->
                intent.intents.forEach { nested ->
                    validateIntent(
                        source = source,
                        intent = nested,
                    )
                }

            else -> Unit
        }
    }

    /**
     * Local presentation mutations may not cross Screen/shell ownership boundaries.
     *
     * NavigateTo is the explicit cross-Screen operation; Open/Close/Toggle/SwapTo remain local to the
     * source feature (or local to the app-wide shell when both owners are null).
     */
    private fun requireSameScope(
        source: UIElement,
        target: UIElement,
    ) {
        val sourceOwner = ownerScreenByElementId[source.elementId]
        val targetOwner = ownerScreenByElementId[target.elementId]

        check(sourceOwner?.screenId == targetOwner?.screenId) {
            "local intent from ${source.elementId} (${sourceOwner?.screenId ?: "shell"}) " +
                "cannot mutate ${target.elementId} (${targetOwner?.screenId ?: "shell"})"
        }
    }

    private fun requireRegistered(item: UIElement) {
        check(item.elementId in placementByElementId) {
            "intent references unregistered element ${item.elementId}"
        }
    }

    fun screen(screenId: String): ScreenElement? = screenById[screenId]

    fun element(elementId: String): UIElement? =
        placementByElementId[elementId]?.element

    fun screenRoot(screen: ScreenElement): ElementPlacement =
        requireNotNull(screenRootById[screen.screenId]) { "screen not registered: ${screen.screenId}" }

    fun placement(element: UIElement): ElementPlacement =
        requireNotNull(placementByElementId[element.elementId]) { "element not registered: ${element.elementId}" }

    fun styleOf(element: UIElement): ElementStyleKey =
        placement(element).styleOverride ?: element.style

    fun parentOf(element: UIElement): UIElement? = parentByElementId[element.elementId]

    fun childrenOf(element: UIElement): List<UIElement> =
        placement(element).children.map { it.element }

    fun ownerScreenOf(element: UIElement): ScreenElement? =
        ownerScreenByElementId[element.elementId]

    fun elementsForScreen(screen: ScreenElement): List<UIElement> =
        placementByElementId.values
            .map { it.element }
            .filter { ownerScreenByElementId[it.elementId]?.screenId == screen.screenId }

    fun navigationTargets(intent: ElementIntent): List<ScreenElement> = when (intent) {
        is ElementIntent.NavigateTo -> listOf(intent.screen)
        is ElementIntent.Sequence -> intent.intents.flatMap(::navigationTargets)
        else -> emptyList()
    }

    fun navigationTargetsFrom(elements: Iterable<UIElement>): List<ScreenElement> =
        elements
            .mapNotNull { it as? ActionElement }
            .flatMap { navigationTargets(it.intent) }
            .distinctBy { it.screenId }
}
