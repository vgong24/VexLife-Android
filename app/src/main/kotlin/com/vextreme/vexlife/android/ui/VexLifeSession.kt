package com.vextreme.vexlife.android.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vextreme.vexlife.android.data.AppRepositories
import com.vextreme.vexlife.android.data.AppRepositoryFactory
import com.vextreme.vexlife.android.data.loghistory.LogHistoryPersistenceQueue
import com.vextreme.vexlife.android.logging.VexLogger
import com.vextreme.vexlife.android.runtime.AppActionRegistrations
import com.vextreme.vexlife.android.runtime.VexRuntime
import com.vextreme.vexlife.android.screens.AppScreens
import com.vextreme.vexlife.android.screens.ScreenActionContext
import com.vextreme.vexlife.android.screens.primary.home.HomeScreen
import kotlinx.coroutines.CoroutineScope

/**
 * One in-process VexLife app session.
 *
 * This object owns state that should survive Android configuration recreation together:
 * - current Screen/navigation/presentation;
 * - current logger session;
 * - diagnostic persistence queue;
 * - current dark/light selection;
 * - immutable app-action wiring.
 *
 * It does NOT claim process-death restoration. Production keeps this session inside
 * [VexLifeViewModel]; Preview/tests may construct one with their own repositories/scope.
 */
internal class VexLifeSession(
    val repositories: AppRepositories,
    val scope: CoroutineScope,
    initialDarkTheme: Boolean = true,
) {
    var darkTheme by mutableStateOf(initialDarkTheme)
        private set

    val logPersistence = LogHistoryPersistenceQueue(
        repository = repositories.logHistory,
        scope = scope,
    )

    val logger = VexLogger(
        onRecord = logPersistence::record,
    )

    /**
     * Reusable semantic Relays live with the app session rather than any one Compose destination.
     * Their dependency collectors are children of the same supplied session scope.
     */
    val relays =
        VexLifeRelays.create(
            repositories = repositories,
            scope = scope,
        )

    val appActions: AppActionRegistrations =
        AppScreens.appActionRegistrations(
            ScreenActionContext(
                repositories = repositories,
                scope = scope,
                logger = logger,
                logPersistence = logPersistence,
                toggleDarkMode = ::toggleDarkMode,
            ),
        )

    val runtime = VexRuntime(
        registry = AppScreens.registry,
        logger = logger,
        initialScreen = HomeScreen,
        primaryScreens = AppScreens.primary,
        currentAppActions = { appActions },
    )

    private fun toggleDarkMode() {
        darkTheme = !darkTheme
    }
}

/**
 * Android configuration-lifetime owner for [VexLifeSession].
 *
 * Activity rotation/recreation receives the same ViewModel instance, so the active Screen, open
 * modal, Gallery selection and logger session do not restart merely because Android rebuilt the UI.
 */
internal class VexLifeViewModel(
    application: Application,
) : AndroidViewModel(application) {
    val session = VexLifeSession(
        repositories = AppRepositoryFactory.production(application),
        scope = viewModelScope,
    )
}
