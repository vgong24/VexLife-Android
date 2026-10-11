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
import com.vextreme.vexlife.android.data.userprofile.UserProfile
import com.vextreme.vexlife.android.data.userprofile.UserProfileRefs
import com.vextreme.vexlife.android.data.userprofile.UserProfileRepository
import com.vextreme.vexlife.android.elements.VexElementTree
import com.vextreme.vexlife.android.logging.VexLogger
import com.vextreme.vexlife.android.pages.AppPages
import com.vextreme.vexlife.android.pages.primary.home.HomePage
import com.vextreme.vexlife.android.pages.shared.debug.DebugMenuPage
import com.vextreme.vexlife.android.pages.shared.debug.DebugMenuPageContent
import com.vextreme.vexlife.android.pages.shared.gallery.GalleryPage
import com.vextreme.vexlife.android.pages.shared.gallery.GalleryPageContent
import com.vextreme.vexlife.android.pages.shared.profile.UserProfilePage
import com.vextreme.vexlife.android.pages.shared.profile.UserProfilePageContent
import com.vextreme.vexlife.android.runtime.AppActionRequest
import com.vextreme.vexlife.android.runtime.AppActions
import com.vextreme.vexlife.android.runtime.VexRuntime
import com.vextreme.vexlife.android.shell.AppFrame
import java.time.Instant
import kotlinx.coroutines.launch

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
    val registry = remember { AppPages.registry }

    val currentActionHandler by rememberUpdatedState<(AppActionRequest) -> Unit> { request ->
        when (request.action.actionId) {
            AppActions.ToggleDarkMode.actionId -> darkTheme = !darkTheme

            AppActions.ClearLogHistory.actionId -> {
                // Reset is one user intention projected to both current-memory and persisted history.
                logger.clearHistory()
                logPersistence.clear()
            }

            AppActions.SaveUserProfile.actionId -> {
                val requestedName = request.submission
                    ?.value(UserProfilePage.DisplayName)
                    ?.trim()
                    .orEmpty()

                if (requestedName.isNotEmpty()) {
                    /*
                     * The form submitted one immutable snapshot. Persist that snapshot; do not read
                     * mutable Compose draft state from here.
                     *
                     * This composition scope is the temporary async owner. VexAppScope can replace
                     * it later without changing the action/repository/page contracts.
                     */
                    appScope.launch {
                        repositories.userProfile.upsert(
                            UserProfile(
                                profileRef = UserProfileRefs.local,
                                displayName = requestedName,
                                updatedAt = Instant.now(),
                            ),
                        )
                    }
                }
            }

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
            onAppAction = { request ->
                currentActionHandler(request)
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
             * GalleryPage     -> pages/shared/gallery/GalleryPage.kt
             * UserProfilePage -> pages/shared/profile/UserProfilePage.kt
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
                label = "shared-page",
            ) { screenId ->
                when (screenId) {
                    DebugMenuPage.screenId -> DebugMenuPageContent(
                        runtime = runtime,
                        logHistoryRepository = logHistoryRepository,
                        darkTheme = darkTheme,
                    )

                    GalleryPage.screenId -> GalleryPageContent(runtime)

                    UserProfilePage.screenId -> UserProfilePageContent(
                        runtime = runtime,
                        repository = userProfileRepository,
                    )
                }
            }
        }
    }
}

