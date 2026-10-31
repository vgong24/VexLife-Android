package com.vextreme.vexlife.android.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vextreme.vexlife.android.VexApplication
import com.vextreme.vexlife.android.VexApplicationDependencyModule
import com.vextreme.vexlife.android.data.AppRepositories
import com.vextreme.vexlife.android.dependency.VexDependencyBuildScope
import com.vextreme.vexlife.android.dependency.VexDependencyFactory
import com.vextreme.vexlife.android.dependency.VexDependencyModule
import com.vextreme.vexlife.android.dependency.VexDependencyProvider
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
import com.vextreme.vexlife.android.widget.vexstream.updateVexStreamPlaybackWidgets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/**
 * One in-process VexLife app session.
 *
 * This object owns state that should survive Android configuration recreation together:
 * - current Screen/navigation/presentation;
 * - current logger session;
 * - diagnostic persistence queue;
 * - current ThemeMode Family;
 * - immutable app-action wiring.
 *
 * The session itself remains process-local. Feature-owned durable truth may restore into a fresh
 * session through its rightful Store; ThemeMode now does exactly that. Production keeps this session
 * inside [VexLifeViewModel]; Preview/tests may construct one without durable feature Stores.
 */
internal class VexLifeSession(
    val repositories: AppRepositories,
    val scope: CoroutineScope,
    executionRoot: VexExecutionRoot,
    themeModeStore: ThemeModeStore? = null,
    initialThemeMode: ThemeMode = ThemeMode.Dark,
) {
    val themeModeFamily =
        ThemeModeFamily(
            initialState = initialThemeMode,
        )

    private val themeModeConnection =
        ThemeModeConnection(
            family = themeModeFamily,
            executionRoot = executionRoot,
            store = themeModeStore,
        )

    val logPersistence = LogHistoryPersistenceQueue(
        repository = repositories.logHistory,
        scope = scope,
    )

    val logger = VexLogger(
        onRecord = logPersistence::record,
        blueprintRegistry = AppScreens.registry,
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
        val selectedMode =
            when (
                themeModeFamily
                    .currentState
            ) {
                ThemeMode.NotSet,
                ThemeMode.Light ->
                    ThemeMode.Dark

                ThemeMode.Dark ->
                    ThemeMode.Light
            }

        themeModeConnection.activate(
            ThemeModeFamily.Control.Select(
                selectedMode,
            ),
        )
    }
}

/**
 * ViewModel/session-lifetime dependency declarations.
 *
 * This child graph may consume application providers but owns its own memoized runtime/session
 * instances. The session provider is intentionally the current migration boundary: Logger/Family
 * internals will move into explicit providers as their reusable construction relationships are
 * refined rather than being abstracted speculatively.
 */
internal class VexLifeSessionDependencyModule(
    applicationDependencies:
        VexApplicationDependencyModule,
    private val scope:
        CoroutineScope,
    private val applicationContext:
        android.content.Context,
) {
    val executionRoot =
        ExecutionRootProvider()

    val session =
        SessionProvider(
            repositories =
                applicationDependencies.repositories,
            themeModeStore =
                applicationDependencies.themeModeStore,
            executionRoot =
                executionRoot,
            scope =
                scope,
        )

    val vexStreamStartup =
        VexStreamStartupProvider(
            repositories =
                applicationDependencies.repositories,
            executionRoot =
                executionRoot,
        )

    val vexStreamArtwork =
        VexStreamArtworkProvider(
            repositories =
                applicationDependencies.repositories,
            executionRoot =
                executionRoot,
            applicationContext =
                applicationContext,
        )

    val module:
        VexDependencyModule =
        VexDependencyModule.of(
            executionRoot,
            session,
            vexStreamStartup,
            vexStreamArtwork,
        )

    internal class ExecutionRootProvider :
        VexDependencyProvider<VexExecutionRoot>() {
        override fun create(
            dependencies:
                VexDependencyBuildScope,
        ):
            VexExecutionRoot =
            VexExecutionRoot(
                dispatcher =
                    Dispatchers.IO,
            )
    }

    internal class SessionProvider(
        private val repositories:
            VexDependencyProvider<AppRepositories>,
        private val themeModeStore:
            VexDependencyProvider<ThemeModeStore>,
        private val executionRoot:
            VexDependencyProvider<VexExecutionRoot>,
        private val scope:
            CoroutineScope,
    ) :
        VexDependencyProvider<VexLifeSession>(
            dependencies =
                listOf(
                    repositories,
                    themeModeStore,
                    executionRoot,
                ),
        ) {
        override fun create(
            dependencies:
                VexDependencyBuildScope,
        ):
            VexLifeSession =
            VexLifeSession(
                repositories =
                    dependencies.require(
                        repositories,
                    ),
                scope =
                    scope,
                executionRoot =
                    dependencies.require(
                        executionRoot,
                    ),
                themeModeStore =
                    dependencies.require(
                        themeModeStore,
                    ),
            )
    }

    internal class VexStreamStartupProvider(
        private val repositories:
            VexDependencyProvider<AppRepositories>,
        private val executionRoot:
            VexDependencyProvider<VexExecutionRoot>,
    ) :
        VexDependencyProvider<VexStreamStartupExecution>(
            dependencies =
                listOf(
                    repositories,
                    executionRoot,
                ),
        ) {
        override fun create(
            dependencies:
                VexDependencyBuildScope,
        ):
            VexStreamStartupExecution =
            VexStreamStartupExecution(
                executionRoot =
                    dependencies.require(
                        executionRoot,
                    ),
                repository =
                    dependencies.require(
                        repositories,
                    )
                        .vexStream,
            )
                .also {
                    it.start()
                }
    }

    internal class VexStreamArtworkProvider(
        private val repositories:
            VexDependencyProvider<AppRepositories>,
        private val executionRoot:
            VexDependencyProvider<VexExecutionRoot>,
        private val applicationContext:
            android.content.Context,
    ) :
        VexDependencyProvider<VexStreamArtworkExecution>(
            dependencies =
                listOf(
                    repositories,
                    executionRoot,
                ),
        ) {
        override fun create(
            dependencies:
                VexDependencyBuildScope,
        ):
            VexStreamArtworkExecution =
            VexStreamArtworkExecution(
                executionRoot =
                    dependencies.require(
                        executionRoot,
                    ),
                repository =
                    dependencies.require(
                        repositories,
                    )
                        .vexStream,
                context =
                    applicationContext,
            )
                .also {
                    it.start()
                }
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
    private val vexApplication =
        application as?
            VexApplication
            ?: error(
                "VexLifeViewModel requires VexApplication as the Android process composition root",
            )

    private val dependencyModule =
        VexLifeSessionDependencyModule(
            applicationDependencies =
                vexApplication.dependencyModule,
            scope =
                viewModelScope,
            applicationContext =
                application.applicationContext,
        )

    private val dependencies:
        VexDependencyFactory =
        vexApplication.dependencies
            .child(
                dependencyModule.module,
            )

    private val executionRoot =
        dependencies.resolve(
            dependencyModule.executionRoot,
        )

    /*
     * Preserve the current startup behavior explicitly. The dependency factory is lazy; resolving
     * these roots is the readable statement that this session currently chooses to start them.
     */
    private val vexStreamStartupExecution =
        dependencies.resolve(
            dependencyModule.vexStreamStartup,
        )

    private val vexStreamArtworkExecution =
        dependencies.resolve(
            dependencyModule.vexStreamArtwork,
        )

    val session =
        dependencies.resolve(
            dependencyModule.session,
        )

    override fun onCleared() {
        executionRoot.forceStopAll(
            "VexLife ViewModel cleared",
        )
        super.onCleared()
    }
}

internal class VexStreamStartupExecution(
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
internal class VexStreamArtworkExecution(
    executionRoot: VexExecutionRoot,
    private val repository: VexStreamMusicRepository,
    private val context: android.content.Context,
) : VexExecution(
        executionRoot,
    ) {
    fun start() {
        launchForCollect(
            repository::state,
        ) { state ->
            if (
                state.hubState is
                VexStreamHubState.Ready &&
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
        }
    }
}
