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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.vextreme.vexlife.android.data.AppRepositories
import com.vextreme.vexlife.android.data.AppRepositoryFactory
import com.vextreme.vexlife.android.data.loghistory.LogHistoryPersistenceQueue
import com.vextreme.vexlife.android.data.loghistory.LogHistoryRepository
import com.vextreme.vexlife.android.data.userprofile.UserProfileRepository
import com.vextreme.vexlife.android.elements.VexElementTree
import com.vextreme.vexlife.android.logging.VexLogger
import com.vextreme.vexlife.android.screens.AppScreens
import com.vextreme.vexlife.android.screens.primary.home.HomeScreen
import com.vextreme.vexlife.android.screens.shared.debug.DebugMenuScreen
import com.vextreme.vexlife.android.screens.shared.debug.DebugMenuScreenContent
import com.vextreme.vexlife.android.screens.shared.debug.debugMenuActionRegistrations
import com.vextreme.vexlife.android.screens.shared.gallery.GalleryScreen
import com.vextreme.vexlife.android.screens.shared.gallery.GalleryScreenContent
import com.vextreme.vexlife.android.screens.shared.gallery.galleryActionRegistrations
import com.vextreme.vexlife.android.screens.shared.profile.UserProfileScreen
import com.vextreme.vexlife.android.screens.shared.profile.UserProfileScreenContent
import com.vextreme.vexlife.android.screens.shared.profile.userProfileActionRegistrations
import com.vextreme.vexlife.android.runtime.AppActionRouter
import com.vextreme.vexlife.android.runtime.VexRuntime
import com.vextreme.vexlife.android.preview.VexPreviewRepositories
import com.vextreme.vexlife.android.preview.VexScreenPreviews
import com.vextreme.vexlife.android.shell.AppFrame

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
    val appContext = LocalContext.current.applicationContext
    val repositories = remember(appContext) {
        AppRepositoryFactory.production(appContext)
    }

    VexLifeApp(repositories = repositories)
}

/**
 * Injectable composition root used by previews/tests and, later, alternate repository environments.
 *
 * Production calls the zero-argument wrapper above. Tooling can supply InMemory repositories without
 * teaching production repositories how to mock themselves or requiring Android Studio Preview to
 * open the real Room database.
 */
@Composable
internal fun VexLifeApp(
    repositories: AppRepositories,
) {
    var darkTheme by rememberSaveable { mutableStateOf(true) }

    val appScope = rememberCoroutineScope()
    val logPersistence = remember(repositories.logHistory, appScope) {
        LogHistoryPersistenceQueue(
            repository = repositories.logHistory,
            scope = appScope,
        )
    }
    val logger = remember(logPersistence) {
        VexLogger(onRecord = logPersistence::record)
    }
    val registry = remember { AppScreens.registry }

    /*
     * Feature effects stay beside the Screens that declare them. This root only composes the
     * registrations. AppActionRouter rejects duplicate ActionKey ownership before dispatch.
     */
    val actionRouter = AppActionRouter(
        registrations =
            debugMenuActionRegistrations(
                toggleDarkMode = { darkTheme = !darkTheme },
                logger = logger,
                logPersistence = logPersistence,
            ) +
                galleryActionRegistrations() +
                userProfileActionRegistrations(
                    repository = repositories.userProfile,
                    scope = appScope,
                ),
    )
    val currentActionRouter by rememberUpdatedState(actionRouter)

    // One runtime instance owns the current navigation/presentation state for this app composition.
    // In a debugger, inspect this variable first; runtime.currentScreen is the active semantic screen.
    val runtime = remember(registry, logger) {
        VexRuntime(
            registry = registry,
            logger = logger,
            initialScreen = HomeScreen,
            primaryScreens = AppScreens.primary,
            onAppAction = { request ->
                currentActionRouter.dispatch(request)
            },
        )
    }

    LaunchedEffect(runtime) {
        runtime.appOpened()
    }

    VexLifeTheme(darkTheme = darkTheme) {
        VexLifeScaffold(
            runtime = runtime,
            logHistoryRepository = repositories.logHistory,
            userProfileRepository = repositories.userProfile,
            darkTheme = darkTheme,
        )
    }
}

@Composable
private fun VexLifeScaffold(
    runtime: VexRuntime,
    logHistoryRepository: LogHistoryRepository,
    userProfileRepository: UserProfileRepository,
    darkTheme: Boolean,
) {
    val primaryScreens = AppScreens.primary
    val pagerState = rememberPagerState(
        initialPage = primaryScreens.indexOfFirst {
            it.screenId == runtime.currentScreen.screenId
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
                VexElementTree(
                    placement = primaryScreens[screenIndex].content,
                    registry = runtime.registry,
                    handler = runtime,
                    modifier = Modifier.fillMaxSize(),
                    selectedChild = runtime.presentation::selected,
                )
            }
        } else {
            /*
             * VISUAL ORIENTATION — pushed/shared screen body
             *
             * Shared screens are not children of whichever primary screen opened them. They live under
             * screens/shared because Home, Events, or Settings may all navigate to the same destination.
             *
             * currentScreen tells us WHICH shared screen is active; the screen-specific file tells us
             * WHAT it looks like:
             *
             * DebugMenuScreen -> screens/shared/debug/DebugMenuScreen.kt
             * GalleryScreen     -> screens/shared/gallery/GalleryScreen.kt
             * UserProfileScreen -> screens/shared/profile/UserProfileScreen.kt
             *
             * If this list grows enough to become hard to scan, earn a renderer/registry abstraction
             * then. For the current small set, this explicit switch is easier to debug than hidden magic.
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
                when (screenId) {
                    DebugMenuScreen.screenId -> DebugMenuScreenContent(
                        runtime = runtime,
                        logHistoryRepository = logHistoryRepository,
                        darkTheme = darkTheme,
                    )

                    GalleryScreen.screenId -> GalleryScreenContent(runtime)

                    UserProfileScreen.screenId -> UserProfileScreenContent(
                        runtime = runtime,
                        repository = userProfileRepository,
                    )
                }
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
