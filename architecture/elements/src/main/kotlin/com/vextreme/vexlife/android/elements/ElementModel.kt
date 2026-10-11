package com.vextreme.vexlife.android.elements

import androidx.annotation.StringRes

/**
 * Stable semantic style key used by the blueprint/registry before Compose chooses a native cell.
 *
 * Keep this as semantic intent ("secondary action"), not a raw color/shape token.
 */
@JvmInline
value class ElementStyleKey(val value: String)

/**
 * Small style vocabulary shared by screen blueprints and the generic renderer.
 */
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

/**
 * Semantic text importance used by the renderer to choose typography.
 */
enum class TextRole {
    Title,
    Body,
    Supporting,
    Label,
}

/**
 * Stable runtime identity for one semantic screen.
 *
 * It is deliberately smaller than a Composable: navigation, logging and AppGraph can refer to the
 * screen without owning how it is currently rendered.
 */
open class ScreenElement(
    val screenId: String,
    @StringRes val titleRes: Int,
) {
    init {
        require(screenId.isNotBlank()) { "screenId must not be blank" }
    }
}

/**
 * Base identity shared by inspectable UI elements.
 *
 * elementId is the stable semantic address used by registry relationships, logging and test tags.
 */
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

/**
 * Stable semantic identifier for an application action whose effect is owned outside the cell.
 */
data class ActionKey(val actionId: String) {
    init {
        require(actionId.isNotBlank()) { "actionId must not be blank" }
    }
}

/**
 * Sentence-like intent grammar declared by blueprint elements.
 *
 * It describes WHAT an interaction requests. VexRuntime interprets it; the renderer does not.
 */
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

/**
 * Marker for UI elements that can request a semantic intent when activated.
 */
interface ActionElement {
    val intent: ElementIntent
}

/**
 * Portable icon meaning. Compose maps these meanings to Android-native vectors in ElementCells.
 */
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
    Person,
}

/**
 * Clickable action cell identity for ordinary buttons.
 *
 * It declares label/style/icon/intent; VexButton owns Android rendering and VexRuntime owns effect.
 */
open class ButtonElement(
    elementId: String,
    @StringRes labelRes: Int,
    style: ElementStyleKey = VexElementStyles.PrimaryAction,
    val icon: ElementIcon? = null,
    override val intent: ElementIntent,
) : UIElement(elementId, "Button", labelRes, style), ActionElement

/**
 * Floating-action-button identity used by shell/screen blueprints.
 */
open class FabElement(
    elementId: String,
    @StringRes labelRes: Int,
    val icon: ElementIcon,
    style: ElementStyleKey = VexElementStyles.FabPrimary,
    override val intent: ElementIntent,
) : UIElement(elementId, "FAB", labelRes, style), ActionElement

/**
 * Toggle-row identity. The element declares the requested intent; current checked state is supplied
 * by the screen/runtime projection rather than stored in the static blueprint.
 */
open class ToggleElement(
    elementId: String,
    @StringRes labelRes: Int,
    style: ElementStyleKey = VexElementStyles.Row,
    override val intent: ElementIntent,
) : UIElement(elementId, "Toggle", labelRes, style), ActionElement

/**
 * Static localized text identity whose [role] communicates typographic meaning to the renderer.
 */
open class TextElement(
    elementId: String,
    @StringRes val textRes: Int,
    val role: TextRole = TextRole.Body,
    style: ElementStyleKey = VexElementStyles.Default,
) : UIElement(elementId, "Text", textRes, style)

/**
 * Blueprint identity for an editable/read-only text field.
 *
 * The element owns label/identity, not the actual typed value. Screen/runtime state supplies the value
 * through VexElementTree so production data is not stored inside the static blueprint.
 */
open class TextFieldElement(
    elementId: String,
    @StringRes labelRes: Int,
    val readOnly: Boolean = false,
    style: ElementStyleKey = VexElementStyles.Default,
) : UIElement(elementId, "TextField", labelRes, style)

/**
 * Immutable semantic snapshot of text-field values at a form submission boundary.
 *
 * Draft typing remains screen/presentation state and is intentionally not an interaction stream.
 * A screen creates this only when the user chooses a semantic action such as Save. Values are keyed by
 * stable element IDs so the receiving action can address fields without depending on Compose state.
 *
 * Do not log [valuesByElementId] by default; form contents may be personal/sensitive and are not
 * needed to understand that Save occurred.
 */
data class FormSubmission(
    val valuesByElementId: Map<String, String>,
) {
    fun value(field: TextFieldElement): String? =
        valuesByElementId[field.elementId]

    companion object {
        fun of(
            vararg values: Pair<TextFieldElement, String>,
        ): FormSubmission =
            FormSubmission(
                valuesByElementId = values.associate { (field, value) ->
                    field.elementId to value
                },
            )
    }
}

/**
 * Image/illustration identity. Platform rendering may choose a native resource for this meaning.
 */
open class ImageElement(
    elementId: String,
    @StringRes val contentDescriptionRes: Int? = null,
    style: ElementStyleKey = VexElementStyles.Default,
) : UIElement(elementId, "Image", contentDescriptionRes, style)

/**
 * Base structural element for elements that contain other registered placements.
 */
open class ContainerElement(
    elementId: String,
    kind: String,
    style: ElementStyleKey = VexElementStyles.Default,
) : UIElement(elementId, kind, style = style)

/**
 * Vertical structural container.
 */
open class ColumnElement(
    elementId: String,
    style: ElementStyleKey = VexElementStyles.Default,
) : ContainerElement(elementId, "Column", style)

/**
 * Raised/grouped surface container used to visually bind related child elements.
 */
open class CardElement(
    elementId: String,
    style: ElementStyleKey = VexElementStyles.RaisedSurface,
) : ContainerElement(elementId, "Card", style)

/**
 * Horizontal structural container.
 */
open class RowElement(
    elementId: String,
    style: ElementStyleKey = VexElementStyles.Row,
) : ContainerElement(elementId, "Row", style)

/**
 * Overlay/stack structural container where children share the same coordinate region.
 */
open class StackElement(
    elementId: String,
    style: ElementStyleKey = VexElementStyles.Default,
) : ContainerElement(elementId, "Stack", style)

/**
 * Vertical container whose Android renderer supplies scrolling.
 */
open class ScrollableColumnElement(
    elementId: String,
    style: ElementStyleKey = VexElementStyles.Default,
) : ContainerElement(elementId, "ScrollableColumn", style)

/**
 * Registered dialog identity. The blueprint records ownership/contents while a platform-specific
 * realization decides when/how the modal is presented.
 */
open class DialogElement(
    elementId: String,
    style: ElementStyleKey = VexElementStyles.RaisedSurface,
) : ContainerElement(elementId, "Dialog", style)

/**
 * Container with one selected child at a time, such as Gallery Elements/AppGraph.
 *
 * Selection state belongs to runtime presentation state; this element only defines the relationship
 * and default child.
 */
open class SwappableContainerElement(
    elementId: String,
    val defaultChild: UIElement,
    style: ElementStyleKey = VexElementStyles.Default,
) : ContainerElement(elementId, "SwappableContainer", style)

/**
 * App-level semantic navigation. The destination becomes [VexRuntime.currentScreen] and therefore
 * participates in the app Back chain.
 */
fun navigateTo(screen: ScreenElement): ElementIntent = ElementIntent.NavigateTo(screen)

fun navigateBack(): ElementIntent = ElementIntent.NavigateBack
fun open(element: UIElement): ElementIntent = ElementIntent.Open(element)
fun close(element: UIElement): ElementIntent = ElementIntent.Close(element)
fun toggle(element: UIElement): ElementIntent = ElementIntent.Toggle(element)

/**
 * Local peer selection inside a SwappableContainerElement.
 *
 * This deliberately does NOT create Screen navigation or Back depth. Use it for tabs/modes such as
 * Gallery Elements <-> AppGraph. If a local interaction needs Back depth, give that feature its own
 * NavigationTrail; if it is a true app destination, use [navigateTo].
 */
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
