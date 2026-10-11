package com.vextreme.vexlife.android.elements

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

fun interface ElementActionHandler {
    fun activate(element: ActionElement)
}

val LocalElementActionHandler = staticCompositionLocalOf<ElementActionHandler> {
    error("ElementActionHandler has not been provided")
}

fun Modifier.vexElement(element: UIElement): Modifier = testTag(element.elementId)

@Composable
fun VexButton(
    element: ButtonElement,
    modifier: Modifier = Modifier,
    style: ElementStyleKey = element.style,
) {
    val handler = LocalElementActionHandler.current
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
            content = content,
        )

        VexElementStyles.DangerAction -> Button(
            onClick = { handler.activate(element) },
            modifier = modifier.vexElement(element),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            ),
            content = content,
        )

        VexElementStyles.Navigation -> OutlinedButton(
            onClick = { handler.activate(element) },
            modifier = modifier.vexElement(element),
            content = content,
        )

        else -> Button(
            onClick = { handler.activate(element) },
            modifier = modifier.vexElement(element),
            content = content,
        )
    }
}

@Composable
fun VexIconButton(
    element: ButtonElement,
    modifier: Modifier = Modifier,
) {
    val handler = LocalElementActionHandler.current
    val label = stringResource(requireNotNull(element.labelRes))
    IconButton(
        onClick = { handler.activate(element) },
        modifier = modifier.vexElement(element),
    ) {
        VexIcon(
            icon = requireNotNull(element.icon) { "${element.elementId} requires an icon" },
            contentDescription = label,
        )
    }
}

@Composable
fun VexFab(
    element: FabElement,
    modifier: Modifier = Modifier,
) {
    val handler = LocalElementActionHandler.current
    val label = stringResource(requireNotNull(element.labelRes))
    FloatingActionButton(
        onClick = { handler.activate(element) },
        modifier = modifier.vexElement(element),
        containerColor = when (element.style) {
            VexElementStyles.FabOption -> MaterialTheme.colorScheme.secondaryContainer
            else -> MaterialTheme.colorScheme.primaryContainer
        },
        contentColor = when (element.style) {
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
    modifier: Modifier = Modifier,
) {
    val handler = LocalElementActionHandler.current
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .vexElement(element),
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
            )
        }
    }
}

@Composable
fun VexContainer(
    element: UIElement,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .vexElement(element),
        shape = MaterialTheme.shapes.large,
        color = when (element.style) {
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
}
