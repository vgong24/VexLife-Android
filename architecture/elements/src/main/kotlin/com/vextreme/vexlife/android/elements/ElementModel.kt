package com.vextreme.vexlife.android.elements

import androidx.annotation.StringRes

@JvmInline
value class ElementStyleKey(val value: String)

object VexElementStyles {
    val Default = ElementStyleKey("style.default")
    val PrimaryAction = ElementStyleKey("style.action.primary")
    val SecondaryAction = ElementStyleKey("style.action.secondary")
    val DangerAction = ElementStyleKey("style.action.danger")
    val Navigation = ElementStyleKey("style.navigation")
    val ToolbarIcon = ElementStyleKey("style.toolbar.icon")
    val FabPrimary = ElementStyleKey("style.fab.primary")
    val FabOption = ElementStyleKey("style.fab.option")
    val Surface = ElementStyleKey("style.surface")
    val RaisedSurface = ElementStyleKey("style.surface.raised")
    val Row = ElementStyleKey("style.row")
}

enum class TextRole {
    Title,
    Body,
    Supporting,
    Label,
}

open class ScreenElement(
    val screenId: String,
    @StringRes val titleRes: Int,
) {
    init {
        require(screenId.isNotBlank()) { "screenId must not be blank" }
    }
}

open class UIElement(
    val elementId: String,
    val kind: String,
    @StringRes val labelRes: Int? = null,
    val style: ElementStyleKey = VexElementStyles.Default,
) {
    init {
        require(elementId.isNotBlank()) { "elementId must not be blank" }
        require(kind.isNotBlank()) { "kind must not be blank" }
    }
}

data class ActionKey(val actionId: String) {
    init {
        require(actionId.isNotBlank()) { "actionId must not be blank" }
    }
}

sealed interface ElementIntent {
    data class NavigateTo(val screen: ScreenElement) : ElementIntent
    data object NavigateBack : ElementIntent
    data class Open(val element: UIElement) : ElementIntent
    data class Close(val element: UIElement) : ElementIntent
    data class Toggle(val element: UIElement) : ElementIntent
    data class SwapTo(val element: UIElement) : ElementIntent
    data class Invoke(val action: ActionKey) : ElementIntent
    data class Sequence(val intents: List<ElementIntent>) : ElementIntent {
        init {
            require(intents.isNotEmpty()) { "intent sequence must not be empty" }
        }
    }
}

interface ActionElement {
    val intent: ElementIntent
}

enum class ElementIcon {
    Add,
    Debug,
    Gallery,
    Home,
    Events,
    Settings,
    Back,
    History,
    Reset,
    Close,
    Save,
    Help,
}

open class ButtonElement(
    elementId: String,
    @StringRes labelRes: Int,
    style: ElementStyleKey = VexElementStyles.PrimaryAction,
    val icon: ElementIcon? = null,
    override val intent: ElementIntent,
) : UIElement(elementId, "Button", labelRes, style), ActionElement

open class FabElement(
    elementId: String,
    @StringRes labelRes: Int,
    val icon: ElementIcon,
    style: ElementStyleKey = VexElementStyles.FabPrimary,
    override val intent: ElementIntent,
) : UIElement(elementId, "FAB", labelRes, style), ActionElement

open class ToggleElement(
    elementId: String,
    @StringRes labelRes: Int,
    style: ElementStyleKey = VexElementStyles.Row,
    override val intent: ElementIntent,
) : UIElement(elementId, "Toggle", labelRes, style), ActionElement

open class TextElement(
    elementId: String,
    @StringRes val textRes: Int,
    val role: TextRole = TextRole.Body,
    style: ElementStyleKey = VexElementStyles.Default,
) : UIElement(elementId, "Text", textRes, style)

open class ImageElement(
    elementId: String,
    @StringRes val contentDescriptionRes: Int? = null,
    style: ElementStyleKey = VexElementStyles.Default,
) : UIElement(elementId, "Image", contentDescriptionRes, style)

open class ContainerElement(
    elementId: String,
    kind: String,
    style: ElementStyleKey = VexElementStyles.Default,
) : UIElement(elementId, kind, style = style)

open class ColumnElement(
    elementId: String,
    style: ElementStyleKey = VexElementStyles.Default,
) : ContainerElement(elementId, "Column", style)

open class CardElement(
    elementId: String,
    style: ElementStyleKey = VexElementStyles.RaisedSurface,
) : ContainerElement(elementId, "Card", style)

open class RowElement(
    elementId: String,
    style: ElementStyleKey = VexElementStyles.Row,
) : ContainerElement(elementId, "Row", style)

open class StackElement(
    elementId: String,
    style: ElementStyleKey = VexElementStyles.Default,
) : ContainerElement(elementId, "Stack", style)

open class ScrollableColumnElement(
    elementId: String,
    style: ElementStyleKey = VexElementStyles.Default,
) : ContainerElement(elementId, "ScrollableColumn", style)

open class DialogElement(
    elementId: String,
    style: ElementStyleKey = VexElementStyles.RaisedSurface,
) : ContainerElement(elementId, "Dialog", style)

open class SwappableContainerElement(
    elementId: String,
    val defaultChild: UIElement,
    style: ElementStyleKey = VexElementStyles.Default,
) : ContainerElement(elementId, "SwappableContainer", style)

fun navigateTo(screen: ScreenElement): ElementIntent = ElementIntent.NavigateTo(screen)
fun navigateBack(): ElementIntent = ElementIntent.NavigateBack
fun open(element: UIElement): ElementIntent = ElementIntent.Open(element)
fun close(element: UIElement): ElementIntent = ElementIntent.Close(element)
fun toggle(element: UIElement): ElementIntent = ElementIntent.Toggle(element)
fun swapTo(element: UIElement): ElementIntent = ElementIntent.SwapTo(element)
fun invoke(action: ActionKey): ElementIntent = ElementIntent.Invoke(action)

infix fun ElementIntent.then(next: ElementIntent): ElementIntent {
    val left = if (this is ElementIntent.Sequence) intents else listOf(this)
    val right = if (next is ElementIntent.Sequence) next.intents else listOf(next)
    return ElementIntent.Sequence(left + right)
}

internal fun ElementIntent.referencedScreens(): List<ScreenElement> = when (this) {
    is ElementIntent.NavigateTo -> listOf(screen)
    ElementIntent.NavigateBack -> emptyList()
    is ElementIntent.Sequence -> intents.flatMap { it.referencedScreens() }
    else -> emptyList()
}
