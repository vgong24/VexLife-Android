package com.vextreme.vexlife.android.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.vextreme.vexlife.android.R
import com.vextreme.vexlife.android.blueprint.GalleryElements
import com.vextreme.vexlife.android.blueprint.Screens
import com.vextreme.vexlife.android.blueprint.ShellElements
import com.vextreme.vexlife.android.elements.ActionElement
import com.vextreme.vexlife.android.elements.LocalElementActionHandler
import com.vextreme.vexlife.android.elements.ScreenElement
import com.vextreme.vexlife.android.elements.UIBlueprintRegistry
import com.vextreme.vexlife.android.elements.UIElement
import com.vextreme.vexlife.android.elements.VexButton
import com.vextreme.vexlife.android.elements.VexContainer
import com.vextreme.vexlife.android.elements.VexFab
import com.vextreme.vexlife.android.elements.VexRow
import com.vextreme.vexlife.android.elements.VexScrollableColumn
import com.vextreme.vexlife.android.elements.VexToggleRow
import com.vextreme.vexlife.android.elements.vexElement
import com.vextreme.vexlife.android.logging.VexLogger
import com.vextreme.vexlife.android.runtime.VexRuntime

@Composable
fun GalleryScreen(
    runtime: VexRuntime,
) {
    val selected = runtime.presentation.selected(GalleryElements.Content)
    val handler = LocalElementActionHandler.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selected.elementId == GalleryElements.ElementsPanel.elementId,
                    onClick = { handler.activate(GalleryElements.ElementsTab) },
                    modifier = Modifier.vexElement(GalleryElements.ElementsTab),
                    icon = { Text("E") },
                    label = { Text(stringResource(R.string.gallery_elements)) },
                )
                NavigationBarItem(
                    selected = selected.elementId == GalleryElements.AppGraphPanel.elementId,
                    onClick = { handler.activate(GalleryElements.AppGraphTab) },
                    modifier = Modifier.vexElement(GalleryElements.AppGraphTab),
                    icon = { Text("G") },
                    label = { Text(stringResource(R.string.gallery_app_graph)) },
                )
            }
        },
    ) { innerPadding ->
        AnimatedContent(
            targetState = selected.elementId,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            transitionSpec = {
                (slideInHorizontally { it } + fadeIn())
                    .togetherWith(slideOutHorizontally { -it / 3 } + fadeOut())
            },
            label = "gallery-panel",
        ) { selectedId ->
            when (selectedId) {
                GalleryElements.AppGraphPanel.elementId -> AppGraphPanel(runtime.registry)
                else -> ElementsPanel()
            }
        }
    }
}

@Composable
private fun ElementsPanel() {
    VexScrollableColumn(
        element = GalleryElements.ElementsPanel,
        modifier = Modifier.fillMaxSize(),
    ) {
        Text(
            text = stringResource(GalleryElements.Intro.textRes),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        VexContainer(GalleryElements.SampleContainer) {
            Text(
                text = stringResource(GalleryElements.SampleText.textRes),
                style = MaterialTheme.typography.bodyMedium,
            )
            VexRow(GalleryElements.SampleRow) {
                VexButton(GalleryElements.SampleButton)
            }
            VexToggleRow(
                element = GalleryElements.SampleToggle,
                checked = false,
            )
            VexFab(GalleryElements.SampleFab)
        }
    }
}

@Composable
private fun AppGraphPanel(
    registry: UIBlueprintRegistry,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(GalleryElements.GraphIntro.textRes),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AppGraphExplorer(registry)
    }
}

private sealed interface GraphNode {
    data class Screen(val screen: ScreenElement) : GraphNode
    data class Element(val element: UIElement) : GraphNode
}

@Composable
private fun AppGraphExplorer(
    registry: UIBlueprintRegistry,
) {
    var path by remember {
        mutableStateOf<List<GraphNode>>(listOf(GraphNode.Screen(Screens.Home)))
    }
    val current = path.last()
    val depth = path.lastIndex

    VexContainer(GalleryElements.GraphViewport) {
        if (depth > 0) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { path = path.dropLast(1) },
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(
                    text = "← " + stringResource(R.string.graph_back),
                    modifier = Modifier.padding(12.dp),
                )
            }
        }

        AnimatedContent(
            targetState = current,
            transitionSpec = {
                (slideInHorizontally { it } + fadeIn())
                    .togetherWith(slideOutHorizontally { -it / 3 } + fadeOut())
            },
            label = "app-graph-depth",
        ) { node ->
            when (node) {
                is GraphNode.Screen -> ScreenGraph(
                    screen = node.screen,
                    registry = registry,
                    depth = depth,
                    onOpen = { path = path + it },
                )
                is GraphNode.Element -> ElementGraph(
                    element = node.element,
                    registry = registry,
                    depth = depth,
                    onOpen = { path = path + it },
                )
            }
        }
    }
}

@Composable
private fun ScreenGraph(
    screen: ScreenElement,
    registry: UIBlueprintRegistry,
    depth: Int,
    onOpen: (GraphNode) -> Unit,
) {
    val local = registry.elementsForScreen(screen)
    val localActions = local.filterIsInstance<ActionElement>().map { it as UIElement }
    val shellActions = registry.shellElements.filterIsInstance<ActionElement>().map { it as UIElement }
    val navigatables = registry.navigationTargetsFrom(localActions + shellActions)
    val directChildren = registry.childrenOf(registry.screenRoot(screen).element)
    val shellTop = registry.childrenOf(ShellElements.Root)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "[" + VexLogger.screenName(screen.screenId) + "]",
            style = MaterialTheme.typography.headlineMedium,
        )
        GraphSection(
            title = stringResource(R.string.graph_buttons),
            nodes = localActions,
            depth = depth,
            onClick = { onOpen(GraphNode.Element(it)) },
        )
        GraphScreenSection(
            title = stringResource(R.string.graph_navigatables),
            screens = navigatables,
            depth = depth,
            onClick = { onOpen(GraphNode.Screen(it)) },
        )
        GraphSection(
            title = stringResource(R.string.graph_children),
            nodes = directChildren,
            depth = depth,
            onClick = { onOpen(GraphNode.Element(it)) },
        )
        GraphSection(
            title = stringResource(R.string.graph_shared_shell),
            nodes = shellTop,
            depth = depth,
            onClick = { onOpen(GraphNode.Element(it)) },
        )
    }
}

@Composable
private fun ElementGraph(
    element: UIElement,
    registry: UIBlueprintRegistry,
    depth: Int,
    onOpen: (GraphNode) -> Unit,
) {
    val children = registry.childrenOf(element)
    val navigationTargets = (element as? ActionElement)
        ?.let { registry.navigationTargets(it.intent) }
        .orEmpty()

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "[" + VexLogger.subjectName(element.elementId) + "]",
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = element.kind + " · " + element.elementId,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        registry.parentOf(element)?.let {
            Text(
                text = "Parent: " + VexLogger.subjectName(it.elementId),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        registry.ownerScreenOf(element)?.let {
            Text(
                text = "Declared screen: " + VexLogger.screenName(it.screenId),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        GraphSection(
            title = stringResource(R.string.graph_children),
            nodes = children,
            depth = depth,
            onClick = { onOpen(GraphNode.Element(it)) },
        )
        GraphScreenSection(
            title = stringResource(R.string.graph_navigatables),
            screens = navigationTargets,
            depth = depth,
            onClick = { onOpen(GraphNode.Screen(it)) },
        )
    }
}

@Composable
private fun GraphSection(
    title: String,
    nodes: List<UIElement>,
    depth: Int,
    onClick: (UIElement) -> Unit,
) {
    Text(title, style = MaterialTheme.typography.titleMedium)
    if (nodes.isEmpty()) {
        Text(
            text = stringResource(R.string.graph_no_items),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        nodes.take(12).forEach { node ->
            GraphRow(
                label = VexLogger.subjectName(node.elementId),
                detail = node.kind,
                depth = depth,
                onClick = { onClick(node) },
            )
        }
    }
}

@Composable
private fun GraphScreenSection(
    title: String,
    screens: List<ScreenElement>,
    depth: Int,
    onClick: (ScreenElement) -> Unit,
) {
    Text(title, style = MaterialTheme.typography.titleMedium)
    if (screens.isEmpty()) {
        Text(
            text = stringResource(R.string.graph_no_items),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        screens.forEach { screen ->
            GraphRow(
                label = VexLogger.screenName(screen.screenId),
                detail = screen.screenId,
                depth = depth,
                onClick = { onClick(screen) },
            )
        }
    }
}

@Composable
private fun GraphRow(
    label: String,
    detail: String,
    depth: Int,
    onClick: () -> Unit,
) {
    val color = when (depth % 3) {
        0 -> MaterialTheme.colorScheme.surfaceContainerHigh
        1 -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.tertiaryContainer
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = color,
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleSmall)
                Text(
                    detail,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text("›", style = MaterialTheme.typography.titleLarge)
        }
    }
}
