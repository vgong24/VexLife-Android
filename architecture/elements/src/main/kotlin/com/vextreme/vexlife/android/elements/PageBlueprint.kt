package com.vextreme.vexlife.android.elements

import androidx.annotation.StringRes

/**
 * One page's readable source-of-truth description.
 *
 * A normal page declares its identity and nested element placement here once; [VexElementTree]
 * renders that same tree. This avoids maintaining a separate "blueprint version" and "Compose
 * version" of the same page.
 *
 * Nested declarations also produce nested stable IDs. For example:
 *
 * scrollableColumn("body") { card("preview") { text("title", ...) } }
 * -> element.<page>.body.preview.title
 *
 * Name an element with buttonRef/toggleRef/etc only when another relationship must address that
 * exact object (navigation, open/close, external inspection). One-off structural/text elements can
 * remain inline in [layout].
 */
abstract class PageBlueprint(
    pageId: String,
    @StringRes titleRes: Int,
) : ScreenElement(
    screenId = "screen." + normalizePath(pageId),
    titleRes = titleRes,
) {
    private val elementPrefix = "element." + normalizePath(pageId)

    abstract val content: ElementPlacement

    fun asScreenBlueprint(): ScreenBlueprint = ScreenBlueprint(this, content)

    protected fun buttonRef(
        id: String,
        @StringRes labelRes: Int,
        intent: ElementIntent,
        style: ElementStyleKey = VexElementStyles.PrimaryAction,
        icon: ElementIcon? = null,
    ): ButtonElement = ButtonElement(
        elementId = elementId(id),
        labelRes = labelRes,
        style = style,
        icon = icon,
        intent = intent,
    )

    protected fun fabRef(
        id: String,
        @StringRes labelRes: Int,
        icon: ElementIcon,
        intent: ElementIntent,
        style: ElementStyleKey = VexElementStyles.FabPrimary,
    ): FabElement = FabElement(
        elementId = elementId(id),
        labelRes = labelRes,
        icon = icon,
        style = style,
        intent = intent,
    )

    protected fun toggleRef(
        id: String,
        @StringRes labelRes: Int,
        intent: ElementIntent,
    ): ToggleElement = ToggleElement(
        elementId = elementId(id),
        labelRes = labelRes,
        intent = intent,
    )

    protected fun textFieldRef(
        id: String,
        @StringRes labelRes: Int,
        readOnly: Boolean = false,
    ): TextFieldElement = TextFieldElement(
        elementId = elementId(id),
        labelRes = labelRes,
        readOnly = readOnly,
    )

    protected fun columnRef(id: String): ColumnElement =
        ColumnElement(elementId(id))

    protected fun scrollableColumnRef(id: String): ScrollableColumnElement =
        ScrollableColumnElement(elementId(id))

    protected fun dialogRef(id: String): DialogElement =
        DialogElement(elementId(id))

    protected fun swappableRef(
        id: String,
        defaultChild: UIElement,
    ): SwappableContainerElement =
        SwappableContainerElement(elementId(id), defaultChild)

    protected fun layout(
        block: PageLayoutScope.() -> Unit,
    ): ElementPlacement {
        val scope = PageLayoutScope(
            page = this,
            relativePath = "",
        ).apply(block)
        return scope.singleRoot()
    }

    internal fun elementId(relativePath: String): String {
        val normalized = normalizePath(relativePath)
        require(normalized.isNotBlank()) { "page element path must not be blank" }
        return "$elementPrefix.$normalized"
    }

    internal fun relativePath(element: UIElement): String =
        element.elementId.removePrefix("$elementPrefix.")
}

/**
 * DSL scope that records spatial/containment relationships while reading like the intended page.
 *
 * The current parent path is carried as [relativePath], so nesting in source and nesting in the
 * registered element graph remain aligned. When debugging "where does this element live?", inspect
 * the page file first, then use UIBlueprintRegistry.parentOf/childrenOf for the runtime projection.
 */
class PageLayoutScope internal constructor(
    private val page: PageBlueprint,
    private val relativePath: String,
) {
    private val placements = mutableListOf<ElementPlacement>()

    internal fun singleRoot(): ElementPlacement {
        require(placements.size == 1) {
            "page layout must have exactly one root element; found ${placements.size}"
        }
        return placements.single()
    }

    internal fun result(): List<ElementPlacement> = placements.toList()

    fun place(
        element: UIElement,
        style: ElementStyleKey? = null,
    ) {
        placements += ElementPlacement(element, style)
    }

    fun button(
        element: ButtonElement,
        style: ElementStyleKey? = null,
    ) = place(element, style)

    fun fab(
        element: FabElement,
        style: ElementStyleKey? = null,
    ) = place(element, style)

    fun toggle(
        element: ToggleElement,
        style: ElementStyleKey? = null,
    ) = place(element, style)

    fun textField(
        element: TextFieldElement,
        style: ElementStyleKey? = null,
    ) = place(element, style)

    fun text(
        id: String,
        @StringRes textRes: Int,
        role: TextRole = TextRole.Body,
    ): TextElement {
        val element = TextElement(
            elementId = page.elementId(childPath(id)),
            textRes = textRes,
            role = role,
        )
        place(element)
        return element
    }

    fun button(
        id: String,
        @StringRes labelRes: Int,
        intent: ElementIntent,
        style: ElementStyleKey = VexElementStyles.PrimaryAction,
        icon: ElementIcon? = null,
    ): ButtonElement {
        val element = ButtonElement(
            elementId = page.elementId(childPath(id)),
            labelRes = labelRes,
            style = style,
            icon = icon,
            intent = intent,
        )
        place(element)
        return element
    }

    fun toggle(
        id: String,
        @StringRes labelRes: Int,
        intent: ElementIntent,
    ): ToggleElement {
        val element = ToggleElement(
            elementId = page.elementId(childPath(id)),
            labelRes = labelRes,
            intent = intent,
        )
        place(element)
        return element
    }

    fun textField(
        id: String,
        @StringRes labelRes: Int,
        readOnly: Boolean = false,
    ): TextFieldElement {
        val element = TextFieldElement(
            elementId = page.elementId(childPath(id)),
            labelRes = labelRes,
            readOnly = readOnly,
        )
        place(element)
        return element
    }

    fun fab(
        id: String,
        @StringRes labelRes: Int,
        icon: ElementIcon,
        intent: ElementIntent,
        style: ElementStyleKey = VexElementStyles.FabPrimary,
    ): FabElement {
        val element = FabElement(
            elementId = page.elementId(childPath(id)),
            labelRes = labelRes,
            icon = icon,
            style = style,
            intent = intent,
        )
        place(element)
        return element
    }

    fun column(
        id: String,
        style: ElementStyleKey? = null,
        block: PageLayoutScope.() -> Unit,
    ) = container(id, { ColumnElement(page.elementId(childPath(id))) }, style, block)

    fun column(
        element: ColumnElement,
        style: ElementStyleKey? = null,
        block: PageLayoutScope.() -> Unit,
    ) = existingContainer(element, style, block)

    fun card(
        id: String,
        style: ElementStyleKey? = null,
        block: PageLayoutScope.() -> Unit,
    ) = container(id, { CardElement(page.elementId(childPath(id))) }, style, block)

    fun row(
        id: String,
        style: ElementStyleKey? = null,
        block: PageLayoutScope.() -> Unit,
    ) = container(id, { RowElement(page.elementId(childPath(id))) }, style, block)

    fun stack(
        id: String,
        style: ElementStyleKey? = null,
        block: PageLayoutScope.() -> Unit,
    ) = container(id, { StackElement(page.elementId(childPath(id))) }, style, block)

    fun scrollableColumn(
        id: String,
        style: ElementStyleKey? = null,
        block: PageLayoutScope.() -> Unit,
    ) = container(id, { ScrollableColumnElement(page.elementId(childPath(id))) }, style, block)

    fun scrollableColumn(
        element: ScrollableColumnElement,
        style: ElementStyleKey? = null,
        block: PageLayoutScope.() -> Unit,
    ) = existingContainer(element, style, block)

    fun dialog(
        element: DialogElement,
        block: PageLayoutScope.() -> Unit,
    ) {
        val scope = PageLayoutScope(
            page = page,
            relativePath = relativeFromElement(element),
        ).apply(block)
        placements += ElementPlacement(
            element = element,
            children = scope.result(),
        )
    }

    fun swappable(
        element: SwappableContainerElement,
        block: PageLayoutScope.() -> Unit,
    ) {
        val scope = PageLayoutScope(
            page = page,
            relativePath = relativeFromElement(element),
        ).apply(block)
        placements += ElementPlacement(
            element = element,
            children = scope.result(),
        )
    }

    private fun existingContainer(
        element: UIElement,
        style: ElementStyleKey?,
        block: PageLayoutScope.() -> Unit,
    ) {
        val path = relativeFromElement(element)
        val children = PageLayoutScope(
            page = page,
            relativePath = path,
        ).apply(block).result()
        placements += ElementPlacement(
            element = element,
            styleOverride = style,
            children = children,
        )
    }

    private fun container(
        id: String,
        element: () -> UIElement,
        style: ElementStyleKey?,
        block: PageLayoutScope.() -> Unit,
    ) {
        val path = childPath(id)
        val item = element()
        val children = PageLayoutScope(
            page = page,
            relativePath = path,
        ).apply(block).result()
        placements += ElementPlacement(
            element = item,
            styleOverride = style,
            children = children,
        )
    }

    private fun childPath(id: String): String =
        listOf(relativePath, normalizePath(id))
            .filter { it.isNotBlank() }
            .joinToString(".")

    private fun relativeFromElement(element: UIElement): String =
        page.relativePath(element)
}

private fun normalizePath(value: String): String =
    value.trim()
        .replace('/', '.')
        .replace(Regex("\\s+"), "-")
        .trim('.')
