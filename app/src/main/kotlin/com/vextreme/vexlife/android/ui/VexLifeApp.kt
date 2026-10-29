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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Modifier
import com.vextreme.vexlife.android.data.AppRepositories
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import com.vextreme.vexlife.android.screens.AppScreens
import com.vextreme.vexlife.android.runtime.VexRuntime
import com.vextreme.vexlife.android.preview.VexPreviewRepositories
import com.vextreme.vexlife.android.preview.VexScreenPreviews
import com.vextreme.vexlife.android.shell.AppFrame
import kotlinx.coroutines.CoroutineScope

/**
 * Application composition root.
 *
 * Read this file for "what gets wired together?", not for individual screen layout. The useful descent
 * path while debugging is:
 *
 * VexLifeApp
 * -> AppFrame (phone-safe shell)
 * -> VexRuntime (current screen + navigation/presentation transitions)
 * -> AppScreens (registered screen objects)
 * -> screens/<role>/<screen>/... (the screen's actual blueprint/source)
 */
@Composable
fun VexLifeApp() {
    val sessionViewModel: VexLifeViewModel = viewModel()
    VexLifeApp(session = sessionViewModel.session)
}

/**
 * Injectable composition root used by previews/tests and alternate repository environments.
 *
 * Production enters through [VexLifeViewModel], whose session survives configuration recreation.
 * Tooling may supply in-memory repositories and receives a composition-scoped session instead.
 */
@Composable
internal fun VexLifeApp(
    repositories: AppRepositories,
) {
    val scope = rememberCoroutineScope()
    val executionRoot =
        remember {
            VexExecutionRoot()
        }
    val session = remember(repositories, scope, executionRoot) {
        VexLifeSession(
            repositories = repositories,
            scope = scope,
            executionRoot = executionRoot,
        )
    }

    DisposableEffect(executionRoot) {
        onDispose {
            executionRoot.forceStopAll(
                "VexLife repository composition disposed",
            )
        }
    }

    VexLifeApp(session = session)
}

/**
 * Session-injectable composition root.
 *
 * Production reaches this through [VexLifeViewModel]. Journey instrumentation also uses this exact
 * boundary so its Relay/logging lifetime mirrors production instead of being tied to the Compose
 * test composition scope.
 */
@Composable
internal fun VexLifeApp(
    session: VexLifeSession,
) {
    val runtime = session.runtime
    val repositories = session.repositories
    val relays = session.relays
    val sessionScope = session.scope
    val themeMode by
        session.themeModeFamily.states.collectAsState()
    val darkTheme =
        themeMode.requireResolvedDarkTheme()

    LaunchedEffect(runtime) {
        runtime.appOpened()
    }

    /*
     * Read the surface facts in this restart scope, not only inside SideEffect. Compose tracks state
     * reads made during composition; reads performed only by the effect body do not invalidate this
     * parent scope when navigation/presentation changes in a child composition.
     */
    val currentScreenId = runtime.currentScreen.screenId
    val transientBlocked = runtime.presentation.hasOpenPresentation

    SideEffect {
        relays.events.surfaceChanged(
            screenId = currentScreenId,
            transientBlocked = transientBlocked,
        )
    }

    VexLifeTheme(darkTheme = darkTheme) {
        VexLifeScaffold(
            runtime = runtime,
            repositories = repositories,
            relays = relays,
            sessionScope = sessionScope,
            darkTheme = darkTheme,
        )
    }
}

@Composable
private fun VexLifeScaffold(
    runtime: VexRuntime,
    repositories: AppRepositories,
    relays: VexLifeRelays,
    sessionScope: CoroutineScope,
    darkTheme: Boolean,
) {
    val primaryScreens = AppScreens.primary
    val pagerState = rememberPagerState(
        initialPage = primaryScreens.indexOfFirst {
            it.screenId == runtime.navigation.primarySelection.screenId
        }.coerceAtLeast(0),
        pageCount = { primaryScreens.size },
    )

    // Runtime navigation -> pager: taps/navigation intents move the visible primary screen pager.
    LaunchedEffect(runtime.currentScreen.screenId) {
        val targetScreenIndex = primaryScreens.indexOfFirst {
            it.screenId == runtime.currentScreen.screenId
        }

        if (targetScreenIndex >= 0 && pagerState.settledPage != targetScreenIndex) {
            pagerState.animateScrollToPage(
                page = targetScreenIndex,
                animationSpec = tween(VexMotionTokens.SpatialMs),
            )
        }
    }

    // Pager -> runtime navigation: a completed swipe becomes semantic navigation only after settling.
    LaunchedEffect(pagerState.settledPage, pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress && runtime.isPrimary()) {
            runtime.onPagerSettled(primaryScreens[pagerState.settledPage])
        }
    }

    val currentScreenBlueprint =
        AppScreens.require(
            runtime.currentScreen.screenId,
        )

    AppFrame(
        runtime = runtime,
        toolbarActions =
            currentScreenBlueprint.toolbarActions,
    ) {
        if (runtime.isPrimary()) {
            /*
             * VISUAL ORIENTATION — primary app body
             *
             * What this dynamic code is actually showing:
             *
             * AppFrame
             * ├── toolbar / FAB / bottom navigation      (shell/AppShell.kt)
             * └── HorizontalPager
             *     ├── HomeScreen.content                  (screens/primary/home/HomeScreen.kt)
             *     ├── EventsScreen.content                (screens/primary/events/EventsScreen.kt)
             *     └── SettingsScreen.content              (screens/primary/settings/SettingsScreen.kt)
             *
             * "screenIndex" below is only the pager index. The screen's real readable blueprint lives in the
             * corresponding ScreenBlueprint file. Start at AppScreens.primary and command-click the screen
             * object when you want to change what a screen looks like; do not edit VexElementTree to
             * make one screen-specific visual change.
             */
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                key = { primaryScreens[it].screenId },
            ) { screenIndex ->
                primaryScreens[screenIndex].renderContent(
                    runtime = runtime,
                    repositories = repositories,
                    relays = relays,
                    sessionScope = sessionScope,
                    darkTheme = darkTheme,
                )
            }
        } else {
            /*
             * VISUAL ORIENTATION — pushed/shared screen body
             *
             * VexLifeApp owns the app-level transition only. The registered AppScreenBlueprint owns
             * how its Screen is realized and which broad composition dependencies it narrows into the
             * actual ScreenContent call.
             *
             * Adding another registered Screen must not require another case here.
             */
            AnimatedContent(
                targetState = runtime.currentScreen.screenId,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    (slideInHorizontally { it } + fadeIn())
                        .togetherWith(slideOutHorizontally { -it / 3 } + fadeOut())
                },
                label = "shared-screen",
            ) { screenId ->
                AppScreens.require(screenId).renderContent(
                    runtime = runtime,
                    repositories = repositories,
                    relays = relays,
                    sessionScope = sessionScope,
                    darkTheme = darkTheme,
                )
            }
        }
    }
}



/**
 * Whole-app smoke preview.
 *
 * Screen previews answer "does this screen/state look right?" This checks the composition of shell,
 * system-safe frame, initial Home screen, and in-memory repository environment without opening Room.
 */
@VexScreenPreviews
@Composable
private fun VexLifeAppPreview() {
    VexLifeApp(
        repositories = VexPreviewRepositories.empty(),
    )
}
