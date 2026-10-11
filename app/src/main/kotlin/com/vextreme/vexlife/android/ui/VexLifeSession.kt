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
import com.vextreme.vexlife.android.screens.primary.home.HomeScreen
import com.vextreme.vexlife.android.screens.shared.debug.debugMenuActionRegistrations
import com.vextreme.vexlife.android.screens.shared.gallery.galleryActionRegistrations
import com.vextreme.vexlife.android.screens.shared.profile.userProfileActionRegistrations
import com.vextreme.vexlife.android.screens.shared.vex.talkToVexActionRegistrations
import com.vextreme.vexlife.android.screens.shared.vexstream.vexStreamMusicActionRegistrations
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
    scope: CoroutineScope,
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

    val appActions: AppActionRegistrations =
        AppActionRegistrations.combine(
            debugMenuActionRegistrations(
                toggleDarkMode = ::toggleDarkMode,
                logger = logger,
                logPersistence = logPersistence,
            ),
            galleryActionRegistrations(),
            userProfileActionRegistrations(
                repository = repositories.userProfile,
                scope = scope,
            ),
            vexStreamMusicActionRegistrations(),
            talkToVexActionRegistrations(),
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
