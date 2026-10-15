package com.vextreme.vexlife.android.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vextreme.vexlife.android.data.AppRepositories
import com.vextreme.vexlife.android.data.loghistory.LogHistoryPersistenceQueue
import com.vextreme.vexlife.android.elements.ButtonElement
import com.vextreme.vexlife.android.elements.ElementPlacement
import com.vextreme.vexlife.android.elements.ScreenRegistration
import com.vextreme.vexlife.android.elements.ScreenBlueprint
import com.vextreme.vexlife.android.elements.VexElementTree
import com.vextreme.vexlife.android.elements.VexModifier
import com.vextreme.vexlife.android.logging.VexLogger
import com.vextreme.vexlife.android.runtime.AppActionRegistrations
import com.vextreme.vexlife.android.runtime.VexRuntime
import com.vextreme.vexlife.android.ui.VexLifeRelays
import kotlinx.coroutines.CoroutineScope

/**
 * App-level construction dependencies available only while a Screen forms its action registrations.
 *
 * This is not a runtime service locator. A Screen should capture only the narrow dependency its
 * registrations need, then discard this construction context.
 */
internal class ScreenActionContext(
    val repositories: AppRepositories,
    val scope: CoroutineScope,
    val logger: VexLogger,
    val logPersistence: LogHistoryPersistenceQueue,
    val toggleDarkMode: () -> Unit,
)

/**
 * VexLife app Screen contract layered over the portable element-module [ScreenBlueprint].
 *
 * A new app Screen is registered once in [AppScreens]. [ScreenBlueprint.content] is its static
 * semantic registration tree. Ordinary Screens render that tree directly; a custom/native Screen
 * may override [renderContent] while preserving the registered identities, actions, and ownership
 * expressed by that tree. Feature-owned Invoke effects may override [appActionRegistrations].
 * Screens with no app effects inherit an empty registration set.
 *
 * [AppScreens] assembles every registered Screen automatically and fails session construction when a
 * declared Invoke action has no owner. Preview presence remains a source-alignment/build contract
 * because Kotlin inheritance cannot require a top-level Android Studio @Preview composable.
 */
abstract class AppScreenBlueprint(
    screenKey: String,
    @StringRes titleRes: Int,
) : ScreenBlueprint(
    screenKey = screenKey,
    titleRes = titleRes,
) {
    internal open fun appActionRegistrations(
        context: ScreenActionContext,
    ): AppActionRegistrations =
        AppActionRegistrations.Empty

    /**
     * Optional registered actions realized by the shared app toolbar while this Screen is current.
     *
     * The Screen still owns each element/intent. AppFrame only renders the supplied registered
     * controls so feature chrome does not become a second feature-specific toolbar implementation.
     */
    internal open val toolbarActions: List<ButtonElement>
        get() = emptyList()

    /**
     * One app-level registration for this Screen.
     *
     * [content] is the Screen's static semantic registration tree. For blueprint-rendered Screens it
     * is also the visible body. For custom renderers it remains the registry/action-coverage source
     * while [renderContent] owns the actual visual composition. Shared toolbar actions are registered
     * as Screen-owned semantic elements without being duplicated into that tree.
     */
    internal fun appRegistration(): ScreenRegistration =
        ScreenRegistration(
            screen = this,
            root = content,
            additionalPlacements =
                toolbarActions.map { action ->
                    ElementPlacement(action)
                },
        )

    /**
     * Android realization bridge for this registered Screen.
     *
     * The app router supplies broad composition dependencies once. This Screen immediately narrows
     * them to whatever its actual renderer needs. Ordinary blueprint-only Screens inherit the
     * generic VexElementTree path; a Screen overrides this only when it owns custom realization,
     * local state projection, or a repository/theme dependency.
     *
     * A custom renderer does not turn [content] into a second hidden layout. [content] continues to
     * own static semantic registration while this method owns the pixels and native composition.
     *
     * Broad app-composition bridge.
     *
     * Existing Screens keep their narrow three-argument renderer. A Screen that consumes a shared
     * session Relay can override this overload and immediately narrow [relays] into its own content.
     */
    @Composable
    internal open fun renderContent(
        runtime: VexRuntime,
        repositories: AppRepositories,
        relays: VexLifeRelays,
        darkTheme: Boolean,
    ) {
        renderContent(
            runtime = runtime,
            repositories = repositories,
            darkTheme = darkTheme,
        )
    }

    @Composable
    internal open fun renderContent(
        runtime: VexRuntime,
        repositories: AppRepositories,
        darkTheme: Boolean,
    ) {
        VexElementTree(
            placement = content,
            registry = runtime.registry,
            handler = runtime,
            modifier = VexModifier.fillMaxSize(),
            selectedChild = runtime.presentation::selected,
        )
    }
}
