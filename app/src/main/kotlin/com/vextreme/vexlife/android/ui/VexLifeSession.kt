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
import com.vextreme.vexlife.android.data.vexstream.VexStreamHubState
import com.vextreme.vexlife.android.data.vexstream.VexStreamLocalCopyState
import com.vextreme.vexlife.android.data.vexstream.VexStreamMusicRepository
import com.vextreme.vexlife.android.execution.VexExecution
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import com.vextreme.vexlife.android.logging.VexLogger
import com.vextreme.vexlife.android.runtime.AppActionRegistrations
import com.vextreme.vexlife.android.runtime.VexRuntime
import com.vextreme.vexlife.android.screens.AppScreens
import com.vextreme.vexlife.android.screens.ScreenActionContext
import com.vextreme.vexlife.android.screens.primary.home.HomeScreen
import com.vextreme.vexlife.android.vexstream.playback.convergeVexStreamCurrentPlaybackArtwork
import com.vextreme.vexlife.android.vexstream.playback.shouldRefreshWidgetProjection
import com.vextreme.vexlife.android.vexstream.playback.vexStreamCurrentPlaybackArtworkTriggers
import com.vextreme.vexlife.android.widget.vexstream.refreshVexStreamPlaybackWidgetsFromCurrentPlayback
import com.vextreme.vexlife.android.widget.vexstream.updateVexStreamPlaybackWidgets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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
    private val executionRoot =
        VexExecutionRoot(
            dispatcher = Dispatchers.IO,
        )
    private val repositories =
        AppRepositoryFactory.production(
            application,
        )
    private val vexStreamStartupExecution =
        VexStreamStartupExecution(
            executionRoot = executionRoot,
            repository = repositories.vexStream,
        ).also {
            it.start()
        }
    private val vexStreamArtworkExecution =
        VexStreamArtworkExecution(
            executionRoot = executionRoot,
            repository = repositories.vexStream,
            context = application.applicationContext,
        ).also {
            it.start()
        }

    val session =
        VexLifeSession(
            repositories = repositories,
            scope = viewModelScope,
        )

    override fun onCleared() {
        executionRoot.forceStopAll(
            "VexLife ViewModel cleared",
        )
        super.onCleared()
    }
}

private class VexStreamStartupExecution(
    executionRoot: VexExecutionRoot,
    private val repository: VexStreamMusicRepository,
) : VexExecution(
        executionRoot,
    ) {
    fun start() {
        launchOnce(
            repository::restorePersistedState,
        )
    }
}

/**
 * Session-owned background custody for ancillary downloaded artwork.
 *
 * The repository owns which current downloaded tracks need artwork and how those bytes are stored.
 * This Execution only gives that finite network/filesystem work an explicit non-UI lifetime whenever
 * current Hub/catalog truth becomes available.
 */
private class VexStreamArtworkExecution(
    executionRoot: VexExecutionRoot,
    private val repository: VexStreamMusicRepository,
    private val context: android.content.Context,
) : VexExecution(
        executionRoot,
    ) {
    private val convergenceMutex =
        Mutex()

    fun start() {
        launchForCollect(
            repository::state,
        ) { state ->
            if (
                state.hubState is
                VexStreamHubState.Ready
            ) {
                if (
                    state.tracks.any {
                        it.localCopy is
                            VexStreamLocalCopyState.Downloaded
                    }
                ) {
                    repository
                        .backfillOfflineArtwork()
                    updateVexStreamPlaybackWidgets(
                        context,
                    )
                }

                convergeCurrentArtworkAndRefreshWidget()
            }
        }

        launchForCollect(
            source =
                ::vexStreamCurrentPlaybackArtworkTriggers,
            argument =
                context,
        ) {
            if (
                repository.state.value.hubState is
                VexStreamHubState.Ready
            ) {
                convergeCurrentArtworkAndRefreshWidget()
            }
        }
    }

    private suspend fun convergeCurrentArtworkAndRefreshWidget() {
        convergenceMutex.withLock {
            val convergence =
                convergeVexStreamCurrentPlaybackArtwork(
                    context =
                        context,
                    repository =
                        repository,
                )

            if (
                convergence
                    .shouldRefreshWidgetProjection
            ) {
                refreshVexStreamPlaybackWidgetsFromCurrentPlayback(
                    context,
                )
            }
        }
    }
}
