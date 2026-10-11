package com.vextreme.vexlife.android.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.vextreme.vexlife.android.elements.ActionKey
import com.vextreme.vexlife.android.elements.UIElement
import com.vextreme.vexlife.android.elements.VexElementTree
import com.vextreme.vexlife.android.logging.VexLogger
import com.vextreme.vexlife.android.pages.AppPages
import com.vextreme.vexlife.android.pages.primary.home.HomePage
import com.vextreme.vexlife.android.pages.shared.debug.DebugMenuPage
import com.vextreme.vexlife.android.pages.shared.debug.DebugMenuPageContent
import com.vextreme.vexlife.android.pages.shared.gallery.GalleryPage
import com.vextreme.vexlife.android.pages.shared.gallery.GalleryPageContent
import com.vextreme.vexlife.android.preview.VexPagePreviews
import com.vextreme.vexlife.android.runtime.AppActions
import com.vextreme.vexlife.android.runtime.VexRuntime
import com.vextreme.vexlife.android.shell.AppFrame

/**
 * Application composition root.
 *
 * Read this file for "what gets wired together?", not for individual page layout. The useful descent
 * path while debugging is:
 *
 * VexLifeApp
 * -> AppFrame (phone-safe shell)
 * -> VexRuntime (current page + navigation/presentation transitions)
 * -> AppPages (registered page objects)
 * -> pages/<role>/<page>/... (the page's actual blueprint/source)
 */
@Composable
fun VexLifeApp() {
    var darkTheme by rememberSaveable { mutableStateOf(true) }
    val logger = remember { VexLogger() }
    val registry = remember { AppPages.registry }

    val currentActionHandler by rememberUpdatedState<(ActionKey, UIElement) -> Unit> { action, _ ->
        when (action.actionId) {
            AppActions.ToggleDarkMode.actionId -> darkTheme = !darkTheme
            AppActions.ResetLogSection.actionId -> logger.startNewSection()
            else -> Unit
        }
    }

    // One runtime instance owns the current navigation/presentation state for this app composition.
    // In a debugger, inspect this variable first; runtime.currentScreen is the active semantic page.
    val runtime = remember(registry, logger) {
        VexRuntime(
            registry = registry,
            logger = logger,
            initialScreen = HomePage,
            primaryScreens = AppPages.primary,
            onAppAction = { action, source -> currentActionHandler(action, source) },
        )
    }

    LaunchedEffect(runtime) {
        runtime.appOpened()
    }

    VexLifeTheme(darkTheme = darkTheme) {
        VexLifeScaffold(
            runtime = runtime,
            logger = logger,
            darkTheme = darkTheme,
        )
    }
}

@Composable
private fun VexLifeScaffold(
    runtime: VexRuntime,
    logger: VexLogger,
    darkTheme: Boolean,
) {
    val primaryPages = AppPages.primary
    val pagerState = rememberPagerState(
        initialPage = primaryPages.indexOfFirst {
            it.screenId == runtime.currentScreen.screenId
        }.coerceAtLeast(0),
        pageCount = { primaryPages.size },
    )

    // Runtime navigation -> pager: taps/navigation intents move the visible primary pager.
    LaunchedEffect(runtime.currentScreen.screenId) {
        val targetPage = primaryPages.indexOfFirst {
            it.screenId == runtime.currentScreen.screenId
        }

        if (targetPage >= 0 && pagerState.settledPage != targetPage) {
            pagerState.animateScrollToPage(
                page = targetPage,
                animationSpec = tween(VexMotionTokens.SpatialMs),
            )
        }
    }

    // Pager -> runtime navigation: a completed swipe becomes semantic navigation only after settling.
    LaunchedEffect(pagerState.settledPage, pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress && runtime.isPrimary()) {
            runtime.onPagerSettled(primaryPages[pagerState.settledPage])
        }
    }

    AppFrame(runtime = runtime) {
        if (runtime.isPrimary()) {
            /*
             * VISUAL ORIENTATION — primary app body
             *
             * What this dynamic code is actually showing:
             *
             * AppFrame
             * ├── toolbar / FAB / bottom navigation      (shell/AppShell.kt)
             * └── HorizontalPager
             *     ├── HomePage.content                  (pages/primary/home/HomePage.kt)
             *     ├── EventsPage.content                (pages/primary/events/EventsPage.kt)
             *     └── SettingsPage.content              (pages/primary/settings/SettingsPage.kt)
             *
             * "page" below is only the pager index. The page's real readable blueprint lives in the
             * corresponding PageBlueprint file. Start at AppPages.primary and command-click the page
             * object when you want to change what a screen looks like; do not edit VexElementTree to
             * make one page-specific visual change.
             */
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                key = { primaryPages[it].screenId },
            ) { page ->
                VexElementTree(
                    placement = primaryPages[page].content,
                    registry = runtime.registry,
                    handler = runtime,
                    modifier = Modifier.fillMaxSize(),
                    selectedChild = runtime.presentation::selected,
                )
            }
        } else {
            /*
             * VISUAL ORIENTATION — pushed/shared page body
             *
             * Shared pages are not children of whichever primary page opened them. They live under
             * pages/shared because Home, Events, or Settings may all navigate to the same destination.
             *
             * currentScreen tells us WHICH shared page is active; the page-specific file tells us
             * WHAT it looks like:
             *
             * DebugMenuPage -> pages/shared/debug/DebugMenuPage.kt
             * GalleryPage   -> pages/shared/gallery/GalleryPage.kt
             *
             * If this list grows enough to become hard to scan, earn a renderer/registry abstraction
             * then. For two special pages, this explicit switch is easier to debug than hidden magic.
             */
            AnimatedContent(
                targetState = runtime.currentScreen.screenId,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    (slideInHorizontally { it } + fadeIn())
                        .togetherWith(slideOutHorizontally { -it / 3 } + fadeOut())
                },
                label = "shared-page",
            ) { screenId ->
                when (screenId) {
                    DebugMenuPage.screenId -> DebugMenuPageContent(
                        runtime = runtime,
                        logger = logger,
                        darkTheme = darkTheme,
                    )

                    GalleryPage.screenId -> GalleryPageContent(runtime)
                }
            }
        }
    }
}



/**
 * Whole-app smoke preview.
 *
 * Page previews answer "does this page/state look right?" This one answers "does the shell + app
 * frame + initial Home composition still fit together?" Future live repositories/services should be
 * supplied through preview composition fixtures rather than making the whole app unpreviewable.
 */
@VexPagePreviews
@Composable
private fun VexLifeAppPreview() {
    VexLifeApp()
}
