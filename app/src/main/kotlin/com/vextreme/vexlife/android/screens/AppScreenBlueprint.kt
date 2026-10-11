package com.vextreme.vexlife.android.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vextreme.vexlife.android.data.AppRepositories
import com.vextreme.vexlife.android.data.loghistory.LogHistoryPersistenceQueue
import com.vextreme.vexlife.android.elements.ScreenBlueprint
import com.vextreme.vexlife.android.elements.VexElementTree
import com.vextreme.vexlife.android.logging.VexLogger
import com.vextreme.vexlife.android.runtime.AppActionRegistrations
import com.vextreme.vexlife.android.runtime.VexRuntime
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
 * A new app Screen is registered once in [AppScreens]. Its static shape still comes from
 * ScreenBlueprint; feature-owned Invoke effects may override [appActionRegistrations]. Screens with
 * no app effects inherit an empty registration set.
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
     * Android realization bridge for this registered Screen.
     *
     * The app router supplies broad composition dependencies once. This Screen immediately narrows
     * them to whatever its actual renderer needs. Ordinary blueprint-only Screens inherit the
     * generic VexElementTree path; a Screen overrides this only when it owns custom realization,
     * local state projection, or a repository/theme dependency.
     */
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
            modifier = Modifier.fillMaxSize(),
            selectedChild = runtime.presentation::selected,
        )
    }
}
