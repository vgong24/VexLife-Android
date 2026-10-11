package com.vextreme.vexlife.android.preview

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.vextreme.vexlife.android.data.AppRepositories
import com.vextreme.vexlife.android.data.loghistory.InMemoryLogHistoryRepository
import com.vextreme.vexlife.android.data.loghistory.LogHistoryFixture
import com.vextreme.vexlife.android.data.userprofile.InMemoryUserProfileRepository
import com.vextreme.vexlife.android.data.userprofile.UserProfileFixture
import com.vextreme.vexlife.android.elements.ScreenBlueprint
import com.vextreme.vexlife.android.elements.VexElementTree
import com.vextreme.vexlife.android.logging.VexLogger
import com.vextreme.vexlife.android.screens.AppScreens
import com.vextreme.vexlife.android.screens.primary.home.HomeScreen
import com.vextreme.vexlife.android.screens.shared.debug.DebugMenuScreen
import com.vextreme.vexlife.android.screens.shared.gallery.GalleryScreen
import com.vextreme.vexlife.android.screens.shared.profile.UserProfileScreen
import com.vextreme.vexlife.android.runtime.AppActionRegistrations
import com.vextreme.vexlife.android.runtime.VexRuntime
import com.vextreme.vexlife.android.shell.AppFrame
import com.vextreme.vexlife.android.shell.AppShell
import com.vextreme.vexlife.android.ui.VexLifeTheme

/**
 * Shared Android Studio forms for screen work.
 *
 * Screen preview functions intentionally stay in the SAME *Screen.kt file as the blueprint they
 * visualize. This helper only centralizes device/theme pressure and reusable fake data.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.BINARY)
@Preview(
    name = "Phone · Dark",
    widthDp = 412,
    heightDp = 915,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showSystemUi = true,
)
@Preview(
    name = "Phone · Light",
    widthDp = 412,
    heightDp = 915,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    showSystemUi = true,
)
@Preview(
    name = "Compact · Large text",
    widthDp = 360,
    heightDp = 720,
    fontScale = 1.45f,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showSystemUi = true,
)
internal annotation class VexScreenPreviews

/**
 * Preview-only initial conditions around the real runtime/screen code.
 *
 * Mocking stops at repository facts. Navigation/rendering still use the production runtime,
 * registry and cells. As Relay/service state arrives, seed fixtures from those rightful owners
 * instead of cloning their state machines here.
 */
internal data class VexPreviewScenario(
    val id: String,
    val startScreen: ScreenBlueprint,
    val repositoriesFactory: () -> AppRepositories = VexPreviewRepositories::empty,
    val prepare: VexPreviewContext.() -> Unit = {},
) {
    companion object {
        fun debugHistoryOpen(): VexPreviewScenario {
            val fixture = LogHistoryFixture.build {
                appOpened(HomeScreen)
                afterSeconds(120)
                click(
                    element = AppShell.Elements.DebugMenu,
                    from = HomeScreen,
                    to = DebugMenuScreen,
                )
            }.append {
                afterSeconds(30)
                click(
                    element = DebugMenuScreen.ResetLog,
                    from = DebugMenuScreen,
                )
            }

            return VexPreviewScenario(
                id = "debug-history-open",
                startScreen = DebugMenuScreen,
                repositoriesFactory = {
                    VexPreviewRepositories.withLogHistory(fixture)
                },
                prepare = {
                    runtime.presentation.open(DebugMenuScreen.LogDialog)
                },
            )
        }

        fun galleryAppGraph(): VexPreviewScenario =
            VexPreviewScenario(
                id = "gallery-app-graph",
                startScreen = GalleryScreen,
                prepare = {
                    runtime.presentation.swapTo(GalleryScreen.AppGraphPanel)
                },
            )

        fun userProfile(displayName: String): VexPreviewScenario {
            val fixture = UserProfileFixture.build {
                displayName(displayName)
            }

            return VexPreviewScenario(
                id = "user-profile-$displayName",
                startScreen = UserProfileScreen,
                repositoriesFactory = {
                    VexPreviewRepositories.withUserProfile(fixture)
                },
            )
        }
    }
}

internal data class VexPreviewContext(
    val runtime: VexRuntime,
    val repositories: AppRepositories,
    val darkTheme: Boolean,
)

internal object VexPreviewRepositories {
    fun empty(): AppRepositories =
        AppRepositories(
            logHistory = InMemoryLogHistoryRepository(),
            userProfile = InMemoryUserProfileRepository(),
        )

    fun withLogHistory(fixture: LogHistoryFixture): AppRepositories =
        empty().copy(
            logHistory = InMemoryLogHistoryRepository(fixture.records),
        )

    fun withUserProfile(fixture: UserProfileFixture): AppRepositories =
        empty().copy(
            userProfile = InMemoryUserProfileRepository(listOf(fixture.profile)),
        )
}

/**
 * Reusable screen host: real AppFrame + VexRuntime, fake only at repository inputs.
 */
@Composable
internal fun VexPreviewHost(
    scenario: VexPreviewScenario,
    content: @Composable (VexPreviewContext) -> Unit,
) {
    val darkTheme = isSystemInDarkTheme()
    val context = remember(scenario.id, darkTheme) {
        val repositories = scenario.repositoriesFactory()
        val runtime = VexRuntime(
            registry = AppScreens.registry,
            logger = VexLogger(),
            initialScreen = HomeScreen,
            primaryScreens = AppScreens.primary,
            currentAppActions = {
                AppActionRegistrations.Empty
            },
        )

        runtime.navigation.push(scenario.startScreen)

        VexPreviewContext(
            runtime = runtime,
            repositories = repositories,
            darkTheme = darkTheme,
        ).also { scenario.prepare(it) }
    }

    VexLifeTheme(darkTheme = context.darkTheme) {
        AppFrame(runtime = context.runtime) {
            content(context)
        }
    }
}

/**
 * Fast path for screens that are only a ScreenBlueprint + generic element tree.
 */
@Composable
internal fun VexBlueprintScreenPreview(screen: ScreenBlueprint) {
    VexPreviewHost(
        scenario = VexPreviewScenario(
            id = "screen-" + screen.screenId,
            startScreen = screen,
        ),
    ) { context ->
        VexElementTree(
            placement = screen.content,
            registry = context.runtime.registry,
            handler = context.runtime,
            modifier = Modifier.fillMaxSize(),
            selectedChild = context.runtime.presentation::selected,
        )
    }
}
