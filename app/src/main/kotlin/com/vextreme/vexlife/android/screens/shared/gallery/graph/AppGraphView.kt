package com.vextreme.vexlife.android.screens.shared.gallery.graph

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.vextreme.vexlife.android.elements.ActionElement
import com.vextreme.vexlife.android.elements.ScreenElement
import com.vextreme.vexlife.android.elements.UIBlueprintRegistry
import com.vextreme.vexlife.android.elements.UIElement
import com.vextreme.vexlife.android.elements.navigation.NavigationTrail
import com.vextreme.vexlife.android.elements.navigation.NavigationTrailContent
import com.vextreme.vexlife.android.logging.VexLogger
import com.vextreme.vexlife.android.screens.primary.home.HomeScreen
import com.vextreme.vexlife.android.runtime.VexRuntime
import com.vextreme.vexlife.android.shell.AppShell

private sealed interface GraphNode {
    data class Screen(val screen: ScreenElement) : GraphNode
    data class Element(val element: UIElement) : GraphNode
}

/**
 * Explores registered UI relationships without becoming a second app-navigation system.
 *
 * [NavigationTrail] is page-local navigation. While this view is mounted, that trail is attached to
 * [VexRuntime], so all three Back surfaces resolve the same local depth first:
 *
 * toolbar Back == Android Back == "Up one graph layer"
 *
 * Only after the graph trail reaches its root does Back fall through and leave Gallery.
 */
@Composable
fun AppGraphView(
    runtime: VexRuntime,
) {
    val registry = runtime.registry
    val trail = remember {
        NavigationTrail<GraphNode>(
            root = GraphNode.Screen(HomeScreen),
        )
    }

    DisposableEffect(runtime, trail) {
        runtime.attachBackLayer(trail)
        onDispose {
            runtime.detachBackLayer(trail)
        }
    }

    val depth = trail.depth
    val visitedScreenIds = trail.path
        .filterIsInstance<GraphNode.Screen>()
        .map { it.screen.screenId }
        .toSet()

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (trail.canGoBack) {
            GraphRow(
                label = "← Up one graph layer",
                detail = "depth $depth",
                depth = depth,
                onClick = { trail.goBack() },
            )
        }

        NavigationTrailContent(
            trail = trail,
            label = "app-graph-depth",
        ) { node ->
            when (node) {
                is GraphNode.Screen -> ScreenNode(
                    screen = node.screen,
                    registry = registry,
                    depth = depth,
                    visitedScreenIds = visitedScreenIds,
                    onOpen = trail::push,
                )

                is GraphNode.Element -> ElementNode(
                    element = node.element,
                    registry = registry,
                    depth = depth,
                    visitedScreenIds = visitedScreenIds,
                    onOpen = trail::push,
                )
            }
        }
    }
}

@Composable
private fun ScreenNode(
    screen: ScreenElement,
    registry: UIBlueprintRegistry,
    depth: Int,
    visitedScreenIds: Set<String>,
    onOpen: (GraphNode) -> Unit,
) {
    val local = registry.elementsForScreen(screen)
    val actions = local.filterIsInstance<ActionElement>().map { it as UIElement }
    val sharedActions = registry.shellElements.filterIsInstance<ActionElement>().map { it as UIElement }
    val navigatables = registry.navigationTargetsFrom(actions + sharedActions)
    val children = registry.childrenOf(registry.screenRoot(screen).element)
    val shellChildren = registry.childrenOf(AppShell.Elements.Root)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "[" + VexLogger.screenName(screen.screenId) + "]",
            style = MaterialTheme.typography.headlineMedium,
        )

        GraphElements("Buttons", actions, depth, onOpen)
        GraphScreens(
            title = "Navigatables",
            screens = navigatables,
            depth = depth,
            currentScreenId = screen.screenId,
            visitedScreenIds = visitedScreenIds,
            onOpen = onOpen,
        )
        GraphElements("Children", children, depth, onOpen)
        GraphElements("Shared shell", shellChildren, depth, onOpen)
    }
}

@Composable
private fun ElementNode(
    element: UIElement,
    registry: UIBlueprintRegistry,
    depth: Int,
    visitedScreenIds: Set<String>,
    onOpen: (GraphNode) -> Unit,
) {
    val children = registry.childrenOf(element)
    val navigatables = (element as? ActionElement)
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
            Text("Parent: " + VexLogger.subjectName(it.elementId))
        }

        registry.ownerScreenOf(element)?.let {
            Text("Declared screen: " + VexLogger.screenName(it.screenId))
        }

        GraphElements("Children", children, depth, onOpen)
        GraphScreens(
            title = "Navigatables",
            screens = navigatables,
            depth = depth,
            currentScreenId = null,
            visitedScreenIds = visitedScreenIds,
            onOpen = onOpen,
        )
    }
}

@Composable
private fun GraphElements(
    title: String,
    nodes: List<UIElement>,
    depth: Int,
    onOpen: (GraphNode) -> Unit,
) {
    Text(title, style = MaterialTheme.typography.titleMedium)

    if (nodes.isEmpty()) {
        Text(
            text = "None registered at this layer.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        nodes.take(12).forEach { element ->
            GraphRow(
                label = VexLogger.subjectName(element.elementId),
                detail = element.kind,
                depth = depth,
                onClick = { onOpen(GraphNode.Element(element)) },
            )
        }
    }
}

@Composable
private fun GraphScreens(
    title: String,
    screens: List<ScreenElement>,
    depth: Int,
    currentScreenId: String?,
    visitedScreenIds: Set<String>,
    onOpen: (GraphNode) -> Unit,
) {
    Text(title, style = MaterialTheme.typography.titleMedium)

    val visible = screens.filterNot { it.screenId == currentScreenId }

    if (visible.isEmpty()) {
        Text(
            text = "None registered at this layer.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        visible.forEach { screen ->
            val alreadyInPath = screen.screenId in visitedScreenIds

            if (alreadyInPath) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = VexLogger.screenName(screen.screenId),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            text = screen.screenId + " · already in current path",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                GraphRow(
                    label = VexLogger.screenName(screen.screenId),
                    detail = screen.screenId,
                    depth = depth,
                    onClick = { onOpen(GraphNode.Screen(screen)) },
                )
            }
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
