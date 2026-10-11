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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Modifier
import com.vextreme.vexlife.android.data.AppRepositories
import com.vextreme.vexlife.android.data.loghistory.LogHistoryRepository
import com.vextreme.vexlife.android.data.userprofile.UserProfileRepository
import com.vextreme.vexlife.android.elements.VexElementTree
import com.vextreme.vexlife.android.screens.AppScreens
import com.vextreme.vexlife.android.screens.shared.debug.DebugMenuScreen
import com.vextreme.vexlife.android.screens.shared.debug.DebugMenuScreenContent
import com.vextreme.vexlife.android.screens.shared.gallery.GalleryScreen
import com.vextreme.vexlife.android.screens.shared.gallery.GalleryScreenContent
import com.vextreme.vexlife.android.screens.shared.profile.UserProfileScreen
import com.vextreme.vexlife.android.screens.shared.profile.UserProfileScreenContent
import com.vextreme.vexlife.android.screens.shared.vex.TalkToVexScreen
import com.vextreme.vexlife.android.screens.shared.vex.TalkToVexScreenContent
import com.vextreme.vexlife.android.screens.shared.vexstream.VexStreamMusicScreen
import com.vextreme.vexlife.android.screens.shared.vexstream.VexStreamMusicScreenContent
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
    val session = remember(repositories, scope) {
        VexLifeSession(
            repositories = repositories,
            scope = scope,
        )
    }

    VexLifeApp(session = session)
}

@Composable
private fun VexLifeApp(
    session: VexLifeSession,
) {
    val runtime = session.runtime
    val darkTheme = session.darkTheme

    LaunchedEffect(runtime) {
        runtime.appOpened()
    }

    VexLifeTheme(darkTheme = darkTheme) {
        VexLifeScaffold(
            runtime = runtime,
            logHistoryRepository = session.repositories.logHistory,
            userProfileRepository = session.repositories.userProfile,
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
             * UserProfileScreen   -> screens/shared/profile/UserProfileScreen.kt
             * VexStreamMusicScreen -> screens/shared/vexstream/VexStreamMusicScreen.kt
             * TalkToVexScreen      -> screens/shared/vex/TalkToVexScreen.kt
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

                    VexStreamMusicScreen.screenId ->
                        VexStreamMusicScreenContent(runtime)

                    TalkToVexScreen.screenId ->
                        TalkToVexScreenContent(runtime)
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
