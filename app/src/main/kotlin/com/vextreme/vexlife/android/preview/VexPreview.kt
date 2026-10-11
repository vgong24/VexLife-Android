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
import com.vextreme.vexlife.android.runtime.AppActionRegistrations
import com.vextreme.vexlife.android.runtime.VexRuntime
import com.vextreme.vexlife.android.screens.AppScreens
import com.vextreme.vexlife.android.shell.AppFrame
import com.vextreme.vexlife.android.ui.VexLifeTheme

/**
 * Shared Android Studio preview ENVIRONMENT for Screen work.
 *
 * This file deliberately does not know feature Screens or their interesting states. A Screen's own
 * *Screen.kt file owns its previews and any local prepare step (selected Gallery panel, open Debug
 * dialog, seeded profile, etc.).
 *
 * Shared responsibilities here are only:
 * - device/theme pressure;
 * - in-memory repository factories;
 * - the real AppFrame / VexRuntime host;
 * - the fast path for ordinary blueprint-only Screens.
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

internal data class VexPreviewContext(
    val runtime: VexRuntime,
    val repositories: AppRepositories,
    val darkTheme: Boolean,
)

/**
 * Reusable fake DATA configurations. These know repository/domain fixtures, not Screen scenarios.
 */
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
 * Reusable Screen preview host.
 *
 * [startScreen], [repositoriesFactory], and [prepareRuntime] are supplied by the Screen file using
 * this host. [prepareRuntime] seeds the initial preview state directly; it is not a user interaction
 * and therefore should not be routed through logging/navigation actions merely to construct a preview.
 *
 * That keeps the reusable mechanism here while keeping the meaning of each preview beside the feature
 * it visualizes.
 */
@Composable
internal fun VexPreviewHost(
    id: String,
    startScreen: ScreenBlueprint,
    repositoriesFactory: () -> AppRepositories = VexPreviewRepositories::empty,
    prepareRuntime: VexPreviewContext.() -> Unit = {},
    content: @Composable (VexPreviewContext) -> Unit,
) {
    val darkTheme = isSystemInDarkTheme()
    val context = remember(id, darkTheme) {
        val repositories = repositoriesFactory()
        val runtime = VexRuntime(
            registry = AppScreens.registry,
            logger = VexLogger(),
            initialScreen = AppScreens.primary.first(),
            primaryScreens = AppScreens.primary,
            currentAppActions = {
                AppActionRegistrations.Empty
            },
        )

        runtime.navigation.push(startScreen)

        VexPreviewContext(
            runtime = runtime,
            repositories = repositories,
            darkTheme = darkTheme,
        ).also { prepareRuntime(it) }
    }

    VexLifeTheme(darkTheme = context.darkTheme) {
        AppFrame(runtime = context.runtime) {
            content(context)
        }
    }
}

/**
 * Fast path for an ordinary Screen whose visible body is exactly its ScreenBlueprint tree.
 *
 * Stateful Screens remain free to use [VexPreviewHost] directly in their own source file.
 */
@Composable
internal fun VexBlueprintScreenPreview(screen: ScreenBlueprint) {
    VexPreviewHost(
        id = "screen-" + screen.screenId,
        startScreen = screen,
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
