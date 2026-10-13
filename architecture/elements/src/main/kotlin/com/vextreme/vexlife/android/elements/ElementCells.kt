package com.vextreme.vexlife.android.elements

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

fun interface ElementActionHandler {
    fun activate(element: ActionElement)
}

fun Modifier.vexElement(element: UIElement): Modifier = testTag(element.elementId)

@Composable
fun VexButton(
    element: ButtonElement,
    handler: ElementActionHandler,
    modifier: Modifier = Modifier,
    style: ElementStyleKey = element.style,
    enabled: Boolean = true,
) {
    val label = stringResource(requireNotNull(element.labelRes))
    val content: @Composable RowScope.() -> Unit = {
        element.icon?.let {
            VexIcon(icon = it, contentDescription = null)
        }
        Text(label)
    }

    when (style) {
        VexElementStyles.SecondaryAction -> FilledTonalButton(
            onClick = { handler.activate(element) },
            modifier = modifier.vexElement(element),
            enabled = enabled,
            content = content,
        )

        VexElementStyles.DangerAction -> Button(
            onClick = { handler.activate(element) },
            modifier = modifier.vexElement(element),
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            ),
            content = content,
        )

        VexElementStyles.Navigation -> OutlinedButton(
            onClick = { handler.activate(element) },
            modifier = modifier.vexElement(element),
            enabled = enabled,
            content = content,
        )

        else -> Button(
            onClick = { handler.activate(element) },
            modifier = modifier.vexElement(element),
            enabled = enabled,
            content = content,
        )
    }
}

@Composable
fun VexIconButton(
    element: ButtonElement,
    handler: ElementActionHandler,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
    iconOverride: ElementIcon? = null,
) {
    val label = stringResource(requireNotNull(element.labelRes))
    val content: @Composable () -> Unit = {
        VexIcon(
            icon =
                iconOverride
                    ?: requireNotNull(element.icon) {
                        "${element.elementId} requires an icon"
                    },
            contentDescription = label,
        )
    }

    if (selected) {
        FilledTonalIconButton(
            onClick = { handler.activate(element) },
            modifier = modifier.vexElement(element),
            enabled = enabled,
            content = content,
        )
    } else {
        IconButton(
            onClick = { handler.activate(element) },
            modifier = modifier.vexElement(element),
            enabled = enabled,
            content = content,
        )
    }
}

@Composable
fun VexActionFab(
    element: ButtonElement,
    handler: ElementActionHandler,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val label = stringResource(requireNotNull(element.labelRes))
    SmallFloatingActionButton(
        onClick = {
            if (enabled) {
                handler.activate(element)
            }
        },
        modifier = modifier.vexElement(element),
    ) {
        VexIcon(
            icon = requireNotNull(element.icon) {
                "${element.elementId} requires an icon"
            },
            contentDescription = label,
        )
    }
}

@Composable
fun VexFab(
    element: FabElement,
    handler: ElementActionHandler,
    modifier: Modifier = Modifier,
    style: ElementStyleKey = element.style,
) {
    val label = stringResource(requireNotNull(element.labelRes))
    FloatingActionButton(
        onClick = { handler.activate(element) },
        modifier = modifier.vexElement(element),
        containerColor = when (style) {
            VexElementStyles.FabOption -> MaterialTheme.colorScheme.secondaryContainer
            else -> MaterialTheme.colorScheme.primaryContainer
        },
        contentColor = when (style) {
            VexElementStyles.FabOption -> MaterialTheme.colorScheme.onSecondaryContainer
            else -> MaterialTheme.colorScheme.onPrimaryContainer
        },
    ) {
        VexIcon(
            icon = element.icon,
            contentDescription = label,
        )
    }
}

@Composable
fun VexToggleRow(
    element: ToggleElement,
    checked: Boolean,
    handler: ElementActionHandler,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(requireNotNull(element.labelRes)),
                style = MaterialTheme.typography.bodyLarge,
            )
            Switch(
                checked = checked,
                onCheckedChange = { handler.activate(element) },
                modifier = Modifier.vexElement(element),
            )
        }
    }
}

@Composable
fun VexTextField(
    element: TextFieldElement,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .vexElement(element),
        label = {
            Text(stringResource(requireNotNull(element.labelRes)))
        },
        singleLine = true,
        readOnly = element.readOnly,
    )
}

@Composable
fun VexContainer(
    element: UIElement,
    modifier: Modifier = Modifier,
    style: ElementStyleKey = element.style,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .vexElement(element),
        shape = MaterialTheme.shapes.large,
        color = when (style) {
            VexElementStyles.RaisedSurface -> MaterialTheme.colorScheme.surfaceContainerHigh
            else -> MaterialTheme.colorScheme.surfaceContainer
        },
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
fun VexRow(
    element: RowElement,
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(12.dp),
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .vexElement(element),
        horizontalArrangement = horizontalArrangement,
        content = content,
    )
}

@Composable
fun VexColumn(
    element: ColumnElement,
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(12.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .vexElement(element),
        verticalArrangement = verticalArrangement,
        content = content,
    )
}

@Composable
fun VexScrollableColumn(
    element: ScrollableColumnElement,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .vexElement(element)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

/**
 * Renders one registered [ElementPlacement] tree into native Compose cells.
 *
 * This function is intentionally generic and recursive. It should look "dynamic" because the
 * screen-specific visual shape lives in the ScreenBlueprint file, not here.
 *
 * Example:
 *
 * HomeScreen.kt declares:
 *
 * scrollableColumn("body") {
 *     card("preview") {
 *         text("title", ...)
 *         text("message", ...)
 *     }
 * }
 *
 * That arrives here as:
 *
 * ScrollableColumnElement
 * └── CardElement
 *     ├── TextElement(title)
 *     └── TextElement(message)
 *
 * and this renderer chooses VexScrollableColumn -> VexContainer -> Text for those nodes.
 *
 * Where to debug:
 * - wrong screen structure/order -> open the owning ScreenBlueprint first;
 * - wrong reusable cell appearance -> inspect the matching VexButton/VexContainer/etc above;
 * - wrong click consequence -> breakpoint the supplied [handler] (normally VexRuntime.activate);
 * - wrong swappable child (for example Gallery Elements/AppGraph) -> inspect [selectedChild] and
 *   PresentationState.selectedByContainer rather than adding screen-specific branches here.
 *
 * Do not add feature names such as Home/Settings/Gallery to this renderer. If a visual need cannot
 * be described by the element vocabulary, improve the reusable cell/placement grammar instead.
 */
@Composable
fun VexElementTree(
    placement: ElementPlacement,
    registry: UIBlueprintRegistry,
    handler: ElementActionHandler,
    modifier: Modifier = Modifier,
    toggleChecked: (ToggleElement) -> Boolean = { false },
    textFieldValue: (TextFieldElement) -> String = { "" },
    onTextFieldValueChange: (TextFieldElement, String) -> Unit = { _, _ -> },
    selectedChild: (SwappableContainerElement) -> UIElement = { it.defaultChild },
) {
    val element = placement.element
    val style = registry.styleOf(element)

    when (element) {
        is DialogElement -> Unit
        is ModalBottomSheetElement -> Unit

        is SwappableContainerElement -> {
            val selected = selectedChild(element)
            val child = placement.children.firstOrNull {
                it.element.elementId == selected.elementId
            } ?: placement.children.firstOrNull()
            if (child != null) {
                VexElementTree(
                    placement = child,
                    registry = registry,
                    handler = handler,
                    modifier = modifier,
                    toggleChecked = toggleChecked,
                    textFieldValue = textFieldValue,
                    onTextFieldValueChange = onTextFieldValueChange,
                    selectedChild = selectedChild,
                )
            }
        }

        is ScrollableColumnElement -> VexScrollableColumn(
            element = element,
            modifier = modifier,
        ) {
            RenderChildren(
                placement,
                registry,
                handler,
                toggleChecked,
                textFieldValue,
                onTextFieldValueChange,
                selectedChild,
            )
        }

        is CardElement -> VexContainer(
            element = element,
            modifier = modifier,
            style = style,
        ) {
            RenderChildren(
                placement,
                registry,
                handler,
                toggleChecked,
                textFieldValue,
                onTextFieldValueChange,
                selectedChild,
            )
        }

        is ColumnElement -> VexColumn(
            element = element,
            modifier = modifier,
        ) {
            RenderChildren(
                placement,
                registry,
                handler,
                toggleChecked,
                textFieldValue,
                onTextFieldValueChange,
                selectedChild,
            )
        }

        is RowElement -> VexRow(
            element = element,
            modifier = modifier,
        ) {
            placement.children.forEach { child ->
                VexElementTree(
                    child,
                    registry,
                    handler,
                    toggleChecked = toggleChecked,
                    textFieldValue = textFieldValue,
                    onTextFieldValueChange = onTextFieldValueChange,
                    selectedChild = selectedChild,
                )
            }
        }

        is StackElement -> Box(
            modifier = modifier.vexElement(element),
        ) {
            placement.children.forEach { child ->
                VexElementTree(
                    child,
                    registry,
                    handler,
                    toggleChecked = toggleChecked,
                    textFieldValue = textFieldValue,
                    onTextFieldValueChange = onTextFieldValueChange,
                    selectedChild = selectedChild,
                )
            }
        }

        is ButtonElement -> VexButton(
            element = element,
            handler = handler,
            modifier = modifier,
            style = style,
        )

        is FabElement -> VexFab(
            element = element,
            handler = handler,
            modifier = modifier,
            style = style,
        )

        is ToggleElement -> VexToggleRow(
            element = element,
            checked = toggleChecked(element),
            handler = handler,
            modifier = modifier,
        )

        is TextFieldElement -> VexTextField(
            element = element,
            value = textFieldValue(element),
            onValueChange = { value ->
                onTextFieldValueChange(element, value)
            },
            modifier = modifier,
        )

        is TextElement -> Text(
            text = stringResource(element.textRes),
            modifier = modifier.vexElement(element),
            style = when (element.role) {
                TextRole.Title -> MaterialTheme.typography.headlineMedium
                TextRole.Body -> MaterialTheme.typography.bodyLarge
                TextRole.Supporting -> MaterialTheme.typography.bodyMedium
                TextRole.Label -> MaterialTheme.typography.labelSmall
            },
            color = when (element.role) {
                TextRole.Supporting,
                TextRole.Label,
                -> MaterialTheme.colorScheme.onSurfaceVariant
                else -> MaterialTheme.colorScheme.onSurface
            },
        )

        is ImageElement -> Unit

        is ContainerElement -> VexContainer(
            element = element,
            modifier = modifier,
            style = style,
        ) {
            RenderChildren(
                placement,
                registry,
                handler,
                toggleChecked,
                textFieldValue,
                onTextFieldValueChange,
                selectedChild,
            )
        }
    }
}

@Composable
private fun ColumnScope.RenderChildren(
    placement: ElementPlacement,
    registry: UIBlueprintRegistry,
    handler: ElementActionHandler,
    toggleChecked: (ToggleElement) -> Boolean,
    textFieldValue: (TextFieldElement) -> String,
    onTextFieldValueChange: (TextFieldElement, String) -> Unit,
    selectedChild: (SwappableContainerElement) -> UIElement,
) {
    placement.children.forEach { child ->
        VexElementTree(
            placement = child,
            registry = registry,
            handler = handler,
            toggleChecked = toggleChecked,
            textFieldValue = textFieldValue,
            onTextFieldValueChange = onTextFieldValueChange,
            selectedChild = selectedChild,
        )
    }
}

@Composable
fun VexIcon(
    icon: ElementIcon,
    contentDescription: String?,
) {
    Icon(
        imageVector = icon.imageVector(),
        contentDescription = contentDescription,
    )
}

private fun ElementIcon.imageVector(): ImageVector = when (this) {
    ElementIcon.Add -> Icons.Rounded.Add
    ElementIcon.Debug -> Icons.Rounded.BugReport
    ElementIcon.Gallery -> Icons.Rounded.PhotoLibrary
    ElementIcon.Home -> Icons.Rounded.Home
    ElementIcon.Events -> Icons.Rounded.Event
    ElementIcon.Settings -> Icons.Rounded.Settings
    ElementIcon.Back -> Icons.Rounded.ArrowBack
    ElementIcon.History -> Icons.Rounded.History
    ElementIcon.Reset -> Icons.Rounded.RestartAlt
    ElementIcon.Close -> Icons.Rounded.Close
    ElementIcon.Save -> Icons.Rounded.Save
    ElementIcon.Help -> Icons.Rounded.HelpOutline
    ElementIcon.Person -> Icons.Rounded.Person
    ElementIcon.Play -> Icons.Rounded.PlayArrow
    ElementIcon.PlayNext -> Icons.Rounded.PlaylistPlay
    ElementIcon.Queue -> Icons.Rounded.QueueMusic
    ElementIcon.Download -> Icons.Rounded.Download
    ElementIcon.RemoveDownload -> Icons.Rounded.Delete
    ElementIcon.Previous -> Icons.Rounded.SkipPrevious
    ElementIcon.PlayPause -> Icons.Rounded.PlayArrow
    ElementIcon.Pause -> Icons.Rounded.Pause
    ElementIcon.Next -> Icons.Rounded.SkipNext
    ElementIcon.Shuffle -> Icons.Rounded.Shuffle
    ElementIcon.Repeat -> Icons.Rounded.Repeat
    ElementIcon.Search -> Icons.Rounded.Search
    ElementIcon.Music -> Icons.Rounded.MusicNote
    ElementIcon.Location -> Icons.Rounded.MyLocation
    ElementIcon.Menu -> Icons.Rounded.Menu
}
