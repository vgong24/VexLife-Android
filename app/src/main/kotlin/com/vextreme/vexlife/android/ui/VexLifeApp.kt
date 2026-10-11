package com.vextreme.vexlife.android.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vextreme.vexlife.android.R
import com.vextreme.vexlife.android.blueprint.AppActions
import com.vextreme.vexlife.android.blueprint.Screens
import com.vextreme.vexlife.android.blueprint.ShellElements
import com.vextreme.vexlife.android.blueprint.VexLifeBlueprint
import com.vextreme.vexlife.android.elements.ActionKey
import com.vextreme.vexlife.android.elements.ButtonElement
import com.vextreme.vexlife.android.elements.ElementActionHandler
import com.vextreme.vexlife.android.elements.LocalElementActionHandler
import com.vextreme.vexlife.android.elements.ScreenElement
import com.vextreme.vexlife.android.elements.UIElement
import com.vextreme.vexlife.android.elements.VexFab
import com.vextreme.vexlife.android.elements.VexIcon
import com.vextreme.vexlife.android.elements.VexIconButton
import com.vextreme.vexlife.android.elements.vexElement
import com.vextreme.vexlife.android.logging.VexLogger
import com.vextreme.vexlife.android.runtime.VexRuntime
import com.vextreme.vexlife.android.ui.screens.DebugMenuScreen
import com.vextreme.vexlife.android.ui.screens.GalleryScreen
import com.vextreme.vexlife.android.ui.screens.PrimaryScreenBody

@Composable
fun VexLifeApp() {
    var darkTheme by rememberSaveable { mutableStateOf(true) }
    val logger = remember { VexLogger() }
    val registry = remember { VexLifeBlueprint.registry }

    val currentActionHandler by rememberUpdatedState<(ActionKey, UIElement) -> Unit> { action, _ ->
        when (action.actionId) {
            AppActions.ToggleDarkMode.actionId -> darkTheme = !darkTheme
            AppActions.ResetLogSection.actionId -> logger.startNewSection()
            else -> Unit
        }
    }

    val runtime = remember(registry, logger) {
        VexRuntime(
            registry = registry,
            logger = logger,
            initialScreen = Screens.Home,
            primaryScreens = Screens.Primary,
            onAppAction = { action, source -> currentActionHandler(action, source) },
        )
    }

    LaunchedEffect(runtime) {
        runtime.appOpened()
    }

    VexLifeTheme(darkTheme = darkTheme) {
        CompositionLocalProvider(
            LocalElementActionHandler provides ElementActionHandler { runtime.activate(it) },
        ) {
            VexLifeScaffold(runtime, logger, darkTheme)
        }
    }
}

@Composable
private fun VexLifeScaffold(
    runtime: VexRuntime,
    logger: VexLogger,
    darkTheme: Boolean,
) {
    val primaryScreens = Screens.Primary
    val pagerState = rememberPagerState(
        initialPage = primaryScreens.indexOfFirst { it.screenId == runtime.currentScreen.screenId }
            .coerceAtLeast(0),
        pageCount = { primaryScreens.size },
    )

    LaunchedEffect(runtime.currentScreen.screenId) {
        val targetPage = primaryScreens.indexOfFirst {
            it.screenId == runtime.currentScreen.screenId
        }
        if (targetPage >= 0 && pagerState.settledPage != targetPage) {
            pagerState.animateScrollToPage(
                page = targetPage,
                animationSpec = tween(VexMotionTokens.SpatialMs),
            )
        }
    }

    LaunchedEffect(pagerState.settledPage, pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress && runtime.isPrimary()) {
            runtime.onPagerSettled(primaryScreens[pagerState.settledPage])
        }
    }

    val isPrimary = runtime.isPrimary()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            VexTopBar(
                currentScreen = runtime.currentScreen,
                showBack = !isPrimary,
            )
        },
        bottomBar = {
            if (isPrimary) {
                PrimaryBottomNavigation(runtime.currentScreen)
            }
        },
        floatingActionButton = {
            if (isPrimary) {
                VexFabMenu(runtime)
            }
        },
    ) { innerPadding ->
        if (isPrimary) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                key = { primaryScreens[it].screenId },
            ) { page ->
                PrimaryScreenBody(primaryScreens[page])
            }
        } else {
            AnimatedContent(
                targetState = runtime.currentScreen.screenId,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                transitionSpec = {
                    (slideInHorizontally { it } + fadeIn())
                        .togetherWith(slideOutHorizontally { -it / 3 } + fadeOut())
                },
                label = "auxiliary-screen",
            ) { screenId ->
                when (screenId) {
                    Screens.DebugMenu.screenId -> DebugMenuScreen(runtime, logger, darkTheme)
                    Screens.Gallery.screenId -> GalleryScreen(runtime)
                }
            }
        }
    }
}

@Composable
private fun VexTopBar(
    currentScreen: ScreenElement,
    showBack: Boolean,
) {
    Surface(
        modifier = Modifier.height(VexLayoutTokens.ToolbarHeight),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                modifier = Modifier
                    .size(46.dp)
                    .vexElement(ShellElements.BrandMark),
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.ic_vex_mark),
                        contentDescription = stringResource(R.string.brand_content_description),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(ShellElements.BrandName.textRes),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.vexElement(ShellElements.BrandName),
                )
                Text(
                    text = stringResource(ShellElements.BrandTagline.textRes),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.vexElement(ShellElements.BrandTagline),
                )
            }

            Text(
                text = stringResource(currentScreen.titleRes),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.widthIn(max = 88.dp),
            )

            if (showBack) {
                VexIconButton(ShellElements.Back)
            }
        }
    }
}

@Composable
private fun PrimaryBottomNavigation(
    currentScreen: ScreenElement,
) {
    val handler = LocalElementActionHandler.current
    NavigationBar(
        modifier = Modifier.height(VexLayoutTokens.BottomBarHeight),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        PrimaryNavigationItem(
            element = ShellElements.Home,
            selected = currentScreen.screenId == Screens.Home.screenId,
            handler = handler,
        )
        PrimaryNavigationItem(
            element = ShellElements.Events,
            selected = currentScreen.screenId == Screens.Events.screenId,
            handler = handler,
        )
        PrimaryNavigationItem(
            element = ShellElements.Settings,
            selected = currentScreen.screenId == Screens.Settings.screenId,
            handler = handler,
        )
    }
}

@Composable
private fun PrimaryNavigationItem(
    element: ButtonElement,
    selected: Boolean,
    handler: ElementActionHandler,
) {
    NavigationBarItem(
        selected = selected,
        onClick = { handler.activate(element) },
        modifier = Modifier.vexElement(element),
        icon = {
            VexIcon(
                icon = requireNotNull(element.icon),
                contentDescription = null,
            )
        },
        label = {
            Text(stringResource(requireNotNull(element.labelRes)))
        },
    )
}

@Composable
private fun VexFabMenu(
    runtime: VexRuntime,
) {
    val expanded = runtime.presentation.isOpen(ShellElements.FabOptions)

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = slideInVertically { it / 2 } + fadeIn(),
            exit = slideOutVertically { it / 2 } + fadeOut(),
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                VexFab(ShellElements.DebugMenu)
                VexFab(ShellElements.Gallery)
            }
        }
        VexFab(ShellElements.Add)
    }
}
