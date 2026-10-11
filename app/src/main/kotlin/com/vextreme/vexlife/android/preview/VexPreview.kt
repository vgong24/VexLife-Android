package com.vextreme.vexlife.android.preview

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.vextreme.vexlife.android.elements.PageBlueprint
import com.vextreme.vexlife.android.elements.VexElementTree
import com.vextreme.vexlife.android.logging.VexLogger
import com.vextreme.vexlife.android.pages.AppPages
import com.vextreme.vexlife.android.pages.primary.home.HomePage
import com.vextreme.vexlife.android.pages.shared.debug.DebugMenuPage
import com.vextreme.vexlife.android.runtime.VexRuntime
import com.vextreme.vexlife.android.shell.AppFrame
import com.vextreme.vexlife.android.shell.AppShell
import com.vextreme.vexlife.android.ui.VexLifeTheme
import java.time.Instant
import java.time.ZoneId

/**
 * Shared preview forms. Page preview functions intentionally stay in the SAME FILE as the page they
 * preview so a worker sees the visual entry point while editing the page.
 *
 * This annotation supplies a few cheap edge conditions automatically:
 * - normal phone / dark;
 * - normal phone / light;
 * - compact phone + large text for wrapping/clipping pressure.
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
internal annotation class VexPagePreviews

/**
 * Preview-only setup description.
 *
 * This support lives in main source only because Kotlin/Compose preview functions intentionally
 * live beside their production PageBlueprint files and therefore cannot depend on a debug-only
 * source set. Everything here is internal and unused by the production entrypoint; it is tooling
 * support, not a product-state owner.
 *
 * Production page structure does not depend on a preview model. A scenario only seeds state around
 * the real page/runtime so Android Studio can exercise a useful state without live services.
 */
internal data class VexPreviewScenario(
    val id: String,
    val startPage: PageBlueprint,
    val loggerFactory: () -> VexLogger = { VexLogger() },
    val prepare: VexPreviewContext.() -> Unit = {},
)

internal data class VexPreviewContext(
    val runtime: VexRuntime,
    val logger: VexLogger,
    val darkTheme: Boolean,
)

/**
 * Reusable preview host using the real AppFrame, registry and runtime.
 *
 * Mocking stops at initial facts. Rendering/navigation are the same production paths used on-device.
 * As Relay/service state becomes real, prefer preview fixtures exposed by those owners rather than
 * duplicating their state machines here.
 */
@Composable
internal fun VexPreviewHost(
    scenario: VexPreviewScenario,
    content: @Composable (VexPreviewContext) -> Unit,
) {
    val darkTheme = isSystemInDarkTheme()

    val context = remember(scenario.id, darkTheme) {
        val logger = scenario.loggerFactory()
        val runtime = VexRuntime(
            registry = AppPages.registry,
            logger = logger,
            initialScreen = HomePage,
            primaryScreens = AppPages.primary,
            onAppAction = { _, _ -> },
        )

        runtime.navigation.push(scenario.startPage)

        VexPreviewContext(
            runtime = runtime,
            logger = logger,
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
 * Fast preview path for ordinary blueprint-only pages.
 */
@Composable
internal fun VexBlueprintPagePreview(
    page: PageBlueprint,
) {
    VexPreviewHost(
        scenario = VexPreviewScenario(
            id = "page-" + page.screenId,
            startPage = page,
        ),
    ) { context ->
        VexElementTree(
            placement = page.content,
            registry = context.runtime.registry,
            handler = context.runtime,
            modifier = Modifier.fillMaxSize(),
            selectedChild = context.runtime.presentation::selected,
        )
    }
}

/**
 * Reusable deterministic preview facts.
 *
 * Keep these small and representative. The preview layer should not become a shadow implementation
 * of product/service state.
 */
internal object VexPreviewMocks {
    fun loggerWithTypicalHistory(): VexLogger {
        val times = listOf(
            Instant.parse("2026-10-11T01:51:00Z"),
            Instant.parse("2026-10-11T01:53:00Z"),
            Instant.parse("2026-10-11T01:54:00Z"),
            Instant.parse("2026-10-11T01:54:30Z"),
        )
        var index = 0

        val logger = VexLogger(
            clock = {
                times[index.coerceAtMost(times.lastIndex)].also {
                    index = (index + 1).coerceAtMost(times.lastIndex)
                }
            },
            zoneId = { ZoneId.of("UTC") },
        )

        logger.appOpened(HomePage)
        logger.recordInteraction(
            element = AppShell.Elements.DebugMenu,
            screenBefore = HomePage,
            screenAfter = DebugMenuPage,
        )
        logger.recordInteraction(
            element = DebugMenuPage.ResetLog,
            screenBefore = DebugMenuPage,
        )
        logger.recordInteraction(
            element = DebugMenuPage.ResetLog,
            screenBefore = DebugMenuPage,
        )

        return logger
    }
}
